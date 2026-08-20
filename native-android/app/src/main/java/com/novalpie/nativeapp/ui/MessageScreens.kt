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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.DirectMessage
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.MessageSettings
import com.novalpie.nativeapp.model.MessageStats
import com.novalpie.nativeapp.model.SiteMessage

@Composable
internal fun MessageCenterScreen(
    state: MessageCenterState,
    hasAuthToken: Boolean,
    onOpenLogin: () -> Unit,
    onRefresh: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onTypeSelected: (Int?) -> Unit,
    onReadSelected: (Boolean?) -> Unit,
    onPrioritySelected: (Int?) -> Unit,
    onToggleSelected: (Long) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onMarkSelectedRead: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMarkAllRead: () -> Unit,
    onToggleStar: (SiteMessage) -> Unit,
    onOpenMessage: (SiteMessage) -> Unit,
    onLoadMore: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var confirmBatchDelete by remember { mutableStateOf(false) }
    if (confirmBatchDelete) {
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text("批量删除") },
            text = { Text("确定删除已选中的 ${state.selectedIds.size} 条消息吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBatchDelete = false
                    onDeleteSelected()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("取消") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MessageCenterHero(state.stats, onRefresh, onOpenSettings) }

        if (!hasAuthToken) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("需要登录", fontWeight = FontWeight.Bold)
                            Text("登录网站账号后才能同步通知与私信", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = onOpenLogin) { Text("登录") }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.query.keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜索标题或内容") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "搜索") } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
        }

        item {
            MessageFilterRail(
                label = "消息类型",
                options = listOf(null to messageTypeLabel(null)) + messageTypeOptions().map { it.value to it.label },
                selected = state.query.messageType,
                onSelected = onTypeSelected
            )
        }
        item {
            MessageFilterRail(
                label = "已读状态",
                options = listOf(null to "全部", false to "未读", true to "已读"),
                selected = state.query.isRead,
                onSelected = onReadSelected
            )
        }
        item {
            MessageFilterRail(
                label = "优先级",
                options = listOf(null to "全部", 0 to "普通", 1 to "重要", 2 to "紧急"),
                selected = state.query.priority,
                onSelected = onPrioritySelected
            )
        }

        state.actionMessage?.let { message -> item { MessageNotice(message) } }

        if (state.selectedIds.isNotEmpty()) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("已选 ${state.selectedIds.size} 条消息", fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Button(onClick = onMarkSelectedRead, enabled = !state.actionLoading) { Text("标记已读") } }
                            item { OutlinedButton(onClick = { confirmBatchDelete = true }, enabled = !state.actionLoading) { Text("删除") } }
                            item { TextButton(onClick = { onSelectAll(false) }) { Text("取消选择") } }
                        }
                    }
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("收件箱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { onSelectAll(true) }) { Text("全选") }
                        TextButton(onClick = onMarkAllRead, enabled = !state.actionLoading) { Text("全部已读") }
                    }
                }
            }
        }

        when (val messages = state.messages) {
            LoadResult.Idle -> item { MessageEmpty("等待同步消息") }
            LoadResult.Loading -> item { MessageLoading("正在同步收件箱") }
            is LoadResult.Error -> item { MessageError(messages.message, onRefresh) }
            is LoadResult.Success -> {
                if (messages.value.isEmpty()) {
                    item { MessageEmpty("没有匹配的消息") }
                } else {
                    items(messages.value, key = { it.id }) { message ->
                        MessageInboxCard(
                            message = message,
                            selected = message.id in state.selectedIds,
                            onToggleSelected = { onToggleSelected(message.id) },
                            onToggleStar = { onToggleStar(message) },
                            onOpen = { onOpenMessage(message) }
                        )
                    }
                }
            }
        }

        if (state.pagination.page < state.pagination.totalPages) {
            item {
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth(), enabled = !state.loadingMore) {
                    Text(if (state.loadingMore) "加载中..." else "加载更多")
                }
            }
        }
    }
}

