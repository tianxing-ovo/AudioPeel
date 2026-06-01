package com.ltx.audiopeel.ui.components

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ltx.audiopeel.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 音频播放器视图
 *
 * @param filePath 音频文件路径
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerView(filePath: String) {
    val context = LocalContext.current
    // 根据文件路径创建ExoPlayer实例
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            setMediaItem(mediaItem)
            prepare()
        }
    }
    // 播放状态和位置
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var metadataDuration by remember(filePath) { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    LaunchedEffect(filePath) {
        metadataDuration = withContext(Dispatchers.IO) {
            getAudioDurationFromMetadata(filePath)
        }
        if (duration <= 0L && metadataDuration > 0L) {
            duration = metadataDuration
        }
    }
    // DisposableEffect用于管理ExoPlayer的生命周期
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            // 监听播放位置变化
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val playerDuration = normalizeDuration(exoPlayer.duration)
                    duration = if (playerDuration > 0L) playerDuration else metadataDuration
                } else if (playbackState == Player.STATE_ENDED) {
                    currentPosition = 0L
                    exoPlayer.seekTo(0L)
                    exoPlayer.pause()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    // LaunchedEffect用于更新当前播放位置
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isSeeking) {
                currentPosition = exoPlayer.currentPosition
            }
            delay(100)
        }
    }
    // 播放控制按钮和进度条
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // 播放进度条
            val interactionSource = remember { MutableInteractionSource() }
            Slider(
                value = if (duration > 0) {
                    (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }, onValueChange = { progress ->
                    isSeeking = true
                    if (duration > 0) {
                        currentPosition = (progress * duration).toLong()
                    }
                }, onValueChangeFinished = {
                    isSeeking = false
                    exoPlayer.seekTo(currentPosition)
                }, modifier = Modifier.weight(1f), interactionSource = interactionSource, thumb = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary, shape = CircleShape
                            )
                    )
                }, track = { sliderState ->
                    SliderDefaults.Track(
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        sliderState = sliderState,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                        modifier = Modifier.height(3.dp)
                    )
                })
            // 显示当前时间和总时长
            Text(
                text = formatTime(currentPosition) + " / " + formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * 格式化时间为字符串格式(分钟:秒)
 *
 * @param ms 时间毫秒数
 * @return 格式化后的时间字符串
 */
private fun formatTime(ms: Long): String {
    val safeMs = normalizeDuration(ms)
    if (safeMs <= 0L) return "00:00"
    val totalSeconds = safeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * 归一化时间毫秒数(将无效值转换为0L)
 *
 * @param ms 时间毫秒数
 * @return 归一化后的时间毫秒数
 */
private fun normalizeDuration(ms: Long): Long {
    if (ms == C.TIME_UNSET || ms <= 0L) return 0L
    return ms
}

/**
 * 从文件路径获取音频文件的总时长(毫秒)
 *
 * @param filePath 音频文件路径
 * @return 音频文件的总时长(毫秒)
 */
private fun getAudioDurationFromMetadata(filePath: String): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(filePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?.coerceAtLeast(0L) ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}
