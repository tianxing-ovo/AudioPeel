package com.ltx.audiopeel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 音频提取格式
 */
enum class OutputFormat(val extension: String, val codec: String) {
    MP3("mp3", "libmp3lame"), AAC("aac", "aac"), WAV("wav", "pcm_s16le"), FLAC("flac", "flac"), OGG(
        "ogg",
        "libvorbis"
    )
}

/**
 * 音频提取状态
 */
sealed class ExtractionState {
    data object Idle : ExtractionState()
    data class Processing(val progress: Float) : ExtractionState()
    data class Success(val outPath: String) : ExtractionState()
    data class Error(val message: String) : ExtractionState()
}

/**
 * 主视图模型
 */
class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val state: StateFlow<ExtractionState> = _state.asStateFlow()
    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()
    private val _selectedFormat = MutableStateFlow(OutputFormat.MP3)
    val selectedFormat: StateFlow<OutputFormat> = _selectedFormat.asStateFlow()

    fun selectVideo(uri: Uri?) {
        _selectedUri.value = uri
        _state.value = ExtractionState.Idle
    }

    fun selectFormat(format: OutputFormat) {
        _selectedFormat.value = format
    }

    /**
     * 提取音频
     *
     * @param context 上下文
     */
    fun extractAudio(context: Context) {
        val uri = _selectedUri.value ?: return
        val format = _selectedFormat.value
        // 检查是否有足够的权限
        _state.value = ExtractionState.Processing(0f)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val tempInputFile = File(context.cacheDir, "temp_input_video")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempInputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw Exception("无法读取视频文件")
                    // 构建输出路径
                    val inPath = tempInputFile.absolutePath
                    val outDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
                        ?: context.cacheDir
                    // 生成唯一文件名
                    val timeStamp =
                        java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    val fileName = "audio_${timeStamp}.${format.extension}"
                    val outFile = File(outDir, fileName)
                    val outPath = outFile.absolutePath
                    // 获取媒体总时长以便计算进度
                    var totalDurationMs = 0L
                    try {
                        val mediaInformationSession = FFprobeKit.getMediaInformation(inPath)
                        val mediaInformation = mediaInformationSession.mediaInformation
                        if (mediaInformation?.duration != null) {
                            totalDurationMs = (mediaInformation.duration.toDouble() * 1000).toLong()
                        }
                    } catch (e: Exception) {
                        Log.e("AudioPeel", "获取时长失败: ${e.message}")
                    }
                    // 构建ffmpeg命令
                    val cmd = "-y -i $inPath -vn -acodec ${format.codec} $outPath"
                    // 执行异步提取
                    FFmpegKit.executeAsync(cmd, { session ->
                        // 完成回调
                        val returnCode = session.returnCode
                        if (ReturnCode.isSuccess(returnCode)) {
                            _state.value = ExtractionState.Success(outPath)
                        } else {
                            val logs = session.allLogsAsString
                            Log.e("AudioPeel", "FFmpeg Error $returnCode: $logs")
                            _state.value = ExtractionState.Error("提取失败！请检查日志。")
                        }
                        // 清理临时文件
                        tempInputFile.delete()
                    }, { _ ->
                        // 日志回调
                    }, { statistics ->
                        // 进度回调
                        if (totalDurationMs > 0) {
                            val timeInMilliseconds = statistics.time
                            if (timeInMilliseconds > 0) {
                                var progress =
                                    timeInMilliseconds.toFloat() / totalDurationMs.toFloat()
                                if (progress > 1f) progress = 1f
                                if (progress < 0f) progress = 0f
                                _state.value = ExtractionState.Processing(progress)
                            }
                        }
                    })
                } catch (e: Exception) {
                    _state.value = ExtractionState.Error(e.message ?: "未知错误")
                }
            }
        }
    }
}