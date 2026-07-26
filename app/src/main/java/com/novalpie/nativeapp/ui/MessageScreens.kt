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
import androidx.compose.material.icons.filled.Send
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
            title = { Text("\u6279\u91cf\u5220\u9664") },
            text = { Text("\u786e\u5b9a\u5220\u9664\u5df2\u9009\u4e2d\u7684 ${state.selectedIds.size} \u6761\u6d88\u606f\u5417\uff1f") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBatchDelete = false
                    onDeleteSelected()
                }) { Text("\u5220\u9664") }
            },
            dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("\u53d6\u6d88") } }
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
                            Text("\u9700\u8981\u767b\u5f55", fontWeight = FontWeight.Bold)
                            Text("\u767b\u5f55\u7f51\u7ad9\u8d26\u53f7\u540e\u624d\u80fd\u540c\u6b65\u901a\u77e5\u4e0e\u79c1\u4fe1", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = onOpenLogin) { Text("\u767b\u5f55") }
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
                label = { Text("\u641c\u7d22\u6807\u9898\u6216\u5185\u5bb9") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "\u641c\u7d22") } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
        }

        item {
            MessageFilterRail(
                label = "\u6d88\u606f\u7c7b\u578b",
                options = listOf(null to messageTypeLabel(null)) + messageTypeOptions().map { it.value to it.label },
                selected = state.query.messageType,
                onSelected = onTypeSelected
            )
        }
        item {
            MessageFilterRail(
                label = "\u5df2\u8bfb\u72b6\u6001",
                options = listOf(null to "\u5168\u90e8", false to "\u672a\u8bfb", true to "\u5df2\u8bfb"),
                selected = state.query.isRead,
                onSelected = onReadSelected
            )
        }
        item {
            MessageFilterRail(
                label = "\u4f18\u5148\u7ea7",
                options = listOf(null to "\u5168\u90e8", 0 to "\u666e\u901a", 1 to "\u91cd\u8981", 2 to "\u7d27\u6025"),
                selected = state.query.priority,
                onSelected = onPrioritySelected
            )
        }

        state.actionMessage?.let { message -> item { MessageNotice(message) } }

        if (state.selectedIds.isNotEmpty()) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("\u5df2\u9009 ${state.selectedIds.size} \u6761\u6d88\u606f", fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Button(onClick = onMarkSelectedRead, enabled = !state.actionLoading) { Text("\u6807\u8bb0\u5df2\u8bfb") } }
                            item { OutlinedButton(onClick = { confirmBatchDelete = true }, enabled = !state.actionLoading) { Text("\u5220\u9664") } }
                            item { TextButton(onClick = { onSelectAll(false) }) { Text("\u53d6\u6d88\u9009\u62e9") } }
                        }
                    }
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("\u6536\u4ef6\u7bb1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { onSelectAll(true) }) { Text("\u5168\u9009") }
                        TextButton(onClick = onMarkAllRead, enabled = !state.actionLoading) { Text("\u5168\u90e8\u5df2\u8bfb") }
                    }
                }
            }
        }

        when (val messages = state.messages) {
            LoadResult.Idle -> item { MessageEmpty("\u7b49\u5f85\u540c\u6b65\u6d88\u606f") }
            LoadResult.Loading -> item { MessageLoading("\u6b63\u5728\u540c\u6b65\u6536\u4ef6\u7bb1") }
            is LoadResult.Error -> item { MessageError(messages.message, onRefresh) }
            is LoadResult.Success -> {
                if (messages.value.isEmpty()) {
                    item { MessageEmpty("\u6ca1\u6709\u5339\u914d\u7684\u6d88\u606f") }
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
                    Text(if (state.loadingMore) "\u52a0\u8f7d\u4e2d..." else "\u52a0\u8f7d\u66f4\u591a")
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
                    Text("\u6211\u7684\u6d88\u606f", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("\u901a\u77e5\u3001\u56de\u590d\u4e0e\u79c1\u4fe1", color = Color.White.copy(alpha = 0.82f))
                }
                Row {
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "\u5237\u65b0", tint = Color.White) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "\u6d88\u606f\u8bbe\u7f6e", tint = Color.White) }
                }
            }
            when (stats) {
                is LoadResult.Success -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { HeroStat("\u672a\u8bfb", stats.value.unreadCount) }
                    item { HeroStat("\u5168\u90e8", stats.value.totalCount) }
                    item { HeroStat("\u91cd\u8981", stats.value.importantCount) }
                    item { HeroStat("\u661f\u6807", stats.value.starredCount) }
                }
                LoadResult.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                else -> Text("\u6682\u672a\u53d6\u5f97\u6d88\u606f\u7edf\u8ba1", color = Color.White.copy(alpha = 0.82f))
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
                            contentDescription = "\u661f\u6807",
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
                            AssistChip(onClick = onOpen, label = { Text(if (message.priority == 2) "\u7d27\u6025" else "\u91cd\u8981") })
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
            title = { Text("\u5220\u9664\u6d88\u606f") },
            text = { Text("\u5220\u9664\u540e\u5c06\u4ece\u6536\u4ef6\u7bb1\u79fb\u9664\uff0c\u786e\u5b9a\u7ee7\u7eed\u5417\uff1f") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("\u5220\u9664") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("\u53d6\u6d88") } }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (val detail = state.detail) {
            LoadResult.Idle -> item { MessageEmpty("\u7b49\u5f85\u52a0\u8f7d\u6d88\u606f") }
            LoadResult.Loading -> item { MessageLoading("\u6b63\u5728\u52a0\u8f7d\u6d88\u606f\u8be6\u60c5") }
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
                                        Text("\u9644\u52a0\u4fe1\u606f", fontWeight = FontWeight.Bold)
                                        message.extraData.forEach { (key, value) -> Text("$key: $value", style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!message.isRead) item { Button(onClick = onMarkRead, enabled = !state.actionLoading) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text("\u6807\u8bb0\u5df2\u8bfb") } }
                        item { OutlinedButton(onClick = onToggleStar, enabled = !state.actionLoading) { Icon(if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder, null); Spacer(Modifier.width(6.dp)); Text(if (message.isStarred) "\u53d6\u6d88\u661f\u6807" else "\u6dfb\u52a0\u661f\u6807") } }
                        if (message.type == 8) item { OutlinedButton(onClick = onOpenConversation) { Icon(Icons.Filled.Mail, null); Spacer(Modifier.width(6.dp)); Text("\u6253\u5f00\u79c1\u4fe1") } }
                        message.actionUrl?.let { url -> item { Button(onClick = { onOpenAction(url) }) { Text(message.actionText ?: "\u6253\u5f00\u76f8\u5173\u5185\u5bb9") } } }
                        item { OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.actionLoading) { Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(6.dp)); Text("\u5220\u9664") } }
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
                Text(state.targetName ?: "\u79c1\u4fe1\u5bf9\u8bdd", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("\u4e0e\u7528\u6237 #${state.targetUserId} \u7684\u5bf9\u8bdd", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, contentDescription = "\u5237\u65b0") }
        }
        when (val messages = state.messages) {
            LoadResult.Idle -> MessageEmpty("\u7b49\u5f85\u52a0\u8f7d\u79c1\u4fe1")
            LoadResult.Loading -> MessageLoading("\u6b63\u5728\u540c\u6b65\u79c1\u4fe1")
            is LoadResult.Error -> MessageError(messages.message, onRetry)
            is LoadResult.Success -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.value.isEmpty()) item { MessageEmpty("\u8fd8\u6ca1\u6709\u79c1\u4fe1\uff0c\u53d1\u9001\u7b2c\u4e00\u6761\u6d88\u606f\u5427") }
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
                placeholder = { Text("\u8f93\u5165\u79c1\u4fe1\u5185\u5bb9") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.sending) {
                Icon(Icons.Filled.Send, contentDescription = "\u53d1\u9001")
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
                Text("\u6d88\u606f\u8bbe\u7f6e", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("\u7ba1\u7406\u901a\u77e5\u65b9\u5f0f\u3001\u514d\u6253\u6270\u65f6\u95f4\u548c\u81ea\u52a8\u5df2\u8bfb", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (val settings = state.settings) {
            LoadResult.Idle -> item { MessageEmpty("\u7b49\u5f85\u52a0\u8f7d\u8bbe\u7f6e") }
            LoadResult.Loading -> item { MessageLoading("\u6b63\u5728\u540c\u6b65\u6d88\u606f\u8bbe\u7f6e") }
            is LoadResult.Error -> item { MessageError(settings.message, onRetry) }
            is LoadResult.Success -> {
                item {
                    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MessageSettingSwitch("\u542f\u7528\u901a\u77e5", "\u63a5\u6536\u7ad9\u5185\u7cfb\u7edf\u6d88\u606f", state.draft.enableNotifications) {
                                onDraftChange { it.copy(enableNotifications = !it.enableNotifications) }
                            }
                            MessageSettingSwitch("\u90ae\u4ef6\u901a\u77e5", "\u901a\u8fc7\u90ae\u4ef6\u63a5\u6536\u91cd\u8981\u901a\u77e5", state.draft.enableEmail) {
                                onDraftChange { it.copy(enableEmail = !it.enableEmail) }
                            }
                            MessageSettingSwitch("\u6d4f\u89c8\u5668\u63a8\u9001", "\u5141\u8bb8\u8bbe\u5907\u63a8\u9001\u6d88\u606f", state.draft.enableBrowserPush) {
                                onDraftChange { it.copy(enableBrowserPush = !it.enableBrowserPush) }
                            }
                        }
                    }
                }
                item {
                    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("\u901a\u77e5\u7c7b\u578b", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("\u672a\u7b5b\u9009\u65f6\u63a5\u6536\u5168\u90e8\u7c7b\u578b", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("\u514d\u6253\u6270\u4e0e\u81ea\u52a8\u5904\u7406", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = state.draft.quietHoursStart.orEmpty(),
                                    onValueChange = { value -> onDraftChange { it.copy(quietHoursStart = value.ifBlank { null }) } },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("\u5f00\u59cb HH:mm") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.draft.quietHoursEnd.orEmpty(),
                                    onValueChange = { value -> onDraftChange { it.copy(quietHoursEnd = value.ifBlank { null }) } },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("\u7ed3\u675f HH:mm") },
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = state.draft.autoReadAfterDays?.toString().orEmpty(),
                                onValueChange = { value -> onDraftChange { it.copy(autoReadAfterDays = value.toIntOrNull()) } },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("\u591a\u5c11\u5929\u540e\u81ea\u52a8\u5df2\u8bfb") },
                                supportingText = { Text("0 \u8868\u793a\u4e0d\u81ea\u52a8\u5904\u7406") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
                state.actionMessage?.let { item { MessageNotice(it) } }
                item {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), enabled = !state.saving) {
                        Text(if (state.saving) "\u4fdd\u5b58\u4e2d..." else "\u4fdd\u5b58\u6d88\u606f\u8bbe\u7f6e")
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
            OutlinedButton(onClick = onRetry) { Text("\u91cd\u8bd5") }
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
