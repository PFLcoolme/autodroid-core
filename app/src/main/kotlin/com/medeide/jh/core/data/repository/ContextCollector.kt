package com.medeide.jh.core.data.repository

import android.content.Context
import android.net.Uri
import com.medeide.jh.core.data.logging.FileLogger
import com.medeide.jh.model.ChatContext
import com.medeide.jh.model.FileInfo
import com.medeide.jh.model.displayNameFromPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 上下文采集器：从文件 URI/路径中读取内容摘要。
 * 采集策略：最多前 2000 行或 50KB（取较小值），UTF-8 编码。
 */
class ContextCollector(private val context: Context) {

    companion object {
        private const val MAX_LINES = 2000
        private const val MAX_BYTES = 50 * 1024 // 50KB
    }

    /** 采集单个文件的内容摘要 */
    suspend fun collectFileContent(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            val text = readFile(uri) ?: return@withContext null
            val summary = truncate(text)
            summary.ifEmpty { null }
        } catch (e: Exception) {
            FileLogger.e("ContextCollector", "collectFileContent failed: $uri", e)
            null
        }
    }

    /** 构建 ChatContext 对象（含文件内容） */
    suspend fun buildContext(
        filePath: String,
        projectRoot: String,
        openFilePaths: List<String>,
    ): ChatContext {
        val fileName = displayNameFromPath(filePath)
        val contentSummary = collectFileContent(filePath)
        val openFiles = openFilePaths.filter { it != filePath }.take(5).map { p ->
            FileInfo(path = p, name = displayNameFromPath(p))
        }
        return ChatContext(
            filePath = filePath,
            fileName = fileName,
            fileContentSummary = contentSummary,
            openFiles = openFiles,
            projectRoot = projectRoot.ifBlank { null },
            workspaceId = projectRoot.ifBlank { "" },
        )
    }

    /** 读取文件内容（支持 content URI 和文件路径） */
    private suspend fun readFile(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            when {
                uri.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        stream.bufferedReader().readText().substring(0, MAX_BYTES)
                    }
                }
                else -> {
                    val file = File(uri)
                    if (file.exists() && file.isFile) {
                        file.readText().substring(0, MAX_BYTES)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            FileLogger.e("ContextCollector", "readFile failed: $uri", e)
            null
        }
    }

    /** 截断文本：最多保留 MAX_LINES 行 */
    private fun truncate(text: String): String {
        val lines = text.lineSequence().take(MAX_LINES).toList()
        return lines.joinToString("\n")
    }
}
