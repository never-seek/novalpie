package com.novalpie.nativeapp.feature.reader.tts

import android.content.Context
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.data.ReaderSessionStore
import com.novalpie.nativeapp.feature.reader.pagination.ReaderAnchor
import com.novalpie.nativeapp.feature.reader.pagination.ReaderAnchorStore
import kotlinx.coroutines.CoroutineScope
import com.novalpie.nativeapp.feature.reader.progress.ReadingProgressSynchronizer

/** Called only by accepted onStart callbacks; loading a next chapter cannot write reading history. */
internal class SpeechProgressRecorder(
    context: Context,
    api: NovalPieApi,
    scope: CoroutineScope,
    synchronizer: ReadingProgressSynchronizer? = null,
    environmentRevision: () -> Long,
) {
    private val progress=ReaderProgressStore(context.applicationContext)
    private val anchors=ReaderAnchorStore(context.applicationContext)
    private val session=ReaderSessionStore(context.applicationContext)
    private val sync = synchronizer ?: ReadingProgressSynchronizer(scope, environmentRevision) { bookId, chapterId ->
        api.saveReadingProgress(bookId, chapterId)
    }
    fun record(chapter:SpeechChapter,index:Int) {
        val position=chapter.positions.getOrNull(index) ?: return // Synthetic engine tests have no real reader position.
        // onStart is already generation-checked and dispatched on Main. SharedPreferences.apply
        // updates memory synchronously and schedules disk I/O; no delayed coroutine may overwrite
        // a later manually chosen reading point after a slow website response returns.
        progress.save(chapter.bookId,chapter.chapterId,chapter.chapterTitle,chapter.bookTitle,chapter.chapterNumber,chapter.chapterCount,
            viewportItemIndex=position.itemIndexWithinChapter,viewportItemScrollOffsetPx=0)
        anchors.save(ReaderAnchor(chapter.bookId,chapter.chapterId,position.blockId,position.textOffset))
        if(session.load()?.bookId==chapter.bookId)session.save(chapter.bookId,chapter.chapterId)
        sync.request(chapter.bookId, chapter.chapterId)
    }
}
