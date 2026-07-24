package com.medeide.jh.screens.home.landscape.workspace.settings.plugin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medeide.jh.core.data.repository.UserPreferencesRepository
import com.medeide.jh.model.Plugin
import com.medeide.jh.model.BuiltinPlugins
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PluginSettingsViewModel(
    private val userPrefs: UserPreferencesRepository,
) : ViewModel() {
    val plugins: List<Plugin> = BuiltinPlugins.ALL
    val enabledPlugins: StateFlow<Set<String>> = userPrefs.enabledPlugins.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val pluginConfigs: StateFlow<Map<String, Map<String, String>>> = userPrefs.pluginConfigs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.togglePlugin(pluginId, enabled)
        }
    }

    fun updatePluginConfig(pluginId: String, key: String, value: String) {
        viewModelScope.launch {
            userPrefs.setPluginConfig(pluginId, key, value)
        }
    }
}
