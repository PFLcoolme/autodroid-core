package com.medeide.jh.core.data.remote

import android.content.Context
import com.medeide.jh.model.MarketManifest
import com.medeide.jh.model.RemotePlugin
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * 插件资源管理器 — 负责校验、解压、保存插件数据文件
 */
class PluginAssetManager(private val context: Context) {

    /** 插件资源根目录 */
    private val pluginsDir: File
        get() = File(context.filesDir, "plugins").apply { mkdirs() }

    // ── 校验 ──

    /** 计算 SHA-256 */
    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream.use { input ->
            val buffer = ByteArray(8192)
            var len: Int
            while (input.read(buffer).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** 验证 Hash（格式: "sha256:xxx" 或纯 hex） */
    fun verifyHash(content: String, expected: String): Boolean {
        val expectedHash = expected.removePrefix("sha256:").lowercase()
        if (expectedHash.length != 64) return false
        return content == expectedHash
    }

    // ── 解压 ──

    /** 解压 ZIP 到插件目录 */
    fun unzip(zipStream: InputStream, pluginDir: File) {
        java.util.zip.ZipInputStream(zipStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val file = File(pluginDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    // ── 写入 ──

    /** 写入 manifest.json 到插件目录 */
    fun writeManifest(pluginDir: File, manifest: MarketManifest) {
        val json = JSONObject().apply {
            put("version", manifest.version)
            val pluginsArr = JSONArray()
            manifest.plugins.forEach { p ->
                pluginsArr.put(JSONObject().apply {
                    put("id", p.id); put("name", p.name); put("version", p.version)
                    put("description", p.description); put("category", p.category)
                    put("author", p.author); put("iconUrl", p.iconUrl ?: "")
                    put("downloadUrl", p.downloadUrl); put("hash", p.hash)
                    put("minAppVersion", p.minAppVersion)
                    put("dataType", p.dataType.name.lowercase())
                })
            }
            put("plugins", pluginsArr)
        }
        File(pluginDir, "manifest.json").writeText(json.toString())
    }

    // ── 读取 ──

    /** 获取已安装的插件目录 */
    fun getPluginDir(pluginId: String): File {
        return File(pluginsDir, pluginId)
    }

    /** 检查插件是否已安装 */
    fun isInstalled(pluginId: String): Boolean {
        return File(pluginsDir, pluginId).exists()
    }

    /** 获取已安装插件的版本 */
    fun getInstalledVersion(pluginId: String): String? {
        val manifestFile = File(pluginsDir, "$pluginId/manifest.json")
        return if (manifestFile.exists()) {
            try {
                JSONObject(manifestFile.readText()).optString("version")
            } catch (_: Exception) { null }
        } else null
    }

    // ── 安装/卸载 ──

    /** 安装插件（解压 + 写清单） */
    fun installPlugin(plugin: RemotePlugin, zipStream: InputStream, hash: String): Result<Unit> {
        return try {
            // 1. 校验
            val actualHash = computeSha256(zipStream)
            if (!verifyHash(actualHash, hash)) {
                return Result.failure(IllegalArgumentException("Hash 校验失败"))
            }

            // 2. 解压
            val pluginDir = File(pluginsDir, plugin.id)
            pluginDir.mkdirs()
            unzip(zipStream, pluginDir)

            // 3. 写清单
            writeManifest(pluginDir, MarketManifest("1", listOf(plugin)))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 卸载插件（删除目录） */
    fun uninstallPlugin(pluginId: String): Boolean {
        return deleteRecursively(File(pluginsDir, pluginId))
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        return file.delete()
    }
}
