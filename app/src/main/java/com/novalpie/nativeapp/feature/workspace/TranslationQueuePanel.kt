package com.novalpie.nativeapp.feature.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.decodeAuthTokenProfile
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun TranslationQueuePanel(configs: List<WorkspaceLocalApiConfig>, initialBookId: Long? = null,
    legacyJobs: List<com.novalpie.nativeapp.model.WorkspaceTranslationJob> = emptyList(),
    onBookRequestConsumed: () -> Unit = {}, header: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val container = remember(context) { AppContainer.from(context) }
    val queue by container.translations.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val startBackgroundTask = com.novalpie.nativeapp.core.rememberBackgroundTaskAction()
    var adding by remember { mutableStateOf(false) }
    var book by remember { mutableStateOf("") }
    var configId by remember(configs) { mutableStateOf(configs.firstOrNull()?.id) }
    var pending by remember { mutableStateOf<TranslationTask?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var retryUncertain by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(container) { container.translations.restore() }
    LaunchedEffect(initialBookId) { initialBookId?.let { book = it.toString(); adding = true; onBookRequestConsumed() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { header() }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("原生自助翻译", style = MaterialTheme.typography.titleLarge)
        Text("使用本人配置，翻译网站提供的待翻章节。页面关闭后任务可继续；失败内容保留检查点。", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { adding = true }, enabled = configs.isNotEmpty()) { Text("添加翻译任务") }
        if (configs.isEmpty()) Text("请先在 API 管理添加自己的翻译配置。不会使用其他用户的共享密钥。")
        notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        queue.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (queue.tasks.isEmpty()) Text("暂无本机翻译任务")
        } }
        if (legacyJobs.isNotEmpty()) {
            item { Text("旧版记录（不会自动执行）", style = MaterialTheme.typography.titleMedium) }
            items(legacyJobs) { old -> OutlinedButton(onClick = {
                book = old.bookId.toString(); configId = old.translatorId?.takeIf { id -> configs.any { it.id == id } } ?: configs.firstOrNull()?.id; adding = true
            }) { Text("${old.bookTitle} · #${old.bookId} · 继续此书") } }
        }
        items(queue.tasks, key = { it.id }) { task ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(translationStatus(task, queue.activeId == task.id && queue.paused))
                task.currentTitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (task.total > 0) LinearProgressIndicator(progress = { task.completed.size.toFloat() / task.total }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        queue.activeId == task.id && !queue.paused -> TextButton(onClick = { container.translations.pause(task.id) }) { Text("暂停") }
                        queue.activeId == task.id -> TextButton(onClick = { container.translations.resume(task.id) }) { Text("继续") }
                        task.phase == TranslationPhase.SubmissionUncertain -> TextButton(onClick = { retryUncertain = task.id }) { Text("核对后重试") }
                        task.phase !in setOf(TranslationPhase.Completed, TranslationPhase.Cancelled, TranslationPhase.Queued) -> TextButton(onClick = {
                            startBackgroundTask { runCatching { TranslationService.resume(context, task.id) }.onFailure { notice = "无法启动任务，请保持应用前台后重试" } }
                        }) { Text("继续 / 重试") }
                    }
                    if (queue.activeId == task.id) TextButton(onClick = { container.translations.cancel(task.id) }) { Text("停止") }
                    else TextButton(onClick = { deleting = task.id }) { Text("删除记录") }
                }
            } }
        }
    }
    if (adding) AlertDialog(onDismissRequest = { if (!busy) adding = false }, title = { Text("添加自助翻译") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(book, { book = it.filter(Char::isDigit) }, label = { Text("站内书籍 ID") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("选择本人配置")
            configs.forEach { item -> FilterChip(selected = configId == item.id, onClick = { configId = item.id }, label = { Text("${item.name} · ${item.model}") }) }
            Text("将读取待翻译章节，调用所选服务（可能消耗其额度），并把确认完整的译文提交到网站。不会获取网站未提供的原稿。", style = MaterialTheme.typography.bodySmall)
            notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { TextButton(enabled = !busy && configId != null, onClick = {
            val id = book.toLongOrNull()?.takeIf { it > 0 }
            val config = configId
            if (id == null || config == null) { notice = "请输入有效书籍ID并选择配置"; return@TextButton }
            busy = true
            scope.launch {
                try {
                    val account = container.environment.token?.let { decodeAuthTokenProfile(it)?.id } ?: error("请先登录")
                    val details = container.api.bookDetail(id)
                    pending = TranslationTask(accountId = account, bookId = id, title = details.title, configId = config)
                    adding = false; notice = null
                } catch (_: Exception) { notice = "无法读取书籍或登录已失效，请重试" } finally { busy = false }
            }
        }) { Text(if (busy) "正在读取…" else "下一步") } }, dismissButton = { TextButton(onClick = { adding = false }, enabled = !busy) { Text("取消") } })
    pending?.let { task -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("确认开始翻译") },
        text = { Text("《${task.title}》\n使用所选本人 API 翻译待翻章节并提交网站，可能消耗该服务额度。确定开始？") },
        confirmButton = { Button(onClick = {
            startBackgroundTask { runCatching { TranslationService.start(context, task) }.onFailure { notice = "无法启动翻译服务，请重试" } }
            pending = null
        }) { Text("开始翻译并提交") } }, dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } }) }
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除本机任务记录") }, text = { Text("同时删除此任务自己的中间结果与检查点，已提交的网站章节不会删除。") },
        confirmButton = { TextButton(onClick = { container.translations.remove(id); deleting = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }) }
    retryUncertain?.let { id -> AlertDialog(onDismissRequest = { retryUncertain = null }, title = { Text("提交结果未确认") },
        text = { Text("请先在网站核对该章是否已有译文。若此前实际已成功，重新提交可能覆盖内容或重复计算积分。仅在确认未成功后重试；已完成模型结果会复用。") },
        confirmButton = { TextButton(onClick = {
            startBackgroundTask { runCatching { TranslationService.resume(context, id, confirmed = true) }.onFailure { notice = "无法启动任务，请保持应用前台后重试" } }
            retryUncertain = null
        }) { Text("已核对，确认重试") } },
        dismissButton = { TextButton(onClick = { retryUncertain = null }) { Text("暂不重试") } }) }
}
