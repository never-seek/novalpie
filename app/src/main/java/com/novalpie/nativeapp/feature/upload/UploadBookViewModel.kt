package com.novalpie.nativeapp.feature.upload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*

/** Parsing, draft and submission state belong to the selected book, not the navigation shell. */
internal class UploadBookViewModel(private val repository: UploadRepository, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private class Entry(val bookId: Long?) {
        var state = UploadBookState(existingNovelId = bookId)
        var serial = 0L
        var job: Job? = null
    }
    private val entries = linkedMapOf<Long?, Entry>()
    private var current = Entry(null).also { entries[null] = it }
    private var environment = 0L
    var state by mutableStateOf(current.state)
        private set

    private fun update(entry: Entry, transform: (UploadBookState) -> UploadBookState) {
        entry.state = transform(entry.state)
        if (current === entry) state = entry.state
    }
    fun enter(bookId: Long?) {
        require(bookId == null || bookId > 0)
        current = entries.getOrPut(bookId) { Entry(bookId) }
        state = current.state
    }
    fun adopt(prepared: UploadBookState): Boolean {
        enter(prepared.existingNovelId)
        if (state.processing) return false
        current.serial++
        current.job?.cancel()
        update(current) { prepared }
        return true
    }
    fun draft(value: UploadBookDraft) = update(current) {
        // A form edit must not erase an in-flight or unconfirmed server operation.
        it.copy(draft = value.copy(chapterCount = (it.chapters as? LoadResult.Success)?.value?.size ?: 0),
            submitResult = if (it.processing || it.submissionUncertain) it.submitResult else LoadResult.Idle,
            actionMessage = if (it.processing || it.submissionUncertain) it.actionMessage else null)
    }
    fun clear() {
        if (state.processing) return
        current.serial++; current.job?.cancel()
        update(current) { UploadBookState(existingNovelId = it.existingNovelId) }
    }

    fun select(uri: String) {
        val entry = current
        if (entry.state.processing || entry.state.submissionUncertain) return
        val serial = ++entry.serial; val account = environment
        update(entry) { it.copy(processing = true, progressLabel = "正在读取 EPUB 文件…", chapters = LoadResult.Loading,
            selectedFile = null, serverFilePath = null, submitResult = LoadResult.Idle, actionMessage = null) }
        entry.job = work.launch {
            try {
                val document = repository.document(uri)
                check(document.displayName.endsWith(".epub", true)) { "仅支持 EPUB 格式文件" }
                check(document.sizeBytes != 0L) { "EPUB 文件为空" }
                if (!fresh(entry, serial, account)) return@launch
                update(entry) { it.copy(selectedFile = document, progressLabel = if (uploadParseMode(document.sizeBytes.coerceAtLeast(0)) == UploadParseMode.SERVER_CHUNKED)
                    "文件超过 50 MiB，正在按 5 MiB 流式分片上传…" else "正在本机解析 EPUB 目录与章节…") }
                val parsed = repository.parse(document)
                check(parsed.chapters.isNotEmpty()) { "EPUB 没有可上传章节" }
                if (!fresh(entry, serial, account)) return@launch
                update(entry) { state -> state.copy(draft = state.draft.copy(
                    title = state.draft.title.ifBlank { parsed.title }, author = state.draft.author.ifBlank { parsed.author },
                    description = state.draft.description.ifBlank { parsed.description },
                    language = parsed.language.ifBlank { state.draft.language }, chapterCount = parsed.chapters.size),
                    chapters = LoadResult.Success(parsed.chapters), serverFilePath = parsed.epubFilePath,
                    processing = false, progressLabel = null, actionMessage = "EPUB 解析完成，共 ${parsed.chapters.size} 章") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (fresh(entry, serial, account)) update(entry) { it.copy(processing = false, progressLabel = null,
                    chapters = LoadResult.Error(apiFailureMessage("解析 EPUB", failure)), actionMessage = apiFailureMessage("解析 EPUB", failure)) }
            }
        }
    }

    fun submit(confirmUncertainRetry: Boolean = false) {
        val entry = current; val snapshot = entry.state
        if (snapshot.processing) return
        if (snapshot.submissionUncertain && !confirmUncertainRetry) {
            update(entry) { it.copy(actionMessage = "上次提交结果未确认，请先核对作品与目录；没有重复提交") }
            return
        }
        val chapters = (snapshot.chapters as? LoadResult.Success)?.value.orEmpty().toList()
        val draft = snapshot.draft.copy(chapterCount = chapters.size)
        val validation = if (entry.bookId == null) validateUploadBookDraft(draft) else when {
            chapters.isEmpty() -> "请先选择并解析 EPUB 文件"
            draft.submitType !in setOf("chinese", "personal", "shared") -> "提交方式无效"
            else -> null
        }
        if (validation != null) { update(entry) { it.copy(actionMessage = validation) }; return }
        val document = snapshot.selectedFile
        if (document == null) { update(entry) { it.copy(actionMessage = "请先选择 EPUB 文件") }; return }
        val request = UploadBookRequest(draft.title, draft.titleTranslation, draft.author, draft.description,
            draft.language, websiteUploadSpans(draft), draft.isAdult, draft.source, draft.sourceUrl,
            normalizeUploadTags(draft.tagsText), draft.submitType, chapters, snapshot.serverFilePath, draft.coverUrl.takeIf { it.isNotBlank() })
        val submission = UploadSubmission(entry.bookId, request, document)
        val serial = ++entry.serial; val account = environment
        update(entry) { it.copy(processing = true, progressLabel = "正在安全上传书籍与 ${chapters.size} 章内容…",
            submitResult = LoadResult.Loading, actionMessage = null, submissionUncertain = false) }
        entry.job = work.launch {
            try {
                val result = repository.submit(submission)
                if (!fresh(entry, serial, account)) return@launch
                if (!result.success) {
                    update(entry) { it.copy(processing = false, progressLabel = null, submitResult = LoadResult.Error(result.message ?: "服务器拒绝上传"), actionMessage = result.message ?: "服务器拒绝上传") }
                    return@launch
                }
                check(result.novelId == null || entry.bookId == null || result.novelId == entry.bookId) { "上传回执书籍身份不符，请核对目录" }
                check(entry.bookId != null || (result.novelId ?: 0) > 0) { "上传回执缺少新书ID，请核对作品列表" }
                update(entry) { it.copy(processing = false, progressLabel = null, submitResult = LoadResult.Success(result),
                    actionMessage = result.message ?: if (entry.bookId == null) "上传成功" else "章节追加成功") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (fresh(entry, serial, account)) update(entry) { it.copy(processing = false, progressLabel = null,
                    submitResult = LoadResult.Error(apiFailureMessage("上传书籍", failure)), submissionUncertain = true,
                    actionMessage = "提交中断或回执未确认，可能已有部分章节写入。请先核对作品和目录，勿直接重复上传；草稿已保留。") }
            }
        }
    }
    private fun fresh(entry: Entry, serial: Long, account: Long) = account == environment && serial == entry.serial
    fun environmentChanged() {
        environment++
        entries.values.forEach { it.job?.cancel() }
        entries.clear(); current = Entry(null).also { entries[null] = it }; state = current.state
    }
    fun close() = work.cancel()
    override fun onCleared() { close() }
}
