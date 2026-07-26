@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.novalpie.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import com.novalpie.nativeapp.model.UploadChapter

@Composable
fun UploadEditorScreen(
    state: UploadEditorState,
    onTabSelected: (EditorTab) -> Unit,
    onOpenDocument: (String) -> Unit,
    onEncodingChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onMetadataChange: (EditorBookMetadata) -> Unit,
    onSplitModeChange: (EditorSplitMode) -> Unit,
    onSplitPatternChange: (String) -> Unit,
    onSplitTargetChange: (String) -> Unit,
    onCustomScriptChange: (String) -> Unit,
    onScriptChunkedChange: (Boolean) -> Unit,
    onScriptChunkSizeChange: (String) -> Unit,
    onCustomScriptResult: (Long, String?, String?) -> Unit,
    onAiConfigSelected: (Long) -> Unit,
    onGenerateAiRegex: () -> Unit,
    onProcessSplit: () -> Unit,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onFindRegexChange: (Boolean) -> Unit,
    onReplaceAll: () -> Unit,
    onUpdateChapter: (Int, String, String) -> Unit,
    onAddChapter: () -> Unit,
    onDeleteChapter: (Int) -> Unit,
    onArchiveNameChange: (String) -> Unit,
    onSaveArchive: () -> Unit,
    onLoadArchive: (String) -> Unit,
    onDeleteArchive: (String) -> Unit,
    onClearArchives: () -> Unit,
    onExportEpub: (String) -> Unit,
    onSendToUpload: () -> Unit,
    onClear: () -> Unit
) {
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onOpenDocument)
    }
    val exportDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri ->
        uri?.toString()?.let(onExportEpub)
    }
    var editingChapter by remember { mutableStateOf<Int?>(null) }
    var deletingArchive by remember { mutableStateOf<EditorArchive?>(null) }
    var confirmClearArchives by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scriptEngine = remember(context) { EditorScriptEngine(context) }

    LaunchedEffect(state.scriptRunId) {
        if (state.scriptRunId <= 0L || !state.busy || state.splitMode != EditorSplitMode.CustomScript) return@LaunchedEffect
        val runId = state.scriptRunId
        val result = runCatching {
            scriptEngine.process(
                script = state.customScript,
                text = state.text,
                chunked = state.scriptChunked,
                targetChunkSize = state.scriptChunkSize.toIntOrNull()?.coerceIn(1_024, 1_000_000) ?: 200_000
            )
        }
        result.fold(
            onSuccess = { processed -> onCustomScriptResult(runId, processed, null) },
            onFailure = { failure -> onCustomScriptResult(runId, null, failure.message ?: "脚本执行失败") }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EditorHero(
            state = state,
            onOpen = { openDocument.launch(arrayOf("text/*", "application/epub+zip", "application/octet-stream", "*/*")) },
            onExport = {
                val name = state.metadata.title.trim().ifBlank { "novalpie-book" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                exportDocument.launch("$name.epub")
            },
            onSend = onSendToUpload,
            onClear = onClear
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EditorTab.values().toList()) { tab ->
                FilterChip(selected = state.selectedTab == tab, onClick = { onTabSelected(tab) }, label = { Text(tab.label) })
            }
        }
        state.actionMessage?.let { message ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                Text(message, modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))

        when (state.selectedTab) {
            EditorTab.Text -> EditorTextTab(state, onEncodingChange, onTextChange, onFindChange, onReplaceChange, onFindRegexChange, onReplaceAll)
            EditorTab.Split -> EditorSplitTab(
                state,
                onSplitModeChange,
                onSplitPatternChange,
                onSplitTargetChange,
                onCustomScriptChange,
                onScriptChunkedChange,
                onScriptChunkSizeChange,
                onAiConfigSelected,
                onGenerateAiRegex,
                onProcessSplit
            )
            EditorTab.Chapters -> EditorChaptersTab(state.chapters, { editingChapter = it }, onAddChapter)
            EditorTab.Metadata -> EditorMetadataTab(state.metadata, onMetadataChange)
            EditorTab.Archives -> EditorArchivesTab(state, onArchiveNameChange, onSaveArchive, onLoadArchive, { deletingArchive = it }, { confirmClearArchives = true })
        }
    }

    editingChapter?.let { index ->
        state.chapters.getOrNull(index)?.let { chapter ->
            EditorChapterDialog(
                index = index,
                chapter = chapter,
                onDismiss = { editingChapter = null },
                onSave = { title, content -> onUpdateChapter(index, title, content); editingChapter = null },
                onDelete = { onDeleteChapter(index); editingChapter = null }
            )
        }
    }
    deletingArchive?.let { archive ->
        EditorConfirmDialog(
            title = "删除存档",
            message = "确定删除“${archive.name}”吗？此操作不可撤销。",
            onDismiss = { deletingArchive = null },
            onConfirm = { onDeleteArchive(archive.id); deletingArchive = null }
        )
    }
    if (confirmClearArchives) {
        EditorConfirmDialog(
            title = "清空所有存档",
            message = "这会删除 App 私有目录中的全部编辑器存档。",
            onDismiss = { confirmClearArchives = false },
            onConfirm = { onClearArchives(); confirmClearArchives = false }
        )
    }
}

