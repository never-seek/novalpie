@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.novalpie.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.UploadActionResult
import com.novalpie.nativeapp.model.UploadChapter

@Composable
fun UploadBookScreen(
    state: UploadBookState,
    hasAuthToken: Boolean,
    onOpenLogin: () -> Unit,
    onPickEpub: (String) -> Unit,
    onDraftChange: (UploadBookDraft) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenBook: (Long) -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onPickEpub)
    }
    val chapters = (state.chapters as? LoadResult.Success)?.value.orEmpty()
    val appendMode = state.existingNovelId != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UploadHero(
                chapterCount = chapters.size,
                processing = state.processing,
                appendMode = appendMode,
                onOpenEditor = onOpenEditor
            )
        }

        if (appendMode) {
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("正在追加到书籍 #${state.existingNovelId}", fontWeight = FontWeight.Bold)
                        Text("只新增解析后的章节，不覆盖书名、作者、封面和已有章节。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (!hasAuthToken) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("需要登录", fontWeight = FontWeight.Bold)
                            Text("源站上传接口要求有效账号会话。", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onOpenLogin) { Text("去登录") }
                    }
                }
            }
        }

        item {
            UploadFileCard(
                document = state.selectedFile,
                processing = state.processing,
                onPick = { picker.launch(arrayOf("application/epub+zip", "application/octet-stream", "*/*")) },
                onClear = onClear
            )
        }

        state.progressLabel?.let { label ->
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text("文件内容采用流式读取，不会把大型 EPUB 和图片一次性分配到内存。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        state.actionMessage?.let { message ->
            item { UploadNotice(message, state.submitResult is LoadResult.Error || state.chapters is LoadResult.Error) }
        }

        if (!appendMode) {
            item { UploadMetadataCard(state.draft, state.processing, onDraftChange) }
        }

        item {
            UploadSubmissionCard(state.draft, state.processing, onDraftChange)
        }

        item {
            UploadChapterSection(state.chapters)
        }

        item {
            UploadSubmitResult(state.submitResult, onOpenBook)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSubmit,
                    enabled = !state.processing && hasAuthToken && chapters.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.processing) "处理中…" else if (appendMode) "确认追加 ${chapters.size} 章" else "确认上传 ${chapters.size} 章")
                }
                Text(
                    if (appendMode) "追加会写入现有书籍。提交前请确认章节顺序与翻译类型。" else "上传会写入 novalpie.cc。提交前请确认书名、作者、标签、成人内容标记与翻译类型。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UploadHero(chapterCount: Int, processing: Boolean, appendMode: Boolean, onOpenEditor: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF7C3AED), Color(0xFFDB2777))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Color.White.copy(alpha = 0.17f), shape = RoundedCornerShape(30.dp)) {
                Text("NOVALPIE STUDIO", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Text(if (appendMode) "追加章节" else "上传书籍", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                "沿用源站 EPUB、章节与翻译提交协议，并针对 Android 大文件做流式处理。",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(if (chapterCount > 0) "$chapterCount 章已就绪" else "等待 EPUB") })
                if (processing) AssistChip(onClick = {}, label = { Text("处理中") })
            }
            OutlinedButton(onClick = onOpenEditor) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("打开文本 / EPUB 编辑器", color = Color.White)
            }
        }
    }
}

