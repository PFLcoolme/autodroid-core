package com.medeide.jh.model

// 远程插件市场数据源类型
enum class PluginDataType {
    SNIPPETS,     // 代码片段包
    RULES,        // 规则集（AI 角色/提示词）
    MCP_TEMPLATES,// MCP 服务器预设配置
    THEMES;       // 高亮主题

    companion object {
        fun fromString(value: String?): PluginDataType {
            return try {
                value?.let { valueOf(it.uppercase()) } ?: THEMES
            } catch (_: Exception) { THEMES }
        }
    }
}

// 远程市场插件条目
data class RemotePlugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: String = "其他",
    val author: String = "",
    val iconUrl: String? = null,
    val downloadUrl: String,
    val hash: String,           // SHA-256
    val minAppVersion: String = "1.3.4",
    val dataType: PluginDataType = PluginDataType.THEMES,
    val configSchema: List<ConfigItem> = emptyList(),
)

// 市场清单（根）
data class MarketManifest(
    val version: String = "1",
    val plugins: List<RemotePlugin>,
)

// 市场安装状态
data class PluginInstallStatus(
    val pluginId: String,
    val installed: Boolean = false,
    val currentVersion: String? = null,
    val updateAvailable: Boolean = false,
    val installing: Boolean = false,
    val installingProgress: Float = 0f,
)
