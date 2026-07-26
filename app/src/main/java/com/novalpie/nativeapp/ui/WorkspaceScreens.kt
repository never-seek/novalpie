@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.novalpie.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.WorkspaceApiConfig
import com.novalpie.nativeapp.model.WorkspaceCookieConfig
import com.novalpie.nativeapp.model.WorkspaceCookieConfigs
import com.novalpie.nativeapp.model.WorkspaceCookieStatus
import com.novalpie.nativeapp.model.WorkspaceHealth
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import com.novalpie.nativeapp.model.WorkspaceTranslationJob

@Composable
internal fun WorkspaceScreen(
    state: WorkspaceState,
    onRefresh: () -> Unit,
    onTabSelected: (WorkspaceTab) -> Unit,
    onSaveApi: (WorkspaceApiDraft) -> Unit,
    onDeleteLocalApi: (WorkspaceLocalApiConfig) -> Unit,
    onDeleteServerApi: (WorkspaceApiConfig) -> Unit,
    onSaveCookie: (WorkspaceCookieDraft) -> Unit,
    onToggleCookie: (WorkspaceCookieConfig) -> Unit,
    onDeleteCookie: (WorkspaceCookieConfig) -> Unit,
    onUpdateJobStatus: (WorkspaceTranslationJob, String) -> Unit,
    onDeleteJob: (WorkspaceTranslationJob) -> Unit,
    onOpenUpload: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WorkspaceHero(state, onRefresh) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WorkspaceTab.values().toList()) { tab ->
                    FilterChip(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
        state.actionMessage?.let { item { WorkspaceNotice(it) } }

        when (state.selectedTab) {
            WorkspaceTab.Overview -> workspaceOverviewItems(state, onRefresh, onOpenUpload)
            WorkspaceTab.Apis -> workspaceApiItems(state, onSaveApi, onDeleteLocalApi, onDeleteServerApi)
            WorkspaceTab.Cookies -> workspaceCookieItems(state, onSaveCookie, onToggleCookie, onDeleteCookie)
            WorkspaceTab.Queue -> workspaceQueueItems(state.jobs, onUpdateJobStatus, onDeleteJob, onOpenUpload)
        }
    }
}

@Composable
private fun WorkspaceHero(state: WorkspaceState, onRefresh: () -> Unit) {
    val health = (state.health as? LoadResult.Success)?.value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0F172A), MaterialTheme.colorScheme.primary)))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("\u5de5\u4f5c\u533a", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("\u7ba1\u7406 API\u3001Cookie \u4e0e\u7ffb\u8bd1\u4efb\u52a1", color = Color.White.copy(alpha = 0.78f))
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "\u5237\u65b0", tint = Color.White) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { WorkspaceHeroStat("API", health?.apiStatus?.total ?: 0) }
                item { WorkspaceHeroStat("\u5065\u5eb7", health?.apiStatus?.healthy ?: 0) }
                item { WorkspaceHeroStat("Cookie", ((state.cookieConfigs as? LoadResult.Success)?.value?.myConfigs?.size ?: 0)) }
                item { WorkspaceHeroStat("\u4efb\u52a1", state.jobs.size) }
            }
        }
    }
}

