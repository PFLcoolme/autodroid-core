package com.medeide.jh.screens.home.landscape.workspace.settings.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medeide.jh.model.Plugin
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PluginSettingsScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PluginSettingsViewModel = koinViewModel()
    val enabledPlugins by viewModel.enabledPlugins.collectAsState(initial = emptySet())

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("管理 IDE 内置功能模块，关闭后可减少资源占用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val categories = viewModel.plugins.groupBy { it.category }
        categories.forEach { (category, items) ->
            PluginGroup(
                title = category, 
                plugins = items, 
                enabledPlugins = enabledPlugins, 
                onToggle = { id, enabled -> viewModel.togglePlugin(id, enabled) },
                onPluginClick = onNavigateToDetail
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
