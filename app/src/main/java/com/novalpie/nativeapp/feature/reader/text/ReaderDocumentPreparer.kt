package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Per-reader bounded parsed-document cache. No parsing/replacement work runs on the caller UI. */
internal class ReaderDocumentPreparer(
    private val maxEntries: Int = 8,
    private val parse: (ReaderContent) -> List<ReaderContentBlock> = ::readerBlocksForContent,
) {
    private data class Key(val book: Long, val source: ReaderChapterContent, val chapterOrder: Int?,
        val contentRules: List<ReaderReplacementRule>, val titleRules: List<ReaderReplacementRule>, val removeDuplicates: Boolean)
    private val cache = LinkedHashMap<Key, ReaderBodyLayoutChapter>(16, 0.75f, true)
    private val mutex = Mutex()
    @Volatile var cachedEntries: Int = 0
        private set

    suspend fun prepare(bookId: Long, sources: List<ReaderChapterContent>, catalog: List<Chapter>,
        rules: ReaderReplacementState, options: ReaderUiOptions): ReaderBodyLayout = withContext(Dispatchers.Default) {
        mutex.withLock {
            val prepared = sources.map { source ->
                currentCoroutineContext().ensureActive()
                val order = readerChapterOrderForId(source.chapterId, catalog)
                val contentRules = readerReplacementRulesForChapter(rules, order, ReaderReplacementTarget.Content)
                val titleRules = readerReplacementRulesForChapter(rules, order, ReaderReplacementTarget.Title)
                val key = Key(bookId, source, order.takeIf { contentRules.isNotEmpty() || titleRules.isNotEmpty() }, contentRules, titleRules, options.removeDuplicateLines)
                cache[key] ?: run {
                    val derived = effectiveReaderChapterContent(source, order, rules)
                    val layout = ReaderBodyLayoutChapter(derived, readerBlocksForDisplay(parse(derived.content), options.removeDuplicateLines))
                    currentCoroutineContext().ensureActive()
                    cache[key] = layout
                    while (cache.size > maxEntries.coerceAtLeast(1)) cache.remove(cache.keys.first())
                    cachedEntries = cache.size
                    layout
                }
            }
            currentCoroutineContext().ensureActive()
            readerBodyLayoutFromPreparedChapters(prepared, options)
        }
    }
}
