package com.medeide.jh.screens.home.landscape.workspace.settings.plugin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medeide.jh.core.data.remote.PluginAssetManager
import com.medeide.jh.core.data.remote.PluginMarketClient
import com.medeide.jh.core.data.remote.RemotePluginManager
import com.medeide.jh.model.PluginInstallStatus
import com.medeide.jh.model.RemotePlugin
import com.medeide.jh.core.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PluginMarketViewModel(
    private val manager: RemotePluginManager,
    private val preferences: UserPreferencesRepository,
    private val assetManager: PluginAssetManager,
) : ViewModel() {

    // UI 状态
    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Idle)
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    sealed interface MarketUiState {
        object Idle : MarketUiState
        object Loading : MarketUiState
        data class Loaded(
            val plugins: List<MarketPluginUi>,
            val statuses: Map<String, PluginInstallStatus>,
        ) : MarketUiState

        object Error : MarketUiState
    }

    data class MarketPluginUi(
        val plugin: RemotePlugin,
        val status: PluginInstallStatus,
    ) {
        val buttonText: String
            get() = when {
                status.installed && !status.updateAvailable -> "已安装"
                status.installed && status.updateAvailable -> "更新"
                else -> "安装"
            }

        val isInstalled: Boolean get() = status.installed
    }

    // 当前子 tab
    var selectedSubTab: Int = 0
        internal set

    // ── 初始化 ──

    init {
        viewModelScope.launch { loadMarket() }
    }

    // ── 数据加载 ──

    private fun loadMarket() {
        _uiState.value = MarketUiState.Loading
        viewModelScope.launch {
            try {
                manager.fetchManifest()
                reload()
            } catch (e: Exception) {
                // 网络失败，使用已安装数据
                _uiState.value = MarketUiState.Error
            }
        }
    }

    private suspend fun reload() {
        val plugins = manager.getAllMarketPlugins().map { remote ->
            val status = manager.getInstallStatuses().find { it.pluginId == remote.id }
                ?: PluginInstallStatus(pluginId = remote.id)
            MarketPluginUi(plugin = remote, status = status)
        }
        val statuses = plugins.associate { it.plugin.id to it.status }
        _uiState.value = MarketUiState.Loaded(plugins = plugins, statuses = statuses)
    }

    // ── 操作 ──

    /** 安装插件 */
    fun installPlugin(plugin: RemotePlugin) {
        _uiState.update { state ->
            (state as? MarketUiState.Loaded)?.let { loaded ->
                val updatedPlugins = loaded.plugins.map {
                    if (it.plugin.id == plugin.id) it.copy(status = it.status.copy(installing = true, installingProgress = 0f))
                    else it
                }
                loaded.copy(plugins = updatedPlugins)
            } ?: state
        }
        viewModelScope.launch {
            manager.installPlugin(plugin)
            reload()
        }
    }

    /** 卸载插件 */
    fun uninstallPlugin(plugin: RemotePlugin) {
        viewModelScope.launch {
            manager.uninstallPlugin(plugin.id)
            reload()
        }
    }

    /** 刷新市场列表 */
    fun refresh() {
        viewModelScope.launch {
            manager.fetchManifest()
            reload()
        }
    }

    /** 检查更新 */
    suspend fun checkUpdates() {
        reload()
    }
}
