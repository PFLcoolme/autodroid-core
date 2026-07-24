package com.medeide.jh.core.data.repository

import android.content.Context
import com.medeide.jh.core.data.logging.FileLogger
import com.medeide.jh.model.ChatContext
import com.medeide.jh.model.FileInfo
import com.medeide.jh.model.chat.ChatMessage
import com.medeide.jh.model.chat.ChatRole
import com.medeide.jh.model.chat.ConversationEntry
import com.medeide.jh.model.chat.ToolCallInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ConversationRepository(private val context: Context) {

    private val storageFile: File
        get() = File(context.filesDir, "conversations.json")

    suspend fun load(): List<ConversationEntry> = withContext(Dispatchers.IO) {
        try {
            if (!storageFile.exists()) return@withContext emptyList()
            val json = storageFile.readText()
            val arr = JSONArray(json)
            val list = (0 until arr.length()).map { i -> parseEntry(arr.getJSONObject(i)) }
            FileLogger.d("ConvRepo", "loaded ${list.size} conversations")
            list
        } catch (e: Exception) {
            FileLogger.e("ConvRepo", "load failed", e)
            emptyList()
        }
    }

    suspend fun save(conversations: List<ConversationEntry>) = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            conversations.forEach { arr.put(toJson(it)) }
            storageFile.writeText(arr.toString())
            FileLogger.d("ConvRepo", "saved ${conversations.size} conversations (${storageFile.length()} bytes)")
        } catch (e: Exception) {
            FileLogger.e("ConvRepo", "save failed", e)
        }
    }

    /** 清理超过保留上限的旧对话 */
    suspend fun cleanup(maxCount: Int = 50) = withContext(Dispatchers.IO) {
        try {
            if (!storageFile.exists()) return@withContext
            val json = storageFile.readText()
            val arr = JSONArray(json)
            if (arr.length() <= maxCount) return@withContext
            // 按 timestamp 排序，保留最新的 maxCount 条
            val sorted = (0 until arr.length()).map { i -> Pair(i, arr.getJSONObject(i)) }
                .sortedBy { it.second.optLong("timestamp") }
            val keep = sorted.takeLast(maxCount)
            val newArr = JSONArray()
            keep.forEach { (_, obj) -> newArr.put(obj) }
            storageFile.writeText(newArr.toString())
            FileLogger.i("ConvRepo", "cleanup: ${arr.length()} -> ${newArr.length()} conversations")
        } catch (e: Exception) {
            FileLogger.e("ConvRepo", "cleanup failed", e)
        }
    }

    private fun parseEntry(obj: JSONObject): ConversationEntry = ConversationEntry(
        id = obj.optString("id", ""),
        title = obj.optString("title", ""),
        messages = parseMessages(obj.optJSONArray("messages")),
        context = parseContext(obj.optJSONObject("context")),
        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
    )

    private fun parseMessages(arr: JSONArray?): List<ChatMessage> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val msg = arr.getJSONObject(i)
            ChatMessage(
                id = msg.optString("id", ""),
                role = ChatRole.valueOf(msg.optString("role", "User")),
                content = msg.optString("content", ""),
                reasoningContent = msg.optString("reasoningContent", ""),
                isStreaming = false,
                timestamp = msg.optLong("timestamp", System.currentTimeMillis()),
            )
        }
    }

    private fun parseContext(obj: JSONObject?): ChatContext? {
        if (obj == null) return null
        try {
            val openFilesArr = obj.optJSONArray("openFiles")
            val openFiles = openFilesArr?.let {
                (0 until it.length()).map { i ->
                    val fo = it.getJSONObject(i)
                    FileInfo(
                        path = fo.optString("path", ""),
                        name = fo.optString("name", ""),
                    )
                }
            } ?: emptyList()
            return ChatContext(
                filePath = obj.optString("filePath", null),
                fileName = obj.optString("fileName", null),
                fileContentSummary = obj.optString("fileContentSummary", null),
                openFiles = openFiles,
                projectRoot = obj.optString("projectRoot", null),
                workspaceId = obj.optString("workspaceId", ""),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
        } catch (e: Exception) {
            FileLogger.e("ConvRepo", "parseContext failed", e)
            return null
        }
    }

    private fun toJson(entry: ConversationEntry): JSONObject = JSONObject().apply {
        put("id", entry.id)
        put("title", entry.title)
        put("timestamp", entry.timestamp)
        put("messages", JSONArray().apply {
            entry.messages.forEach { msg ->
                put(JSONObject().apply {
                    put("id", msg.id)
                    put("role", msg.role.name)
                    put("content", msg.content)
                    put("reasoningContent", msg.reasoningContent)
                    put("timestamp", msg.timestamp)
                })
            }
        })
        entry.context?.let { ctx ->
            put("context", JSONObject().apply {
                put("filePath", ctx.filePath ?: JSONObject.NULL)
                put("fileName", ctx.fileName ?: JSONObject.NULL)
                put("fileContentSummary", ctx.fileContentSummary ?: JSONObject.NULL)
                put("openFiles", JSONArray().apply {
                    ctx.openFiles.forEach { fo ->
                        put(JSONObject().apply {
                            put("path", fo.path)
                            put("name", fo.name)
                        })
                    }
                })
                put("projectRoot", ctx.projectRoot ?: JSONObject.NULL)
                put("workspaceId", ctx.workspaceId)
                put("createdAt", ctx.createdAt)
            })
        }
    }
}
