package com.novalpie.nativeapp.feature.editor

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.ui.*
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.IdentityHashMap

/** The editor owns its document/history and asynchronous operations, independently of navigation. */
internal class EditorViewModel(private val repository: EditorRepository, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private val editorDocumentHistory = EditorDocumentHistory()
    private var editorRequestSerial = 0L
    private var editorProcessorRequestSerial = 0L
    private var scriptSerial = 0L
    private var scriptInput: String? = null
    private var documentRevision = 0L
    private var chapterEditTail: Job? = null
    private var chapterSuccessors = IdentityHashMap<UploadChapter, UploadChapter?>()
    var uploadEditorState by mutableStateOf(UploadEditorState())
        private set
    private inline fun <T> editorResult(block: () -> T): Result<T> = try { Result.success(block()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
    fun close() = work.cancel()
    override fun onCleared() { close() }
    fun environmentChanged(accountChanged: Boolean = true) {
        documentRevision++
        chapterEditTail = null
        editorRequestSerial++
        editorProcessorRequestSerial++
        scriptSerial++
        scriptInput = null
        work.coroutineContext.cancelChildren()
        if (accountChanged) {
            editorDocumentHistory.clear()
            uploadEditorState = UploadEditorState(scriptRunId = scriptSerial)
        } else uploadEditorState = uploadEditorState.copy(busy = false, scriptRunId = scriptSerial,
            actionMessage = "连接设置已变化，未继续旧请求；编辑草稿保留")
    }
    fun openUploadEditor() {
        val configs = repository.configs().filter { it.endpoint.isNotBlank() && it.model.isNotBlank() && it.apiKey.isNotBlank() }
        uploadEditorState = uploadEditorState.copy(archives = repository.archives(), aiConfigs = configs,
            selectedAiConfigId = uploadEditorState.selectedAiConfigId?.takeIf { id -> configs.any { it.id == id } } ?: configs.firstOrNull()?.id,
            actionMessage = null)
    }

    fun selectEditorTab(tab: EditorTab) {
        uploadEditorState = uploadEditorState.copy(selectedTab = tab, actionMessage = null)
    }

    private fun editorSnapshot(state: UploadEditorState) = EditorDocumentSnapshot(
        text = state.text,
        cursorPosition = state.cursorPosition,
        chapters = state.chapters.toList(),
        markerValidationErrors = state.markerValidationErrors.toList()
    )

    private fun withEditorHistoryFlags(state: UploadEditorState) = state.copy(
        canUndo = editorDocumentHistory.canUndo,
        canRedo = editorDocumentHistory.canRedo
    )

    private fun commitEditorDocumentChange(
        previous: UploadEditorState,
        updated: UploadEditorState,
        queuedChapterEdit: Boolean = false,
    ): UploadEditorState {
        if (!queuedChapterEdit) documentRevision++
        editorDocumentHistory.record(editorSnapshot(previous), editorSnapshot(updated))
        return withEditorHistoryFlags(updated)
    }

    private fun replaceEditorDocument(updated: UploadEditorState): UploadEditorState {
        documentRevision++
        editorDocumentHistory.clear()
        return withEditorHistoryFlags(updated)
    }

    private fun restoreEditorDocument(
        state: UploadEditorState,
        snapshot: EditorDocumentSnapshot,
        actionMessage: String
    ): UploadEditorState = withEditorHistoryFlags(
        state.copy(
            text = snapshot.text,
            cursorPosition = snapshot.cursorPosition.coerceIn(0, snapshot.text.length),
            chapters = snapshot.chapters,
            markerValidationErrors = snapshot.markerValidationErrors,
            selectedTab = if (snapshot.chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
            actionMessage = actionMessage
        )
    )

    /** Atomic text/selection updates keep a history snapshot at the exact caret position. */
    fun updateEditorDocument(value: String, cursorPosition: Int) {
        val state = uploadEditorState
        val textChanged = value != state.text
        val updated = state.copy(
            text = value,
            cursorPosition = cursorPosition.coerceIn(0, value.length),
            chapters = if (textChanged) emptyList() else state.chapters,
            markerValidationErrors = if (textChanged) emptyList() else state.markerValidationErrors,
            selectedTab = if (textChanged) EditorTab.Text else state.selectedTab,
            actionMessage = null
        )
        uploadEditorState = if (textChanged) commitEditorDocumentChange(state, updated) else withEditorHistoryFlags(updated)
    }

    fun updateEditorText(value: String) {
        updateEditorDocument(value, uploadEditorState.cursorPosition)
    }

    fun updateEditorCursor(position: Int) {
        uploadEditorState = withEditorHistoryFlags(uploadEditorState.copy(
            cursorPosition = position.coerceIn(0, uploadEditorState.text.length)
        ))
    }

    fun undoEditorDocument() {
        if (afterChapterEdits(::undoEditorDocument)) return
        val state = uploadEditorState
        if (state.busy) return
        val previous = editorDocumentHistory.undo(editorSnapshot(state)) ?: return
        documentRevision++
        uploadEditorState = restoreEditorDocument(state, previous, "撤销上一步编辑")
    }

    fun redoEditorDocument() {
        if (afterChapterEdits(::redoEditorDocument)) return
        val state = uploadEditorState
        if (state.busy) return
        val next = editorDocumentHistory.redo(editorSnapshot(state)) ?: return
        documentRevision++
        uploadEditorState = restoreEditorDocument(state, next, "重做上一步编辑")
    }

    fun updateEditorEncoding(value: String) {
        uploadEditorState = uploadEditorState.copy(encoding = value, actionMessage = null)
    }

    fun updateEditorMetadata(value: EditorBookMetadata) {
        uploadEditorState = uploadEditorState.copy(metadata = value, actionMessage = null)
    }

    fun updateEditorSplitMode(value: EditorSplitMode) {
        uploadEditorState = uploadEditorState.copy(splitMode = value, actionMessage = null)
    }

    fun updateEditorSplitPattern(value: String) {
        uploadEditorState = uploadEditorState.copy(splitPattern = value, actionMessage = null)
    }

    fun updateEditorSplitTarget(value: String) {
        uploadEditorState = uploadEditorState.copy(splitTarget = value.filter(Char::isDigit), actionMessage = null)
    }

    fun updateEditorCustomScript(value: String) {
        uploadEditorState = uploadEditorState.copy(customScript = value, actionMessage = null)
    }

    fun updateEditorScriptChunked(value: Boolean) {
        uploadEditorState = uploadEditorState.copy(scriptChunked = value, actionMessage = null)
    }

    fun updateEditorScriptChunkSize(value: String) {
        uploadEditorState = uploadEditorState.copy(scriptChunkSize = value.filter(Char::isDigit), actionMessage = null)
    }

    fun updateEditorApiEndpoint(value: String) {
        uploadEditorState = uploadEditorState.copy(apiEndpoint = value, actionMessage = null)
    }

    fun updateEditorApiTimeout(value: String) {
        uploadEditorState = uploadEditorState.copy(apiTimeoutSeconds = value.filter(Char::isDigit), actionMessage = null)
    }

    fun updateEditorApiMarkerMode(value: EditorMarkerMode) {
        uploadEditorState = uploadEditorState.copy(apiMarkerMode = value, actionMessage = null)
    }

    fun updateEditorBatchMode(value: EditorBatchMode) {
        val state = uploadEditorState
        val target = if (state.batchTarget == state.batchMode.defaultTarget) value.defaultTarget else state.batchTarget
        uploadEditorState = state.copy(batchMode = value, batchTarget = target, actionMessage = null)
    }

    fun updateEditorBatchTarget(value: String) {
        uploadEditorState = uploadEditorState.copy(batchTarget = value.filter(Char::isDigit), actionMessage = null)
    }

    fun selectEditorAiConfig(id: Long) {
        if (uploadEditorState.aiConfigs.none { it.id == id }) return
        uploadEditorState = uploadEditorState.copy(selectedAiConfigId = id, actionMessage = null)
    }

    fun generateEditorRegexWithAi() {
        val requested = uploadEditorState
        if (afterChapterEdits {
            val current = uploadEditorState
            if (current.splitPattern == requested.splitPattern && current.splitMode == requested.splitMode &&
                current.selectedAiConfigId == requested.selectedAiConfigId) generateEditorRegexWithAi()
            else uploadEditorState = current.copy(actionMessage = "规则或 AI 配置已修改，未继续旧请求")
        }) return
        val state = uploadEditorState
        if (state.busy) return
        if (state.chapters.size < 2) {
            uploadEditorState = state.copy(actionMessage = "请先生成至少两个章节标题")
            return
        }
        val config = state.aiConfigs.firstOrNull { it.id == state.selectedAiConfigId }
        if (config == null) {
            uploadEditorState = state.copy(actionMessage = "请先在工作区保存可用的本地 API 配置")
            return
        }
        uploadEditorState = state.copy(busy = true, actionMessage = "正在生成章节正则…")
        val serial = ++editorProcessorRequestSerial
        work.launch {
            val result = editorResult {
                repository.generateRegex(config, state.chapters.take(20).map { it.title })
            }
            if (serial != editorProcessorRequestSerial) return@launch
            if (uploadEditorState.chapters != state.chapters || uploadEditorState.splitPattern != state.splitPattern) {
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "目录或规则已修改，未覆盖新的设置")
                return@launch
            }
            uploadEditorState = result.fold(
                onSuccess = { regex ->
                    uploadEditorState.copy(
                        splitMode = EditorSplitMode.Regex,
                        splitPattern = regex,
                        selectedTab = EditorTab.Split,
                        busy = false,
                        actionMessage = "AI 已生成正则，请检查后再执行分章"
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(
                        busy = false,
                        actionMessage = apiFailureMessage("AI 生成正则", failure)
                    )
                }
            )
        }
    }

    fun updateEditorFind(value: String) {
        uploadEditorState = uploadEditorState.copy(findText = value, actionMessage = null)
    }

    fun updateEditorReplace(value: String) {
        uploadEditorState = uploadEditorState.copy(replaceText = value, actionMessage = null)
    }

    fun updateEditorFindUsesRegex(value: Boolean) {
        uploadEditorState = uploadEditorState.copy(findUsesRegex = value, actionMessage = null)
    }

    fun updateEditorArchiveName(value: String) {
        uploadEditorState = uploadEditorState.copy(archiveName = value, actionMessage = null)
    }

    fun queueEditorDocuments(rawUris: List<String>) {
        if (uploadEditorState.busy || rawUris.isEmpty()) return
        val serial = ++editorRequestSerial
        uploadEditorState = uploadEditorState.copy(busy = true, actionMessage = "正在加入文件…")
        work.launch {
            val result = editorResult {
                val documents = mutableListOf<UploadDocument>()
                for (rawUri in rawUris.distinct()) {
                    documents += repository.document(rawUri)
                }
                documents
            }
            if (!isFreshRequestSerial(serial, editorRequestSerial)) return@launch
            uploadEditorState = result.fold(
                onSuccess = { documents ->
                    val previous = uploadEditorState.files.associateBy(UploadDocument::uri)
                    val merged = (previous.values + documents).distinctBy(UploadDocument::uri)
                    uploadEditorState.copy(
                        files = merged,
                        selectedTab = EditorTab.Files,
                        busy = false,
                        actionMessage = "已加入 ${documents.size} 个文件"
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("加入编辑文件", failure))
                }
            )
        }
    }

    fun removeQueuedEditorDocument(rawUri: String) {
        if (uploadEditorState.busy) return
        val updated = uploadEditorState.files.filterNot { it.uri == rawUri }
        if (updated.size == uploadEditorState.files.size) return
        uploadEditorState = uploadEditorState.copy(files = updated, actionMessage = "文件已移除")
    }

    fun selectEditorDocument(rawUri: String) {
        if (uploadEditorState.busy) return
        val before = uploadEditorState
        val serial = ++editorRequestSerial
        uploadEditorState = uploadEditorState.copy(busy = true, actionMessage = "正在打开文件…")
        work.launch {
            val result = editorResult {
                repository.loadDocument(rawUri, before.encoding, before.metadata)
            }
            if (!isFreshRequestSerial(serial, editorRequestSerial)) return@launch
            if (uploadEditorState.text != before.text || uploadEditorState.chapters != before.chapters) {
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "草稿已修改，未用文件覆盖新内容")
                return@launch
            }
            uploadEditorState = result.fold(
                onSuccess = { loaded ->
                    replaceEditorDocument(uploadEditorState.copy(
                        text = loaded.text,
                        cursorPosition = 0,
                        fileName = loaded.document.displayName,
                        files = (uploadEditorState.files + loaded.document).distinctBy(UploadDocument::uri),
                        metadata = if (uploadEditorState.metadata == before.metadata) loaded.metadata else uploadEditorState.metadata,
                        chapters = loaded.chapters,
                        markerValidationErrors = emptyList(),
                        selectedTab = if (loaded.chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                        busy = false,
                        actionMessage = if (loaded.chapters.isEmpty()) "文件已加载，请配置分章规则" else "EPUB 已加载，共 ${loaded.chapters.size} 章"
                    ))
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("打开编辑文件", failure))
                }
            )
        }
    }

    fun importQueuedEditorDocuments() {
        val state = uploadEditorState
        if (state.busy) return
        if (state.files.isEmpty()) {
            uploadEditorState = state.copy(actionMessage = "请先在文件面板添加 .txt、.md、.epub 或 .zip 文件")
            return
        }
        val serial = ++editorRequestSerial
        uploadEditorState = state.copy(busy = true, actionMessage = "正在批量导入 ${state.files.size} 个文件…")
        work.launch {
            val result = editorResult {
                val chapters = repository.importDocuments(state.files, state.encoding)
                withContext(Dispatchers.Default) { EditorChapterDocument(chapters, EditorProcessor.toWebsiteIdentifiers(chapters)) }
            }
            if (!isFreshRequestSerial(serial, editorRequestSerial)) return@launch
            if (uploadEditorState.text != state.text || uploadEditorState.chapters != state.chapters) {
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "草稿已修改，未用批量导入覆盖新内容")
                return@launch
            }
            uploadEditorState = result.fold(
                onSuccess = { document ->
                    val chapters = document.chapters
                    val fileName = if (state.files.size == 1) state.files.single().displayName
                    else "批量导入（${state.files.size} 个文件）"
                    replaceEditorDocument(uploadEditorState.copy(
                        text = document.text,
                        cursorPosition = 0,
                        fileName = fileName,
                        chapters = chapters,
                        markerValidationErrors = emptyList(),
                        selectedTab = EditorTab.Chapters,
                        busy = false,
                        actionMessage = "已从 ${state.files.size} 个文件导入 ${chapters.size} 章"
                    ))
                },
                onFailure = { failure ->
                    uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("批量导入编辑文件", failure))
                }
            )
        }
    }

    fun processEditorSplit() {
        val state = uploadEditorState
        if (state.busy) return
        if (state.text.isBlank()) {
            uploadEditorState = state.copy(actionMessage = "请先加载或输入文本")
            return
        }
        editorSplitTargetError(
            state.splitMode,
            editorSplitPattern(state),
            editorSplitTarget(state),
            state.customScript,
            state.scriptChunked,
            state.scriptChunkSize
        )?.let { error ->
            uploadEditorState = state.copy(actionMessage = error)
            return
        }
        if (state.splitMode == EditorSplitMode.CustomScript) {
            scriptInput = state.text
            uploadEditorState = state.copy(
                busy = true,
                scriptRunId = ++scriptSerial,
                actionMessage = "正在本地沙箱执行脚本…"
            )
            return
        }
        if (state.splitMode == EditorSplitMode.ApiProcess) {
            processEditorThroughApi(state)
            return
        }
        if (state.splitMode == EditorSplitMode.Manual) {
            uploadEditorState = state.copy(actionMessage = "请使用下方的标识符手动工具")
            return
        }
        val serial = ++editorProcessorRequestSerial
        val revision = documentRevision
        uploadEditorState = state.copy(busy = true, actionMessage = "正在后台分章…")
        work.launch {
            val result = withContext(Dispatchers.Default) { editorResult {
                val chapters = when (state.splitMode) {
                    EditorSplitMode.Regex -> EditorProcessor.splitByRegex(
                        state.text,
                        state.splitPattern.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                    )
                    EditorSplitMode.MarkdownH1 -> EditorProcessor.splitByMarkdown(state.text, 1)
                    EditorSplitMode.MarkdownH2 -> EditorProcessor.splitByMarkdown(state.text, 2)
                    EditorSplitMode.KeywordNumber -> EditorProcessor.splitByKeywordNumber(
                        state.text,
                        state.splitPattern.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                    )
                    EditorSplitMode.CharacterCount -> EditorProcessor.splitByCharacterCount(state.text, state.splitTarget.toInt())
                    EditorSplitMode.ParagraphCount -> EditorProcessor.splitByParagraphCount(state.text, state.splitTarget.toInt())
                    EditorSplitMode.BatchGenerate -> when (state.batchMode) {
                        EditorBatchMode.Paragraphs -> EditorProcessor.splitByParagraphCount(state.text, state.batchTarget.toInt())
                        EditorBatchMode.Characters -> EditorProcessor.splitByCharacterInterval(state.text, state.batchTarget.toInt())
                        EditorBatchMode.Chapters -> EditorProcessor.splitEvenlyByChapterCount(state.text, state.batchTarget.toInt())
                    }
                    EditorSplitMode.ApiProcess,
                    EditorSplitMode.CustomScript,
                    EditorSplitMode.Manual -> emptyList()
                }
                EditorChapterDocument(chapters, EditorProcessor.toWebsiteIdentifiers(chapters))
            } }
    
            if (serial != editorProcessorRequestSerial) return@launch
            if (revision != documentRevision || uploadEditorState.text != state.text || uploadEditorState.chapters != state.chapters ||
                uploadEditorState.splitMode != state.splitMode || uploadEditorState.splitPattern != state.splitPattern ||
                uploadEditorState.splitTarget != state.splitTarget || uploadEditorState.batchMode != state.batchMode ||
                uploadEditorState.batchTarget != state.batchTarget) {
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "内容或规则已变化，未覆盖新草稿")
                return@launch
            }
            uploadEditorState = uploadEditorState.copy(busy = false)
    
            val updated = result.fold(
                onSuccess = { document ->
                    val chapters = document.chapters
                    if (chapters.isEmpty()) uploadEditorState.copy(actionMessage = "没有匹配到章节标题，请调整规则")
                    else uploadEditorState.copy(
                        text = document.text,
                        cursorPosition = 0,
                        chapters = chapters,
                        markerValidationErrors = emptyList(),
                        selectedTab = EditorTab.Chapters,
                        actionMessage = "已生成 ${chapters.size} 章"
                    )
                },
                onFailure = { failure -> uploadEditorState.copy(actionMessage = "分章失败：${failure.message ?: "规则无效"}") }
            )
            uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
                commitEditorDocumentChange(state, updated)
            } else {
                withEditorHistoryFlags(updated)
            }
        }
    }

    private fun editorSplitPattern(state: UploadEditorState): String = when (state.splitMode) {
        EditorSplitMode.ApiProcess -> state.apiEndpoint
        else -> state.splitPattern
    }

    private fun editorSplitTarget(state: UploadEditorState): String = when (state.splitMode) {
        EditorSplitMode.ApiProcess -> state.apiTimeoutSeconds
        EditorSplitMode.BatchGenerate -> state.batchTarget
        else -> state.splitTarget
    }

    private fun processEditorThroughApi(state: UploadEditorState) {
        val serial = ++editorProcessorRequestSerial
        val processorInput = when (state.apiMarkerMode) {
            EditorMarkerMode.Incremental -> state.text
            EditorMarkerMode.Full -> EditorProcessor.clearWebsiteIdentifiers(state.text)
        }
        uploadEditorState = state.copy(busy = true, actionMessage = "正在发送文本到接口处理…")
        work.launch {
            val result = editorResult {
                repository.processText(
                    endpoint = state.apiEndpoint,
                    text = processorInput,
                    timeoutSeconds = state.apiTimeoutSeconds.toInt()
                )
            }
            if (!isFreshRequestSerial(serial, editorProcessorRequestSerial)) return@launch
            if (uploadEditorState.text != state.text || uploadEditorState.chapters != state.chapters) {
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "草稿已修改，未用旧接口结果覆盖新内容")
                return@launch
            }
            val updated = result.fold(
                onSuccess = { processedText ->
                    val markerErrors = EditorProcessor.validateWebsiteIdentifiers(processedText)
                    val chapters = if (markerErrors.isEmpty()) {
                        EditorProcessor.parseWebsiteIdentifiers(processedText)
                    } else {
                        emptyList()
                    }
                    uploadEditorState.copy(
                        text = processedText,
                        cursorPosition = 0,
                        chapters = chapters,
                        markerValidationErrors = markerErrors,
                        selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                        busy = false,
                        actionMessage = when {
                            markerErrors.isNotEmpty() -> "接口处理已返回文本，但章节标识符需要修复"
                            chapters.isEmpty() -> "接口处理完成，返回文本未包含章节标识符"
                            else -> "接口处理完成，已识别 ${chapters.size} 章"
                        }
                    )
                },
                onFailure = { failure ->
                    uploadEditorState.copy(
                        busy = false,
                        actionMessage = apiFailureMessage("接口处理", failure)
                    )
                }
            )
            uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
                commitEditorDocumentChange(state, updated)
            } else {
                withEditorHistoryFlags(updated)
            }
        }
    }

    fun insertEditorTitleMarkerAtCursor() {
        applyEditorMarkerOperation(
            successMessage = "已插入标题标识符",
            cursorPosition = ::standaloneEditorMarkerCursor
        ) { state ->
            EditorProcessor.insertWebsiteTitleMarkerAtCursor(state.text, state.cursorPosition)
        }
    }

    fun insertEditorContentMarkerAtCursor() {
        applyEditorMarkerOperation(
            successMessage = "已插入内容标识符",
            cursorPosition = ::standaloneEditorMarkerCursor
        ) { state ->
            EditorProcessor.insertWebsiteContentMarkerAtCursor(state.text, state.cursorPosition)
        }
    }

    fun insertEditorChapterAtCursor() {
        applyEditorMarkerOperation("已在光标位置插入新章节") { state ->
            EditorProcessor.insertWebsiteChapterAtCursor(state.text, state.cursorPosition)
        }
    }

    fun deleteEditorChapterAtCursor() {
        applyEditorMarkerOperation("已删除光标所在章节并重新编号") { state ->
            EditorProcessor.deleteWebsiteChapterAtCursor(state.text, state.cursorPosition)
        }
    }

    fun renumberEditorMarkers() {
        applyEditorMarkerOperation("已重新编号所有章节标识符") { state ->
            EditorProcessor.renumberWebsiteIdentifiers(state.text)
        }
    }

    fun clearEditorMarkers() {
        val state = uploadEditorState
        val cleared = EditorProcessor.clearWebsiteIdentifiers(state.text)
        val updated = state.copy(
            text = cleared,
            cursorPosition = state.cursorPosition.coerceIn(0, cleared.length),
            chapters = emptyList(),
            markerValidationErrors = emptyList(),
            selectedTab = EditorTab.Text,
            actionMessage = "已清除所有章节标识符"
        )
        uploadEditorState = if (cleared == state.text) {
            withEditorHistoryFlags(updated)
        } else {
            commitEditorDocumentChange(state, updated)
        }
    }

    fun validateEditorMarkers() {
        val state = uploadEditorState
        val errors = EditorProcessor.validateWebsiteIdentifiers(state.text)
        uploadEditorState = state.copy(
            markerValidationErrors = errors,
            actionMessage = if (errors.isEmpty()) "所有章节标识符验证通过" else "发现 ${errors.size} 个章节标识符问题"
        )
    }

    private fun standaloneEditorMarkerCursor(state: UploadEditorState, updatedText: String): Int {
        val cursor = state.cursorPosition.coerceIn(0, state.text.length)
        val prefixLength = if (cursor > 0 && !state.text.substring(0, cursor).endsWith("\n")) 1 else 0
        return (cursor + prefixLength + "##__T[00001]__##".length).coerceIn(0, updatedText.length)
    }

    private fun applyEditorMarkerOperation(
        successMessage: String,
        cursorPosition: (UploadEditorState, String) -> Int = { state, updatedText ->
            state.cursorPosition.coerceIn(0, updatedText.length)
        },
        operation: (UploadEditorState) -> String
    ) {
        val state = uploadEditorState
        val result = editorResult { operation(state) }
        val updated = result.fold(
            onSuccess = { updatedText ->
                val errors = EditorProcessor.validateWebsiteIdentifiers(updatedText)
                val chapters = if (errors.isEmpty()) EditorProcessor.parseWebsiteIdentifiers(updatedText) else emptyList()
                state.copy(
                    text = updatedText,
                    cursorPosition = cursorPosition(state, updatedText).coerceIn(0, updatedText.length),
                    chapters = chapters,
                    markerValidationErrors = errors,
                    selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
                    actionMessage = if (errors.isEmpty()) successMessage else "$successMessage，但仍有标识符问题"
                )
            },
            onFailure = { failure ->
                state.copy(actionMessage = "标识符处理失败：${failure.message ?: "规则无效"}")
            }
        )
        uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
            commitEditorDocumentChange(state, updated)
        } else {
            withEditorHistoryFlags(updated)
        }
    }

    fun completeEditorCustomScript(runId: Long, processedText: String?, error: String?) {
        val state = uploadEditorState
        if (state.scriptRunId != runId || !state.busy) return
        if (scriptInput != state.text) {
            uploadEditorState = state.copy(busy = false, actionMessage = "脚本执行期间内容已变化，未覆盖新草稿")
            return
        }
        if (!error.isNullOrBlank() || processedText == null) {
            uploadEditorState = state.copy(
                busy = false,
                actionMessage = "脚本执行失败：${error ?: "未返回文本"}"
            )
            return
        }
        val markerErrors = EditorProcessor.validateWebsiteIdentifiers(processedText)
        val chapters = if (markerErrors.isEmpty()) EditorProcessor.parseWebsiteIdentifiers(processedText) else emptyList()
        uploadEditorState = commitEditorDocumentChange(state, state.copy(
            text = processedText,
            cursorPosition = 0,
            chapters = chapters,
            markerValidationErrors = markerErrors,
            selectedTab = if (chapters.isEmpty()) EditorTab.Text else EditorTab.Chapters,
            busy = false,
            actionMessage = if (markerErrors.isNotEmpty()) {
                "脚本处理完成，但章节标识符需要修复"
            } else if (chapters.isEmpty()) {
                "脚本处理完成；未发现网站章节标识，已保留处理后的文本"
            } else {
                "脚本处理完成，已生成 ${chapters.size} 章"
            }
        ))
    }

    fun replaceEditorText() {
        val state = uploadEditorState
        if (state.findText.isEmpty()) {
            uploadEditorState = state.copy(actionMessage = "请输入查找内容")
            return
        }
        val result = editorResult {
            if (state.findUsesRegex) state.text.replace(Regex(state.findText), state.replaceText)
            else state.text.replace(state.findText, state.replaceText)
        }
        val updated = result.fold(
            onSuccess = { replaced ->
                val changed = replaced != state.text
                state.copy(
                    text = replaced,
                    cursorPosition = state.cursorPosition.coerceIn(0, replaced.length),
                    chapters = if (changed) emptyList() else state.chapters,
                    markerValidationErrors = if (changed) emptyList() else state.markerValidationErrors,
                    selectedTab = if (changed) EditorTab.Text else state.selectedTab,
                    actionMessage = if (changed) "替换完成" else "未找到匹配项"
                )
            },
            onFailure = { failure -> state.copy(actionMessage = "替换失败：${failure.message ?: "正则无效"}") }
        )
        uploadEditorState = if (updated.text != state.text || updated.chapters != state.chapters) {
            commitEditorDocumentChange(state, updated)
        } else {
            withEditorHistoryFlags(updated)
        }
    }

    fun updateEditorChapter(index: Int, title: String, content: String) {
        enqueueChapterEdit(EditorChapterEdit.Update(title, content), index, "章节已更新")
    }

    fun addEditorChapter() {
        enqueueChapterEdit(EditorChapterEdit.Add, null, "已添加章节")
    }

    fun deleteEditorChapter(index: Int) {
        enqueueChapterEdit(EditorChapterEdit.Delete, index, "章节已删除并重新编号")
    }

    private fun enqueueChapterEdit(edit: EditorChapterEdit, index: Int?, message: String) {
        val previous = chapterEditTail
        if (previous == null) chapterSuccessors = IdentityHashMap()
        val successors = chapterSuccessors
        val target = index?.let { uploadEditorState.chapters.getOrNull(it) }
        val revision = documentRevision
        val waitingForOperation = previous == null && uploadEditorState.busy
        uploadEditorState = uploadEditorState.copy(busy = true, actionMessage = "章节编辑已排队…")
        lateinit var queued: Job
        queued = work.launch(start = CoroutineStart.LAZY) {
            try {
                previous?.join()
                if (waitingForOperation) snapshotFlow { uploadEditorState.busy }.first { !it }
                if (revision != documentRevision) {
                    uploadEditorState = uploadEditorState.copy(actionMessage = "草稿已变化，未应用旧章节编辑")
                    return@launch
                }
                uploadEditorState = uploadEditorState.copy(busy = true)
                val before = uploadEditorState
                // Dialog indices refer to the last published list, not an earlier queued deletion.
                var currentTarget = target
                while (currentTarget != null && successors.containsKey(currentTarget)) currentTarget = successors[currentTarget]
                val resolvedIndex = if (target == null) index else before.chapters.indexOfFirst { it === currentTarget }
                val result = withContext(Dispatchers.Default) { editorResult { editChapterDocument(before, edit, resolvedIndex) } }
                if (revision != documentRevision) {
                    uploadEditorState = uploadEditorState.copy(actionMessage = "草稿已变化，未用旧章节编辑覆盖新内容")
                    return@launch
                }
                uploadEditorState = result.fold(onSuccess = { document ->
                    before.chapters.forEachIndexed { oldIndex, chapter ->
                        val nextIndex = if (edit == EditorChapterEdit.Delete && resolvedIndex != null) {
                            when { oldIndex == resolvedIndex -> -1; oldIndex > resolvedIndex -> oldIndex - 1; else -> oldIndex }
                        } else oldIndex
                        val next = document.chapters.getOrNull(nextIndex)
                        if (chapter !== next) successors[chapter] = next
                    }
                    commitEditorDocumentChange(before, uploadEditorState.copy(
                        chapters = document.chapters, text = document.text,
                        cursorPosition = before.cursorPosition.coerceIn(0, document.text.length),
                        markerValidationErrors = emptyList(), actionMessage = message), queuedChapterEdit = true)
                }, onFailure = { uploadEditorState.copy(actionMessage = "章节编辑未完成：${it.message}") })
            } finally {
                if (chapterEditTail === queued) {
                    chapterEditTail = null
                    successors.clear()
                    uploadEditorState = uploadEditorState.copy(busy = false)
                }
            }
        }
        chapterEditTail = queued
        queued.start()
    }

    /** Existing Unit callbacks can request save/export/AI immediately after an accepted edit. */
    private fun afterChapterEdits(action: () -> Unit): Boolean {
        val pending = chapterEditTail ?: return false
        val revision = documentRevision
        work.launch {
            pending.join()
            if (revision == documentRevision) action()
            else uploadEditorState = uploadEditorState.copy(actionMessage = "草稿已变化，请重新确认操作")
        }
        return true
    }

    fun saveEditorArchive() {
        if (afterChapterEdits(::saveEditorArchive)) return
        val state = uploadEditorState
        if (state.text.isBlank() && state.chapters.isEmpty()) {
            uploadEditorState = state.copy(actionMessage = "没有可保存的编辑内容")
            return
        }
        if (state.busy) return
        uploadEditorState = state.copy(busy = true, actionMessage = "正在保存存档…")
        work.launch {
            val result = editorResult {
                val timestamp = System.currentTimeMillis()
                val archive = EditorArchive(
                    id = "archive_${timestamp}_${(0..9999).random()}",
                    name = state.archiveName.trim().ifBlank { state.metadata.title.ifBlank { "存档 $timestamp" } },
                    timestamp = timestamp,
                    textContent = state.text,
                    metadata = state.metadata,
                    fileName = state.fileName,
                    chapterCount = state.chapters.size,
                    totalWords = state.chapters.sumOf { it.content.length }.takeIf { it > 0 } ?: state.text.length
                )
                withContext(Dispatchers.IO) { repository.saveArchive(archive) }
            }
            uploadEditorState = result.fold(
                onSuccess = {
                    uploadEditorState.copy(
                        archiveName = "",
                        archives = repository.archives(),
                        busy = false,
                        actionMessage = "存档已保存"
                    )
                },
                onFailure = { failure -> uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("保存存档", failure)) }
            )
        }
    }

    fun loadEditorArchive(id: String) {
        if (uploadEditorState.busy) return
        val request = ++editorRequestSerial
        val before = uploadEditorState
        uploadEditorState = before.copy(busy = true, actionMessage = "正在校验存档…")
        work.launch {
            val result = try { Result.success(withContext(Dispatchers.IO) { repository.loadArchive(id) }) }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (failure: Exception) { Result.failure(failure) }
            if (request != editorRequestSerial) return@launch
            val current = uploadEditorState.copy(busy = false)
            if (current.text != before.text || current.metadata != before.metadata) {
                uploadEditorState = current.copy(actionMessage = "编辑内容已变化，未用存档覆盖新草稿")
                return@launch
            }
            uploadEditorState = result.fold(onSuccess = { archive ->
                if (archive == null) current.copy(actionMessage = "存档不存在，当前编辑内容保留")
                else replaceEditorDocument(current.copy(text = archive.textContent, metadata = archive.metadata,
                    fileName = archive.fileName, chapters = emptyList(), selectedTab = EditorTab.Text,
                    actionMessage = "存档已加载，请重新生成章节目录"))
            }, onFailure = { current.copy(actionMessage = apiFailureMessage("读取存档", it)) })
        }
    }

    fun deleteEditorArchive(id: String) {
        editorResult { repository.deleteArchive(id) }
            .onSuccess { uploadEditorState = uploadEditorState.copy(archives = repository.archives(), actionMessage = "存档已删除") }
            .onFailure { uploadEditorState = uploadEditorState.copy(actionMessage = apiFailureMessage("删除存档", it)) }
    }

    fun clearEditorArchives() {
        editorResult { repository.clearArchives() }
            .onSuccess { uploadEditorState = uploadEditorState.copy(archives = emptyList(), actionMessage = "所有存档已清空") }
            .onFailure { uploadEditorState = uploadEditorState.copy(actionMessage = apiFailureMessage("清空存档", it)) }
    }

    fun exportEditorEpub(rawUri: String) {
        if (afterChapterEdits { exportEditorEpub(rawUri) }) return
        val snapshot = uploadEditorState
        validateEditorOutput(snapshot)?.let { uploadEditorState = snapshot.copy(actionMessage = it); return }
        if (snapshot.busy) return
        uploadEditorState = snapshot.copy(busy = true, actionMessage = "正在生成 EPUB…")
        work.launch {
            val result = editorResult { repository.export(rawUri, snapshot.metadata, snapshot.chapters) }
            uploadEditorState = result.fold(
                onSuccess = { uploadEditorState.copy(busy = false, actionMessage = "EPUB 已生成") },
                onFailure = { uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("生成 EPUB", it)) })
        }
    }

    fun sendEditorToUpload(appendBookId: Long?, stillRequested: () -> Boolean,
        onPrepared: suspend (UploadBookState) -> Boolean, onNavigated: () -> Unit) {
        if (afterChapterEdits { sendEditorToUpload(appendBookId, stillRequested, onPrepared, onNavigated) }) return
        val snapshot = uploadEditorState
        validateEditorOutput(snapshot)?.let { uploadEditorState = snapshot.copy(actionMessage = it); return }
        if (snapshot.busy) return
        val serial = ++editorRequestSerial
        uploadEditorState = snapshot.copy(busy = true, actionMessage = "正在生成上传文件…")
        work.launch {
            var prepared: UploadBookState? = null
            try {
                prepared = repository.prepareUpload(appendBookId, snapshot.metadata, snapshot.chapters)
                if (serial != editorRequestSerial || !stillRequested()) {
                    uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "页面已变化，编辑草稿保留")
                    return@launch
                }
                if (!onPrepared(prepared)) {
                    uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "目标书籍仍有上传任务，编辑草稿保留")
                    return@launch
                }
                prepared = null
                uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = "已发送到上传页")
                onNavigated()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { uploadEditorState = uploadEditorState.copy(busy = false, actionMessage = apiFailureMessage("生成上传文件", failure)) }
            finally { prepared?.let { repository.discardPrepared(it) } }
        }
    }

    fun clearUploadEditor() {
        documentRevision++
        chapterEditTail = null
        editorRequestSerial++
        editorProcessorRequestSerial++
        scriptSerial++
        scriptInput = null
        work.coroutineContext.cancelChildren()
        editorDocumentHistory.clear()
        uploadEditorState = UploadEditorState(
            scriptRunId = scriptSerial,
            archives = repository.archives(),
            aiConfigs = uploadEditorState.aiConfigs,
            selectedAiConfigId = uploadEditorState.selectedAiConfigId
        )
    }

    private fun validateEditorOutput(state: UploadEditorState): String? = when {
        state.metadata.title.isBlank() -> "请填写书名"
        state.metadata.author.isBlank() -> "请填写作者"
        state.chapters.isEmpty() -> "请先生成章节目录"
        else -> null
    }

}
