package com.ltx.audiopeel

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 音频提取格式
 */
enum class OutputFormat(val extension: String) {
    MP3("mp3"), M4A("m4a"), WAV("wav"), FLAC("flac"), OGG("ogg")
}

/**
 * 音频提取状态
 */
sealed class ExtractionState {
    data object Idle : ExtractionState()
    data class Processing(val progress: Float) : ExtractionState()
    data object Cancelled : ExtractionState()
    data class Success(val outPath: String) : ExtractionState()
    data class Error(val message: String? = null, val messageResId: Int? = null) : ExtractionState()
}

/**
 * 主视图模型
 */
class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val state: StateFlow<ExtractionState> = _state.asStateFlow()
    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()
    private val _selectedFormat = MutableStateFlow(OutputFormat.M4A)
    val selectedFormat: StateFlow<OutputFormat> = _selectedFormat.asStateFlow()
    private var cachedSourceCodec: String? = null
    private var lastProgressUpdate = 0L
    @Volatile
    private var currentSessionId: Long? = null
    @Volatile
    private var cancelRequested = false

    /**
     * 探测视频内嵌音频编码
     *
     * @return Pair(源编码名, 推荐输出格式)
     */
    private fun detectAudio(context: Context, uri: Uri): Pair<String, OutputFormat>? {
        // 创建媒体提取器
        val extractor = MediaExtractor()
        try {
            // 设置数据源
            extractor.setDataSource(context, uri, null)
            // 遍历所有轨道
            for (i in 0 until extractor.trackCount) {
                // 获取轨道格式
                val mime = extractor.getTrackFormat(i)
                    .getString(MediaFormat.KEY_MIME) ?: continue
                // 判断是否为音轨
                if (!mime.startsWith("audio/")) continue
                // 按完整 MIME 映射为内部编码名
                val codec = when (mime) {
                    "audio/mp4a-latm" -> "aac"
                    "audio/mpeg" -> "mp3"
                    "audio/flac" -> "flac"
                    "audio/vorbis" -> "vorbis"
                    "audio/opus" -> "opus"
                    "audio/raw" -> "pcm"
                    else -> mime.removePrefix("audio/")
                }
                // 根据编码名推荐输出格式
                val recommended = when (codec) {
                    "aac" -> OutputFormat.M4A
                    "mp3" -> OutputFormat.MP3
                    "flac" -> OutputFormat.FLAC
                    "vorbis", "opus" -> OutputFormat.OGG
                    else -> OutputFormat.M4A
                }
                return codec to recommended
            }
        } catch (e: Exception) {
            Log.w("AudioPeel", "检测音频编码失败: ${e.message}", e)
        } finally {
            runCatching { extractor.release() }
                .onFailure { Log.w("AudioPeel", "释放 MediaExtractor 失败", it) }
        }
        return null
    }

    /**
     * 选择视频文件
     *
     * @param context 上下文
     * @param uri 视频URI
     */
    fun selectVideo(context: Context, uri: Uri?) {
        // 获取全局Application上下文
        val appContext = context.applicationContext
        // 更新当前选中的视频URI
        _selectedUri.value = uri
        // 更新状态为空闲
        _state.value = ExtractionState.Idle
        // 如果视频URI为空
        if (uri == null) {
            // 清空缓存的源编码
            cachedSourceCodec = null
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 获取Pair(源编码名, 推荐输出格式)
                val detected = detectAudio(appContext, uri)
                // 更新缓存的源编码
                cachedSourceCodec = detected?.first
                // 如果检测到音频编码
                if (detected != null) {
                    // 更新当前选定的输出格式
                    _selectedFormat.value = detected.second
                }
            }
        }
    }

    /**
     * 选择输出格式
     *
     * @param format 输出格式
     */
    fun selectFormat(format: OutputFormat) {
        _selectedFormat.value = format
    }

    /**
     * 提取音频
     *
     * @param context 上下文
     */
    fun extractAudio(context: Context) {
        // 获取全局Application上下文
        val appContext = context.applicationContext
        val uri = _selectedUri.value ?: return
        if (_state.value is ExtractionState.Processing) return
        // 获取当前选定的输出格式
        val format = _selectedFormat.value
        cancelRequested = false
        lastProgressUpdate = 0L
        currentSessionId = null
        _state.value = ExtractionState.Processing(0f)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // 构建输出路径
                    val outDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        ?: appContext.cacheDir
                    // 清理之前提取的音频文件
                    outDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
                    // 生成唯一文件名
                    val timeStamp = SimpleDateFormat(
                        "yyyyMMdd_HHmmss", Locale.getDefault()
                    ).format(Date())
                    val fileName = "audio_${timeStamp}.${format.extension}"
                    val outFile = File(outDir, fileName)
                    val outPath = outFile.absolutePath
                    // 使用Android原生API获取媒体信息
                    var totalDurationMs = 0L
                    // 获取时长
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(appContext, uri)
                        totalDurationMs = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0L
                    } catch (e: Exception) {
                        Log.w("AudioPeel", "获取时长失败: ${e.message}", e)
                    } finally {
                        runCatching { retriever.release() }
                            .onFailure { Log.w("AudioPeel", "释放 MediaMetadataRetriever 失败", it) }
                    }
                    // 如果缓存的源编码为空
                    if (cachedSourceCodec == null) {
                        // 探测音频编码并更新缓存的源编码
                        cachedSourceCodec = detectAudio(appContext, uri)?.first
                    }
                    // 获取缓存的源编码
                    val sourceAudioCodec = cachedSourceCodec.orEmpty()
                    // 将 content URI 转为 FFmpeg 可读的 SAF 输入参数
                    val safInput = FFmpegKitConfig.getSafParameterForRead(appContext, uri)
                    // 限制线程数在1-4之间
                    val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
                    // 构建ffmpeg命令
                    val base = "-y -nostdin -i $safInput -vn -threads $threads"
                    // 源编码与目标格式兼容则复制音轨否则按所选格式转码
                    val cmd = when (format) {
                        OutputFormat.MP3 -> {
                            if (sourceAudioCodec == "mp3") {
                                "$base -c:a copy \"$outPath\""
                            } else {
                                "$base -c:a libmp3lame -q:a 2 \"$outPath\""
                            }
                        }
                        OutputFormat.M4A -> {
                            if (sourceAudioCodec == "aac") {
                                "$base -c:a copy -f mp4 -movflags +faststart \"$outPath\""
                            } else {
                                "$base -c:a aac -b:a 192k -f mp4 -movflags +faststart \"$outPath\""
                            }
                        }
                        OutputFormat.WAV -> "$base -c:a pcm_s16le -ar 44100 \"$outPath\""
                        OutputFormat.FLAC -> {
                            if (sourceAudioCodec == "flac") {
                                "$base -c:a copy \"$outPath\""
                            } else {
                                "$base -c:a flac -compression_level 5 \"$outPath\""
                            }
                        }
                        OutputFormat.OGG -> {
                            when (sourceAudioCodec) {
                                "vorbis", "opus" ->
                                    "$base -c:a copy -f ogg \"$outPath\""
                                else ->
                                    "$base -c:a libvorbis -q:a 5 \"$outPath\""
                            }
                        }
                    }
                    // 执行异步提取
                    Log.d("AudioPeel", "FFmpeg命令: $cmd")
                    val session = FFmpegKit.executeAsync(cmd, { currentSession ->
                        // 完成回调
                        val returnCode = currentSession.returnCode
                        val cancelled = cancelRequested || ReturnCode.isCancel(returnCode)
                        currentSessionId = null
                        cancelRequested = false
                        if (cancelled) {
                            if (outFile.exists()) {
                                outFile.delete()
                            }
                            _state.value = ExtractionState.Cancelled
                        } else if (ReturnCode.isSuccess(returnCode)) {
                            _state.value = ExtractionState.Success(outPath)
                        } else {
                            val logs = currentSession.allLogsAsString
                            Log.e("AudioPeel", "FFmpeg Error $returnCode: $logs")
                            if (outFile.exists()) {
                                // 删除生成的不完整缓存文件
                                outFile.delete()
                            }
                            _state.value =
                                ExtractionState.Error(messageResId = R.string.extraction_failed)
                        }
                    }, { _ -> }, { statistics ->
                        // 进度回调
                        if (totalDurationMs > 0) {
                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate >= 200) {
                                lastProgressUpdate = now
                                val timeInMilliseconds = statistics.time
                                if (timeInMilliseconds > 0) {
                                    val progress =
                                        (timeInMilliseconds.toFloat() / totalDurationMs.toFloat()).coerceIn(
                                            0f, 1f
                                        )
                                    _state.value = ExtractionState.Processing(progress)
                                }
                            }
                        }
                    })
                    currentSessionId = session.sessionId
                } catch (e: Exception) {
                    currentSessionId = null
                    if (cancelRequested) {
                        cancelRequested = false
                        _state.value = ExtractionState.Cancelled
                    } else {
                        _state.value = ExtractionState.Error(
                            message = e.message,
                            messageResId = if (e.message == null) R.string.unknown_error else null
                        )
                    }
                }
            }
        }
    }

    /* 取消提取 */
    fun cancelExtraction() {
        if (_state.value !is ExtractionState.Processing) return
        cancelRequested = true
        val sessionId = currentSessionId
        if (sessionId != null) {
            FFmpegKit.cancel(sessionId)
        } else {
            FFmpegKit.cancel()
        }
    }

    /* ViewModel销毁时取消提取 */
    override fun onCleared() {
        super.onCleared()
        cancelExtraction()
    }
}
