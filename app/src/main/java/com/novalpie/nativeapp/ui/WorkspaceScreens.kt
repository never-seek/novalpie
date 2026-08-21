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
    onToggleServerApi: (WorkspaceApiConfig) -> Unit,
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
            WorkspaceTab.Apis -> workspaceApiItems(
                state = state,
                onSaveApi = onSaveApi,
                onDeleteLocalApi = onDeleteLocalApi,
                onDeleteServerApi = onDeleteServerApi,
                onToggleServerApi = onToggleServerApi
            )
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
                    Text("工作区", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("管理 API、Cookie 与翻译任务", color = Color.White.copy(alpha = 0.78f))
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = Color.White) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { WorkspaceHeroStat("API", health?.apiStatus?.total ?: 0) }
                item { WorkspaceHeroStat("健康", health?.apiStatus?.healthy ?: 0) }
                item { WorkspaceHeroStat("Cookie", ((state.cookieConfigs as? LoadResult.Success)?.value?.myConfigs?.size ?: 0)) }
                item { WorkspaceHeroStat("任务", state.jobs.size) }
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
                Text("健康状态与统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("监控可用大模型与 Cookie 状态", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRefresh) { Text("刷新") }
        }
    }
    when (val health = state.health) {
        LoadResult.Idle, LoadResult.Loading -> item { WorkspaceLoading("正在检查服务状态") }
        is LoadResult.Error -> item { WorkspaceError(health.message, onRefresh) }
        is LoadResult.Success -> {
            item {
                val status = health.value.apiStatus
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceMetricCard("配置", status.total.toString(), Modifier.weight(1f))
                    WorkspaceMetricCard("激活", status.active.toString(), Modifier.weight(1f))
                    WorkspaceMetricCard("健康", status.healthy.toString(), Modifier.weight(1f))
                }
            }
            if (health.value.translators.isEmpty()) {
                item { WorkspaceEmpty("暂无翻译器健康数据") }
            } else {
                items(health.value.translators, key = { it.id }) { translator ->
                    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(translator.name, fontWeight = FontWeight.Bold)
                                WorkspaceStatusChip(if (translator.isHealthy && translator.isActive) "健康" else "异常", translator.isHealthy && translator.isActive)
                            }
                            Text(listOfNotNull(translator.model, translator.endpoint).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${translator.responseTimeMs} ms · ${translator.successRate}%", style = MaterialTheme.typography.labelMedium)
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
                    Text("上传新书", fontWeight = FontWeight.Bold)
                    Text("使用专业编辑器处理文本与 EPUB", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onOpenUpload) { Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(6.dp)); Text("打开") }
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
    onDeleteServerApi: (WorkspaceApiConfig) -> Unit,
    onToggleServerApi: (WorkspaceApiConfig) -> Unit
) {
    item { WorkspaceApiHeader(onSaveApi) }
    item { Text("本地 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (state.localApis.isEmpty()) item { WorkspaceEmpty("暂无本地 API，可以添加第一个 API") }
    items(state.localApis, key = { "local-${it.id}" }) { config ->
        WorkspaceLocalApiCard(config, onSaveApi, onDeleteLocalApi)
    }
    item { Text("服务器共享 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    when (val configs = state.apiConfigs) {
        LoadResult.Idle, LoadResult.Loading -> item { WorkspaceLoading("正在同步 API 配置") }
        is LoadResult.Error -> item { WorkspaceError(configs.message, null) }
        is LoadResult.Success -> {
            if (configs.value.isEmpty()) item { WorkspaceEmpty("暂无服务器共享 API") }
            items(configs.value, key = { "server-${it.id}" }) { config ->
                WorkspaceServerApiCard(config, onSaveApi, onDeleteServerApi, onToggleServerApi)
            }
        }
    }
}

@Composable
private fun WorkspaceApiHeader(onSaveApi: (WorkspaceApiDraft) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("API 管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("本地配置与服务器共享分开管理", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { showDialog = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("添加") }
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
    if (deleting) WorkspaceDeleteDialog("删除 API", "确定删除 ${config.name} 吗？", { deleting = false }) { deleting = false; onDelete(config) }
    WorkspaceApiCardBody(
        name = config.name,
        model = config.model,
        endpoint = config.endpoint,
        apiKey = config.apiKey,
        badges = listOf(if (config.sharedToServer) "已共享" else "仅本机", "并发 ${config.concurrency}"),
        onEdit = { editing = true },
        onDelete = { deleting = true }
    )
}

@Composable
private fun WorkspaceServerApiCard(
    config: WorkspaceApiConfig,
    onSave: (WorkspaceApiDraft) -> Unit,
    onDelete: (WorkspaceApiConfig) -> Unit,
    onToggle: (WorkspaceApiConfig) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var confirmingToggle by remember { mutableStateOf(false) }
    if (editing) WorkspaceApiDialog(
        WorkspaceApiDraft(serverId = config.id, name = config.name, model = config.model, endpoint = config.endpoint, apiKey = config.apiKey.orEmpty(), concurrency = config.concurrency.toString(), shareToServer = true),
        { editing = false }
    ) { editing = false; onSave(it) }
    if (deleting) WorkspaceDeleteDialog("删除共享 API", "该配置将从服务器删除。", { deleting = false }) { deleting = false; onDelete(config) }
    if (confirmingToggle) {
        val actionLabel = workspaceApiToggleActionLabel(config.isActive)
        AlertDialog(
            onDismissRequest = { confirmingToggle = false },
            title = { Text("${actionLabel}共享 API") },
            text = { Text("确定要${actionLabel} ${config.name} 吗？这会立即影响服务器上的翻译服务。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingToggle = false
                    onToggle(config)
                }) { Text(actionLabel) }
            },
            dismissButton = { TextButton(onClick = { confirmingToggle = false }) { Text("取消") } }
        )
    }
    val actualStatus = config.actualStatus?.takeIf(String::isNotBlank)
    val activationLabel = workspaceApiStatusLabel(config.activationStatus, config.isActive)
    WorkspaceApiCardBody(
        name = config.name,
        model = config.model,
        endpoint = config.endpoint,
        apiKey = config.apiKey,
        badges = buildList {
            add(activationLabel)
            actualStatus?.let { raw ->
                val actualLabel = workspaceApiStatusLabel(raw, config.isActive)
                if (actualLabel != activationLabel) add("实际 $actualLabel")
            }
            config.approvalStatus?.let(::add)
            add(if (config.isHealthy == true) "健康" else "未检测")
            add("${config.totalRequests} 次")
        },
        onEdit = { editing = true },
        onDelete = { deleting = true },
        statusActionLabel = workspaceApiToggleActionLabel(config.isActive),
        onStatusAction = { confirmingToggle = true }
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
    onDelete: () -> Unit,
    statusActionLabel: String? = null,
    onStatusAction: (() -> Unit)? = null
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(name, fontWeight = FontWeight.Bold) }
                Row { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "编辑") }; IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除") } }
            }
            Text("$model · ${maskWorkspaceApiKey(apiKey)}", style = MaterialTheme.typography.bodySmall)
            Text(endpoint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(badges) { AssistChip(onClick = {}, label = { Text(it) }) } }
            if (statusActionLabel != null && onStatusAction != null) {
                OutlinedButton(onClick = onStatusAction) { Text(statusActionLabel) }
            }
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
        LoadResult.Idle, LoadResult.Loading -> item { WorkspaceLoading("正在同步 Cookie 配置") }
        is LoadResult.Error -> item { WorkspaceError(configs.message, null) }
        is LoadResult.Success -> {
            item { Text("我的配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (configs.value.myConfigs.isEmpty()) item { WorkspaceEmpty("暂无 Cookie 配置") }
            items(configs.value.myConfigs, key = { "mine-${it.id}" }) { config ->
                WorkspaceCookieCard(config, editable = true, onSaveCookie, onToggleCookie, onDeleteCookie)
            }
            item { Text("其他共享配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (configs.value.sharedConfigs.isEmpty()) item { WorkspaceEmpty("暂无其他共享 Cookie") }
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
            Text("Cookie 管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if ((status as? LoadResult.Success)?.value?.hasCookie == true) "服务器已有可用 Cookie" else "尚未确认可用 Cookie", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { adding = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("添加") }
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
    if (deleting) WorkspaceDeleteDialog("删除 Cookie", "此操作不可撤销。", { deleting = false }) { deleting = false; onDelete(config) }
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(config.configKey, fontWeight = FontWeight.Bold) }
                WorkspaceStatusChip(if (config.isHealthy == true) "健康" else if (config.isHealthy == false) "异常" else "未检测", config.isHealthy == true)
            }
            config.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(config.proxyIp ?: "无代理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("提供人: ${config.updatedByUsername ?: "我"} · ${config.lastCheckAt ?: "未检测"}", style = MaterialTheme.typography.labelSmall)
            if (editable) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editing = true }) { Text("编辑") }
                OutlinedButton(onClick = { onToggle(config) }) { Text(if (config.isActive) "禁用" else "启用") }
                TextButton(onClick = { deleting = true }) { Text("删除") }
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
            Column { Text("任务队列", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("本机翻译任务与进度", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedButton(onClick = onOpenUpload) { Text("上传新书") }
        }
    }
    if (jobs.isEmpty()) item { WorkspaceEmpty("任务队列为空") }
    items(jobs, key = { it.id }) { job ->
        ElevatedCard(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(job.bookTitle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); WorkspaceStatusChip(job.status, job.status == "completed") }
                Text("${job.translatorName} · ${job.completedChapters}/${job.chapterCount} 章", style = MaterialTheme.typography.bodySmall)
                if (job.chapterCount > 0) LinearProgressIndicator(progress = { (job.completedChapters.toFloat() / job.chapterCount).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (job.status == "paused") Button(onClick = { onUpdateStatus(job, "pending") }) { Icon(Icons.Filled.PlayArrow, null); Text("继续") }
                    else if (job.status != "completed") OutlinedButton(onClick = { onUpdateStatus(job, "paused") }) { Icon(Icons.Filled.Pause, null); Text("暂停") }
                    TextButton(onClick = { onDelete(job) }) { Text("删除") }
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
        title = { Text(if (initial.id == null && initial.serverId == null) "添加 API" else "编辑 API") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, label = { Text("API 名称") }, singleLine = true)
                OutlinedTextField(draft.model, { draft = draft.copy(model = it) }, label = { Text("模型") }, singleLine = true)
                OutlinedTextField(draft.endpoint, { draft = draft.copy(endpoint = it) }, label = { Text("API 端点") }, singleLine = true)
                OutlinedTextField(draft.apiKey, { draft = draft.copy(apiKey = it) }, label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(draft.concurrency, { draft = draft.copy(concurrency = it) }, label = { Text("并发数") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("共享到服务器", fontWeight = FontWeight.SemiBold); Text("关闭时只保存在本机", style = MaterialTheme.typography.bodySmall) }; Switch(draft.shareToServer, { draft = draft.copy(shareToServer = it) }) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { val validation = validateWorkspaceApiDraft(draft); if (validation == null) onSave(draft) else error = validation }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WorkspaceCookieDialog(initial: WorkspaceCookieDraft, onDismiss: () -> Unit, onSave: (WorkspaceCookieDraft) -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "添加 Cookie" else "编辑 Cookie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.configKey, { draft = draft.copy(configKey = it) }, label = { Text("配置键名") }, enabled = initial.id == null, singleLine = true)
                OutlinedTextField(draft.description, { draft = draft.copy(description = it) }, label = { Text("配置说明") })
                OutlinedTextField(draft.cookieRaw, { draft = draft.copy(cookieRaw = it) }, label = { Text(if (initial.id == null) "Cookie 内容" else "Cookie 内容（留空表示不修改）") }, minLines = 3)
                OutlinedTextField(draft.proxyIp, { draft = draft.copy(proxyIp = it) }, label = { Text("代理配置") }, supportingText = { Text("IP:PORT 或 http(s)://user:pass@host:port") })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("启用此配置"); Switch(draft.isActive, { draft = draft.copy(isActive = it) }) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { val validation = validateWorkspaceCookieDraft(draft); if (validation == null) onSave(draft) else error = validation }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WorkspaceDeleteDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onConfirm) { Text("删除") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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
private fun WorkspaceError(message: String, onRetry: (() -> Unit)?) { ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(message); onRetry?.let { OutlinedButton(onClick = it) { Text("重试") } } } } }

@Composable
private fun WorkspaceEmpty(label: String) { Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = RoundedCornerShape(16.dp)) { Text(label, modifier = Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun WorkspaceNotice(message: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) { Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
