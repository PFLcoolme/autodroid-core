package com.medeide.jh.core.data.remote

import com.medeide.jh.model.ConfigItem
import com.medeide.jh.model.MarketManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class PluginMarketClient {
    companion object {
        const val DEFAULT_MANIFEST_URL = "https://raw.githubusercontent.com/example/medeide-plugins/main/manifest.json"
    }

    /**
     * 从远程市场拉取插件清单
     */
    suspend fun fetchManifest(manifestUrl: String = DEFAULT_MANIFEST_URL): MarketManifest = withContext(Dispatchers.IO) {
        val url = URL(manifestUrl)
        val response = url.readText()
        parseManifest(response)
    }

    /**
     * 解析 manifest.json 为 MarketManifest
     */
    private fun parseManifest(json: String): MarketManifest {
        val root = JSONObject(json)
        val version = root.optString("version", "1")
        val pluginsArr = root.getJSONArray("plugins")
        val plugins = mutableListOf<com.medeide.jh.model.RemotePlugin>()

        for (i in 0 until pluginsArr.length()) {
            val obj = pluginsArr.getJSONObject(i)
            val iconUrl = if (obj.has("iconUrl")) obj.optString("iconUrl") else null
            plugins += com.medeide.jh.model.RemotePlugin(
                id = obj.getString("id"),
                name = obj.optString("name", ""),
                version = obj.optString("version", "1.0.0"),
                description = obj.optString("description", ""),
                category = obj.optString("category", "其他"),
                author = obj.optString("author", ""),
                iconUrl = iconUrl,
                downloadUrl = obj.getString("downloadUrl"),
                hash = obj.optString("hash", ""),
                minAppVersion = obj.optString("minAppVersion", "1.3.4"),
                dataType = com.medeide.jh.model.PluginDataType.fromString(obj.optString("dataType")),
                configSchema = parseConfigSchema(obj.optJSONArray("configSchema")),
            )
        }

        return MarketManifest(version = version, plugins = plugins)
    }

    /**
     * 解析 configSchema（JSON 数组 → List<ConfigItem>）
     */
    private fun parseConfigSchema(arr: JSONArray?): List<ConfigItem> {
        val result = mutableListOf<ConfigItem>()
        if (arr == null) return result
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val type = obj.optString("type", "text").lowercase()
            val key = obj.getString("key")
            val label = obj.optString("label", key)
            val defaultValue = obj.optString("default", "")
            when (type) {
                "text" -> result.add(ConfigItem.Text(key, label, defaultValue))
                "switch" -> result.add(ConfigItem.Switch(key, label, defaultValue.toBooleanStrictOrNull() ?: false))
            }
        }
        return result
    }
}
