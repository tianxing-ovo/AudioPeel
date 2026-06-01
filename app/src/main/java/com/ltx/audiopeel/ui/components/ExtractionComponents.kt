package com.ltx.audiopeel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltx.audiopeel.ExtractionState
import com.ltx.audiopeel.OutputFormat
import com.ltx.audiopeel.R
import java.io.File

/**
 * 视频选择卡片
 *
 * @param isSelected 是否已选择视频
 * @param onSelectClick 选择视频按钮点击事件
 * @param modifier 修饰符
 */
@Composable
fun VideoSelectorCard(
    isSelected: Boolean,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = stringResource(R.string.video_content_description),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSelected) stringResource(R.string.video_selected) else stringResource(R.string.select_video_prompt),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSelectClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (!isSelected) stringResource(R.string.select_video) else stringResource(R.string.change_video))
            }
        }
    }
}

/**
 * 格式选择与提取操作区
 *
 * @param visible 是否可见
 * @param selectedFormat 当前选中的格式
 * @param extractionState 提取状态
 * @param onFormatSelect 选中格式事件
 * @param onExtractClick 提取音频事件
 * @param onCancelClick 取消提取事件
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectorSection(
    visible: Boolean,
    selectedFormat: OutputFormat,
    extractionState: ExtractionState,
    onFormatSelect: (OutputFormat) -> Unit,
    onExtractClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.output_format),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val formats = OutputFormat.entries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                formats.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { onFormatSelect(format) },
                        label = { Text(format.name) })
                }
            }
            // 提取音频按钮
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onExtractClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = extractionState !is ExtractionState.Processing
            ) {
                if (extractionState is ExtractionState.Processing) {
                    val progressFloat = extractionState.progress
                    val progressPercent = (progressFloat * 100).toInt()
                    CircularProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.extracting_progress, progressPercent))
                } else {
                    Text(stringResource(R.string.start_extraction), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            // 取消提取按钮
            AnimatedVisibility(
                visible = extractionState is ExtractionState.Processing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_extraction))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.cancel_extraction))
                }
            }
        }
    }
}

/**
 * 提取结果显示区
 *
 * @param extractionState 提取状态
 * @param onShareClick 分享事件
 * @param onSaveClick 保存事件
 * @param onOpenFolderClick 打开文件夹事件
 * @param modifier 修饰符
 */
@Composable
fun ExtractionResultSection(
    extractionState: ExtractionState,
    onShareClick: (File, String) -> Unit,
    onSaveClick: (File, String) -> Unit,
    onOpenFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = extractionState is ExtractionState.Success || extractionState is ExtractionState.Error,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        when (extractionState) {
            is ExtractionState.Success -> {
                ExtractionSuccessCard(
                    outPath = extractionState.outPath,
                    onShareClick = onShareClick,
                    onSaveClick = onSaveClick,
                    onOpenFolderClick = onOpenFolderClick
                )
            }
            is ExtractionState.Error -> {
                val errorMessage = when {
                    extractionState.messageResId != null -> stringResource(extractionState.messageResId)
                    else -> extractionState.message ?: ""
                }
                ExtractionFailedCard(message = errorMessage)
            }
            else -> {}
        }
    }
}

/**
 * 提取成功卡片
 *
 * @param outPath 输出路径
 * @param onShareClick 分享事件
 * @param onSaveClick 保存事件
 * @param onOpenFolderClick 打开文件夹事件
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionSuccessCard(
    outPath: String,
    onShareClick: (File, String) -> Unit,
    onSaveClick: (File, String) -> Unit,
    onOpenFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customFileName by remember(outPath) {
        mutableStateOf(File(outPath).nameWithoutExtension)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.success),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.extraction_success), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val isNameValid = customFileName.trim().isNotEmpty()
            // 自定义文件名输入框
            OutlinedTextField(
                value = customFileName,
                onValueChange = { customFileName = it },
                label = { Text(stringResource(R.string.filename_no_extension)) },
                isError = !isNameValid,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            if (!isNameValid) {
                Text(
                    text = stringResource(R.string.filename_cannot_be_empty),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 2.dp),
                    textAlign = TextAlign.Start
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 音频播放器
            AudioPlayerView(outPath)
            Spacer(modifier = Modifier.height(12.dp))
            // 分享和保存按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onShareClick(File(outPath), customFileName.trim())
                    }, enabled = isNameValid, modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.share))
                }
                OutlinedButton(
                    onClick = {
                        onSaveClick(File(outPath), customFileName.trim())
                    }, enabled = isNameValid, modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = stringResource(R.string.save_file))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_file))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onOpenFolderClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.open_folder),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.open_audio_folder))
            }
        }
    }
}

/**
 * 提取失败卡片
 *
 * @param message 错误信息
 * @param modifier 修饰符
 */
@Composable
fun ExtractionFailedCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = stringResource(R.string.error),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.extraction_failed),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
