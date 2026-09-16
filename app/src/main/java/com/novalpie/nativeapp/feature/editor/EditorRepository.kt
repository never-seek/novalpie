package com.novalpie.nativeapp.feature.editor

import android.content.Context
import android.net.Uri
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.UUID

internal data class EditorLoadedDocument(val document: UploadDocument, val text: String,
    val metadata: EditorBookMetadata, val chapters: List<UploadChapter>)

internal interface EditorRepository {
    fun configs(): List<WorkspaceLocalApiConfig>
    fun archives(): List<EditorArchive>
    suspend fun document(uri: String): UploadDocument
    suspend fun loadDocument(uri: String, encoding: String, metadata: EditorBookMetadata): EditorLoadedDocument
    suspend fun importDocuments(documents: List<UploadDocument>, encoding: String): List<UploadChapter>
    suspend fun generateRegex(config: WorkspaceLocalApiConfig, chapterTitles: List<String>): String
    suspend fun processText(endpoint: String, text: String, timeoutSeconds: Int): String
    fun saveArchive(archive: EditorArchive)
    fun loadArchive(id: String): EditorArchive?
    fun deleteArchive(id: String)
    fun clearArchives()
    suspend fun export(uri: String, metadata: EditorBookMetadata, chapters: List<UploadChapter>)
    suspend fun prepareUpload(bookId: Long?, metadata: EditorBookMetadata, chapters: List<UploadChapter>): UploadBookState
    fun discardPrepared(prepared: UploadBookState)
}

