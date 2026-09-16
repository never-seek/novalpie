package com.novalpie.nativeapp.feature.editor

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/** A real dispatch boundary whose return can be held before the UI resumes. */
internal class EditorUiQueue : CoroutineDispatcher() {
    private val queue = LinkedBlockingQueue<Runnable>()
    override fun dispatch(context: CoroutineContext, block: Runnable) { queue.add(block) }
    fun next(): Runnable = checkNotNull(queue.poll(10, TimeUnit.SECONDS)) { "No UI continuation within 10 seconds" }
    fun runNext() = next().run()
    fun drain() { while (true) (queue.poll() ?: return).run() }
}

internal fun uiAllocations(block: () -> Unit): Long {
    // Android's compile stubs omit management APIs; the unit-test worker runs on a full JDK.
    val bean = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null)
    val type = Class.forName("com.sun.management.ThreadMXBean")
    check(type.getMethod("isThreadAllocatedMemorySupported").invoke(bean) == true)
    type.getMethod("setThreadAllocatedMemoryEnabled", Boolean::class.javaPrimitiveType).invoke(bean, true)
    val bytes = type.getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    val id = Thread.currentThread().id
    val before = bytes.invoke(bean, id) as Long
    block()
    return (bytes.invoke(bean, id) as Long) - before
}

/** Only document/archive/export I/O is replaced; processing and history remain real. */
internal open class EditorMemoryRepository : EditorRepository {
    val saved = mutableListOf<EditorArchive>()
    var loadedChapters = emptyList<UploadChapter>()
    override fun configs() = listOf(WorkspaceLocalApiConfig(1, "合成配置", "fixture", "https://fixture.invalid", "test-only-not-a-real-key"))
    override fun archives() = saved.toList()
    override suspend fun document(uri: String) = UploadDocument(uri, "fixture.txt", 1)
    override suspend fun loadDocument(uri: String, encoding: String, metadata: EditorBookMetadata) =
        EditorLoadedDocument(document(uri), "loaded fixture", metadata, loadedChapters)
    override suspend fun importDocuments(documents: List<UploadDocument>, encoding: String) = loadedChapters
    override suspend fun generateRegex(config: WorkspaceLocalApiConfig, chapterTitles: List<String>) = "^第.+章"
    override suspend fun processText(endpoint: String, text: String, timeoutSeconds: Int) = error("unused")
    override fun saveArchive(archive: EditorArchive) { saved += archive }
    override fun loadArchive(id: String) = saved.find { it.id == id }
    override fun deleteArchive(id: String) { saved.removeAll { it.id == id } }
    override fun clearArchives() { saved.clear() }
    override suspend fun export(uri: String, metadata: EditorBookMetadata, chapters: List<UploadChapter>) = Unit
    override suspend fun prepareUpload(bookId: Long?, metadata: EditorBookMetadata, chapters: List<UploadChapter>): UploadBookState = error("unused")
    override fun discardPrepared(prepared: UploadBookState) = Unit
}
