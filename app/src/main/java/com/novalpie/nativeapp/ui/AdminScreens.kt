package com.novalpie.nativeapp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.AdminBaseUrlRule
import com.novalpie.nativeapp.model.AdminCookieConfig
import com.novalpie.nativeapp.model.AdminKeyItem
import com.novalpie.nativeapp.model.AdminReviewRequest
import com.novalpie.nativeapp.model.AdminShopItem
import com.novalpie.nativeapp.model.LoadResult

private data class AdminConfirmation(
    val title: String,
    val message: String,
    val destructive: Boolean = false,
    val action: () -> Unit
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AdminScreen(
    state: AdminState,
    onRefresh: () -> Unit,
    onSectionSelected: (AdminSection) -> Unit,
    onReviewQueryChange: (AdminReviewQuery) -> Unit,
    onApplyReviewQuery: () -> Unit,
    onResetReviewQuery: () -> Unit,
    onOperationLogQueryChange: (AdminOperationLogQuery) -> Unit,
    onApplyOperationLogQuery: () -> Unit,
    onResetOperationLogQuery: () -> Unit,
    onToggleReviewSetting: (String) -> Unit,
    onReviewAction: (Long, String) -> Unit,
    onUpdateKeyStatus: (Long, String) -> Unit,
    onDeleteKey: (Long) -> Unit,
    onSaveCookie: (AdminCookieConfig, String?) -> Unit,
    onToggleCookie: (AdminCookieConfig) -> Unit,
    onDeleteCookie: (Long) -> Unit,
    onSaveRule: (AdminBaseUrlRule) -> Unit,
    onSetRuleAction: (AdminBaseUrlRule, String) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onSaveShopItem: (AdminShopItem) -> Unit,
    onToggleShopItem: (AdminShopItem) -> Unit,
    onDeleteShopItem: (Long) -> Unit
) {
    var confirmation by remember { mutableStateOf<AdminConfirmation?>(null) }
    var cookieEditor by remember { mutableStateOf<AdminCookieConfig?>(null) }
    var ruleEditor by remember { mutableStateOf<AdminBaseUrlRule?>(null) }
    var shopEditor by remember { mutableStateOf<AdminShopItem?>(null) }
    confirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(pending.title) },
            text = { Text(pending.message) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmation = null
                        pending.action()
                    }
                ) { Text(if (pending.destructive) "确认删除" else "确认") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("取消") } }
        )
    }
    cookieEditor?.let { initial ->
        AdminCookieEditorDialog(
            initial = initial,
            onDismiss = { cookieEditor = null },
            onSave = { config, raw ->
                cookieEditor = null
                onSaveCookie(config, raw)
            }
        )
    }
    ruleEditor?.let { initial ->
        AdminRuleEditorDialog(
            initial = initial,
            onDismiss = { ruleEditor = null },
            onSave = { rule ->
                ruleEditor = null
                onSaveRule(rule)
            }
        )
    }
    shopEditor?.let { initial ->
        AdminShopEditorDialog(
            initial = initial,
            onDismiss = { shopEditor = null },
            onSave = { item ->
                shopEditor = null
                onSaveShopItem(item)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("管理后台", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "仅管理员可访问 · 数据与操作来自实时网站接口",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdminSection.values()) { section ->
                    FilterChip(
                        selected = state.section == section,
                        onClick = { onSectionSelected(section) },
                        label = { Text(adminSectionLabel(section)) }
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(adminSectionLabel(state.section), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onRefresh, enabled = !state.actionLoading) { Text("刷新") }
            }
        }
        state.actionMessage?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) { Text(message, modifier = Modifier.padding(12.dp)) }
            }
        }

        when (state.section) {
            AdminSection.Overview -> adminOverviewItems(state)
            AdminSection.Review -> adminReviewItems(
                state,
                onQueryChange = onReviewQueryChange,
                onApplyQuery = onApplyReviewQuery,
                onResetQuery = onResetReviewQuery,
                onToggle = { kind ->
                    confirmation = AdminConfirmation(
                        title = "修改审核设置",
                        message = "确认切换${if (kind == "upload") "上传" else "删除"}请求的自动审核设置？"
                    ) { onToggleReviewSetting(kind) }
                },
                onAction = { request, action ->
                    confirmation = AdminConfirmation(
                        title = if (action == "approve") "通过审核" else "拒绝审核",
                        message = "确认${if (action == "approve") "通过" else "拒绝"}请求 #${request.id}？"
                    ) { onReviewAction(request.id, action) }
                }
            )
            AdminSection.Keys -> adminKeyItems(
                state,
                onStatus = { key, status ->
                    confirmation = AdminConfirmation("修改 Key 状态", "确认将 ${key.name} 设为 $status？") {
                        onUpdateKeyStatus(key.id, status)
                    }
                },
                onDelete = { key ->
                    confirmation = AdminConfirmation(
                        "删除 Key",
                        "确认永久删除 ${key.name}？此操作不可撤销。",
                        destructive = true
                    ) { onDeleteKey(key.id) }
                }
            )
            AdminSection.OperationLogs -> adminOperationLogItems(
                state,
                onQueryChange = onOperationLogQueryChange,
                onApplyQuery = onApplyOperationLogQuery,
                onResetQuery = onResetOperationLogQuery
            )
            AdminSection.Scraper -> adminScraperItems(
                state,
                onAddCookie = { cookieEditor = AdminCookieConfig(0, "", isActive = true) },
                onEditCookie = { cookieEditor = it },
                onToggleCookie = { config ->
                    confirmation = AdminConfirmation("修改 Cookie 状态", "确认${if (config.isActive) "停用" else "启用"} ${config.configKey}？") {
                        onToggleCookie(config)
                    }
                },
                onDeleteCookie = { config ->
                    confirmation = AdminConfirmation("删除 Cookie 配置", "确认删除 ${config.configKey}？", true) {
                        onDeleteCookie(config.id)
                    }
                },
                onAddRule = { ruleEditor = AdminBaseUrlRule(0, "", "manual") },
                onEditRule = { ruleEditor = it },
                onRuleAction = { rule, action ->
                    confirmation = AdminConfirmation("修改 BaseURL 规则", "确认将 ${rule.pattern} 设为 $action？") {
                        onSetRuleAction(rule, action)
                    }
                },
                onDeleteRule = { rule ->
                    confirmation = AdminConfirmation("删除 BaseURL 规则", "确认删除 ${rule.pattern}？", true) {
                        onDeleteRule(rule.id)
                    }
                }
            )
            AdminSection.Shop -> adminShopItems(
                state,
                onAdd = { shopEditor = AdminShopItem(0, "", price = 0, type = "frame", isActive = true) },
                onEdit = { shopEditor = it },
                onToggle = { item ->
                    confirmation = AdminConfirmation("修改商品状态", "确认${if (item.isActive) "下架" else "上架"} ${item.name}？") {
                        onToggleShopItem(item)
                    }
                },
                onDelete = { item ->
                    confirmation = AdminConfirmation("删除商品", "确认删除 ${item.name}？", true) {
                        onDeleteShopItem(item.id)
                    }
                }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminOverviewItems(state: AdminState) {
    when (val value = state.overview) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载管理总览") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> {
            val stats = value.value
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMetricCard("待审核", stats.pendingReviewTotal.toString(), Modifier.weight(1f))
                    AdminMetricCard("作品", stats.activeNovelTotal.toString(), Modifier.weight(1f))
                    AdminMetricCard("用户", stats.registeredUserTotal.toString(), Modifier.weight(1f))
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("近期注册", fontWeight = FontWeight.Bold)
                        stats.recentUserDaily.forEach { day ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(day.date)
                                Text(day.count.toString(), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminReviewItems(
    state: AdminState,
    onQueryChange: (AdminReviewQuery) -> Unit,
    onApplyQuery: () -> Unit,
    onResetQuery: () -> Unit,
    onToggle: (String) -> Unit,
    onAction: (AdminReviewRequest, String) -> Unit
) {
    item {
        when (val value = state.reviewSettings) {
            LoadResult.Idle, LoadResult.Loading -> AdminStatusCard("正在加载审核设置")
            is LoadResult.Error -> AdminStatusCard(value.message)
            is LoadResult.Success -> ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("自动审核", fontWeight = FontWeight.Bold)
                    AdminSwitchRow("自动通过上传", value.value.autoApproveUpload) { onToggle("upload") }
                    AdminSwitchRow("自动通过删除", value.value.autoApproveDelete) { onToggle("delete") }
                }
            }
        }
    }
    item {
        AdminReviewFilterCard(
            query = state.reviewQuery,
            enabled = !state.actionLoading,
            onQueryChange = onQueryChange,
            onApply = onApplyQuery,
            onReset = onResetQuery
        )
    }
    when (val value = state.reviewRequests) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载审核请求") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> {
            if (value.value.isEmpty()) item { AdminStatusCard("暂无审核请求") }
            items(value.value, key = { it.id }) { request ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(request.title ?: "请求 #${request.id}", fontWeight = FontWeight.Bold)
                        Text("${request.type} · ${request.status} · ${request.username ?: "未知用户"}")
                        request.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (request.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onAction(request, "approve") }, enabled = !state.actionLoading) { Text("通过") }
                                OutlinedButton(onClick = { onAction(request, "reject") }, enabled = !state.actionLoading) { Text("拒绝") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReviewFilterCard(
    query: AdminReviewQuery,
    enabled: Boolean,
    onQueryChange: (AdminReviewQuery) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("筛选审核请求", fontWeight = FontWeight.Bold)
            AdminStringFilterRail(
                label = "类型",
                options = adminReviewTypeOptions(),
                selected = query.type,
                onSelected = { onQueryChange(query.copy(type = it)) }
            )
            AdminStringFilterRail(
                label = "状态",
                options = adminReviewStatusOptions(),
                selected = query.status,
                onSelected = { onQueryChange(query.copy(status = it)) }
            )
            OutlinedTextField(
                value = query.keyword,
                onValueChange = { onQueryChange(query.copy(keyword = it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("关键词") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, enabled = enabled) { Text("应用筛选") }
                TextButton(onClick = onReset, enabled = enabled && query != AdminReviewQuery()) { Text("清空") }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminKeyItems(
    state: AdminState,
    onStatus: (AdminKeyItem, String) -> Unit,
    onDelete: (AdminKeyItem) -> Unit
) {
    when (val value = state.keys) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载 Key") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> {
            if (value.value.isEmpty()) item { AdminStatusCard("暂无 Key") }
            items(value.value, key = { it.id }) { key ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(key.name, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(key.providerName, key.model, key.approvalStatus).joinToString(" · "))
                        key.baseUrl?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("pending", "approved", "rejected").forEach { status ->
                                OutlinedButton(onClick = { onStatus(key, status) }, enabled = !state.actionLoading && key.approvalStatus != status) {
                                    Text(status)
                                }
                            }
                            TextButton(onClick = { onDelete(key) }, enabled = !state.actionLoading) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminOperationLogItems(
    state: AdminState,
    onQueryChange: (AdminOperationLogQuery) -> Unit,
    onApplyQuery: () -> Unit,
    onResetQuery: () -> Unit
) {
    item {
        AdminOperationLogFilterCard(
            query = state.operationLogQuery,
            actionTypes = (state.operationLogs as? LoadResult.Success)?.value?.actionTypes.orEmpty(),
            enabled = !state.actionLoading,
            onQueryChange = onQueryChange,
            onApply = onApplyQuery,
            onReset = onResetQuery
        )
    }
    when (val value = state.operationLogs) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载操作日志") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> {
            item { Text("共 ${value.value.total} 条 · ${value.value.totalPages} 页", style = MaterialTheme.typography.bodySmall) }
            items(value.value.items, key = { it.id }) { log ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.action, fontWeight = FontWeight.Bold)
                            Text(log.status, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("用户 ${log.userId ?: "-"} · 作品 ${log.novelId ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        log.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        log.createdAt?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOperationLogFilterCard(
    query: AdminOperationLogQuery,
    actionTypes: List<String>,
    enabled: Boolean,
    onQueryChange: (AdminOperationLogQuery) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    val actionOptions = adminOperationActionOptions(actionTypes)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("筛选操作日志", fontWeight = FontWeight.Bold)
            AdminStringFilterRail(
                label = "操作",
                options = actionOptions,
                selected = query.action,
                onSelected = { onQueryChange(query.copy(action = it, page = 1)) }
            )
            AdminStringFilterRail(
                label = "状态",
                options = adminOperationStatusOptions(),
                selected = query.status,
                onSelected = { onQueryChange(query.copy(status = it, page = 1)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query.userId,
                    onValueChange = { onQueryChange(query.copy(userId = it.filter(Char::isDigit), page = 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("用户 ID") }
                )
                OutlinedTextField(
                    value = query.novelId,
                    onValueChange = { onQueryChange(query.copy(novelId = it.filter(Char::isDigit), page = 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("作品 ID") }
                )
            }
            OutlinedTextField(
                value = query.keyword,
                onValueChange = { onQueryChange(query.copy(keyword = it, page = 1)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("关键词") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query.startDate,
                    onValueChange = { onQueryChange(query.copy(startDate = it, page = 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("开始日期") }
                )
                OutlinedTextField(
                    value = query.endDate,
                    onValueChange = { onQueryChange(query.copy(endDate = it, page = 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("结束日期") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, enabled = enabled) { Text("应用筛选") }
                TextButton(onClick = onReset, enabled = enabled && query != AdminOperationLogQuery()) { Text("清空") }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminScraperItems(
    state: AdminState,
    onAddCookie: () -> Unit,
    onEditCookie: (AdminCookieConfig) -> Unit,
    onToggleCookie: (AdminCookieConfig) -> Unit,
    onDeleteCookie: (AdminCookieConfig) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (AdminBaseUrlRule) -> Unit,
    onRuleAction: (AdminBaseUrlRule, String) -> Unit,
    onDeleteRule: (AdminBaseUrlRule) -> Unit
) {
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cookie 配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onAddCookie) { Text("新增") }
        }
    }
    when (val value = state.cookieConfigs) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载 Cookie 配置") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> items(value.value, key = { "cookie-${it.id}" }) { config ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(config.configKey, fontWeight = FontWeight.Bold)
                        Text(config.proxyIp ?: "未配置代理", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onEditCookie(config) }) { Text("编辑") }
                    Switch(checked = config.isActive, onCheckedChange = { onToggleCookie(config) })
                    TextButton(onClick = { onDeleteCookie(config) }) { Text("删除") }
                }
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("BaseURL 规则", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onAddRule) { Text("新增") }
        }
    }
    when (val value = state.baseUrlRules) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载 BaseURL 规则") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> items(value.value, key = { "rule-${it.id}" }) { rule ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rule.pattern, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { onEditRule(rule) }) { Text("编辑") }
                        listOf("allow", "block", "manual").forEach { action ->
                            OutlinedButton(onClick = { onRuleAction(rule, action) }, enabled = rule.action != action) { Text(action) }
                        }
                        TextButton(onClick = { onDeleteRule(rule) }) { Text("删除") }
                    }
                }
            }
        }
    }
    item { Text("调度日志", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    item {
        when (val value = state.schedulerLogs) {
            LoadResult.Idle, LoadResult.Loading -> AdminStatusCard("正在加载调度日志")
            is LoadResult.Error -> AdminStatusCard(value.message)
            is LoadResult.Success -> ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${value.value.totalLines} 行 · ${value.value.fileSizeMb ?: 0.0} MB", style = MaterialTheme.typography.labelSmall)
                    value.value.logs.takeLast(100).forEach { line ->
                        Text(line, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adminShopItems(
    state: AdminState,
    onAdd: () -> Unit,
    onEdit: (AdminShopItem) -> Unit,
    onToggle: (AdminShopItem) -> Unit,
    onDelete: (AdminShopItem) -> Unit
) {
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onAdd) { Text("新增商品") }
        }
    }
    when (val value = state.shopItems) {
        LoadResult.Idle, LoadResult.Loading -> item { AdminStatusCard("正在加载商品") }
        is LoadResult.Error -> item { AdminStatusCard(value.message) }
        is LoadResult.Success -> {
            if (value.value.isEmpty()) item { AdminStatusCard("暂无商品") }
            items(value.value, key = { it.id }) { item ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("${item.type} · ${item.price} 积分")
                            item.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                        }
                        TextButton(onClick = { onEdit(item) }) { Text("编辑") }
                        Switch(checked = item.isActive, onCheckedChange = { onToggle(item) })
                        TextButton(onClick = { onDelete(item) }) { Text("删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCookieEditorDialog(
    initial: AdminCookieConfig,
    onDismiss: () -> Unit,
    onSave: (AdminCookieConfig, String?) -> Unit
) {
    var key by remember(initial.id) { mutableStateOf(initial.configKey) }
    var description by remember(initial.id) { mutableStateOf(initial.description.orEmpty()) }
    var cookieRaw by remember(initial.id) { mutableStateOf("") }
    var proxy by remember(initial.id) { mutableStateOf(initial.proxyIp.orEmpty()) }
    var active by remember(initial.id) { mutableStateOf(initial.isActive) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id > 0) "编辑 Cookie 配置" else "新增 Cookie 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("配置键名") }, enabled = initial.id <= 0, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("说明") }, singleLine = true)
                OutlinedTextField(
                    cookieRaw,
                    { cookieRaw = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(if (initial.id > 0) "新 Cookie（留空不修改）" else "Cookie") },
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(proxy, { proxy = it }, Modifier.fillMaxWidth(), label = { Text("代理 IP/URL") }, singleLine = true)
                AdminSwitchRow("启用", active) { active = !active }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            configKey = key.trim(),
                            description = description.trim().ifBlank { null },
                            proxyIp = proxy.trim().ifBlank { null },
                            isActive = active
                        ),
                        cookieRaw.trim().ifBlank { null }
                    )
                },
                enabled = key.isNotBlank() && (initial.id > 0 || cookieRaw.contains("="))
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AdminRuleEditorDialog(
    initial: AdminBaseUrlRule,
    onDismiss: () -> Unit,
    onSave: (AdminBaseUrlRule) -> Unit
) {
    var pattern by remember(initial.id) { mutableStateOf(initial.pattern) }
    var action by remember(initial.id) { mutableStateOf(initial.action) }
    var description by remember(initial.id) { mutableStateOf(initial.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id > 0) "编辑 BaseURL 规则" else "新增 BaseURL 规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(pattern, { pattern = it }, Modifier.fillMaxWidth(), label = { Text("匹配规则") }, enabled = initial.id <= 0, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("allow", "block", "manual").forEach { value ->
                        OutlinedButton(onClick = { action = value }, enabled = action != value) { Text(value) }
                    }
                }
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("说明") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initial.copy(pattern = pattern.trim(), action = action, description = description.trim().ifBlank { null }))
                },
                enabled = pattern.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AdminShopEditorDialog(
    initial: AdminShopItem,
    onDismiss: () -> Unit,
    onSave: (AdminShopItem) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var description by remember(initial.id) { mutableStateOf(initial.description.orEmpty()) }
    var price by remember(initial.id) { mutableStateOf(initial.price.toString()) }
    var type by remember(initial.id) { mutableStateOf(initial.type) }
    var imageUrl by remember(initial.id) { mutableStateOf(initial.imageUrl.orEmpty()) }
    var badgeHtml by remember(initial.id) { mutableStateOf(initial.badgeHtml.orEmpty()) }
    var badgeCss by remember(initial.id) { mutableStateOf(initial.badgeCss.orEmpty()) }
    var active by remember(initial.id) { mutableStateOf(initial.isActive) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id > 0) "编辑商品" else "新增商品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("说明") }, minLines = 2)
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("积分价格") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { type = "frame" }, enabled = type != "frame") { Text("头像框") }
                    OutlinedButton(onClick = { type = "badge" }, enabled = type != "badge") { Text("徽章") }
                }
                if (type == "frame") {
                    OutlinedTextField(imageUrl, { imageUrl = it }, Modifier.fillMaxWidth(), label = { Text("图片 URL") }, singleLine = true)
                } else {
                    OutlinedTextField(badgeHtml, { badgeHtml = it }, Modifier.fillMaxWidth(), label = { Text("徽章 HTML") }, minLines = 2)
                    OutlinedTextField(badgeCss, { badgeCss = it }, Modifier.fillMaxWidth(), label = { Text("徽章 CSS") }, minLines = 2)
                }
                AdminSwitchRow("上架", active) { active = !active }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            price = price.toLongOrNull() ?: 0,
                            type = type,
                            imageUrl = imageUrl.trim().ifBlank { null },
                            badgeHtml = badgeHtml.trim().ifBlank { null },
                            badgeCss = badgeCss.trim().ifBlank { null },
                            isActive = active
                        )
                    )
                },
                enabled = name.isNotBlank() && (price.toLongOrNull() ?: -1) >= 0
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AdminMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AdminSwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun AdminStatusCard(message: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AdminStringFilterRail(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options, key = { it.first.ifBlank { "__all" } }) { option ->
                FilterChip(
                    selected = selected == option.first,
                    onClick = { onSelected(option.first) },
                    label = { Text(option.second) }
                )
            }
        }
    }
}

internal fun adminReviewTypeOptions(): List<Pair<String, String>> =
    listOf("" to "全部", "upload" to "上传", "delete" to "删除")

internal fun adminReviewStatusOptions(): List<Pair<String, String>> =
    listOf("" to "全部", "pending" to "待审核", "approved" to "已通过", "rejected" to "已拒绝")

internal fun adminOperationStatusOptions(): List<Pair<String, String>> =
    listOf("" to "全部", "success" to "成功", "failed" to "失败", "pending" to "处理中")

internal fun adminOperationActionOptions(actionTypes: List<String>): List<Pair<String, String>> {
    val normalized = actionTypes
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { it to it }
    return listOf("" to "全部") + normalized
}

private fun adminSectionLabel(section: AdminSection): String = when (section) {
    AdminSection.Overview -> "总览"
    AdminSection.Review -> "审核"
    AdminSection.Keys -> "Key"
    AdminSection.OperationLogs -> "操作日志"
    AdminSection.Scraper -> "抓取管理"
    AdminSection.Shop -> "商店"
}
