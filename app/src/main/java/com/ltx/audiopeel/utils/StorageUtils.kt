package com.ltx.audiopeel.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ltx.audiopeel.R
import java.io.File

/**
 * 分享文件
 *
 * @param context 上下文
 * @param file 要分享的文件
 * @param customName 自定义文件名
 */
fun shareFile(context: Context, file: File, customName: String) {
    try {
        val finalName = customName.takeIf { it.endsWith(".${file.extension}") }
            ?: "$customName.${file.extension}"
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
        context.startActivity(
            Intent.createChooser(
                intent, context.getString(R.string.share_audio_title)
            )
        )
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
        val finalName = customName.takeIf { it.endsWith(".${file.extension}") }
            ?: "$customName.${file.extension}"
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
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: Exception) {
                    // 删除失败的文件
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    throw e
                }
            }
        } else {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val destDir = File(baseDir, "AudioPeel")
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, finalName)
            file.copyTo(destFile, overwrite = true)
        }
        Toast.makeText(
            context, context.getString(R.string.toast_saved_to_music), Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            context.getString(R.string.toast_save_failed, e.message ?: ""),
            Toast.LENGTH_SHORT
        ).show()
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
        Toast.makeText(
            context, context.getString(R.string.toast_cannot_open_folder), Toast.LENGTH_SHORT
        ).show()
    }
}
