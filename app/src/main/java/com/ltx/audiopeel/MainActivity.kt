package com.ltx.audiopeel

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltx.audiopeel.ui.components.ExtractionResultSection
import com.ltx.audiopeel.ui.components.FormatSelectorSection
import com.ltx.audiopeel.ui.components.VideoSelectorCard
import com.ltx.audiopeel.ui.theme.AudioPeelTheme
import com.ltx.audiopeel.utils.openMusicFolder
import com.ltx.audiopeel.utils.saveToDownloads
import com.ltx.audiopeel.utils.shareFile

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

/**
 * 音频提取应用主入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioExtractorApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val selectedUri by viewModel.selectedUri.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    // 视频选择器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideo(context, uri)
        }
    }
    // 应用主布局
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
            // 视频选择卡片
            VideoSelectorCard(
                isSelected = selectedUri != null, onSelectClick = {
                    launcher.launch("video/*")
                })
            Spacer(modifier = Modifier.height(16.dp))
            // 输出格式与提取控制
            FormatSelectorSection(
                visible = selectedUri != null,
                selectedFormat = selectedFormat,
                extractionState = state,
                onFormatSelect = { viewModel.selectFormat(it) },
                onExtractClick = { viewModel.extractAudio(context) },
                onCancelClick = { viewModel.cancelExtraction() })
            Spacer(modifier = Modifier.height(16.dp))
            // 提取结果
            ExtractionResultSection(
                extractionState = state,
                onShareClick = { file, name -> shareFile(context, file, name) },
                onSaveClick = { file, name -> saveToDownloads(context, file, name) },
                onOpenFolderClick = { openMusicFolder(context) })
        }
    }
}