@Composable
private fun WorkspaceHeroStat(label: String, value: Int) {
    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workspaceOverviewItems(
    state: WorkspaceState,
    onRefresh: () -> Unit,
    onOpenUpload: () -> Unit
) {
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("\u5065\u5eb7\u72b6\u6001\u4e0e\u7edf\u8ba1", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("\u76d1\u63a7\u53ef\u7528\u5927\u6a21\u578b\u4e0e Cookie \u72b6\u6001", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRefresh) { Text("\u5237\u65b0") }
        }
    }
    when (val health = state.health) {
        LoadResult.Idle, LoadResult.Loading -> item { WorkspaceLoading("\u6b63\u5728\u68c0\u67e5\u670d\u52a1\u72b6\u6001") }
        is LoadResult.Error -> item { WorkspaceError(health.message, onRefresh) }
        is LoadResult.Success -> {
            item {
                val status = health.value.apiStatus
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceMetricCard("\u914d\u7f6e", status.total.toString(), Modifier.weight(1f))
                    WorkspaceMetricCard("\u6fc0\u6d3b", status.active.toString(), Modifier.weight(1f))
                    WorkspaceMetricCard("\u5065\u5eb7", status.healthy.toString(), Modifier.weight(1f))
                }
            }
            if (health.value.translators.isEmpty()) {
                item { WorkspaceEmpty("\u6682\u65e0\u7ffb\u8bd1\u5668\u5065\u5eb7\u6570\u636e") }
            } else {
                items(health.value.translators, key = { it.id }) { translator ->
                    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(translator.name, fontWeight = FontWeight.Bold)
                                WorkspaceStatusChip(if (translator.isHealthy && translator.isActive) "\u5065\u5eb7" else "\u5f02\u5e38", translator.isHealthy && translator.isActive)
                            }
                            Text(listOfNotNull(translator.model, translator.endpoint).joinToString(" \u00b7 "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${translator.responseTimeMs} ms \u00b7 ${translator.successRate}%", style = MaterialTheme.typography.labelMedium)
                            translator.lastHealthError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
    item {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("\u4e0a\u4f20\u65b0\u4e66", fontWeight = FontWeight.Bold)
                    Text("\u4f7f\u7528\u4e13\u4e1a\u7f16\u8f91\u5668\u5904\u7406\u6587\u672c\u4e0e EPUB", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onOpenUpload) { Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(6.dp)); Text("\u6253\u5f00") }
            }
        }
    }
}

@Composable
private fun WorkspaceMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workspaceApiItems(
    state: WorkspaceState,
    onSaveApi: (WorkspaceApiDraft) -> Unit,
    onDeleteLocalApi: (WorkspaceLocalApiConfig) -> Unit,
    onDeleteServerApi: (WorkspaceApiConfig) -> Unit
) {
    item { WorkspaceApiHeader(onSaveApi) }
    item { Text("\u672c\u5730 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (state.localApis.isEmpty()) item { WorkspaceEmpty("\u6682\u65e0\u672c\u5730 API\uff0c\u53ef\u4ee5\u6dfb\u52a0\u7b2c\u4e00\u4e2a API") }
    items(state.localApis, key = { "local-${it.id}" }) { config ->
        WorkspaceLocalApiCard(config, onSaveApi, onDeleteLocalApi)
    }
    item { Text("\u670d\u52a1\u5668\u5171\u4eab API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    when (val configs = state.apiConfigs) {
        LoadResult.Idle, LoadResult.Loading -> item { WorkspaceLoading("\u6b63\u5728\u540c\u6b65 API \u914d\u7f6e") }
        is LoadResult.Error -> item { WorkspaceError(configs.message, null) }
        is LoadResult.Success -> {
            if (configs.value.isEmpty()) item { WorkspaceEmpty("\u6682\u65e0\u670d\u52a1\u5668\u5171\u4eab API") }
            items(configs.value, key = { "server-${it.id}" }) { config ->
                WorkspaceServerApiCard(config, onSaveApi, onDeleteServerApi)
            }
        }
    }
}

@Composable
private fun WorkspaceApiHeader(onSaveApi: (WorkspaceApiDraft) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("API \u7ba1\u7406", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("\u672c\u5730\u914d\u7f6e\u4e0e\u670d\u52a1\u5668\u5171\u4eab\u5206\u5f00\u7ba1\u7406", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { showDialog = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("\u6dfb\u52a0") }
    }
    if (showDialog) WorkspaceApiDialog(WorkspaceApiDraft(), { showDialog = false }) { draft -> showDialog = false; onSaveApi(draft) }
}

@Composable
private fun WorkspaceLocalApiCard(
    config: WorkspaceLocalApiConfig,
    onSave: (WorkspaceApiDraft) -> Unit,
    onDelete: (WorkspaceLocalApiConfig) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    if (editing) WorkspaceApiDialog(
        WorkspaceApiDraft(config.id, config.serverId, config.name, config.model, config.endpoint, config.apiKey, config.concurrency.toString(), config.sharedToServer),
        { editing = false }
    ) { editing = false; onSave(it) }
    if (deleting) WorkspaceDeleteDialog("\u5220\u9664 API", "\u786e\u5b9a\u5220\u9664 ${config.name} \u5417\uff1f", { deleting = false }) { deleting = false; onDelete(config) }
    WorkspaceApiCardBody(
        name = config.name,
        model = config.model,
        endpoint = config.endpoint,
        apiKey = config.apiKey,
        badges = listOf(if (config.sharedToServer) "\u5df2\u5171\u4eab" else "\u4ec5\u672c\u673a", "\u5e76\u53d1 ${config.concurrency}"),
        onEdit = { editing = true },
        onDelete = { deleting = true }
    )
}

@Composable
private fun WorkspaceServerApiCard(
    config: WorkspaceApiConfig,
    onSave: (WorkspaceApiDraft) -> Unit,
    onDelete: (WorkspaceApiConfig) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    if (editing) WorkspaceApiDialog(
        WorkspaceApiDraft(serverId = config.id, name = config.name, model = config.model, endpoint = config.endpoint, apiKey = config.apiKey.orEmpty(), concurrency = config.concurrency.toString(), shareToServer = true),
        { editing = false }
    ) { editing = false; onSave(it) }
    if (deleting) WorkspaceDeleteDialog("\u5220\u9664\u5171\u4eab API", "\u8be5\u914d\u7f6e\u5c06\u4ece\u670d\u52a1\u5668\u5220\u9664\u3002", { deleting = false }) { deleting = false; onDelete(config) }
    WorkspaceApiCardBody(
        name = config.name,
        model = config.model,
        endpoint = config.endpoint,
        apiKey = config.apiKey,
        badges = listOfNotNull(config.approvalStatus, if (config.isHealthy == true) "\u5065\u5eb7" else "\u672a\u68c0\u6d4b", "${config.totalRequests} \u6b21"),
        onEdit = { editing = true },
        onDelete = { deleting = true }
    )
}

@Composable
private fun WorkspaceApiCardBody(
    name: String,
    model: String,
    endpoint: String,
    apiKey: String?,
    badges: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(name, fontWeight = FontWeight.Bold) }
                Row { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "\u7f16\u8f91") }; IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "\u5220\u9664") } }
            }
            Text("$model \u00b7 ${maskWorkspaceApiKey(apiKey)}", style = MaterialTheme.typography.bodySmall)
            Text(endpoint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(badges) { AssistChip(onClick = {}, label = { Text(it) }) } }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workspaceCookieItems(
    state: WorkspaceState,
    onSaveCookie: (WorkspaceCookieDraft) -> Unit,
    onToggleCookie: (WorkspaceCookieConfig) -> Unit,
    onDeleteCookie: (WorkspaceCookieConfig) -> Unit
) {
    item { WorkspaceCookieHeader(state.cookieStatus, onSaveCookie) }
    when (val configs = state.cookieConfigs) {
        LoadResult.Idle, LoadResult.Loading -> item { WorkspaceLoading("\u6b63\u5728\u540c\u6b65 Cookie \u914d\u7f6e") }
        is LoadResult.Error -> item { WorkspaceError(configs.message, null) }
        is LoadResult.Success -> {
            item { Text("\u6211\u7684\u914d\u7f6e", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (configs.value.myConfigs.isEmpty()) item { WorkspaceEmpty("\u6682\u65e0 Cookie \u914d\u7f6e") }
            items(configs.value.myConfigs, key = { "mine-${it.id}" }) { config ->
                WorkspaceCookieCard(config, editable = true, onSaveCookie, onToggleCookie, onDeleteCookie)
            }
            item { Text("\u5176\u4ed6\u5171\u4eab\u914d\u7f6e", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (configs.value.sharedConfigs.isEmpty()) item { WorkspaceEmpty("\u6682\u65e0\u5176\u4ed6\u5171\u4eab Cookie") }
            items(configs.value.sharedConfigs, key = { "shared-${it.id}" }) { config ->
                WorkspaceCookieCard(config, editable = false, onSaveCookie, onToggleCookie, onDeleteCookie)
            }
        }
    }
}

@Composable
private fun WorkspaceCookieHeader(status: LoadResult<WorkspaceCookieStatus>, onSave: (WorkspaceCookieDraft) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    if (adding) WorkspaceCookieDialog(WorkspaceCookieDraft(), { adding = false }) { adding = false; onSave(it) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Cookie \u7ba1\u7406", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if ((status as? LoadResult.Success)?.value?.hasCookie == true) "\u670d\u52a1\u5668\u5df2\u6709\u53ef\u7528 Cookie" else "\u5c1a\u672a\u786e\u8ba4\u53ef\u7528 Cookie", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { adding = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("\u6dfb\u52a0") }
    }
}

@Composable
private fun WorkspaceCookieCard(
    config: WorkspaceCookieConfig,
    editable: Boolean,
    onSave: (WorkspaceCookieDraft) -> Unit,
    onToggle: (WorkspaceCookieConfig) -> Unit,
    onDelete: (WorkspaceCookieConfig) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    if (editing) WorkspaceCookieDialog(
        WorkspaceCookieDraft(config.id, config.configKey, config.description.orEmpty(), "", config.proxyIp.orEmpty(), config.isActive),
        { editing = false }
    ) { editing = false; onSave(it) }
    if (deleting) WorkspaceDeleteDialog("\u5220\u9664 Cookie", "\u6b64\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500\u3002", { deleting = false }) { deleting = false; onDelete(config) }
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(config.configKey, fontWeight = FontWeight.Bold) }
                WorkspaceStatusChip(if (config.isHealthy == true) "\u5065\u5eb7" else if (config.isHealthy == false) "\u5f02\u5e38" else "\u672a\u68c0\u6d4b", config.isHealthy == true)
            }
            config.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(config.proxyIp ?: "\u65e0\u4ee3\u7406", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("\u63d0\u4f9b\u4eba: ${config.updatedByUsername ?: "\u6211"} \u00b7 ${config.lastCheckAt ?: "\u672a\u68c0\u6d4b"}", style = MaterialTheme.typography.labelSmall)
            if (editable) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editing = true }) { Text("\u7f16\u8f91") }
                OutlinedButton(onClick = { onToggle(config) }) { Text(if (config.isActive) "\u7981\u7528" else "\u542f\u7528") }
                TextButton(onClick = { deleting = true }) { Text("\u5220\u9664") }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workspaceQueueItems(
    jobs: List<WorkspaceTranslationJob>,
    onUpdateStatus: (WorkspaceTranslationJob, String) -> Unit,
    onDelete: (WorkspaceTranslationJob) -> Unit,
    onOpenUpload: () -> Unit
) {
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("\u4efb\u52a1\u961f\u5217", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("\u672c\u673a\u7ffb\u8bd1\u4efb\u52a1\u4e0e\u8fdb\u5ea6", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedButton(onClick = onOpenUpload) { Text("\u4e0a\u4f20\u65b0\u4e66") }
        }
    }
    if (jobs.isEmpty()) item { WorkspaceEmpty("\u4efb\u52a1\u961f\u5217\u4e3a\u7a7a") }
    items(jobs, key = { it.id }) { job ->
        ElevatedCard(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(job.bookTitle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); WorkspaceStatusChip(job.status, job.status == "completed") }
                Text("${job.translatorName} \u00b7 ${job.completedChapters}/${job.chapterCount} \u7ae0", style = MaterialTheme.typography.bodySmall)
                if (job.chapterCount > 0) LinearProgressIndicator(progress = { (job.completedChapters.toFloat() / job.chapterCount).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (job.status == "paused") Button(onClick = { onUpdateStatus(job, "pending") }) { Icon(Icons.Filled.PlayArrow, null); Text("\u7ee7\u7eed") }
                    else if (job.status != "completed") OutlinedButton(onClick = { onUpdateStatus(job, "paused") }) { Icon(Icons.Filled.Pause, null); Text("\u6682\u505c") }
                    TextButton(onClick = { onDelete(job) }) { Text("\u5220\u9664") }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceApiDialog(initial: WorkspaceApiDraft, onDismiss: () -> Unit, onSave: (WorkspaceApiDraft) -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null && initial.serverId == null) "\u6dfb\u52a0 API" else "\u7f16\u8f91 API") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, label = { Text("API \u540d\u79f0") }, singleLine = true)
                OutlinedTextField(draft.model, { draft = draft.copy(model = it) }, label = { Text("\u6a21\u578b") }, singleLine = true)
                OutlinedTextField(draft.endpoint, { draft = draft.copy(endpoint = it) }, label = { Text("API \u7aef\u70b9") }, singleLine = true)
                OutlinedTextField(draft.apiKey, { draft = draft.copy(apiKey = it) }, label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(draft.concurrency, { draft = draft.copy(concurrency = it) }, label = { Text("\u5e76\u53d1\u6570") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("\u5171\u4eab\u5230\u670d\u52a1\u5668", fontWeight = FontWeight.SemiBold); Text("\u5173\u95ed\u65f6\u53ea\u4fdd\u5b58\u5728\u672c\u673a", style = MaterialTheme.typography.bodySmall) }; Switch(draft.shareToServer, { draft = draft.copy(shareToServer = it) }) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { val validation = validateWorkspaceApiDraft(draft); if (validation == null) onSave(draft) else error = validation }) { Text("\u4fdd\u5b58") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") } }
    )
}

@Composable
private fun WorkspaceCookieDialog(initial: WorkspaceCookieDraft, onDismiss: () -> Unit, onSave: (WorkspaceCookieDraft) -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "\u6dfb\u52a0 Cookie" else "\u7f16\u8f91 Cookie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.configKey, { draft = draft.copy(configKey = it) }, label = { Text("\u914d\u7f6e\u952e\u540d") }, enabled = initial.id == null, singleLine = true)
                OutlinedTextField(draft.description, { draft = draft.copy(description = it) }, label = { Text("\u914d\u7f6e\u8bf4\u660e") })
                OutlinedTextField(draft.cookieRaw, { draft = draft.copy(cookieRaw = it) }, label = { Text(if (initial.id == null) "Cookie \u5185\u5bb9" else "Cookie \u5185\u5bb9\uff08\u7559\u7a7a\u8868\u793a\u4e0d\u4fee\u6539\uff09") }, minLines = 3)
                OutlinedTextField(draft.proxyIp, { draft = draft.copy(proxyIp = it) }, label = { Text("\u4ee3\u7406\u914d\u7f6e") }, supportingText = { Text("IP:PORT \u6216 http(s)://user:pass@host:port") })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("\u542f\u7528\u6b64\u914d\u7f6e"); Switch(draft.isActive, { draft = draft.copy(isActive = it) }) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { val validation = validateWorkspaceCookieDraft(draft); if (validation == null) onSave(draft) else error = validation }) { Text("\u4fdd\u5b58") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") } }
    )
}

@Composable
private fun WorkspaceDeleteDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onConfirm) { Text("\u5220\u9664") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") } })
}

@Composable
private fun WorkspaceStatusChip(label: String, good: Boolean) {
    Surface(color = if (good) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = if (good) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkspaceLoading(label: String) { Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun WorkspaceError(message: String, onRetry: (() -> Unit)?) { ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(message); onRetry?.let { OutlinedButton(onClick = it) { Text("\u91cd\u8bd5") } } } } }

@Composable
private fun WorkspaceEmpty(label: String) { Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = RoundedCornerShape(16.dp)) { Text(label, modifier = Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun WorkspaceNotice(message: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) { Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
