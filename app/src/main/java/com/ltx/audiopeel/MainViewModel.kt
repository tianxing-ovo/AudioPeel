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
    private var lastProgressUpdate = 0L
    @Volatile
    private var currentSessionId: Long? = null
    @Volatile
    private var cancelRequested = false

    /**
     * 选择视频文件
     *
     * @param context 上下文
     * @param uri 视频URI
     */
    fun selectVideo(context: Context, uri: Uri?) {
        // 获取全局Application上下文
        val appContext = context.applicationContext
        _selectedUri.value = uri
        _state.value = ExtractionState.Idle
        if (uri == null) return
        // 检测音频编码并自动选择最佳输出格式
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(appContext, uri, null)
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            _selectedFormat.value = when {
                                mime.contains("mp4a") -> OutputFormat.M4A
                                mime == "audio/mpeg" -> OutputFormat.MP3
                                mime.contains("flac") -> OutputFormat.FLAC
                                mime.contains("vorbis") -> OutputFormat.OGG
                                mime.contains("opus") -> OutputFormat.OGG
                                else -> OutputFormat.M4A
                            }
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AudioPeel", "检测音频编码失败: ${e.message}")
                } finally {
                    try {
                        // 确保释放Native资源
                        extractor.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                    var sourceAudioCodec = ""
                    // 获取时长
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(appContext, uri)
                        totalDurationMs = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0L
                    } catch (e: Exception) {
                        Log.e("AudioPeel", "获取时长失败: ${e.message}")
                    } finally {
                        try {
                            // 确保释放Native资源
                            retriever.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    // 获取音频编码格式
                    val extractor = MediaExtractor()
                    try {
                        extractor.setDataSource(appContext, uri, null)
                        for (i in 0 until extractor.trackCount) {
                            val trackFormat = extractor.getTrackFormat(i)
                            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                            if (mime.startsWith("audio/")) {
                                sourceAudioCodec = when {
                                    mime.contains("mp4a") -> "aac"
                                    mime == "audio/mpeg" -> "mp3"
                                    mime.contains("vorbis") -> "vorbis"
                                    mime.contains("opus") -> "opus"
                                    mime.contains("flac") -> "flac"
                                    mime.contains("raw") -> "pcm"
                                    else -> mime.substringAfter("audio/")
                                }
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AudioPeel", "获取音频编码失败: ${e.message}")
                    } finally {
                        try {
                            // 确保释放Native资源
                            extractor.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    // 为FFmpeg创建独立的SAF
                    val safInput = FFmpegKitConfig.getSafParameterForRead(appContext, uri)
                    // 限制线程数在1-4之间
                    val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
                    // 构建ffmpeg命令
                    val base = "-y -nostdin -i $safInput -vn -threads $threads"
                    val cmd = when (format) {
                        OutputFormat.MP3 -> {
                            // 如果源音频已是MP3
                            if (sourceAudioCodec == "mp3") {
                                // 直接复制音频流
                                "$base -c:a copy \"$outPath\""
                            } else {
                                "$base -c:a libmp3lame -q:a 2 \"$outPath\""
                            }
                        }
                        OutputFormat.M4A -> {
                            // 如果源音频已是AAC
                            if (sourceAudioCodec == "aac") {
                                // 直接复制音频流
                                "$base -c:a copy -f mp4 -movflags +faststart \"$outPath\""
                            } else {
                                "$base -c:a aac -b:a 192k -f mp4 -movflags +faststart \"$outPath\""
                            }
                        }
                        OutputFormat.WAV -> "$base -c:a pcm_s16le -ar 44100 \"$outPath\""
                        OutputFormat.FLAC -> "$base -c:a flac -compression_level 5 \"$outPath\""
                        OutputFormat.OGG -> "$base -c:a libvorbis -q:a 5 \"$outPath\""
                    }
                    // 执行异步提取
                    Log.d("AudioPeel", "SAF输入: $safInput")
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
