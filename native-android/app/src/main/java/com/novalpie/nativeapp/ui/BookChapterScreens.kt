@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.novalpie.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.LoadResult

@Composable
internal fun BookChapterManagerScreen(
    state: BookChapterManagerState,
    onRetry: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onMove: (Long, Int) -> Unit,
    onSaveOrder: () -> Unit,
    onOpenEditor: (Chapter?) -> Unit,
    onUpdateEditor: (ManagedChapterDraft) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
    onDelete: (Long) -> Unit,
    onBatchDelete: () -> Unit,
    onTranslationMode: (String) -> Unit,
    onTranslate: () -> Unit,
    onOpenIllustrations: (Chapter) -> Unit,
    onDismissIllustrations: () -> Unit,
    onUploadIllustrations: (List<String>) -> Unit,
    onDeleteIllustration: (Long) -> Unit,
    onInsertIllustrationPlaceholder: (Int) -> Unit,
    onAppend: () -> Unit
) {
    val chapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()
    var deleteTarget by remember { mutableStateOf<Chapter?>(null) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var confirmTranslation by remember { mutableStateOf(false) }

    state.editor?.let { draft ->
        ManagedChapterEditorDialog(
            draft = draft,
            busy = state.actionLoading,
            message = state.actionMessage,
            onChange = onUpdateEditor,
            onDismiss = onDismissEditor,
            onSave = onSaveEditor
        )
    }
    state.illustrationChapter?.let { chapter ->
        ManagedChapterIllustrationDialog(
            state = state,
            chapter = chapter,
            onDismiss = onDismissIllustrations,
            onUpload = onUploadIllustrations,
            onDelete = onDeleteIllustration,
            onInsertPlaceholder = onInsertIllustrationPlaceholder
        )
    }
    deleteTarget?.let { chapter ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除章节") },
            text = { Text("确定删除第 ${chapter.number ?: "?"} 章《${chapter.title}》吗？删除后无法恢复。") },
            confirmButton = { TextButton(onClick = { deleteTarget = null; onDelete(chapter.id) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
    if (confirmBatchDelete) {
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text("批量删除章节") },
            text = { Text("确定删除选中的 ${state.selectedIds.size} 个章节吗？此操作不可恢复。") },
            confirmButton = { TextButton(onClick = { confirmBatchDelete = false; onBatchDelete() }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("取消") } }
        )
    }
    if (confirmTranslation) {
        AlertDialog(
            onDismissRequest = { confirmTranslation = false },
            title = { Text("提交翻译") },
            text = { Text("将选中的 ${state.selectedIds.size} 章提交到${if (state.translationMode == "shared") "共享" else "个人"}翻译任务。") },
            confirmButton = { TextButton(onClick = { confirmTranslation = false; onTranslate() }) { Text("提交") } },
            dismissButton = { TextButton(onClick = { confirmTranslation = false }) { Text("取消") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("章节管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("书籍 #${state.bookId} · 调整顺序后请先保存，再执行编辑、删除、插图或翻译。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Button(onClick = { onOpenEditor(null) }, enabled = !state.actionLoading && !state.orderDirty) { Icon(Icons.Filled.Add, null); Text("插入章节") } }
                item { OutlinedButton(onClick = onAppend, enabled = !state.actionLoading && !state.orderDirty) { Text("批量追加 EPUB") } }
                item { OutlinedButton(onClick = onSelectAll, enabled = chapters.isNotEmpty()) { Text(if (state.selectedIds.size == chapters.size) "取消全选" else "全选") } }
                if (state.orderDirty) item { Button(onClick = onSaveOrder, enabled = !state.actionLoading) { Text("保存顺序") } }
            }
        }
        state.actionMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        if (state.editorLoading) item { LoadingBlock("正在加载章节正文") }
        if (state.selectedIds.isNotEmpty()) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("已选择 ${state.selectedIds.size} 章", fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = state.translationMode == "personal", onClick = { onTranslationMode("personal") }, label = { Text("个人翻译") }) }
                            item { FilterChip(selected = state.translationMode == "shared", onClick = { onTranslationMode("shared") }, label = { Text("共享翻译") }) }
                            item { OutlinedButton(onClick = { confirmTranslation = true }, enabled = !state.actionLoading && !state.orderDirty) { Text("提交翻译") } }
                            item { OutlinedButton(onClick = { confirmBatchDelete = true }, enabled = !state.actionLoading && !state.orderDirty) { Text("批量删除") } }
                        }
                    }
                }
            }
        }
        when (val result = state.chapters) {
            LoadResult.Idle, LoadResult.Loading -> item { LoadingBlock("正在加载章节") }
            is LoadResult.Error -> item { ErrorBlock(result.message, "重新加载", onRetry) }
            is LoadResult.Success -> if (result.value.isEmpty()) {
                item { Text("暂无章节，可插入第一章或从 EPUB 批量追加。") }
            } else {
                items(result.value, key = { it.id }) { chapter ->
                    ManagedChapterRow(
                        chapter = chapter,
                        selected = chapter.id in state.selectedIds,
                        first = chapter.id == result.value.first().id,
                        last = chapter.id == result.value.last().id,
                        busy = state.actionLoading,
                        onToggle = { onToggleSelection(chapter.id) },
                        onUp = { onMove(chapter.id, -1) },
                        onDown = { onMove(chapter.id, 1) },
                        onEdit = { onOpenEditor(chapter) },
                        onIllustrations = { onOpenIllustrations(chapter) },
                        onDelete = { deleteTarget = chapter }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagedChapterRow(
    chapter: Chapter,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onEdit: () -> Unit,
    onIllustrations: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = !busy)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("第 ${chapter.number ?: "?"} 章", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(chapter.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                chapter.wordCount?.let { Text("$it 字", style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onUp, enabled = !first && !busy) { Icon(Icons.Filled.ArrowUpward, "上移") }
            IconButton(onClick = onDown, enabled = !last && !busy) { Icon(Icons.Filled.ArrowDownward, "下移") }
            IconButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Filled.Edit, "编辑") }
            IconButton(onClick = onIllustrations, enabled = !busy) { Icon(Icons.Filled.Image, "插图") }
            IconButton(onClick = onDelete, enabled = !busy) { Icon(Icons.Filled.Delete, "删除") }
        }
    }
}

@Composable
private fun ManagedChapterIllustrationDialog(
    state: BookChapterManagerState,
    chapter: Chapter,
    onDismiss: () -> Unit,
    onUpload: (List<String>) -> Unit,
    onDelete: (Long) -> Unit,
    onInsertPlaceholder: (Int) -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val values = uris.map { it.toString() }
        if (values.isNotEmpty()) onUpload(values)
    }
    var preview by remember { mutableStateOf<ChapterIllustration?>(null) }
    var deleteTarget by remember { mutableStateOf<ChapterIllustration?>(null) }
    val busy = state.uploadingIllustrations || state.deletingIllustrationId != null
    preview?.let { image ->
        ImagePreviewDialog(image.src, "插图 ${chapterIllustrationPlaceholder(image.index)}", onDismiss = { preview = null })
    }
    deleteTarget?.let { image ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除章节插图") },
            text = { Text("确定删除 ${chapterIllustrationPlaceholder(image.index)} 吗？正文里的占位符不会自动移除。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    onDelete(image.id)
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("章节插图") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(chapter.title, fontWeight = FontWeight.Bold)
                Text("网页占位符格式：[[img:N]]。打开同一章节正文编辑器后，可把占位符直接插入草稿。", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("image/*")) },
                    enabled = !busy
                ) { Text(if (state.uploadingIllustrations) "上传中..." else "上传插图") }
                state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                when (val result = state.illustrations) {
                    LoadResult.Idle, LoadResult.Loading -> LoadingBlock("正在加载章节插图")
                    is LoadResult.Error -> Text(result.message, color = MaterialTheme.colorScheme.error)
                    is LoadResult.Success -> {
                        Text("共 ${result.value.total} 张", style = MaterialTheme.typography.labelMedium)
                        if (result.value.images.isEmpty()) {
                            Text("暂无插图。")
                        } else {
                            result.value.images.forEach { image ->
                                ManagedChapterIllustrationRow(
                                    image = image,
                                    canInsert = state.editor?.chapterId == chapter.id,
                                    deleting = state.deletingIllustrationId == image.id,
                                    onPreview = { preview = image },
                                    onInsert = { onInsertPlaceholder(image.index) },
                                    onDelete = { deleteTarget = image }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("关闭") } }
    )
}

@Composable
private fun ManagedChapterIllustrationRow(
    image: ChapterIllustration,
    canInsert: Boolean,
    deleting: Boolean,
    onPreview: () -> Unit,
    onInsert: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(image.src)
                    .crossfade(true)
                    .precision(Precision.EXACT)
                    .build(),
                contentDescription = chapterIllustrationPlaceholder(image.index),
                modifier = Modifier.width(72.dp).height(96.dp).clickable(onClick = onPreview),
                contentScale = ContentScale.Crop,
                loading = { LoadingBlock("图") },
                error = { Text("图") }
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(chapterIllustrationPlaceholder(image.index), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedButton(onClick = onPreview) { Text("预览") } }
                    item { OutlinedButton(onClick = onInsert, enabled = canInsert) { Text("插入") } }
                    item { OutlinedButton(onClick = onDelete, enabled = !deleting) { Text(if (deleting) "删除中..." else "删除") } }
                }
            }
        }
    }
}

@Composable
private fun ManagedChapterEditorDialog(
    draft: ManagedChapterDraft,
    busy: Boolean,
    message: String?,
    onChange: (ManagedChapterDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val validation = validateManagedChapterDraft(draft)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.chapterId == null) "插入章节" else "编辑章节") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (draft.chapterId == null) {
                    OutlinedTextField(
                        value = draft.insertAt.toString(),
                        onValueChange = { onChange(draft.copy(insertAt = it.toIntOrNull() ?: 0)) },
                        label = { Text("插入位置") },
                        singleLine = true,
                        enabled = !busy
                    )
                }
                OutlinedTextField(value = draft.title, onValueChange = { onChange(draft.copy(title = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("章节标题") }, enabled = !busy)
                OutlinedTextField(value = draft.content, onValueChange = { onChange(draft.copy(content = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("章节正文") }, minLines = 12, enabled = !busy)
                (validation ?: message)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = validation == null && !busy) { Text(if (busy) "保存中..." else "保存") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } }
    )
}
