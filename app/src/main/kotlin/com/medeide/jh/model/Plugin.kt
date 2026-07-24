package com.medeide.jh.model

// 配置项定义
sealed class ConfigItem {
    abstract val key: String
    abstract val label: String
    
    data class Text(override val key: String, override val label: String, val defaultValue: String = "") : ConfigItem()
    data class Switch(override val key: String, override val label: String, val defaultValue: Boolean = false) : ConfigItem()
}

// 内置插件定义
data class Plugin(
    val id: String,
    val name: String,
    val description: String,
    val category: String = "内置",
    val isBuiltin: Boolean = true,
    val configSchema: List<ConfigItem> = emptyList(), // 配置定义
)

object BuiltinPlugins {
    val ALL = listOf(
        Plugin(id = "medeide-git", name = "Git 增强", description = "侧边栏 Git 面板、状态查看、快速提交", category = "版本控制"),
        Plugin(id = "medeide-terminal", name = "内置终端", description = "编辑器底部集成 Termux 终端", category = "开发工具"),
        Plugin(id = "medeide-search", name = "全局搜索", description = "侧边栏搜索替换与全局检索", category = "开发工具"),
        Plugin(id = "medeide-analyzer", name = "代码分析", description = "基础代码检查与问题定位", category = "AI 辅助"),
        Plugin(id = "medeide-snippets", name = "代码片段", description = "常用代码模板与快速插入", category = "编辑增强"),
        Plugin(id = "medeide-preview", name = "多媒体预览", description = "图片、视频、音频与 Markdown 预览", category = "预览"),
        Plugin(
            id = "medeide-mcp", 
            name = "MCP 扩展", 
            description = "管理 MCP 服务器与工具调用", 
            category = "AI 辅助",
            configSchema = listOf(
                ConfigItem.Text("mcp_server_url", "服务器地址"),
                ConfigItem.Switch("auto_connect", "自动连接")
            )
        ),
        Plugin(id = "medeide-compress", name = "压缩解压", description = "支持带密码的 ZIP 压缩与解压", category = "文件工具"),
    )
}