@Composable
private fun EditorHero(
    state: UploadEditorState,
    onOpen: () -> Unit,
    onExport: () -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF111827), Color(0xFF3730A3), Color(0xFF7C3AED))))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("EPUB 编辑器", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${state.fileName ?: "未打开文件"} · ${state.chapters.size} 章 · ${formatEditorCount(state.text.length)} 字符",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onClear, enabled = !state.busy) { Icon(Icons.Filled.Delete, "清空编辑器", tint = Color.White) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { EditorHeroButton("打开", Icons.Filled.FolderOpen, state.busy, onOpen) }
                item { EditorHeroButton("生成 EPUB", Icons.Filled.Download, state.busy || state.chapters.isEmpty(), onExport) }
                item { EditorHeroButton("发送到上传", Icons.AutoMirrored.Filled.Send, state.busy || state.chapters.isEmpty(), onSend) }
            }
        }
    }
}

@Composable
private fun EditorHeroButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, disabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = !disabled) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.White)
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.White)
    }
}

@Composable
private fun EditorTextTab(
    state: UploadEditorState,
    onEncodingChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onFindRegexChange: (Boolean) -> Unit,
    onReplaceAll: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("打开编码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(listOf("UTF-8", "UTF-16LE", "UTF-16BE", "GB18030", "GBK", "Big5", "Shift_JIS", "EUC-JP", "EUC-KR", "windows-1252")) { encoding ->
                    FilterChip(selected = state.encoding.equals(encoding, true), onClick = { onEncodingChange(encoding) }, label = { Text(encoding) })
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FindReplace, null, tint = MaterialTheme.colorScheme.primary)
                        Text("查找 / 替换", fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(state.findText, onFindChange, label = { Text("查找") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(state.replaceText, onReplaceChange, label = { Text("替换为") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("正则表达式"); Spacer(Modifier.width(8.dp)); Switch(state.findUsesRegex, onFindRegexChange) }
                        Button(onClick = onReplaceAll) { Text("全部替换") }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("正文文本") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                minLines = 18,
                maxLines = 32
            )
        }
    }
}

@Composable
private fun EditorSplitTab(
    state: UploadEditorState,
    onModeChange: (EditorSplitMode) -> Unit,
    onPatternChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onCustomScriptChange: (String) -> Unit,
    onScriptChunkedChange: (Boolean) -> Unit,
    onScriptChunkSizeChange: (String) -> Unit,
    onAiConfigSelected: (Long) -> Unit,
    onGenerateAiRegex: () -> Unit,
    onProcess: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoFixHigh, null, tint = MaterialTheme.colorScheme.primary)
                        Text("分章工具", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("与源站编辑器一致，规则只在本机处理文本。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EditorSplitMode.values().toList()) { mode ->
                            FilterChip(selected = state.splitMode == mode, onClick = { onModeChange(mode) }, label = { Text(mode.label) })
                        }
                    }
                    when (state.splitMode) {
                        EditorSplitMode.Regex,
                        EditorSplitMode.KeywordNumber -> OutlinedTextField(
                            state.splitPattern,
                            onPatternChange,
                            label = { Text(if (state.splitMode == EditorSplitMode.Regex) "每行一个正则" else "每行一个关键词") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                        EditorSplitMode.CustomScript -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "脚本必须定义 processText(text, options)，仅在本地无网络/文件权限沙箱中运行。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                state.customScript,
                                onCustomScriptChange,
                                label = { Text("JavaScript") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 12,
                                maxLines = 24,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("分块处理", fontWeight = FontWeight.SemiBold)
                                    Text("大文本按换行拆分并依次执行", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(state.scriptChunked, onScriptChunkedChange)
                            }
                            if (state.scriptChunked) {
                                OutlinedTextField(
                                    state.scriptChunkSize,
                                    onScriptChunkSizeChange,
                                    label = { Text("每块目标字符数（1024 - 1000000）") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            Text(
                                "辅助函数：insertMarker、findMatches、splitByParagraphs、splitByWords、getParagraphs、getWordCount、getLineCount。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EditorSplitMode.CharacterCount,
                        EditorSplitMode.ParagraphCount -> OutlinedTextField(
                            state.splitTarget,
                            onTargetChange,
                            label = { Text(if (state.splitMode == EditorSplitMode.CharacterCount) "每章目标字符数" else "每章目标段落数") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        else -> Text("将识别对应 Markdown 标题并按出现顺序生成目录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.splitMode == EditorSplitMode.Regex) {
                        HorizontalDivider()
                        Text("AI 生成正则", fontWeight = FontWeight.SemiBold)
                        if (state.aiConfigs.isEmpty()) {
                            Text(
                                "工作区没有保存可用的本地 API 配置。API key 不会在这里显示。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.aiConfigs, key = { it.id }) { config ->
                                    FilterChip(
                                        selected = state.selectedAiConfigId == config.id,
                                        onClick = { onAiConfigSelected(config.id) },
                                        label = { Text("${config.name.ifBlank { "API" }} · ${config.model}") }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onGenerateAiRegex,
                                enabled = !state.busy && state.chapters.size >= 2 && state.selectedAiConfigId != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("根据前 20 个章节标题生成正则")
                            }
                            Text(
                                "请求沿用源站 OpenAI-compatible /v1/chat/completions 协议；生成后仅填入正则框，不会自动执行分章。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(onClick = onProcess, enabled = state.text.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Text("生成章节目录")
                    }
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("源站章节标识符", fontWeight = FontWeight.Bold)
                    Text("支持 `##__T[00001]__##` 标题标识与 `##__C[00001]__##` 内容标识；生成 EPUB 前目录会使用连续编号。", style = MaterialTheme.typography.bodySmall)
                    Text("当前文本 ${formatEditorCount(state.text.length)} 字符，已生成 ${state.chapters.size} 章。", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EditorChaptersTab(chapters: List<UploadChapter>, onEdit: (Int) -> Unit, onAdd: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("章节目录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("共 ${chapters.size} 章，点击卡片编辑标题与正文", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAdd) { Icon(Icons.Filled.Add, null); Text("新增") }
            }
        }
        if (chapters.isEmpty()) {
            item { EditorEmpty("还没有章节。先在“分章”页生成目录，或手动新增章节。") }
        } else {
            itemsIndexed(chapters, key = { index, chapter -> "${chapter.chapterNumber}-$index-${chapter.title}" }) { index, chapter ->
                ElevatedCard(onClick = { onEdit(index) }, shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                            Text("${chapter.chapterNumber}", modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(chapter.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${formatEditorCount(chapter.content.length)} 字符", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.Edit, "编辑章节", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorMetadataTab(metadata: EditorBookMetadata, onChange: (EditorBookMetadata) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("书籍信息", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(metadata.title, { onChange(metadata.copy(title = it)) }, label = { Text("书名 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(metadata.author, { onChange(metadata.copy(author = it)) }, label = { Text("作者 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(metadata.description, { onChange(metadata.copy(description = it)) }, label = { Text("简介") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
        item {
            Text("语言", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                listOf("zh" to "中文", "ja" to "日本語", "other" to "其他").forEach { (value, label) ->
                    FilterChip(selected = metadata.language == value, onClick = { onChange(metadata.copy(language = value)) }, label = { Text(label) })
                }
            }
        }
        item { OutlinedTextField(metadata.tags, { onChange(metadata.copy(tags = it)) }, label = { Text("标签（逗号分隔）") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        item { OutlinedTextField(metadata.source, { onChange(metadata.copy(source = it)) }, label = { Text("来源") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(metadata.sourceUrl, { onChange(metadata.copy(sourceUrl = it)) }, label = { Text("来源链接") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("19禁内容", fontWeight = FontWeight.Bold); Text("生成与上传时保留成人标记", style = MaterialTheme.typography.bodySmall) }
                    Switch(metadata.isAdult, { onChange(metadata.copy(isAdult = it)) })
                }
            }
        }
    }
}

@Composable
private fun EditorArchivesTab(
    state: UploadEditorState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (EditorArchive) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Filled.Archive, null, tint = MaterialTheme.colorScheme.primary); Text("保存当前存档", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    OutlinedTextField(state.archiveName, onNameChange, label = { Text("存档名称（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = onSave, enabled = !state.busy && (state.text.isNotBlank() || state.chapters.isNotEmpty()), modifier = Modifier.fillMaxWidth()) { Text("保存存档") }
                    Text("正文与索引分文件保存；列表不会把全部长文本重新载入内存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("已保存的存档（${state.archives.size}）", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (state.archives.isNotEmpty()) TextButton(onClick = onClear) { Text("清空") }
            }
        }
        if (state.archives.isEmpty()) item { EditorEmpty("暂无存档") }
        else items(state.archives, key = EditorArchive::id) { archive ->
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(archive.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${archive.chapterCount} 章 · ${formatEditorCount(archive.totalWords)} 字符 · ${archive.fileName ?: "本地编辑"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onLoad(archive.id) }, modifier = Modifier.weight(1f)) { Text("加载") }
                        OutlinedButton(onClick = { onDelete(archive) }) { Icon(Icons.Filled.Delete, null); Text("删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorChapterDialog(
    index: Int,
    chapter: UploadChapter,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(index, chapter.title) { mutableStateOf(chapter.title) }
    var content by remember(index, chapter.content) { mutableStateOf(chapter.content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑第 ${index + 1} 章") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("章节标题") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(content, { content = it }, label = { Text("章节正文") }, modifier = Modifier.fillMaxWidth(), minLines = 12, maxLines = 22, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)) }
                item { Text("${formatEditorCount(content.length)} 字符", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, content) }) { Text("保存") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
private fun EditorConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditorEmpty(message: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatEditorCount(value: Int): String = when {
    value >= 10_000 -> "%.1f万".format(value / 10_000.0)
    value >= 1_000 -> "%.1fk".format(value / 1_000.0)
    else -> value.toString()
}