@Composable
private fun UploadFileCard(
    document: UploadDocument?,
    processing: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.padding(12.dp).size(26.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("EPUB 文件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("源站阈值：50 MiB；超出后自动 5 MiB 分片", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (document == null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(16.dp)) {
                    Text("选择 EPUB 后将自动读取元数据、目录和正文。图片不会被整体解码到内存。", modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(document.displayName, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatUploadBytes(document.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPick, enabled = !processing, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.UploadFile, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (document == null) "选择 EPUB" else "更换文件")
                }
                if (document != null) {
                    OutlinedButton(onClick = onClear, enabled = !processing) {
                        Icon(Icons.Filled.Delete, null)
                        Text("清空")
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadMetadataCard(
    draft: UploadBookDraft,
    processing: Boolean,
    onDraftChange: (UploadBookDraft) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("书籍信息", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("字段与源站上传页保持一致", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(draft.title, { onDraftChange(draft.copy(title = it)) }, label = { Text("中文书名 *") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.titleTranslation, { onDraftChange(draft.copy(titleTranslation = it)) }, label = { Text("原文书名（可选）") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.author, { onDraftChange(draft.copy(author = it)) }, label = { Text("作者 *") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.description, { onDraftChange(draft.copy(description = it)) }, label = { Text("简介") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), minLines = 4)
            Text("语言", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("zh" to "中文", "ja" to "日本語", "other" to "其他").forEach { (value, label) ->
                    FilterChip(selected = draft.language == value, onClick = { onDraftChange(draft.copy(language = value)) }, label = { Text(label) }, enabled = !processing)
                }
            }
            OutlinedTextField(draft.source, { onDraftChange(draft.copy(source = it)) }, label = { Text("来源") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.sourceUrl, { onDraftChange(draft.copy(sourceUrl = it)) }, label = { Text("来源链接") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.coverUrl, { onDraftChange(draft.copy(coverUrl = it)) }, label = { Text("封面图片链接") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.tagsText, { onDraftChange(draft.copy(tagsText = it)) }, label = { Text("标签（逗号或换行分隔）") }, enabled = !processing, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("19禁内容", fontWeight = FontWeight.SemiBold)
                    Text("书籍包含成人内容时必须开启", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = draft.isAdult, onCheckedChange = { onDraftChange(draft.copy(isAdult = it)) }, enabled = !processing)
            }
        }
    }
}

@Composable
private fun UploadSubmissionCard(
    draft: UploadBookDraft,
    processing: Boolean,
    onDraftChange: (UploadBookDraft) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("提交方式", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            listOf(
                Triple("chinese", "中文书籍", "直接提交中文正文"),
                Triple("personal", "个人翻译", "仅进入个人翻译工作流"),
                Triple("shared", "共享翻译", "进入共享翻译工作流")
            ).forEach { (value, title, subtitle) ->
                Surface(
                    color = if (draft.submitType == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(15.dp),
                    onClick = { if (!processing) onDraftChange(draft.copy(submitType = value)) }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(selected = draft.submitType == value, onClick = { onDraftChange(draft.copy(submitType = value)) }, label = { Text(title) }, enabled = !processing)
                        Text(subtitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadChapterSection(chaptersState: LoadResult<List<UploadChapter>>) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("章节预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val count = (chaptersState as? LoadResult.Success)?.value?.size ?: 0
                if (count > 0) AssistChip(onClick = {}, label = { Text("$count 章") })
            }
            when (chaptersState) {
                LoadResult.Idle -> Text("选择 EPUB 后在此核对章节顺序。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LoadResult.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is LoadResult.Error -> Text(chaptersState.message, color = MaterialTheme.colorScheme.error)
                is LoadResult.Success -> {
                    chaptersState.value.take(16).forEachIndexed { index, chapter ->
                        if (index > 0) Divider()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${chapter.chapterNumber}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Column(Modifier.weight(1f)) {
                                Text(chapter.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                Text("${chapter.content.length} 字符", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (chaptersState.value.size > 16) {
                        Text("仅预览前 16 章；提交时会上传全部 ${chaptersState.value.size} 章。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadSubmitResult(result: LoadResult<UploadActionResult>, onOpenBook: (Long) -> Unit) {
    when (result) {
        LoadResult.Idle -> Unit
        LoadResult.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is LoadResult.Error -> UploadNotice(result.message, true)
        is LoadResult.Success -> Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF15803D))
                Column(Modifier.weight(1f)) {
                    Text("上传成功", color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                    result.value.novelId?.let { Text("书籍 ID：$it", color = Color(0xFF166534)) }
                }
                result.value.novelId?.let { id -> TextButton(onClick = { onOpenBook(id) }) { Text("查看") } }
            }
        }
    }
}

@Composable
private fun UploadNotice(message: String, isError: Boolean) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun formatUploadBytes(value: Long): String = when {
    value < 0L -> "大小未知"
    value >= 1024L * 1024L * 1024L -> "%.2f GiB".format(value / (1024.0 * 1024.0 * 1024.0))
    value >= 1024L * 1024L -> "%.2f MiB".format(value / (1024.0 * 1024.0))
    value >= 1024L -> "%.1f KiB".format(value / 1024.0)
    else -> "$value B"
}
