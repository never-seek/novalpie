package com.novalpie.nativeapp.feature.reader.tts

import android.content.Context
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.data.ReaderSessionStore
import com.novalpie.nativeapp.feature.reader.pagination.ReaderAnchor
import com.novalpie.nativeapp.feature.reader.pagination.ReaderAnchorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Called only by accepted onStart callbacks; loading a next chapter cannot write reading history. */
internal class SpeechProgressRecorder(context:Context,private val api:NovalPieApi,private val scope:CoroutineScope,private val environmentRevision:()->Long) {
    private val progress=ReaderProgressStore(context.applicationContext)
    private val anchors=ReaderAnchorStore(context.applicationContext)
    private val session=ReaderSessionStore(context.applicationContext)
    private val writes=Mutex()
    private var lastSynchronized:Triple<Long,Long,Long>?=null
    fun record(chapter:SpeechChapter,index:Int) {
        val position=chapter.positions.getOrNull(index) ?: return // Synthetic engine tests have no real reader position.
        val revision=environmentRevision()
        scope.launch {
            writes.withLock {
                if(revision!=environmentRevision())return@withLock
                withContext(Dispatchers.IO) {
                    progress.save(chapter.bookId,chapter.chapterId,chapter.chapterTitle,chapter.bookTitle,chapter.chapterNumber,chapter.chapterCount,
                        viewportItemIndex=position.itemIndexWithinChapter,viewportItemScrollOffsetPx=0)
                    anchors.save(ReaderAnchor(chapter.bookId,chapter.chapterId,position.blockId,position.textOffset))
                    if(session.load()?.bookId==chapter.bookId)session.save(chapter.bookId,chapter.chapterId)
                }
                val identity=Triple(revision,chapter.bookId,chapter.chapterId)
                if(identity!=lastSynchronized&&revision==environmentRevision()) {
                    if(runCatching{api.saveReadingProgress(chapter.bookId,chapter.chapterId)}.isSuccess)lastSynchronized=identity
                }
            }
        }
    }
}
