package com.ltx.audiopeel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltx.audiopeel.ui.theme.AudioPeelTheme
import java.io.File

/**
 * 主活动
 *
 * @author tianxing
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioPeelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    AudioExtractorApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioExtractorApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val selectedUri by viewModel.selectedUri.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideo(uri)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        contentDescription = "视频",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedUri != null) "已选择视频" else "请选择需要提取音频的视频",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (selectedUri == null) "选择视频" else "更换视频")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = selectedUri != null, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "输出格式",
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
                                onClick = { viewModel.selectFormat(format) },
                                label = { Text(format.name) })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.extractAudio(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = state !is ExtractionState.Processing
                    ) {
                        if (state is ExtractionState.Processing) {
                            val progressFloat = (state as ExtractionState.Processing).progress
                            val progressPercent = (progressFloat * 100).toInt()

                            CircularProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("正在提取 $progressPercent%")
                        } else {
                            Text("开始提取音频", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = state is ExtractionState.Success || state is ExtractionState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                when (val currentState = state) {
                    is ExtractionState.Success -> {
                        var customFileName by remember(currentState.outPath) {
                            mutableStateOf(File(currentState.outPath).nameWithoutExtension)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    contentDescription = "成功",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("提取成功", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                val isNameValid = customFileName.trim().isNotEmpty()

                                OutlinedTextField(
                                    value = customFileName,
                                    onValueChange = { customFileName = it },
                                    label = { Text("文件名无需后缀") },
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
                                        text = "文件名不能为空",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, top = 2.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            shareFile(
                                                context,
                                                File(currentState.outPath),
                                                customFileName.trim()
                                            )
                                        }, enabled = isNameValid, modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "分享")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("分享")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            saveToDownloads(
                                                context,
                                                File(currentState.outPath),
                                                customFileName.trim()
                                            )
                                        }, enabled = isNameValid, modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.SaveAlt, contentDescription = "保存")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("保存文件")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { openMusicFolder(context) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = "打开文件夹",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("打开音频文件夹")
                                }
                            }
                        }
                    }

                    is ExtractionState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "错误",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "提取失败",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    currentState.message,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

/**
 * 分享文件
 *
 * @param context 上下文
 * @param file 要分享的文件
 * @param customName 自定义文件名
 */
fun shareFile(context: Context, file: File, customName: String) {
    try {
        var finalName = customName
        if (!finalName.endsWith(".${file.extension}")) {
            finalName += ".${file.extension}"
        }
        var fileToShare = file
        if (file.name != finalName) {
            val renamedFile = File(file.parent, finalName)
            file.copyTo(renamedFile, overwrite = true)
            fileToShare = renamedFile
        }
        // 处理Android Q及以上版本的文件分享
        val uri =
            FileProvider.getUriForFile(context, "${context.packageName}.provider", fileToShare)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享音频"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 保存文件到下载文件夹
 *
 * @param context 上下文
 * @param file 要保存的文件
 * @param customName 自定义文件名
 */
fun saveToDownloads(context: Context, file: File, customName: String) {
    try {
        var finalName = customName
        if (!finalName.endsWith(".${file.extension}")) {
            finalName += ".${file.extension}"
        }
        // 处理Android Q及以上版本的文件保存
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, finalName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/*")
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AudioPeel"
                )
            }
            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } else {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val destDir = File(baseDir, "AudioPeel")
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, finalName)
            file.copyTo(destFile, overwrite = true)
        }
        Toast.makeText(context, "已保存到音乐文件夹", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 打开音乐文件夹
 *
 * @param context 上下文
 */
fun openMusicFolder(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 直接打开Music/AudioPeel目录
            val musicUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:${Environment.DIRECTORY_MUSIC}/AudioPeel"
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(musicUri, "vnd.android.document/directory")
            }
            context.startActivity(intent)
        } else {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val musicDir = File(baseDir, "AudioPeel")
            if (!musicDir.exists()) musicDir.mkdirs()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(musicDir), "resource/folder")
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "无法打开文件夹", Toast.LENGTH_SHORT).show()
    }
}