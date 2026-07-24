package com.medeide.jh.screens.home.landscape.workspace.settings.plugin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medeide.jh.model.ConfigItem
import com.medeide.jh.model.Plugin
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    pluginId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PluginSettingsViewModel = koinViewModel()
    val plugin = viewModel.plugins.find { it.id == pluginId } ?: return
    val enabledPlugins by viewModel.enabledPlugins.collectAsState(initial = emptySet())
    val pluginConfigs by viewModel.pluginConfigs.collectAsState(initial = emptyMap())
    val currentConfig = pluginConfigs[pluginId] ?: emptyMap()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(plugin.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("描述", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(plugin.description, style = MaterialTheme.typography.bodyMedium)
            
            HorizontalDivider()
            
            Text("配置项", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            if (plugin.configSchema.isEmpty()) {
                Text("该插件没有可配置项。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                plugin.configSchema.forEach { config ->
                    ConfigItemRenderer(
                        config = config,
                        value = currentConfig[config.key] ?: "",
                        onValueChange = { newValue -> viewModel.updatePluginConfig(pluginId, config.key, newValue) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigItemRenderer(
    config: ConfigItem,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (config) {
        is ConfigItem.Text -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(config.label) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        is ConfigItem.Switch -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(config.label)
                Switch(
                    checked = value.toBoolean(),
                    onCheckedChange = { onValueChange(it.toString()) }
                )
            }
        }
    }
}