@Composable
private fun MessageCenterHero(
    stats: LoadResult<MessageStats>,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("我的消息", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("通知、回复与私信", color = Color.White.copy(alpha = 0.82f))
                }
                Row {
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = Color.White) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "消息设置", tint = Color.White) }
                }
            }
            when (stats) {
                is LoadResult.Success -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { HeroStat("未读", stats.value.unreadCount) }
                    item { HeroStat("全部", stats.value.totalCount) }
                    item { HeroStat("重要", stats.value.importantCount) }
                    item { HeroStat("星标", stats.value.starredCount) }
                }
                LoadResult.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                else -> Text("暂未取得消息统计", color = Color.White.copy(alpha = 0.82f))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: Int) {
    Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun <T> MessageFilterRail(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
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
private fun MessageInboxCard(
    message: SiteMessage,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onToggleStar: () -> Unit,
    onOpen: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.secondaryContainer
                !message.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!message.isRead) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        message.title,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (message.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "星标",
                            tint = if (message.isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                message.content?.let { content ->
                    Text(
                        plainMessageText(content),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = onOpen, label = { Text(messageTypeLabel(message.type)) })
                        if (message.priority > 0) {
                            AssistChip(onClick = onOpen, label = { Text(if (message.priority == 2) "紧急" else "重要") })
                        }
                    }
                    Text(messageDateLabel(message.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun MessageDetailScreen(
    state: MessageDetailState,
    onRetry: () -> Unit,
    onMarkRead: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    onOpenAction: (String) -> Unit,
    onOpenConversation: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除消息") },
            text = { Text("删除后将从收件箱移除，确定继续吗？") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (val detail = state.detail) {
            LoadResult.Idle -> item { MessageEmpty("等待加载消息") }
            LoadResult.Loading -> item { MessageLoading("正在加载消息详情") }
            is LoadResult.Error -> item { MessageError(detail.message, onRetry) }
            is LoadResult.Success -> {
                val message = detail.value
                item {
                    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AssistChip(onClick = {}, label = { Text(messageTypeLabel(message.type)) })
                            Text(message.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                message.username?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                                Text(messageDateLabel(message.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SelectionContainer {
                                Text(
                                    plainMessageText(message.content.orEmpty()),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (message.extraData.isNotEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(14.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("附加信息", fontWeight = FontWeight.Bold)
                                        message.extraData.forEach { (key, value) -> Text("$key: $value", style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!message.isRead) item { Button(onClick = onMarkRead, enabled = !state.actionLoading) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text("标记已读") } }
                        item { OutlinedButton(onClick = onToggleStar, enabled = !state.actionLoading) { Icon(if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder, null); Spacer(Modifier.width(6.dp)); Text(if (message.isStarred) "取消星标" else "添加星标") } }
                        if (message.type == 8) item { OutlinedButton(onClick = onOpenConversation) { Icon(Icons.Filled.Mail, null); Spacer(Modifier.width(6.dp)); Text("打开私信") } }
                        message.actionUrl?.let { url -> item { Button(onClick = { onOpenAction(url) }) { Text(message.actionText ?: "打开相关内容") } } }
                        item { OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.actionLoading) { Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(6.dp)); Text("删除") } }
                    }
                }
                state.actionMessage?.let { item { MessageNotice(it) } }
            }
        }
    }
}

@Composable
internal fun MessageConversationScreen(
    state: MessageConversationState,
    currentUserId: Long?,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(state.targetName ?: "私信对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("与用户 #${state.targetUserId} 的对话", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
        }
        when (val messages = state.messages) {
            LoadResult.Idle -> MessageEmpty("等待加载私信")
            LoadResult.Loading -> MessageLoading("正在同步私信")
            is LoadResult.Error -> MessageError(messages.message, onRetry)
            is LoadResult.Success -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.value.isEmpty()) item { MessageEmpty("还没有私信，发送第一条消息吧") }
                items(messages.value, key = { it.id }) { message ->
                    DirectMessageBubble(message, currentUserId)
                }
            }
        }
        state.actionMessage?.let { MessageNotice(it) }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入私信内容") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.sending) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
        }
    }
}

@Composable
private fun DirectMessageBubble(message: DirectMessage, currentUserId: Long?) {
    val outgoing = currentUserId != null && message.executeUserId == currentUserId
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (outgoing) 18.dp else 4.dp,
                bottomEnd = if (outgoing) 4.dp else 18.dp
            ),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(message.content)
                Text(messageDateLabel(message.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun MessageSettingsScreen(
    state: MessageSettingsState,
    onRetry: () -> Unit,
    onDraftChange: ((MessageSettings) -> MessageSettings) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("消息设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("管理通知方式、免打扰时间和自动已读", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (val settings = state.settings) {
            LoadResult.Idle -> item { MessageEmpty("等待加载设置") }
            LoadResult.Loading -> item { MessageLoading("正在同步消息设置") }
            is LoadResult.Error -> item { MessageError(settings.message, onRetry) }
            is LoadResult.Success -> {
                item {
                    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MessageSettingSwitch("启用通知", "接收站内系统消息", state.draft.enableNotifications) {
                                onDraftChange { it.copy(enableNotifications = !it.enableNotifications) }
                            }
                            MessageSettingSwitch("邮件通知", "通过邮件接收重要通知", state.draft.enableEmail) {
                                onDraftChange { it.copy(enableEmail = !it.enableEmail) }
                            }
                            MessageSettingSwitch("浏览器推送", "允许设备推送消息", state.draft.enableBrowserPush) {
                                onDraftChange { it.copy(enableBrowserPush = !it.enableBrowserPush) }
                            }
                        }
                    }
                }
                item {
                    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("通知类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("未筛选时接收全部类型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(messageTypeOptions()) { option ->
                                    val active = state.draft.notificationTypes?.contains(option.value) ?: true
                                    FilterChip(
                                        selected = active,
                                        onClick = {
                                            onDraftChange { current ->
                                                val all = (1..10).toSet()
                                                val selected = current.notificationTypes ?: all
                                                val next = if (option.value in selected) selected - option.value else selected + option.value
                                                current.copy(notificationTypes = if (next == all) null else next)
                                            }
                                        },
                                        label = { Text(option.label) }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("免打扰与自动处理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = state.draft.quietHoursStart.orEmpty(),
                                    onValueChange = { value -> onDraftChange { it.copy(quietHoursStart = value.ifBlank { null }) } },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("开始 HH:mm") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.draft.quietHoursEnd.orEmpty(),
                                    onValueChange = { value -> onDraftChange { it.copy(quietHoursEnd = value.ifBlank { null }) } },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("结束 HH:mm") },
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = state.draft.autoReadAfterDays?.toString().orEmpty(),
                                onValueChange = { value -> onDraftChange { it.copy(autoReadAfterDays = value.toIntOrNull()) } },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("多少天后自动已读") },
                                supportingText = { Text("0 表示不自动处理") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
                state.actionMessage?.let { item { MessageNotice(it) } }
                item {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), enabled = !state.saving) {
                        Text(if (state.saving) "保存中..." else "保存消息设置")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageSettingSwitch(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun MessageLoading(label: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageError(message: String, onRetry: () -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            OutlinedButton(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun MessageEmpty(label: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Mail, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageNotice(message: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

private fun plainMessageText(value: String): String = value
    .replace(Regex("<[^>]+>"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun messageDateLabel(value: String?): String = value
    ?.replace('T', ' ')
    ?.take(16)
    .orEmpty()
