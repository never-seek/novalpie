package com.novalpie.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.novalpie.nativeapp.model.AdminBaseUrlRule
import com.novalpie.nativeapp.model.AdminCookieConfig
import com.novalpie.nativeapp.model.AdminKeyItem
import com.novalpie.nativeapp.model.AdminOperationLog
import com.novalpie.nativeapp.model.AdminOverviewStats
import com.novalpie.nativeapp.model.AdminReviewRequest
import com.novalpie.nativeapp.model.AdminShopItem
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.design.NovalPieElevation
import com.novalpie.nativeapp.ui.design.NovalPieRadius
import com.novalpie.nativeapp.ui.design.NovalPieSize
import com.novalpie.nativeapp.ui.design.NovalPieSpacing
import com.novalpie.nativeapp.ui.design.NpCard
import com.novalpie.nativeapp.ui.design.NpChip
import com.novalpie.nativeapp.ui.design.NpChipRow
import com.novalpie.nativeapp.ui.design.NpChipTone
import com.novalpie.nativeapp.ui.design.NpEmptyState
import com.novalpie.nativeapp.ui.design.NpErrorState
import com.novalpie.nativeapp.ui.design.NpSearchField
import com.novalpie.nativeapp.ui.design.NpSectionHeader
import com.novalpie.nativeapp.ui.design.NpSkeleton

private data class AdminActionConfirmation(
    val title: String,
    val message: String,
    val destructive: Boolean = false,
    val action: () -> Unit
)

private enum class AdminKeyPane { Keys, Rules }

private enum class AdminScraperPane { Cookies, Logs }

private enum class AdminShopLayout { Grid, List }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AdminScreen(
    state: AdminState,
    onRefresh: () -> Unit,
    onSectionSelected: (AdminSection) -> Unit,
    onOverviewDaysChange: (Int) -> Unit,
    onReviewQueryChange: (AdminReviewQuery) -> Unit,
    onApplyReviewQuery: () -> Unit,
    onResetReviewQuery: () -> Unit,
    onApproveAllReviews: () -> Unit,
    onOperationLogQueryChange: (AdminOperationLogQuery) -> Unit,
    onApplyOperationLogQuery: () -> Unit,
    onResetOperationLogQuery: () -> Unit,
    onOperationLogPageChange: (Int) -> Unit,
    onShopQueryChange: (AdminShopQuery) -> Unit,
    onApplyShopQuery: () -> Unit,
    onResetShopQuery: () -> Unit,
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
    var confirmation by remember { mutableStateOf<AdminActionConfirmation?>(null) }
    var cookieEditor by remember { mutableStateOf<AdminCookieConfig?>(null) }
    var ruleEditor by remember { mutableStateOf<AdminBaseUrlRule?>(null) }
    var shopEditor by remember { mutableStateOf<AdminShopItem?>(null) }
    var keyPane by rememberSaveable { mutableStateOf(AdminKeyPane.Keys) }
    var scraperPane by rememberSaveable { mutableStateOf(AdminScraperPane.Cookies) }
    var shopLayout by rememberSaveable { mutableStateOf(AdminShopLayout.Grid) }
    var expandedLogId by rememberSaveable { mutableStateOf<Long?>(null) }

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
                    },
                    colors = if (pending.destructive) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (pending.destructive) "确认删除" else "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) { Text("取消") }
            }
        )
    }
    cookieEditor?.let { initial ->
        AdminCookieEditorDialog(
            initial = initial,
            onDismiss = { cookieEditor = null },
            onSave = { config, raw ->
                cookieEditor = null
                confirmation = AdminActionConfirmation(
                    title = if (config.id > 0) "保存 Cookie 配置" else "新增 Cookie 配置",
                    message = "确认提交 ${config.configKey} 的配置？只有确认后才会写入站点。"
                ) { onSaveCookie(config, raw) }
            }
        )
    }
    ruleEditor?.let { initial ->
        AdminRuleEditorDialog(
            initial = initial,
            onDismiss = { ruleEditor = null },
            onSave = { rule ->
                ruleEditor = null
                confirmation = AdminActionConfirmation(
                    title = if (rule.id > 0) "保存 BaseURL 规则" else "新增 BaseURL 规则",
                    message = "确认提交 ${rule.pattern} 的规则？只有确认后才会写入站点。"
                ) { onSaveRule(rule) }
            }
        )
    }
    shopEditor?.let { initial ->
        AdminShopEditorDialog(
            initial = initial,
            onDismiss = { shopEditor = null },
            onSave = { item ->
                shopEditor = null
                confirmation = AdminActionConfirmation(
                    title = if (item.id > 0) "保存商品" else "新建商品",
                    message = "确认提交“${item.name}”？本地图片仅作草稿预览，不会上传；仅图片 URL 或徽章内容会写入站点。"
                ) { onSaveShopItem(item) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = NovalPieSpacing.screenHorizontal,
            top = NovalPieSpacing.lg,
            end = NovalPieSpacing.screenHorizontal,
            bottom = NovalPieSpacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.lg)
    ) {
        item {
            AdminScreenHeader(
                section = state.section,
                refreshing = state.actionLoading || adminSectionIsLoading(state),
                onRefresh = onRefresh
            )
        }
        item {
            AdminSectionRail(
                selected = state.section,
                onSelected = onSectionSelected
            )
        }
        state.actionMessage?.let { message ->
            item { AdminInlineMessage(message) }
        }

        when (state.section) {
            AdminSection.Overview -> adminDashboardItems(
                state = state,
                onDaysChange = onOverviewDaysChange,
                onRefresh = onRefresh,
                onSectionSelected = onSectionSelected
            )
            AdminSection.Review -> adminReviewItems(
                state = state,
                onQueryChange = onReviewQueryChange,
                onApplyQuery = onApplyReviewQuery,
                onResetQuery = onResetReviewQuery,
                onApproveAll = {
                    confirmation = AdminActionConfirmation(
                        title = "批量通过审核请求",
                        message = "将通过当前筛选结果中所有待审核请求；缺失作品的请求会由源站忽略。"
                    ) { onApproveAllReviews() }
                },
                onToggle = { kind ->
                    confirmation = AdminActionConfirmation(
                        title = "修改自动审核",
                        message = "确认切换${if (kind == "upload") "上传" else "删除"}请求的自动审核设置？"
                    ) { onToggleReviewSetting(kind) }
                },
                onAction = { request, action ->
                    confirmation = AdminActionConfirmation(
                        title = if (action == "approve") "通过审核" else "拒绝审核",
                        message = "确认${if (action == "approve") "通过" else "拒绝"}请求 #${request.id}？"
                    ) { onReviewAction(request.id, action) }
                }
            )
            AdminSection.Keys -> adminKeyItems(
                state = state,
                pane = keyPane,
                onPaneChange = { keyPane = it },
                onRefresh = onRefresh,
                onStatus = { key, status ->
                    confirmation = AdminActionConfirmation(
                        title = "修改 Key 状态",
                        message = "确认将 ${key.name} 设为${adminKeyStatusLabel(status)}？"
                    ) { onUpdateKeyStatus(key.id, status) }
                },
                onDelete = { key ->
                    confirmation = AdminActionConfirmation(
                        title = "删除 Key",
                        message = "确认永久删除 ${key.name}？此操作不可撤销。",
                        destructive = true
                    ) { onDeleteKey(key.id) }
                },
                onAddRule = { ruleEditor = AdminBaseUrlRule(0, "", "manual") },
                onEditRule = { ruleEditor = it },
                onRuleAction = { rule, action ->
                    confirmation = AdminActionConfirmation(
                        title = "修改 BaseURL 规则",
                        message = "确认将 ${rule.pattern} 设为${adminBaseUrlActionLabel(action)}？"
                    ) { onSetRuleAction(rule, action) }
                },
                onDeleteRule = { rule ->
                    confirmation = AdminActionConfirmation(
                        title = "删除 BaseURL 规则",
                        message = "确认删除 ${rule.pattern}？此操作不可撤销。",
                        destructive = true
                    ) { onDeleteRule(rule.id) }
                }
            )
            AdminSection.OperationLogs -> adminOperationLogItems(
                state = state,
                expandedLogId = expandedLogId,
                onExpandedLogChange = { expandedLogId = it },
                onQueryChange = onOperationLogQueryChange,
                onApplyQuery = onApplyOperationLogQuery,
                onResetQuery = onResetOperationLogQuery,
                onPageChange = onOperationLogPageChange
            )
            AdminSection.Scraper -> adminScraperItems(
                state = state,
                pane = scraperPane,
                onPaneChange = { scraperPane = it },
                onRefresh = onRefresh,
                onAddCookie = { cookieEditor = AdminCookieConfig(0, "", isActive = true) },
                onEditCookie = { cookieEditor = it },
                onToggleCookie = { config ->
                    confirmation = AdminActionConfirmation(
                        title = "修改 Cookie 状态",
                        message = "确认${if (config.isActive) "停用" else "启用"} ${config.configKey}？"
                    ) { onToggleCookie(config) }
                },
                onDeleteCookie = { config ->
                    confirmation = AdminActionConfirmation(
                        title = "删除 Cookie 配置",
                        message = "确认删除 ${config.configKey}？此操作不可撤销。",
                        destructive = true
                    ) { onDeleteCookie(config.id) }
                }
            )
            AdminSection.Shop -> adminShopItems(
                state = state,
                layout = shopLayout,
                onLayoutChange = { shopLayout = it },
                onQueryChange = onShopQueryChange,
                onApplyQuery = onApplyShopQuery,
                onResetQuery = onResetShopQuery,
                onAdd = { shopEditor = AdminShopItem(0, "", price = 0, type = "frame", isActive = true) },
                onEdit = { shopEditor = it },
                onToggle = { item ->
                    confirmation = AdminActionConfirmation(
                        title = "修改商品状态",
                        message = "确认${if (item.isActive) "下架" else "上架"} ${item.name}？"
                    ) { onToggleShopItem(item) }
                },
                onDelete = { item ->
                    confirmation = AdminActionConfirmation(
                        title = "删除商品",
                        message = "确认删除 ${item.name}？默认只下架，不会清理用户背包。",
                        destructive = true
                    ) { onDeleteShopItem(item.id) }
                }
            )
        }
    }
}

@Composable
private fun AdminScreenHeader(
    section: AdminSection,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    val title = when (section) {
        AdminSection.Overview -> "管理控制台"
        AdminSection.Review -> "上传 / 删除审核"
        AdminSection.Keys -> "Key 管理"
        AdminSection.OperationLogs -> "用户操作日志"
        AdminSection.Scraper -> "爬虫与调度管理"
        AdminSection.Shop -> "商店商品管理"
    }
    val subtitle = when (section) {
        AdminSection.Overview -> "监控平台运行状态，处理核心事务"
        AdminSection.Review -> "处理用户的书籍上传与删除请求"
        AdminSection.Keys -> "审核工作区 API Key，并管理 BaseURL 策略"
        AdminSection.OperationLogs -> "查看和筛选用户的各种操作记录"
        AdminSection.Scraper -> "配置爬虫 Cookie 参数，查看调度日志与运行状态"
        AdminSection.Shop -> "管理头像框、徽章及其他虚拟商品"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = NovalPieSpacing.xxs)
            )
        }
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "刷新",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSectionRail(
    selected: AdminSection,
    onSelected: (AdminSection) -> Unit
) {
    NpCard(contentPadding = NovalPieSpacing.sm) {
        Text(
            text = "管理模块",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = NovalPieSpacing.xs)
        )
        NpChipRow(modifier = Modifier.padding(horizontal = NovalPieSpacing.xxs)) {
            AdminSection.values().forEach { section ->
                FilterChip(
                    selected = selected == section,
                    onClick = { onSelected(section) },
                    label = { Text(adminSectionDisplayLabel(section)) }
                )
            }
        }
    }
}

