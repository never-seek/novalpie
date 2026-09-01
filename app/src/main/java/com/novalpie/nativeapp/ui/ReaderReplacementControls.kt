package com.novalpie.nativeapp.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderReplacementOwner
import com.novalpie.nativeapp.model.ReaderReplacementRegexFlag
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementScope
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderReplacementSettingsControls(
    state: ReaderReplacementState,
    currentChapterOrder: Int?,
    prefillSource: String?,
    options: ReaderUiOptions,
    textColor: Color,
    metaColor: Color,
    onOptionsChange: (ReaderUiOptions) -> Unit,
    onSourceChange: (ReaderReplacementRuleSource) -> Unit,
    onSaveRule: (ReaderReplacementRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onSetSharedRuleVisible: (String, Boolean) -> Unit,
    onCloneSharedRule: (ReaderReplacementRule) -> Unit,
    onSharedRulesEnabledChange: (Boolean) -> Unit,
    onDefaultSharedRulesEnabledChange: (Boolean) -> Unit,
    onResetSharedRulesOverride: () -> Unit,
    onPrefillConsumed: () -> Unit,
) {
    val context = LocalContext.current
    var editingRule by remember { mutableStateOf<ReaderReplacementRule?>(null) }
    var clipboardMessage by remember { mutableStateOf<String?>(null) }

    fun createRule(source: String = "") {
        editingRule = ReaderReplacementRule(
            id = "local-${UUID.randomUUID()}",
            novelId = state.novelId,
            source = source,
            replacement = "",
            owner = ReaderReplacementOwner.Personal,
            scope = ReaderReplacementScope.WholeBook,
        )
    }

    LaunchedEffect(prefillSource, state.novelId) {
        val source = prefillSource?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        if (state.novelId <= 0L) return@LaunchedEffect
        if (state.source != ReaderReplacementRuleSource.Personal) {
            onSourceChange(ReaderReplacementRuleSource.Personal)
        }
        clipboardMessage = null
        createRule(source)
        onPrefillConsumed()
    }

    ReaderSettingsSection(title = "正文来源模式", textColor = textColor, metaColor = metaColor) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            readerReplaceModeOptions().forEach { (key, label) ->
                FilterChip(
                    selected = options.replaceMode == key,
                    onClick = { onOptionsChange(options.copy(replaceMode = key)) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }

    ReaderSettingsSection(title = "文本替换", textColor = textColor, metaColor = metaColor) {
        Text(
            text = "个人规则优先于公共规则；规则只改变阅读和听书的显示文本，不修改原章节。",
            style = MaterialTheme.typography.bodySmall,
            color = metaColor,
        )
        ReaderToggleRow(
            label = "本书应用公共规则",
            checked = state.sharedRulesEnabled,
            textColor = textColor,
            metaColor = metaColor,
        ) {
            onSharedRulesEnabledChange(!state.sharedRulesEnabled)
        }
        Text(
            text = if (state.sharedRulesEnabled) {
                "已应用未屏蔽的公共规则；下方可针对本书单独关闭某一条。"
            } else {
                "当前只应用我的规则；仍可在“全部规则”中查看、复制或管理公共规则。"
            },
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
        )
        ReaderToggleRow(
            label = "新书默认应用公共规则",
            checked = state.defaultSharedRulesEnabled,
            textColor = textColor,
            metaColor = metaColor,
        ) {
            onDefaultSharedRulesEnabledChange(!state.defaultSharedRulesEnabled)
        }
        Text(
            text = if (state.sharedRulesEnabledOverride == null) "本书正在跟随默认设置" else "本书已使用独立设置",
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
        )
        if (state.sharedRulesEnabledOverride != null) {
            TextButton(onClick = onResetSharedRulesOverride) {
                Text("恢复跟随默认设置")
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderReplacementRuleSource.entries.forEach { source ->
                FilterChip(
                    selected = state.source == source,
                    onClick = { onSourceChange(source) },
                    label = { Text(if (source == ReaderReplacementRuleSource.Personal) "我的规则" else "全部规则") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        state.actionMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        clipboardMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }

        if (state.source == ReaderReplacementRuleSource.Personal) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    clipboardMessage = null
                    createRule()
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("添加替换规则")
            }
            TextButton(
                onClick = {
                    val clipboardText = runCatching {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                    }.getOrNull()
                    val source = readerReplacementSourceFromClipboard(clipboardText)
                    if (source.isBlank()) {
                        clipboardMessage = "剪贴板中没有可用的替换前文本"
                    } else {
                        clipboardMessage = null
                        createRule(source)
                    }
                },
            ) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("从剪贴板添加")
            }
            if (state.personalRules.isEmpty()) {
                Text("暂无个人规则。可复制公共规则后再按本书需要修改。", style = MaterialTheme.typography.bodySmall, color = metaColor)
            } else {
                state.personalRules.sortedWith(compareBy<ReaderReplacementRule> { it.order }.thenBy { it.id }).forEach { rule ->
                    ReaderReplacementRuleRow(
                        rule = rule,
                        enabled = rule.isEnabled,
                        textColor = textColor,
                        metaColor = metaColor,
                        onEnabledChange = { enabled -> onSaveRule(rule.copy(isEnabled = enabled)) },
                        onEdit = { editingRule = rule },
                        onDelete = { onDeleteRule(rule.id) },
                    )
                }
            }
        } else {
            when (val shared = state.sharedRules) {
                LoadResult.Idle, LoadResult.Loading -> Text("正在加载公共规则…", style = MaterialTheme.typography.bodySmall, color = metaColor)
                is LoadResult.Error -> Text(shared.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                is LoadResult.Success -> {
                    val displayRules = readerReplacementRulesForDisplay(
                        source = ReaderReplacementRuleSource.All,
                        personalRules = state.personalRules,
                        sharedRules = shared.value,
                    )
                    if (displayRules.isEmpty()) {
                        Text("当前书籍暂无公共规则。", style = MaterialTheme.typography.bodySmall, color = metaColor)
                    } else {
                        displayRules.forEach { rule ->
                            val isShared = rule.owner == ReaderReplacementOwner.Shared
                            val visible = !isShared || rule.id !in state.hiddenSharedRuleIds
                            ReaderReplacementRuleRow(
                                rule = rule,
                                enabled = visible,
                                textColor = textColor,
                                metaColor = metaColor,
                                onEnabledChange = { enabled ->
                                    if (isShared) onSetSharedRuleVisible(rule.id, enabled)
                                    else onSaveRule(rule.copy(isEnabled = enabled))
                                },
                                onEdit = if (isShared) null else { { editingRule = rule } },
                                onDelete = if (isShared) null else { { onDeleteRule(rule.id) } },
                                onClone = if (isShared) ({ onCloneSharedRule(rule) }) else null,
                            )
                        }
                    }
                }
            }
        }
    }

    editingRule?.let { rule ->
        ReaderReplacementRuleEditor(
            initial = rule,
            currentChapterOrder = currentChapterOrder,
            onDismiss = { editingRule = null },
            onSave = { saved ->
                onSaveRule(saved)
                editingRule = null
            },
        )
    }
}

@Composable
private fun ReaderReplacementRuleRow(
    rule: ReaderReplacementRule,
    enabled: Boolean,
    textColor: Color,
    metaColor: Color,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onClone: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
        Column(modifier = Modifier.weight(1f)) {
            Text("${rule.source} → ${rule.replacement}", color = textColor, style = MaterialTheme.typography.bodyMedium)
            Text(
                listOfNotNull(
                    if (rule.isRegex) "正则" else "文字",
                    readerReplacementScopeLabel(rule.scope),
                    readerReplacementTargetLabel(rule.target),
                    if (rule.owner == ReaderReplacementOwner.Shared) "公共" else "我的",
                ).joinToString(" · "),
                color = metaColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        onClone?.let { clone ->
            IconButton(onClick = clone) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "复制到我的规则")
            }
        }
        onEdit?.let { edit ->
            IconButton(onClick = edit) { Icon(Icons.Filled.Edit, contentDescription = "编辑规则") }
        }
        onDelete?.let { delete ->
            IconButton(onClick = delete) { Icon(Icons.Filled.Delete, contentDescription = "删除规则") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderReplacementRuleEditor(
    initial: ReaderReplacementRule,
    currentChapterOrder: Int?,
    onDismiss: () -> Unit,
    onSave: (ReaderReplacementRule) -> Unit,
) {
    var source by remember(initial.id) { mutableStateOf(initial.source) }
    var replacement by remember(initial.id) { mutableStateOf(initial.replacement) }
    var isRegex by remember(initial.id) { mutableStateOf(initial.isRegex) }
    var regexFlags by remember(initial.id) { mutableStateOf(initial.regexFlags) }
    var target by remember(initial.id) { mutableStateOf(initial.target) }
    var scope by remember(initial.id) { mutableStateOf(initial.scope) }
    var rangeStart by remember(initial.id) {
        mutableStateOf(
            (initial.scope as? ReaderReplacementScope.ChapterRange)?.startOrder?.toString()
                ?: currentChapterOrder?.toString().orEmpty(),
        )
    }
    var rangeEnd by remember(initial.id) {
        mutableStateOf(
            (initial.scope as? ReaderReplacementScope.ChapterRange)?.endOrder?.toString()
                ?: currentChapterOrder?.toString().orEmpty(),
        )
    }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.source.isBlank()) "添加替换规则" else "编辑替换规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it; validationMessage = null },
                    label = { Text("替换前") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("替换后（留空可删除）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isRegex,
                        onCheckedChange = {
                            isRegex = it
                            if (!it) regexFlags = emptySet()
                            validationMessage = null
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("使用正则表达式（支持 $1 分组）")
                }
                if (isRegex) {
                    Text("正则选项", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ReaderReplacementRegexFlag.entries.forEach { flag ->
                            FilterChip(
                                selected = flag in regexFlags,
                                onClick = {
                                    regexFlags = regexFlags.toMutableSet().apply {
                                        if (!add(flag)) remove(flag)
                                    }
                                },
                                label = { Text(readerReplacementRegexFlagLabel(flag)) },
                            )
                        }
                    }
                    Text(
                        "全局匹配默认开启；可按需启用忽略大小写、多行或点匹配换行。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("作用位置", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReaderReplacementTarget.entries.forEach { option ->
                        FilterChip(
                            selected = target == option,
                            onClick = { target = option },
                            label = { Text(readerReplacementTargetLabel(option)) },
                        )
                    }
                }
                Text("作用范围", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val availableScopes = buildList {
                        add(ReaderReplacementScope.WholeBook to "全书")
                        currentChapterOrder?.let { order ->
                            add(ReaderReplacementScope.CurrentChapter(order) to "当前章")
                            add(ReaderReplacementScope.ChapterRange(order, order) to "章节范围")
                        }
                    }
                    availableScopes.forEach { (candidate, label) ->
                        FilterChip(
                            selected = scope::class == candidate::class,
                            onClick = { scope = candidate },
                            label = { Text(label) },
                        )
                    }
                }
                if (currentChapterOrder == null) {
                    Text(
                        "正在同步章节目录，当前章和章节范围暂不可选",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (scope is ReaderReplacementScope.ChapterRange) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = rangeStart,
                            onValueChange = { rangeStart = it; validationMessage = null },
                            label = { Text("起始章") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = rangeEnd,
                            onValueChange = { rangeEnd = it; validationMessage = null },
                            label = { Text("结束章") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
                validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val resolvedScope = readerReplacementScopeForEditor(scope, rangeStart, rangeEnd)
                if (resolvedScope == null) {
                    validationMessage = "请输入有效的起始章和结束章"
                } else {
                    val candidate = initial.copy(
                        source = source.trim(),
                        replacement = replacement,
                        isRegex = isRegex,
                        regexFlags = if (isRegex) regexFlags else emptySet(),
                        target = target,
                        scope = resolvedScope,
                    )
                    val validation = validateReaderReplacementRule(candidate)
                    if (validation.isValid) onSave(candidate) else validationMessage = validation.message
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun readerReplacementScopeLabel(scope: ReaderReplacementScope): String = when (scope) {
    ReaderReplacementScope.WholeBook -> "全书"
    is ReaderReplacementScope.CurrentChapter -> "第${scope.chapterOrder}章"
    is ReaderReplacementScope.ChapterRange -> "第${scope.startOrder}-${scope.endOrder}章"
}

private fun readerReplacementTargetLabel(target: ReaderReplacementTarget): String = when (target) {
    ReaderReplacementTarget.Content -> "正文"
    ReaderReplacementTarget.Title -> "标题"
    ReaderReplacementTarget.Both -> "标题和正文"
}

private fun readerReplacementRegexFlagLabel(flag: ReaderReplacementRegexFlag): String = when (flag) {
    ReaderReplacementRegexFlag.IgnoreCase -> "忽略大小写 i"
    ReaderReplacementRegexFlag.Multiline -> "多行 ^$ m"
    ReaderReplacementRegexFlag.DotMatchesAll -> "点匹配换行 s"
}
