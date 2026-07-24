package com.medeide.jh.screens.home.landscape.workspace.settings.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.medeide.jh.screens.home.landscape.workspace.settings.plugin.PluginMarketViewModel
import com.medeide.jh.screens.home.landscape.workspace.settings.plugin.PluginMarketViewModel.MarketUiState
import kotlinx.coroutines.launch

@Composable
fun PluginMarketScreen(
    viewModel: PluginMarketViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部工具栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("远程插件市场", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Icon(
                Icons.Default.Refresh,
                "刷新市场",
                modifier = Modifier.size(24.dp).clickable { scope.launch { viewModel.refresh() } },
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        // 子 tab（市场 / 已安装）
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val tabs = listOf("市场", "已安装")
            tabs.forEachIndexed { index, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (index == viewModel.selectedSubTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == viewModel.selectedSubTab) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.clickable { viewModel.selectedSubTab = index }.padding(vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 内容
        when (val state = uiState) {
            is MarketUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MarketUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("市场加载失败，请检查网络", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { scope.launch { viewModel.refresh() } }) {
                            Text("重试")
                        }
                    }
                }
            }
            is MarketUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (viewModel.selectedSubTab == 0) {
                        // 市场 tab
                        items(state.plugins, key = { it.plugin.id }) { marketPlugin ->
                            MarketPluginCard(marketPlugin = marketPlugin, onInstall = { viewModel.installPlugin(marketPlugin.plugin) })
                        }
                    } else {
                        // 已安装 tab
                        val installed = state.plugins.filter { it.isInstalled }
                        if (installed.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("暂无已安装插件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(installed, key = { it.plugin.id }) { marketPlugin ->
                                InstalledPluginCard(marketPlugin = marketPlugin, onUninstall = { viewModel.uninstallPlugin(marketPlugin.plugin) })
                            }
                        }
                    }
                }
            }
            MarketUiState.Idle -> Unit
            else -> Unit
        }
    }
}

// ── 市场插件卡片 ──

@Composable
private fun MarketPluginCard(
    marketPlugin: PluginMarketViewModel.MarketPluginUi,
    onInstall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(marketPlugin.plugin.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("${marketPlugin.plugin.author} · v${marketPlugin.plugin.version}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when {
                    marketPlugin.status.installed -> {
                        Icon(Icons.Default.CheckCircle, "已安装", tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    }
                    marketPlugin.status.updateAvailable -> {
                        Icon(Icons.Default.Download, "可更新", tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(marketPlugin.plugin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(marketPlugin.plugin.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 2.dp))
                Text(marketPlugin.plugin.dataType.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 2.dp))
            }
            if (marketPlugin.status.installing) {
                LinearProgressIndicator(
                    progress = 0.7f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 8.dp).progressSemantics(),
                )
            }
            TextButton(onClick = onInstall, enabled = !marketPlugin.status.installing) {
                Text(marketPlugin.buttonText)
            }
        }
    }
}

// ── 已安装插件卡片 ──

@Composable
private fun InstalledPluginCard(
    marketPlugin: PluginMarketViewModel.MarketPluginUi,
    onUninstall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(marketPlugin.plugin.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("v${marketPlugin.plugin.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onUninstall) {
                    Text("卸载")
                }
            }
        }
    }
}