@Composable
private fun AdminInlineMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(NovalPieSpacing.md)
        )
    }
}

private fun LazyListScope.adminDashboardItems(
    state: AdminState,
    onDaysChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSectionSelected: (AdminSection) -> Unit
) {
    when (val value = state.overview) {
        LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载运营总览")
        is LoadResult.Error -> adminErrorItem(value.message, "重试", onRefresh)
        is LoadResult.Success -> {
            val stats = value.value
            item {
                AdminDashboardMetricGrid(stats = stats)
            }
            item {
                AdminGrowthCard(
                    stats = stats,
                    days = state.overviewDays,
                    onDaysChange = onDaysChange
                )
            }
            item {
                NpSectionHeader(title = "快捷入口", subtitle = "直接进入常用管理模块")
            }
            item {
                AdminShortcutGrid(onSectionSelected)
            }
        }
    }
}

@Composable
private fun AdminDashboardMetricGrid(stats: AdminOverviewStats) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                label = "待审核总数",
                value = stats.pendingReviewTotal.toString(),
                detail = "上传 ${stats.pendingReviewUpload} · 删除 ${stats.pendingReviewDelete}",
                icon = Icons.Filled.Info,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                label = "待审核 Key",
                value = stats.pendingKeys.toString(),
                detail = "已通过 ${stats.approvedKeys} · 在线 ${stats.activeTranslators}",
                icon = Icons.Filled.Tune,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                label = "注册用户",
                value = stats.registeredUserTotal.toString(),
                detail = "今日新增 ${stats.todayUsers}",
                icon = Icons.Filled.Person,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            AdminMetricCard(
                modifier = Modifier.weight(1f),
                label = "可用作品",
                value = stats.activeNovelTotal.toString(),
                detail = "站内公开作品",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(NovalPieRadius.md),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = NovalPieElevation.card)
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(NovalPieSize.iconMd))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminGrowthCard(
    stats: AdminOverviewStats,
    days: Int,
    onDaysChange: (Int) -> Unit
) {
    val values = stats.recentUserDaily
    val peak = values.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("用户增长趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "最近 $days 天的每日新增用户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilterChip(
                selected = false,
                onClick = {},
                enabled = false,
                label = { Text("$days 天") }
            )
        }
        NpChipRow {
            adminOverviewDayOptions().forEach { option ->
                FilterChip(
                    selected = days == option,
                    onClick = { onDaysChange(option) },
                    label = { Text("最近 $option 天") }
                )
            }
        }
        if (values.isEmpty()) {
            Text(
                text = "源站暂未返回增长数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = NovalPieSpacing.xl)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEach { day ->
                    val barHeight = (22f + 82f * day.count.toFloat() / peak.toFloat()).dp
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
                    ) {
                        Text(day.count.toString(), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.66f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = NovalPieRadius.sm, topEnd = NovalPieRadius.sm))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = day.date.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminShortcutGrid(onSectionSelected: (AdminSection) -> Unit) {
    val shortcuts = listOf(
        Triple(AdminSection.Review, "审核中心", "处理上传与删除请求"),
        Triple(AdminSection.Shop, "商店管理", "管理商品、素材与配置"),
        Triple(AdminSection.OperationLogs, "操作日志", "查看用户操作记录"),
        Triple(AdminSection.Scraper, "爬虫管理", "Cookie 配置与运行日志"),
        Triple(AdminSection.Keys, "Key 管理", "API Key 审核与规则")
    )
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        shortcuts.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
            ) {
                row.forEach { (section, title, description) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(NovalPieRadius.md))
                            .clickable { onSectionSelected(section) },
                        shape = RoundedCornerShape(NovalPieRadius.md),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = NovalPieElevation.card
                    ) {
                        Column(
                            modifier = Modifier.padding(NovalPieSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                        ) {
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun LazyListScope.adminReviewItems(
    state: AdminState,
    onQueryChange: (AdminReviewQuery) -> Unit,
    onApplyQuery: () -> Unit,
    onResetQuery: () -> Unit,
    onApproveAll: () -> Unit,
    onToggle: (String) -> Unit,
    onAction: (AdminReviewRequest, String) -> Unit
) {
    item {
        when (val value = state.reviewSettings) {
            LoadResult.Idle, LoadResult.Loading -> AdminLoadingCard("正在加载审核配置")
            is LoadResult.Error -> AdminInlineError(value.message, "重试", onApplyQuery)
            is LoadResult.Success -> NpCard {
                Text("自动审核配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "开启后符合条件的请求会自动通过，不进入等待队列。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AdminToggleRow("自动通过上传", value.value.autoApproveUpload) { onToggle("upload") }
                AdminToggleRow("自动通过删除", value.value.autoApproveDelete) { onToggle("delete") }
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
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("审核请求", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = onApproveAll,
                enabled = !state.actionLoading &&
                    (state.reviewQuery.status.isBlank() || state.reviewQuery.status == "pending")
            ) {
                Text("一键通过及忽略")
            }
        }
    }
    when (val value = state.reviewRequests) {
        LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载审核请求")
        is LoadResult.Error -> adminErrorItem(value.message, "重试", onApplyQuery)
        is LoadResult.Success -> {
            if (value.value.isEmpty()) {
                item {
                    NpEmptyState(
                        title = "暂无相关审核请求",
                        description = "尝试调整筛选条件或刷新页面"
                    )
                }
            }
            items(value.value, key = { it.id }) { request ->
                AdminReviewRequestCard(
                    request = request,
                    enabled = !state.actionLoading,
                    onAction = onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminReviewFilterCard(
    query: AdminReviewQuery,
    enabled: Boolean,
    onQueryChange: (AdminReviewQuery) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    NpCard {
        Text("全部请求", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AdminFilterRail(
            label = "状态",
            options = adminReviewStatusOptions(),
            selected = query.status,
            onSelected = { onQueryChange(query.copy(status = it)) }
        )
        NpSearchField(
            value = query.keyword,
            onValueChange = { onQueryChange(query.copy(keyword = it)) },
            onSearch = { onApply() },
            placeholder = "搜索书名 / 用户…",
            clearContentDescription = "清除审核搜索"
        )
        AdminFilterRail(
            label = "类型",
            options = listOf("" to "所有类型", "upload" to "仅上传", "delete" to "仅删除"),
            selected = query.type,
            onSelected = { onQueryChange(query.copy(type = it)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
            Button(onClick = onApply, enabled = enabled) { Text("搜索") }
            TextButton(onClick = onReset, enabled = enabled && query != AdminReviewQuery()) { Text("重置") }
        }
    }
}

@Composable
private fun AdminReviewRequestCard(
    request: AdminReviewRequest,
    enabled: Boolean,
    onAction: (AdminReviewRequest, String) -> Unit
) {
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                Text(
                    text = request.title ?: "请求 #${request.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        request.username?.let { "用户 $it" } ?: request.userId?.let { "用户 #$it" },
                        request.createdAt
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AdminStatusTag(adminReviewStatusLabel(request.status), adminReviewStatusTone(request.status))
        }
        NpChipRow {
            NpChip(adminReviewTypeLabel(request.type), NpChipTone.Source)
            request.novelId?.let { NpChip("作品 #$it") }
        }
        request.reason?.takeIf(String::isNotBlank)?.let { reason ->
            Text(reason, style = MaterialTheme.typography.bodySmall)
        }
        if (request.status == "pending") {
            Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
                FilledTonalButton(onClick = { onAction(request, "approve") }, enabled = enabled) { Text("通过") }
                TextButton(onClick = { onAction(request, "reject") }, enabled = enabled) { Text("拒绝") }
            }
        }
    }
}

private fun LazyListScope.adminKeyItems(
    state: AdminState,
    pane: AdminKeyPane,
    onPaneChange: (AdminKeyPane) -> Unit,
    onRefresh: () -> Unit,
    onStatus: (AdminKeyItem, String) -> Unit,
    onDelete: (AdminKeyItem) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (AdminBaseUrlRule) -> Unit,
    onRuleAction: (AdminBaseUrlRule, String) -> Unit,
    onDeleteRule: (AdminBaseUrlRule) -> Unit
) {
    item {
        AdminTwoPaneRail(
            firstLabel = "Key 审核列表",
            secondLabel = "BaseURL 规则设定",
            firstSelected = pane == AdminKeyPane.Keys,
            onFirst = { onPaneChange(AdminKeyPane.Keys) },
            onSecond = { onPaneChange(AdminKeyPane.Rules) }
        )
    }
    when (pane) {
        AdminKeyPane.Keys -> when (val value = state.keys) {
            LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载 Key 审核列表")
            is LoadResult.Error -> adminErrorItem(value.message, "重试", onRefresh)
            is LoadResult.Success -> {
                if (value.value.isEmpty()) {
                    item {
                        NpEmptyState(
                            title = "暂无符合条件的 Key",
                            description = "Key 审核列表会显示工作区共享的 API Key"
                        )
                    }
                }
                items(value.value, key = { it.id }) { key ->
                    AdminKeyCard(
                        key = key,
                        enabled = !state.actionLoading,
                        onStatus = onStatus,
                        onDelete = onDelete
                    )
                }
            }
        }
        AdminKeyPane.Rules -> adminBaseUrlRuleItems(
            value = state.baseUrlRules,
            actionLoading = state.actionLoading,
            onRefresh = onRefresh,
            onAddRule = onAddRule,
            onEditRule = onEditRule,
            onRuleAction = onRuleAction,
            onDeleteRule = onDeleteRule
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTwoPaneRail(
    firstLabel: String,
    secondLabel: String,
    firstSelected: Boolean,
    onFirst: () -> Unit,
    onSecond: () -> Unit
) {
    NpCard(contentPadding = NovalPieSpacing.sm) {
        NpChipRow {
            FilterChip(selected = firstSelected, onClick = onFirst, label = { Text(firstLabel) })
            FilterChip(selected = !firstSelected, onClick = onSecond, label = { Text(secondLabel) })
        }
    }
}

@Composable
private fun AdminKeyCard(
    key: AdminKeyItem,
    enabled: Boolean,
    onStatus: (AdminKeyItem, String) -> Unit,
    onDelete: (AdminKeyItem) -> Unit
) {
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                Text(key.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = listOfNotNull(key.providerName, key.model).joinToString(" · ").ifBlank { "未标注提供者" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AdminStatusTag(adminKeyStatusLabel(key.approvalStatus), adminKeyStatusTone(key.approvalStatus))
        }
        key.baseUrl?.takeIf(String::isNotBlank)?.let { url ->
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        key.createdAt?.let {
            Text("创建于 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NpChipRow {
            listOf("pending", "approved", "rejected").forEach { status ->
                OutlinedButton(
                    onClick = { onStatus(key, status) },
                    enabled = enabled && key.approvalStatus != status
                ) {
                    Text(adminKeyStatusLabel(status))
                }
            }
            TextButton(onClick = { onDelete(key) }, enabled = enabled) { Text("删除") }
        }
    }
}

private fun LazyListScope.adminBaseUrlRuleItems(
    value: LoadResult<List<AdminBaseUrlRule>>,
    actionLoading: Boolean,
    onRefresh: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (AdminBaseUrlRule) -> Unit,
    onRuleAction: (AdminBaseUrlRule, String) -> Unit,
    onDeleteRule: (AdminBaseUrlRule) -> Unit
) {
    item {
        NpSectionHeader(
            title = "规则设定",
            subtitle = "管理工作区共享 BaseURL 的白名单 / 黑名单策略",
            actionLabel = "添加规则",
            onAction = onAddRule
        )
    }
    when (value) {
        LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载 BaseURL 规则")
        is LoadResult.Error -> adminErrorItem(value.message, "重试", onRefresh)
        is LoadResult.Success -> {
            val defaultRule = value.value.firstOrNull { it.pattern == "*" }
            defaultRule?.let { rule ->
                item {
                    NpCard {
                        Text("系统默认策略", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "未匹配到下方任何特定规则时，将应用此策略。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AdminRuleActionRow(rule = rule, enabled = !actionLoading, onRuleAction = onRuleAction)
                    }
                }
            }
            val specificRules = value.value.filterNot { it.pattern == "*" }
            if (specificRules.isEmpty()) {
                item {
                    NpEmptyState(
                        title = "暂无特定规则",
                        description = "可添加匹配模式、动作和备注来覆盖默认策略",
                        actionLabel = "添加规则",
                        onAction = onAddRule
                    )
                }
            }
            items(specificRules, key = { it.id }) { rule ->
                NpCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                            Text(rule.pattern, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            rule.description?.takeIf(String::isNotBlank)?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            rule.createdAt?.takeIf(String::isNotBlank)?.let { createdAt ->
                                Text(
                                    text = "创建于 $createdAt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AdminStatusTag(adminBaseUrlActionLabel(rule.action), adminBaseUrlActionTone(rule.action))
                    }
                    AdminRuleActionRow(rule = rule, enabled = !actionLoading, onRuleAction = onRuleAction)
                    NpChipRow {
                        TextButton(onClick = { onEditRule(rule) }, enabled = !actionLoading) { Text("编辑") }
                        TextButton(onClick = { onDeleteRule(rule) }, enabled = !actionLoading) { Text("删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRuleActionRow(
    rule: AdminBaseUrlRule,
    enabled: Boolean,
    onRuleAction: (AdminBaseUrlRule, String) -> Unit
) {
    NpChipRow {
        listOf("allow", "block", "manual").forEach { action ->
            OutlinedButton(
                onClick = { onRuleAction(rule, action) },
                enabled = enabled && rule.action != action
            ) {
                Text(adminBaseUrlActionLabel(action))
            }
        }
    }
}

private fun LazyListScope.adminOperationLogItems(
    state: AdminState,
    expandedLogId: Long?,
    onExpandedLogChange: (Long?) -> Unit,
    onQueryChange: (AdminOperationLogQuery) -> Unit,
    onApplyQuery: () -> Unit,
    onResetQuery: () -> Unit,
    onPageChange: (Int) -> Unit
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
        LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载操作日志")
        is LoadResult.Error -> adminErrorItem(value.message, "重试", onApplyQuery)
        is LoadResult.Success -> {
            item {
                Text(
                    text = "共 ${value.value.total} 条记录 · 第 ${state.operationLogQuery.page} / ${value.value.totalPages.coerceAtLeast(1)} 页",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (value.value.items.isEmpty()) {
                item {
                    NpEmptyState(
                        title = "暂无操作日志",
                        description = "尝试调整筛选条件"
                    )
                }
            }
            items(value.value.items, key = { it.id }) { log ->
                AdminOperationLogCard(
                    log = log,
                    expanded = expandedLogId == log.id,
                    onToggleExpanded = {
                        onExpandedLogChange(if (expandedLogId == log.id) null else log.id)
                    }
                )
            }
            if (value.value.totalPages > 1) {
                item {
                    AdminPagination(
                        page = state.operationLogQuery.page,
                        totalPages = value.value.totalPages,
                        onPageChange = onPageChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminOperationLogFilterCard(
    query: AdminOperationLogQuery,
    actionTypes: List<String>,
    enabled: Boolean,
    onQueryChange: (AdminOperationLogQuery) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    NpCard {
        Text("筛选条件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AdminFilterRail(
            label = "操作类型",
            options = adminOperationActionOptions(actionTypes),
            selected = query.action,
            onSelected = { onQueryChange(query.copy(action = it, page = 1)) }
        )
        AdminFilterRail(
            label = "状态",
            options = adminOperationStatusOptions(),
            selected = query.status,
            onSelected = { onQueryChange(query.copy(status = it, page = 1)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            AdminCompactTextField(
                value = query.userId,
                label = "用户 ID",
                modifier = Modifier.weight(1f),
                onValueChange = { onQueryChange(query.copy(userId = it.filter(Char::isDigit), page = 1)) }
            )
            AdminCompactTextField(
                value = query.novelId,
                label = "作品 ID",
                modifier = Modifier.weight(1f),
                onValueChange = { onQueryChange(query.copy(novelId = it.filter(Char::isDigit), page = 1)) }
            )
        }
        AdminCompactTextField(
            value = query.keyword,
            label = "搜索内容、结果或 IP 地址",
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onQueryChange(query.copy(keyword = it, page = 1)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            AdminCompactTextField(
                value = query.startDate,
                label = "开始日期",
                modifier = Modifier.weight(1f),
                onValueChange = { onQueryChange(query.copy(startDate = it, page = 1)) }
            )
            AdminCompactTextField(
                value = query.endDate,
                label = "结束日期",
                modifier = Modifier.weight(1f),
                onValueChange = { onQueryChange(query.copy(endDate = it, page = 1)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
            Button(onClick = onApply, enabled = enabled) { Text("搜索") }
            TextButton(onClick = onReset, enabled = enabled && query != AdminOperationLogQuery()) { Text("重置") }
        }
    }
}

@Composable
private fun AdminCompactTextField(
    value: String,
    label: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun AdminOperationLogCard(
    log: AdminOperationLog,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(NovalPieRadius.sm),
                color = adminOperationStatusColor(log.status)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.padding(NovalPieSpacing.sm).size(NovalPieSize.iconMd),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(adminOperationActionLabel(log.action), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    AdminStatusTag(adminOperationStatusLabel(log.status), adminOperationStatusTone(log.status))
                }
                Text(
                    text = listOfNotNull(
                        log.username?.let { "用户 $it" } ?: log.userId?.let { "用户 #$it" },
                        log.novelTitle ?: log.novelId?.let { "作品 #$it" },
                        log.createdAt
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        log.message?.takeIf(String::isNotBlank)?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, maxLines = if (expanded) Int.MAX_VALUE else 2)
        }
        TextButton(onClick = onToggleExpanded) { Text(if (expanded) "收起详情" else "查看详情") }
        if (expanded) {
            AdminOperationLogDetails(log)
        }
    }
}

@Composable
private fun AdminOperationLogDetails(log: AdminOperationLog) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        NpChipRow {
            NpChip("日志 #${log.id}")
            log.chapterId?.let { NpChip("章节 #$it") }
            log.ipAddress?.let { NpChip("IP $it", NpChipTone.Neutral) }
        }
        AdminDetailLine("用户 ID", log.userId?.toString())
        AdminDetailLine("邮箱", log.email)
        AdminDetailLine("创建时间", log.createdAt)
        AdminDetailLine("更新时间", log.updatedAt)
        log.content?.takeIf(String::isNotBlank)?.let { content ->
            AdminDetailBlock("操作内容", content)
        }
        log.result?.takeIf(String::isNotBlank)?.let { result ->
            AdminDetailBlock("操作结果", result)
        }
        log.userAgent?.takeIf(String::isNotBlank)?.let { agent ->
            AdminDetailBlock("用户代理", agent)
        }
    }
}

@Composable
private fun AdminDetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AdminDetailBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(NovalPieRadius.sm),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(NovalPieSpacing.sm)
            )
        }
    }
}

@Composable
private fun AdminPagination(page: Int, totalPages: Int, onPageChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { onPageChange(page - 1) }, enabled = page > 1) { Text("上一页") }
        Text("第 $page / $totalPages 页", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { onPageChange(page + 1) }, enabled = page < totalPages) { Text("下一页") }
    }
}

private fun LazyListScope.adminScraperItems(
    state: AdminState,
    pane: AdminScraperPane,
    onPaneChange: (AdminScraperPane) -> Unit,
    onRefresh: () -> Unit,
    onAddCookie: () -> Unit,
    onEditCookie: (AdminCookieConfig) -> Unit,
    onToggleCookie: (AdminCookieConfig) -> Unit,
    onDeleteCookie: (AdminCookieConfig) -> Unit
) {
    item {
        AdminTwoPaneRail(
            firstLabel = "Cookie 配置",
            secondLabel = "调度日志",
            firstSelected = pane == AdminScraperPane.Cookies,
            onFirst = { onPaneChange(AdminScraperPane.Cookies) },
            onSecond = { onPaneChange(AdminScraperPane.Logs) }
        )
    }
    when (pane) {
        AdminScraperPane.Cookies -> {
            item {
                NpSectionHeader(
                    title = "Cookie 配置",
                    subtitle = "管理爬虫 Cookie、代理和健康状态",
                    actionLabel = "新建配置",
                    onAction = onAddCookie
                )
            }
            when (val value = state.cookieConfigs) {
                LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载 Cookie 配置")
                is LoadResult.Error -> adminErrorItem(value.message, "重试", onRefresh)
                is LoadResult.Success -> {
                    if (value.value.isEmpty()) {
                        item {
                            NpEmptyState(
                                title = "暂无 Cookie 配置",
                                description = "可在这里添加用于爬虫的 Cookie 配置",
                                actionLabel = "新建配置",
                                onAction = onAddCookie
                            )
                        }
                    }
                    items(value.value, key = { it.id }) { config ->
                        AdminCookieConfigCard(
                            config = config,
                            enabled = !state.actionLoading,
                            onEdit = onEditCookie,
                            onToggle = onToggleCookie,
                            onDelete = onDeleteCookie
                        )
                    }
                }
            }
        }
        AdminScraperPane.Logs -> {
            item { NpSectionHeader(title = "调度日志", subtitle = "服务端返回的最近运行记录") }
            when (val value = state.schedulerLogs) {
                LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载调度日志")
                is LoadResult.Error -> adminErrorItem(value.message, "刷新", onRefresh)
                is LoadResult.Success -> item {
                    NpCard {
                        Text(
                            text = buildString {
                                append("${value.value.totalLines} 行")
                                value.value.fileSizeMb?.let { append(" · ${"%.2f".format(it)} MB") }
                                value.value.lastModified?.let { append(" · 更新于 $it") }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (value.value.logs.isEmpty()) {
                            Text("暂无日志", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            value.value.logs.takeLast(100).forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCookieConfigCard(
    config: AdminCookieConfig,
    enabled: Boolean,
    onEdit: (AdminCookieConfig) -> Unit,
    onToggle: (AdminCookieConfig) -> Unit,
    onDelete: (AdminCookieConfig) -> Unit
) {
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(NovalPieRadius.sm),
                color = if (config.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.padding(NovalPieSpacing.sm).size(NovalPieSize.iconMd),
                    tint = if (config.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                Text(config.configKey, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                NpChipRow {
                    AdminStatusTag(if (config.isActive) "已启用" else "已禁用", if (config.isActive) NpChipTone.Status else NpChipTone.Neutral)
                    config.isHealthy?.let { healthy ->
                        AdminStatusTag(if (healthy) "健康" else "异常", if (healthy) NpChipTone.Status else NpChipTone.Warning)
                    }
                }
            }
        }
        config.description?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        config.proxyIp?.takeIf(String::isNotBlank)?.let { NpChip("已配置代理", NpChipTone.Neutral) }
        config.lastError?.takeIf(String::isNotBlank)?.let { error ->
            Text("最近错误：$error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        val summary = listOfNotNull(
            config.updatedAt?.let { "更新于 $it" },
            config.updatedByUsername?.let { "由 $it 更新" },
            if (config.successCount > 0 || config.failCount > 0) "✓ ${config.successCount} · ✕ ${config.failCount}" else null
        ).joinToString(" · ")
        if (summary.isNotBlank()) {
            Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NpChipRow {
            TextButton(onClick = { onEdit(config) }, enabled = enabled) { Text("编辑") }
            OutlinedButton(onClick = { onToggle(config) }, enabled = enabled) { Text(if (config.isActive) "禁用" else "启用") }
            TextButton(onClick = { onDelete(config) }, enabled = enabled) { Text("删除") }
        }
    }
}

private fun LazyListScope.adminShopItems(
    state: AdminState,
    layout: AdminShopLayout,
    onLayoutChange: (AdminShopLayout) -> Unit,
    onQueryChange: (AdminShopQuery) -> Unit,
    onApplyQuery: () -> Unit,
    onResetQuery: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (AdminShopItem) -> Unit,
    onToggle: (AdminShopItem) -> Unit,
    onDelete: (AdminShopItem) -> Unit
) {
    item {
        AdminShopFilterCard(
            query = state.shopQuery,
            layout = layout,
            enabled = !state.actionLoading,
            onLayoutChange = onLayoutChange,
            onQueryChange = onQueryChange,
            onApply = onApplyQuery,
            onReset = onResetQuery,
            onAdd = onAdd
        )
    }
    when (val value = state.shopItems) {
        LoadResult.Idle, LoadResult.Loading -> adminLoadingItem("正在加载商品")
        is LoadResult.Error -> adminErrorItem(value.message, "重试", onApplyQuery)
        is LoadResult.Success -> {
            if (value.value.isEmpty()) {
                item {
                    NpEmptyState(
                        title = "没有找到相关商品",
                        description = "尝试调整商品类型、状态或关键词",
                        actionLabel = "重置筛选",
                        onAction = onResetQuery
                    )
                }
            }
            when (layout) {
                AdminShopLayout.Grid -> items(value.value.chunked(2), key = { row -> row.joinToString("-") { it.id.toString() } }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
                    ) {
                        row.forEach { item ->
                            AdminShopGridCard(
                                item = item,
                                enabled = !state.actionLoading,
                                modifier = Modifier.weight(1f),
                                onEdit = onEdit,
                                onToggle = onToggle,
                                onDelete = onDelete
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                AdminShopLayout.List -> items(value.value, key = { it.id }) { item ->
                    AdminShopListCard(
                        item = item,
                        enabled = !state.actionLoading,
                        onEdit = onEdit,
                        onToggle = onToggle,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminShopFilterCard(
    query: AdminShopQuery,
    layout: AdminShopLayout,
    enabled: Boolean,
    onLayoutChange: (AdminShopLayout) -> Unit,
    onQueryChange: (AdminShopQuery) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onAdd: () -> Unit
) {
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("商品视图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("头像框、徽章及其他虚拟商品", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            NpChipRow {
                FilterChip(selected = layout == AdminShopLayout.Grid, onClick = { onLayoutChange(AdminShopLayout.Grid) }, label = { Text("网格") })
                FilterChip(selected = layout == AdminShopLayout.List, onClick = { onLayoutChange(AdminShopLayout.List) }, label = { Text("列表") })
            }
        }
        AdminFilterRail(
            label = "商品类型",
            options = listOf("" to "全部", "frame" to "头像框", "badge" to "徽章"),
            selected = query.type,
            onSelected = { onQueryChange(query.copy(type = it)) }
        )
        Text("上架状态", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NpChipRow {
            listOf(null to "所有状态", true to "已上架", false to "已下架").forEach { (active, label) ->
                FilterChip(
                    selected = query.isActive == active,
                    onClick = { onQueryChange(query.copy(isActive = active)) },
                    label = { Text(label) }
                )
            }
        }
        NpSearchField(
            value = query.keyword,
            onValueChange = { onQueryChange(query.copy(keyword = it)) },
            onSearch = { onApply() },
            placeholder = "搜索商品名称…",
            clearContentDescription = "清除商品搜索"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
            Button(onClick = onApply, enabled = enabled) { Text("搜索") }
            TextButton(onClick = onReset, enabled = enabled && query != AdminShopQuery()) { Text("重置") }
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onAdd, enabled = enabled) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(NovalPieSize.iconSm))
                Spacer(modifier = Modifier.width(NovalPieSpacing.xxs))
                Text("新建商品")
            }
        }
    }
}

@Composable
private fun AdminShopGridCard(
    item: AdminShopItem,
    enabled: Boolean,
    modifier: Modifier,
    onEdit: (AdminShopItem) -> Unit,
    onToggle: (AdminShopItem) -> Unit,
    onDelete: (AdminShopItem) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(NovalPieRadius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = NovalPieElevation.card)
    ) {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)
        ) {
            AdminShopPreview(item = item, compact = false)
            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            NpChipRow {
                NpChip(adminShopTypeLabel(item.type), NpChipTone.Source)
                AdminStatusTag(if (item.isActive) "已上架" else "已下架", if (item.isActive) NpChipTone.Status else NpChipTone.Neutral)
            }
            Text("${item.price} 积分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(description, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            NpChipRow {
                TextButton(onClick = { onEdit(item) }, enabled = enabled) { Text("编辑") }
                TextButton(onClick = { onToggle(item) }, enabled = enabled) { Text(if (item.isActive) "下架" else "上架") }
                TextButton(onClick = { onDelete(item) }, enabled = enabled) { Text("删除") }
            }
        }
    }
}

@Composable
private fun AdminShopListCard(
    item: AdminShopItem,
    enabled: Boolean,
    onEdit: (AdminShopItem) -> Unit,
    onToggle: (AdminShopItem) -> Unit,
    onDelete: (AdminShopItem) -> Unit
) {
    NpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            AdminShopPreview(item = item, compact = true)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    AdminStatusTag(if (item.isActive) "已上架" else "已下架", if (item.isActive) NpChipTone.Status else NpChipTone.Neutral)
                }
                Text("${adminShopTypeLabel(item.type)} · ${item.price} 积分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.description?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                NpChipRow {
                    TextButton(onClick = { onEdit(item) }, enabled = enabled) { Text("编辑") }
                    OutlinedButton(onClick = { onToggle(item) }, enabled = enabled) { Text(if (item.isActive) "下架" else "上架") }
                    TextButton(onClick = { onDelete(item) }, enabled = enabled) { Text("删除") }
                }
            }
        }
    }
}

@Composable
internal fun AdminShopPreview(item: AdminShopItem, compact: Boolean) {
    val size = if (compact) 64.dp else 112.dp
    val shape = RoundedCornerShape(NovalPieRadius.sm)
    Surface(
        modifier = Modifier
            .size(size)
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (item.type == "frame" && !item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "${item.name} 预览",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (item.type == "badge") {
            val colors = remember(item.badgeCss, item.name, item.imageUrl) {
                adminShopBadgePreviewColors(item.badgeCss, item.name, item.imageUrl)
            }
            val label = remember(item.badgeHtml, item.name) {
                adminShopBadgePreviewText(item.badgeHtml, item.name)
            }
            val backgroundImageUrl = remember(item.badgeCss) {
                adminShopBadgePreviewBackgroundImageUrl(item.badgeCss)
            }
            val textColor = remember(item.badgeCss) { adminShopBadgePreviewTextColor(item.badgeCss) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors), shape)
                    .border(1.dp, Color.White.copy(alpha = 0.46f), shape)
                    .padding(NovalPieSpacing.xs),
                contentAlignment = Alignment.Center
            ) {
                if (backgroundImageUrl != null) {
                    AsyncImage(
                        model = backgroundImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = adminShopBadgePreviewContentScale(item.badgeCss)
                    )
                }
                Text(
                    text = label,
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(NovalPieSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CardGiftcard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (compact) NovalPieSize.iconMd else NovalPieSize.iconLg)
                )
                Text("徽章", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminFilterRail(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NpChipRow {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option.first,
                    onClick = { onSelected(option.first) },
                    label = { Text(option.second) }
                )
            }
        }
    }
}

@Composable
private fun AdminToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun AdminStatusTag(label: String, tone: NpChipTone) {
    NpChip(label = label, tone = tone)
}

private fun LazyListScope.adminLoadingItem(label: String) {
    item { AdminLoadingCard(label) }
}

@Composable
private fun AdminLoadingCard(label: String) {
    NpCard {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NpSkeleton(height = 18.dp, widthFraction = 0.66f)
        NpSkeleton(height = 14.dp)
        NpSkeleton(height = 14.dp, widthFraction = 0.84f)
    }
}

private fun LazyListScope.adminErrorItem(message: String, retryLabel: String, onRetry: () -> Unit) {
    item { AdminInlineError(message, retryLabel, onRetry) }
}

@Composable
private fun AdminInlineError(message: String, retryLabel: String, onRetry: () -> Unit) {
    NpErrorState(message = message, retryLabel = retryLabel, onRetry = onRetry)
}

private fun adminSectionIsLoading(state: AdminState): Boolean = when (state.section) {
    AdminSection.Overview -> state.overview is LoadResult.Loading
    AdminSection.Review -> state.reviewSettings is LoadResult.Loading || state.reviewRequests is LoadResult.Loading
    AdminSection.Keys -> state.keys is LoadResult.Loading || state.baseUrlRules is LoadResult.Loading
    AdminSection.OperationLogs -> state.operationLogs is LoadResult.Loading
    AdminSection.Scraper -> state.cookieConfigs is LoadResult.Loading || state.schedulerLogs is LoadResult.Loading
    AdminSection.Shop -> state.shopItems is LoadResult.Loading
}

internal fun adminOverviewDayOptions(): List<Int> = listOf(5, 15, 30)

internal fun adminSectionDisplayLabel(section: AdminSection): String = when (section) {
    AdminSection.Overview -> "总览"
    AdminSection.Review -> "审核"
    AdminSection.Keys -> "Key"
    AdminSection.OperationLogs -> "操作日志"
    AdminSection.Scraper -> "爬虫"
    AdminSection.Shop -> "商店"
}

internal fun adminReviewTypeLabel(type: String): String = when (type) {
    "upload" -> "上传请求"
    "delete" -> "删除请求"
    else -> type.ifBlank { "未知类型" }
}

internal fun adminReviewStatusLabel(status: String): String = when (status) {
    "approved" -> "已通过"
    "rejected" -> "已拒绝"
    else -> "待审核"
}

internal fun adminKeyStatusLabel(status: String): String = when (status) {
    "approved" -> "已通过"
    "rejected" -> "已拒绝"
    else -> "待审核"
}

internal fun adminOperationStatusLabel(status: String): String = when (status) {
    "success", "completed" -> "成功"
    "failed", "error" -> "失败"
    "pending", "processing", "running" -> "进行中"
    else -> status.ifBlank { "未知" }
}

internal fun adminOperationActionLabel(action: String): String = when (action) {
    "fetch_new_chapter", "fetch_new_chapters", "fetch_chapters" -> "获取新章"
    "retranslate" -> "重新翻译"
    "download_epub" -> "下载 EPUB"
    "download_raw" -> "下载生肉"
    "download_txt" -> "下载 TXT"
    "user_translate" -> "用户翻译"
    "fetch_original" -> "获取原文"
    "batch_translate" -> "批量翻译"
    "upload_novel", "upload_book" -> "上传小说"
    "delete_novel" -> "删除小说"
    "edit_chapter" -> "编辑章节"
    else -> action.ifBlank { "未知操作" }
}

internal fun adminBaseUrlActionLabel(action: String): String = when (action) {
    "allow" -> "允许"
    "block" -> "阻止"
    else -> "人工"
}

internal fun adminShopTypeLabel(type: String): String = when (type) {
    "frame" -> "头像框"
    "badge" -> "徽章"
    else -> type.ifBlank { "商品" }
}

/**
 * Badge CSS is server-authored. Native previews extract only background paint metadata and never
 * execute CSS or HTML. The gradient parser intentionally looks at `background` before border,
 * shadow, or text colors so different source badges stay visually different.
 */
internal fun adminShopBadgePreviewResolvedCss(css: String?): String {
    val source = css.orEmpty()
    if (source.isBlank()) return source
    val variables = Regex(
        """(--[a-z0-9_-]+)\s*:\s*([^;{}]+)""",
        RegexOption.IGNORE_CASE,
    ).findAll(source).associate { match ->
        match.groupValues[1] to match.groupValues[2].trim()
    }
    if (variables.isEmpty()) return source

    val variableReference = Regex(
        """var\(\s*(--[a-z0-9_-]+)(?:\s*,\s*([^)]*))?\s*\)""",
        RegexOption.IGNORE_CASE,
    )
    var resolved = source
    repeat(6) {
        val next = variableReference.replace(resolved) { match ->
            variables[match.groupValues[1]]
                ?: match.groupValues.getOrNull(2)?.trim().orEmpty()
                .ifBlank { match.value }
        }
        if (next == resolved) return resolved
        resolved = next
    }
    return resolved
}

internal fun adminShopBadgePreviewColors(css: String?): List<Color> {
    val source = adminShopBadgePreviewResolvedCss(css)
    val background = Regex(
        """background(?:-color|-image)?\s*:\s*([^;]+)""",
        RegexOption.IGNORE_CASE
    ).findAll(source).joinToString(" ") { match ->
        match.groupValues.drop(1).firstOrNull(String::isNotBlank).orEmpty()
    }.ifBlank { source }
    val vividColors = mutableListOf<Color>()
    val rgba = Regex(
        """rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*(?:[01](?:\.\d+)?|\.\d+))?\s*\)""",
        RegexOption.IGNORE_CASE
    )
    fun addColor(red: Int, green: Int, blue: Int) {
        val color = Color(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))
        if (color !in vividColors) vividColors += color
    }
    rgba.findAll(background).forEach { match ->
        addColor(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }
    val hex = Regex("""#([0-9a-f]{3}|[0-9a-f]{6})(?![0-9a-f])""", RegexOption.IGNORE_CASE)
    hex.findAll(background).forEach { match ->
        val token = match.groupValues[1]
        val expanded = if (token.length == 3) token.map { "$it$it" }.joinToString("") else token
        addColor(expanded.substring(0, 2).toInt(16), expanded.substring(2, 4).toInt(16), expanded.substring(4, 6).toInt(16))
    }
    return when {
        vividColors.size >= 2 -> vividColors.take(4)
        vividColors.size == 1 -> listOf(vividColors.single(), vividColors.single())
        else -> listOf(Color(0xFF22D3EE), Color(0xFF7C3AED), Color(0xFFA855F7))
    }
}

/**
 * Mirrors the source UserBadge fallback variants when an item has no custom HTML/CSS.  The web
 * chooses these from the badge name (or its artwork filename); native has no reason to collapse
 * every such badge to the same default indigo pill.
 */
internal fun adminShopBadgeFallbackColors(name: String?, imageUrl: String?): List<Color> {
    val source = listOfNotNull(name, imageUrl)
        .joinToString(" ")
        .lowercase()
    return when {
        source.contains("新手") -> listOf(Color(0xFF34D399), Color(0xFF60A5FA))
        source.contains("土豪") || source.contains("富") || source.contains("壕") || source.contains("rich") ->
            listOf(Color(0xFFF59E0B), Color(0xFFF97316))
        source.contains("赛博") || source.contains("cyber") -> listOf(Color(0xFF22D3EE), Color(0xFFA855F7))
        source.contains("自然") || source.contains("森林") || source.contains("绿") || source.contains("nature") ->
            listOf(Color(0xFF22C55E), Color(0xFF10B981))
        else -> listOf(Color(0xFF60A5FA), Color(0xFFA78BFA))
    }
}

/** Prefer the item's source CSS; fall back to the exact named UserBadge palette. */
internal fun adminShopBadgePreviewColors(css: String?, name: String?, imageUrl: String?): List<Color> =
    if (css.isNullOrBlank()) adminShopBadgeFallbackColors(name, imageUrl) else adminShopBadgePreviewColors(css)

/**
 * Resolves a source-owned Badge asset without accepting executable URI schemes.
 *
 * Source Badge CSS can reference `/uploads/...`, `//images...`, or a relative asset in addition
 * to absolute URLs. The previous preview kept only absolute HTTP URLs, so valid uploaded WebP
 * artwork silently became an empty native Badge.
 */
internal fun adminShopBadgePreviewAssetUrl(
    raw: String?,
    baseUrl: String = "https://novalpie.cc",
): String? {
    val value = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
    if (
        value.startsWith("data:", ignoreCase = true) ||
        value.startsWith("javascript:", ignoreCase = true) ||
        value.startsWith("file:", ignoreCase = true) ||
        value.startsWith("content:", ignoreCase = true)
    ) {
        return null
    }

    return runCatching {
        java.net.URI(baseUrl.trimEnd('/') + "/")
            .resolve(value)
            .normalize()
            .takeIf { uri ->
                uri.scheme.equals("http", ignoreCase = true) ||
                    uri.scheme.equals("https", ignoreCase = true)
            }
            ?.takeIf { uri -> !uri.host.isNullOrBlank() }
            ?.toString()
    }.getOrNull()
}

/** Extracts the first safely resolvable CSS background asset without executing source CSS. */
internal fun adminShopBadgePreviewBackgroundImageUrl(css: String?): String? {
    val urlPattern = Regex(
        """url\(\s*(?:(['"])(.*?)\1|([^\s)]+))\s*\)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return urlPattern.findAll(adminShopBadgePreviewResolvedCss(css))
        .map { match -> match.groupValues[2].ifBlank { match.groupValues[3] } }
        .mapNotNull(::adminShopBadgePreviewAssetUrl)
        .firstOrNull()
}

/** Preserve the source badge's rounded/square silhouette without accepting arbitrary CSS. */
internal fun adminShopBadgePreviewCornerRadius(css: String?, maxRadiusDp: Int = 21): Int {
    val maxRadius = maxRadiusDp.coerceAtLeast(0)
    val match = Regex("""border-radius\s*:\s*(\d+(?:\.\d+)?)(px|rem|%)?""", RegexOption.IGNORE_CASE)
        .find(adminShopBadgePreviewResolvedCss(css))
        // The source component defaults to `border-radius: 9999px`; a 42dp preview can only
        // visibly use half of its height, so keep that pill silhouette without oversized values.
        ?: return maxRadius
    val value = match.groupValues.getOrNull(1)?.toFloatOrNull() ?: return maxRadius
    return when (match.groupValues.getOrNull(2)?.lowercase()) {
        "%" -> (value.coerceIn(0f, 50f) / 50f * maxRadius).toInt()
        "rem" -> (value * 16f).toInt().coerceIn(0, maxRadius)
        else -> value.toInt().coerceIn(0, maxRadius)
    }
}

/** Source badge background images commonly use contain for artwork and cover for banners. */
internal fun adminShopBadgePreviewContentScale(css: String?): ContentScale = when {
    Regex("""(?:background-size\s*:\s*contain|/\s*contain\b)""", RegexOption.IGNORE_CASE)
        .containsMatchIn(adminShopBadgePreviewResolvedCss(css)) -> ContentScale.Fit
    else -> ContentScale.Crop
}

/** Border paint is visual metadata, not executable CSS. Fall back to the website default. */
internal fun adminShopBadgePreviewBorderColor(css: String?): Color {
    val token = Regex("""border(?:-color)?\s*:\s*(?:\d+(?:\.\d+)?px\s+(?:solid|dashed|dotted)\s+)?(#[0-9a-f]{3,6}|rgba?\([^)]*\))""", RegexOption.IGNORE_CASE)
        .find(adminShopBadgePreviewResolvedCss(css))
        ?.groupValues
        ?.getOrNull(1)
        ?: return Color.White.copy(alpha = 0.46f)
    return adminShopBadgePreviewColors("background: $token;").firstOrNull() ?: Color.White.copy(alpha = 0.46f)
}

/** Source custom badge styles can override the default white label with `color`. */
internal fun adminShopBadgePreviewTextColor(css: String?): Color {
    val token = Regex("""(?:^|[;{}\s])color\s*:\s*(#[0-9a-f]{3,6}|rgba?\([^)]*\))""", RegexOption.IGNORE_CASE)
        .find(adminShopBadgePreviewResolvedCss(css))
        ?.groupValues
        ?.getOrNull(1)
        ?: return Color.White
    return adminShopBadgePreviewColors("background: $token;").firstOrNull() ?: Color.White
}

internal fun adminShopBadgePreviewText(html: String?, fallback: String): String =
    html.orEmpty()
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { fallback }

/**
 * The source replaces template variables before mounting badge HTML. Keep visible text only;
 * icon fonts and unsafe attributes are deliberately not executed in the native client.
 */
internal fun adminShopBadgePreviewForeground(
    html: String?,
    fallback: String,
    description: String? = null,
    id: Long? = null,
): String? {
    val source = html.orEmpty()
    if (source.isBlank()) return null
    val text = adminShopBadgePreviewText(
        source
            .replace(Regex("""\{\{\s*name\s*}}""", RegexOption.IGNORE_CASE), fallback)
            .replace(Regex("""\{\{\s*description\s*}}""", RegexOption.IGNORE_CASE), description.orEmpty())
            .replace(Regex("""\{\{\s*id\s*}}""", RegexOption.IGNORE_CASE), id?.toString().orEmpty()),
        fallback
    )
    return text.takeIf(String::isNotBlank)
}

/** The source badge template commonly includes this small leading ornament. */
internal fun adminShopBadgePreviewHasDot(html: String?): Boolean =
    Regex("""\bbadge__(?:dot|icon)\b""", RegexOption.IGNORE_CASE).containsMatchIn(html.orEmpty())

/** Extract only the badge-dot background paint, with the source's white fallback. */
internal fun adminShopBadgePreviewDotColor(css: String?): Color {
    val source = adminShopBadgePreviewResolvedCss(css)
    val dotRule = Regex(
        """\.badge__(?:dot|icon)\s*\{([^}]*)\}""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).find(source)?.groupValues?.getOrNull(1)
    if (dotRule.isNullOrBlank()) return Color.White
    return adminShopBadgePreviewColors(dotRule).firstOrNull() ?: Color.White
}

/** Local Android document URIs are intentionally never serialized to the shop API. */
internal fun adminShopRemoteImageUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val localScheme = listOf("content:", "file:", "android.resource:", "data:")
    return value.takeUnless { candidate -> localScheme.any { candidate.startsWith(it, ignoreCase = true) } }
}

internal fun adminShopLocalPreviewNeedsRemoteUrl(
    type: String,
    remoteImageUrl: String?,
    hasLocalPreview: Boolean
): Boolean = type == "frame" && hasLocalPreview && remoteImageUrl == null

@Composable
private fun adminReviewStatusTone(status: String): NpChipTone = when (status) {
    "approved" -> NpChipTone.Status
    "rejected" -> NpChipTone.Warning
    else -> NpChipTone.Neutral
}

@Composable
private fun adminKeyStatusTone(status: String): NpChipTone = when (status) {
    "approved" -> NpChipTone.Status
    "rejected" -> NpChipTone.Warning
    else -> NpChipTone.Neutral
}

@Composable
private fun adminOperationStatusTone(status: String): NpChipTone = when (status) {
    "success", "completed" -> NpChipTone.Status
    "failed", "error" -> NpChipTone.Warning
    else -> NpChipTone.Neutral
}

@Composable
private fun adminBaseUrlActionTone(action: String): NpChipTone = when (action) {
    "allow" -> NpChipTone.Status
    "block" -> NpChipTone.Warning
    else -> NpChipTone.Neutral
}

@Composable
private fun adminOperationStatusColor(status: String): Color = when (status) {
    "success", "completed" -> MaterialTheme.colorScheme.tertiaryContainer
    "failed", "error" -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surfaceContainerHigh
}
