package com.medeide.jh.model

import android.net.Uri
import java.util.UUID

/** AI 对话上下文：文件路径 + 内容摘要 + 项目结构 */
data class ChatContext(
    /** 当前编辑的文件路径 (content:// URI 或绝对路径) */
    val filePath: String? = null,
    /** 当前文件名 */
    val fileName: String? = null,
    /** 文件内容摘要（前 2000 行，最多 50KB） */
    val fileContentSummary: String? = null,
    /** 其他已打开的文件列表 */
    val openFiles: List<FileInfo> = emptyList(),
    /** 项目根目录 */
    val projectRoot: String? = null,
    /** 工作空间标识（通常是项目根目录路径） */
    val workspaceId: String = "",
    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis(),
)

/** 已打开的文件信息 */
data class FileInfo(
    val path: String,
    val name: String,
)

/** 从文件路径解析出 ChatContext */
fun buildContextFromPath(filePath: String, projectRoot: String, openFilePaths: List<String>): ChatContext {
    val fileName = displayNameFromPath(filePath)
    return ChatContext(
        filePath = filePath,
        fileName = fileName,
        projectRoot = projectRoot.ifBlank { null },
        workspaceId = projectRoot.ifBlank { "" },
        openFiles = openFilePaths.filter { it != filePath }.take(5).map { p ->
            FileInfo(path = p, name = displayNameFromPath(p))
        },
    )
}
