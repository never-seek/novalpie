package com.novalpie.nativeapp.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun ForumCreateScreen(
    state: ForumCreateState,
    onDraftChange: (ForumCreateDraft) -> Unit,
    onSubmit: () -> Unit,
    onOpenLogin: () -> Unit
) {
    val draft = state.draft
    val validation = validateForumCreateDraft(draft, state.isAdmin)
    var showSubmitConfirmation by remember { mutableStateOf(false) }

    if (showSubmitConfirmation) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            title = { Text("确认发布") },
            text = { Text("帖子将立即发布到“${forumCategoryOptions(state.isAdmin).firstOrNull { it.id == draft.type }?.title.orEmpty()}”分区。") },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitConfirmation = false
                    onSubmit()
                }) { Text("发布") }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) { Text("继续编辑") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("发布帖子", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "分享想法、推书或反馈，正文支持 Markdown。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.accessMessage?.let { message ->
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(message, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = onOpenLogin) { Text("升级或切换账号") }
                    }
                }
            }
        }

        if (draft.type.isBlank()) {
            item { Text("选择发布分区", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(forumCategoryOptions(state.isAdmin), key = { it.id }) { category ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.accessMessage == null) {
                            onDraftChange(draft.copy(type = category.id))
                        }
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(category.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                ForumSelectedCategory(
                    draft = draft,
                    isAdmin = state.isAdmin,
                    onDraftChange = onDraftChange
                )
            }
            item {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { onDraftChange(draft.copy(title = it.take(100))) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题 *") },
                    supportingText = { Text("${draft.title.length}/100") },
                    singleLine = true,
                    enabled = !state.submitting && state.accessMessage == null
                )
            }
            item {
                OutlinedTextField(
                    value = draft.content,
                    onValueChange = { onDraftChange(draft.copy(content = it.take(10_000))) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("正文 *") },
                    placeholder = { Text("支持标题、粗体、列表、引用、代码和链接等 Markdown 语法") },
                    supportingText = { Text("${draft.content.length}/10000") },
                    minLines = 10,
                    enabled = !state.submitting && state.accessMessage == null
                )
            }
            item {
                ForumMarkdownPreview(draft.content)
            }
            item {
                ForumTagEditor(
                    draft = draft,
                    enabled = !state.submitting && state.accessMessage == null,
                    onDraftChange = onDraftChange
                )
            }
            item {
                ForumPollEditor(
                    draft = draft,
                    enabled = !state.submitting && state.accessMessage == null,
                    onDraftChange = onDraftChange
                )
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("发布须知", fontWeight = FontWeight.Bold)
                        Text("• 内容应真实准确并遵守社区规范")
                        Text("• 标题应简明，标签应与主题相关")
                        Text("• 重要链接和复现步骤请直接写入正文")
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val message = state.actionMessage ?: validation.message
                    if (!message.isNullOrBlank()) {
                        Text(
                            message,
                            color = if (validation.canSubmit && state.actionMessage == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Button(
                        onClick = { showSubmitConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = validation.canSubmit && !state.submitting && state.accessMessage == null
                    ) {
                        Text(if (state.submitting) "发布中…" else "发布帖子")
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumSelectedCategory(
    draft: ForumCreateDraft,
    isAdmin: Boolean,
    onDraftChange: (ForumCreateDraft) -> Unit
) {
    val category = forumCategoryOptions(isAdmin).firstOrNull { it.id == draft.type }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(category?.title ?: draft.type, fontWeight = FontWeight.Bold)
                category?.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextButton(onClick = { onDraftChange(draft.copy(type = "")) }) { Text("更换") }
        }
    }
}

@Composable
private fun ForumMarkdownPreview(content: String) {
    if (content.isBlank()) return
    val paragraphs = readerParagraphsFromContent(content)
    val links = forumContentLinks(paragraphs)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("正文预览", fontWeight = FontWeight.Bold)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    paragraphs.take(12).forEach { Text(it) }
                }
            }
            if (links.isNotEmpty()) {
                Text("链接", style = MaterialTheme.typography.labelLarge)
                links.take(4).forEach { link ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(link, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumTagEditor(
    draft: ForumCreateDraft,
    enabled: Boolean,
    onDraftChange: (ForumCreateDraft) -> Unit
) {
    fun addTag() {
        val tag = draft.tagDraft.trim()
        if (tag.isBlank() || tag in draft.tags || draft.tags.size >= 5 || tag.length > 20) return
        onDraftChange(draft.copy(tags = draft.tags + tag, tagDraft = ""))
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("标签（选填）", fontWeight = FontWeight.Bold)
            if (draft.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(draft.tags) { tag ->
                        AssistChip(
                            onClick = { onDraftChange(draft.copy(tags = draft.tags - tag)) },
                            label = { Text("#$tag  ×") },
                            enabled = enabled
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.tagDraft,
                    onValueChange = { onDraftChange(draft.copy(tagDraft = it.take(20))) },
                    modifier = Modifier.weight(1f),
                    label = { Text("输入标签") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() }),
                    enabled = enabled
                )
                OutlinedButton(onClick = { addTag() }, enabled = enabled && draft.tags.size < 5) { Text("添加") }
            }
            Text("最多 5 个，每个不超过 20 个字符", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ForumPollEditor(
    draft: ForumCreateDraft,
    enabled: Boolean,
    onDraftChange: (ForumCreateDraft) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("投票（可选）", fontWeight = FontWeight.Bold)
                    Text("2–10 个选项", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = draft.pollEnabled,
                    onCheckedChange = { onDraftChange(draft.copy(pollEnabled = it)) },
                    enabled = enabled
                )
            }
            if (draft.pollEnabled) {
                OutlinedTextField(
                    value = draft.pollQuestion,
                    onValueChange = { onDraftChange(draft.copy(pollQuestion = it.take(200))) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("投票问题") },
                    placeholder = { Text("留空时使用帖子标题") },
                    enabled = enabled
                )
                draft.pollOptions.forEachIndexed { index, option ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { value ->
                                onDraftChange(draft.copy(pollOptions = draft.pollOptions.toMutableList().apply { this[index] = value.take(200) }))
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("选项 ${index + 1}") },
                            enabled = enabled
                        )
                        TextButton(
                            onClick = {
                                val next = draft.pollOptions.toMutableList().apply { removeAt(index) }
                                onDraftChange(draft.copy(pollOptions = next, pollMaxChoices = draft.pollMaxChoices.coerceAtMost(next.size)))
                            },
                            enabled = enabled && draft.pollOptions.size > 2
                        ) { Text("删除") }
                    }
                }
                OutlinedButton(
                    onClick = { onDraftChange(draft.copy(pollOptions = draft.pollOptions + "")) },
                    enabled = enabled && draft.pollOptions.size < 10
                ) { Text("添加选项") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("允许多选")
                    Switch(
                        checked = draft.pollAllowMultiple,
                        onCheckedChange = { onDraftChange(draft.copy(pollAllowMultiple = it)) },
                        enabled = enabled
                    )
                }
                if (draft.pollAllowMultiple) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("最多选择")
                        OutlinedButton(
                            onClick = { onDraftChange(draft.copy(pollMaxChoices = (draft.pollMaxChoices - 1).coerceAtLeast(2))) },
                            enabled = enabled && draft.pollMaxChoices > 2
                        ) { Text("−") }
                        Text(draft.pollMaxChoices.toString(), fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { onDraftChange(draft.copy(pollMaxChoices = (draft.pollMaxChoices + 1).coerceAtMost(draft.pollOptions.size))) },
                            enabled = enabled && draft.pollMaxChoices < draft.pollOptions.size
                        ) { Text("+") }
                    }
                }
                OutlinedTextField(
                    value = draft.pollEndsAt,
                    onValueChange = { onDraftChange(draft.copy(pollEndsAt = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("截止时间（选填）") },
                    placeholder = { Text("例如 2026-07-20T08:00:00.000Z") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    enabled = enabled
                )
            }
        }
    }
}
