package com.medeide.jh.core.data.remote

import android.content.Context
import com.medeide.jh.core.data.repository.UserPreferencesRepository
import com.medeide.jh.model.MarketManifest
import com.medeide.jh.model.PluginInstallStatus
import com.medeide.jh.model.RemotePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * 远程插件管理器 — 整合网络+存储+偏好设置
 */
class RemotePluginManager(
    private val context: Context,
    private val marketClient: PluginMarketClient,
    private val assetManager: PluginAssetManager,
    private val preferences: UserPreferencesRepository,
) {
    private var _manifest: MarketManifest? = null
    val manifest: MarketManifest? get() = _manifest

    // ── 拉取清单 ──

    suspend fun fetchManifest(url: String = PluginMarketClient.DEFAULT_MANIFEST_URL): MarketManifest {
        _manifest = marketClient.fetchManifest(url)
        return _manifest!!
    }

    // ── 获取所有市场插件 ──

    fun getAllMarketPlugins(): List<RemotePlugin> = manifest?.plugins ?: emptyList()

    // ── 获取安装状态 ──

    /** 获取所有插件（内置 + 远程已安装）的安装状态 */
    suspend fun getInstallStatuses(): List<PluginInstallStatus> {
        val enabledPlugins = preferences.enabledPlugins.first()
        return getAllMarketPlugins().map { plugin ->
            PluginInstallStatus(
                pluginId = plugin.id,
                installed = plugin.id in enabledPlugins,
                currentVersion = assetManager.getInstalledVersion(plugin.id),
                updateAvailable = assetManager.getInstalledVersion(plugin.id) != plugin.version,
            )
        }
    }

    // ── 安装 ──

    suspend fun installPlugin(remotePlugin: RemotePlugin): Result<Unit> = withContext(Dispatchers.IO) {
        // 下载 ZIP 资源
        val downloadUrl = URL(remotePlugin.downloadUrl)
        val connection = downloadUrl.openConnection() as java.net.HttpURLConnection
        return@withContext try {
            connection.inputStream.use { input ->
                assetManager.installPlugin(remotePlugin, input, remotePlugin.hash).onSuccess {
                    // 注册到 enabledPlugins
                    val enabled = preferences.enabledPlugins.first().toMutableSet()
                    enabled += remotePlugin.id
                    preferences.setEnabledPlugins(enabled)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── 卸载 ──

    suspend fun uninstallPlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        assetManager.uninstallPlugin(pluginId).let { success ->
            if (success) {
                // 从 enabledPlugins 移除
                val enabled = preferences.enabledPlugins.first().toMutableSet()
                enabled -= pluginId
                preferences.setEnabledPlugins(enabled)
            }
            if (success) Result.success(Unit) else Result.failure(Exception("卸载失败"))
        }
    }

    // ── 批量检查更新 ──

    suspend fun checkUpdates(): List<RemotePlugin> {
        return getAllMarketPlugins().filter { plugin ->
            val installed = assetManager.getInstalledVersion(plugin.id)
            installed != plugin.version
        }
    }
}