internal class AndroidEditorRepository(context: Context, private val api: NovalPieApi,
    private val readDocument: suspend (String) -> UploadDocument,
    private val source: (UploadDocument) -> UploadFileSource,
) : EditorRepository {
    private val application = context.applicationContext
    private val store = EditorArchiveStore(application)
    private val local = WorkspaceLocalStore(application)
    private val preparedDirectory = File(application.cacheDir, "editor-upload-files").canonicalFile
    override fun configs() = local.loadApis()
    override fun archives() = store.list()
    override suspend fun document(uri: String) = readDocument(uri)
    override fun saveArchive(archive: EditorArchive) = store.save(archive)
    override fun loadArchive(id: String) = store.load(id)
    override fun deleteArchive(id: String) = store.delete(id)
    override fun clearArchives() = store.clear()
    override suspend fun generateRegex(config: WorkspaceLocalApiConfig, chapterTitles: List<String>) =
        api.generateEditorRegex(config.endpoint, config.apiKey, config.model, chapterTitles)
    override suspend fun processText(endpoint: String, text: String, timeoutSeconds: Int) =
        api.processEditorTextWithApi(endpoint, text, timeoutSeconds)
    override suspend fun loadDocument(uri: String, encoding: String, metadata: EditorBookMetadata): EditorLoadedDocument {
        val document = readDocument(uri)
        return if (document.displayName.endsWith(".epub", true)) {
            withContext(Dispatchers.IO) {
                val parsed = EpubParser.parse(source(document))
                EditorLoadedDocument(document, EditorProcessor.toWebsiteIdentifiers(parsed.chapters),
                    EditorBookMetadata(title = parsed.title, author = parsed.author, description = parsed.description, language = parsed.language), parsed.chapters)
            }
        } else EditorLoadedDocument(document, readEditorText(document, encoding), metadata, emptyList())
    }
    override suspend fun export(uri: String, metadata: EditorBookMetadata, chapters: List<UploadChapter>) = withContext(Dispatchers.IO) {
        application.contentResolver.openOutputStream(Uri.parse(uri), "w")?.use { EpubWriter.write(it, metadata, chapters) }
            ?: throw IOException("无法写入目标文件")
    }
    override suspend fun prepareUpload(bookId: Long?, metadata: EditorBookMetadata, chapters: List<UploadChapter>): UploadBookState {
        // Ownership must outlive the IO block: prompt cancellation can discard its return value.
        var unreceivedFile: File? = null
        try {
            val prepared = withContext(Dispatchers.IO) {
                check(preparedDirectory.parentFile == application.cacheDir.canonicalFile)
                check(preparedDirectory.isDirectory || preparedDirectory.mkdirs()) { "无法创建上传暂存目录" }
                val file = File(preparedDirectory, "editor-${UUID.randomUUID()}.epub")
                unreceivedFile = file
                file.outputStream().use { EpubWriter.write(it, metadata, chapters) }
                UploadBookState(existingNovelId = bookId, draft = UploadBookDraft(title = metadata.title, author = metadata.author,
                    description = metadata.description, language = metadata.language, isAdult = metadata.isAdult,
                    source = metadata.source, sourceUrl = metadata.sourceUrl, tagsText = metadata.tags, chapterCount = chapters.size),
                    selectedFile = UploadDocument(file.toURI().toString(), file.name, file.length(), "application/epub+zip"),
                    chapters = LoadResult.Success(chapters.toList()), actionMessage = "编辑器内容已准备好，请核对后确认上传")
            }
            // No suspension between transferring ownership and returning to the caller.
            unreceivedFile = null
            return prepared
        } finally { unreceivedFile?.delete() }
    }
    override fun discardPrepared(prepared: UploadBookState) {
        val uri = prepared.selectedFile?.uri?.let(Uri::parse) ?: return
        if (uri.scheme != "file") return
        val file = File(uri.path ?: return).canonicalFile
        require(file.parentFile == preparedDirectory && file.name.matches(Regex("editor-[0-9a-f-]{36}\\.epub"))) { "暂存文件不属于本任务" }
        file.delete()
    }
    private suspend fun readEditorText(document: UploadDocument, encoding: String): String = withContext(Dispatchers.IO) {
        val charset = runCatching { Charset.forName(encoding) }.getOrElse { throw IOException("不支持的编码：$encoding") }
        InputStreamReader(source(document).openStream(), charset).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(16 * 1024)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                result.append(buffer, 0, read)
                if (result.length > 50_000_000) throw IOException("文本超过 5000 万字符，请先分割文件")
            }
            result.toString()
        }
    }

    override suspend fun importDocuments(
        documents: List<UploadDocument>,
        encoding: String
    ): List<UploadChapter> = withContext(Dispatchers.IO) {
        val charset = runCatching { Charset.forName(encoding) }
            .getOrElse { throw IOException("不支持的编码：$encoding") }
        val imported = mutableListOf<UploadChapter>()

        documents.forEach { document ->
            when {
                document.displayName.endsWith(".epub", ignoreCase = true) -> {
                    imported += EpubParser.parse(source(document)).chapters
                }
                document.displayName.endsWith(".zip", ignoreCase = true) -> {
                    val entries = EditorBatchImporter.readArchive(source(document).openStream(), charset)
                    entries.forEach { entry ->
                        imported += UploadChapter(
                            title = editorBatchChapterTitle(entry.displayName, imported.size + 1),
                            content = entry.text,
                            chapterNumber = imported.size + 1
                        )
                    }
                }
                EditorBatchImporter.isTextFile(document.displayName) -> {
                    imported += UploadChapter(
                        title = editorBatchChapterTitle(document.displayName, imported.size + 1),
                        content = readEditorText(document, encoding),
                        chapterNumber = imported.size + 1
                    )
                }
                else -> throw IOException("不支持的批量文件：${document.displayName}")
            }
        }

        if (imported.isEmpty()) throw IOException("没有可导入的章节内容")
        val totalCharacters = imported.sumOf { it.title.length + it.content.length }
        if (totalCharacters > EditorBatchImporter.MAX_TOTAL_CHARACTERS) {
            throw IOException("批量文本超过 5000 万字符，请减少文件后重试")
        }
        imported.mapIndexed { index, chapter ->
            chapter.copy(
                title = chapter.title.trim().ifBlank { "第 ${index + 1} 章" },
                chapterNumber = index + 1
            )
        }
    }

    private fun editorBatchChapterTitle(displayName: String, index: Int): String = displayName
        .substringBeforeLast('.', displayName)
        .replace(Regex("[\\r\\n\\t]"), " ")
        .trim()
        .ifBlank { "第 $index 章" }


}
