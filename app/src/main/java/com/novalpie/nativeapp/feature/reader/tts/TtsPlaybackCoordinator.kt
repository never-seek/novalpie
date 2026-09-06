package com.novalpie.nativeapp.feature.reader.tts

import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.ui.readerTtsEngineSettingsChanged
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class SpeechChapter(
    val bookId: Long,
    val chapterId: Long,
    val bookTitle: String,
    val chapterTitle: String,
    val segments: List<String>,
    val nextChapterId: Long? = null,
    val textRevision: Long = 0,
    val positions:List<SpeechTextPosition> = emptyList(),
    val chapterNumber:Int?=null,
    val chapterCount:Int?=null,
)

internal fun interface SpeechChapterSource {
    suspend fun load(bookId: Long,chapterId: Long): SpeechChapter
}

internal interface SpeechEngine {
    fun start(segments:List<String>,startIndex:Int,settings:ReaderTtsSettings,onSegment:(Int)->Unit,onFinished:()->Unit,onError:(String)->Unit)
    fun stop()
    fun close()
}

internal enum class SpeechStatus { Stopped, Loading, Speaking, Paused, Error }
internal data class SpeechPlaybackState(
    val status: SpeechStatus = SpeechStatus.Stopped,
    val chapter: SpeechChapter? = null,
    val segmentIndex: Int = 0,
    val message: String? = null,
    val generation: Long = 0,
)

/**
 * Application-scoped playback, independent of a Compose page. Commands and engine callbacks run
 * on the supplied Main scope. Every stop/pause/navigation/rule change invalidates ALL old events,
 * including a chapter request that ignores coroutine cancellation.
 */
internal class TtsPlaybackCoordinator(
    private val engine: SpeechEngine,
    private val source: SpeechChapterSource,
    private val scope: CoroutineScope,
    private val onPosition:(SpeechChapter,Int)->Unit={_,_->},
) {
    private val mutable = MutableStateFlow(SpeechPlaybackState())
    val state = mutable.asStateFlow()
    private var serial = 0L
    private var settings = ReaderTtsSettings()
    private var nextJob: Job? = null
    private var pendingNext: Pair<Long,Long>? = null

    fun start(chapter:SpeechChapter,settings:ReaderTtsSettings,startIndex:Int=0) {
        this.settings=settings
        pendingNext=null
        play(chapter,startIndex)
    }

    fun preparePaused(chapter:SpeechChapter,settings:ReaderTtsSettings,startIndex:Int,message:String) {
        invalidate()
        this.settings=settings
        pendingNext=null
        mutable.value=SpeechPlaybackState(SpeechStatus.Paused,chapter.copy(segments=chapter.segments.toList()),
            startIndex.coerceIn(0,chapter.segments.lastIndex.coerceAtLeast(0)),message,serial)
    }

    private fun play(chapter:SpeechChapter,startIndex:Int) {
        invalidate()
        if(chapter.bookId<=0 || chapter.chapterId<=0 || chapter.segments.isEmpty() || chapter.segments.all(String::isBlank)) {
            mutable.value=SpeechPlaybackState(SpeechStatus.Error,chapter,message="当前章节没有可朗读的正文",generation=serial)
            return
        }
        val snapshot=chapter.copy(segments=chapter.segments.toList())
        val index=startIndex.coerceIn(snapshot.segments.indices)
        val generation=serial
        mutable.value=SpeechPlaybackState(SpeechStatus.Loading,snapshot,index,generation=generation)
        try {
            engine.start(snapshot.segments,index,settings,
                onSegment={position->
                    if(generation==serial && position in snapshot.segments.indices && position>=mutable.value.segmentIndex) {
                        mutable.value=mutable.value.copy(status=SpeechStatus.Speaking,segmentIndex=position,message=null)
                        onPosition(snapshot,position)
                    }
                },
                onFinished={
                    if(generation==serial && mutable.value.status in setOf(SpeechStatus.Loading,SpeechStatus.Speaking)) {
                        val next=snapshot.nextChapterId
                        if(settings.enableAutoNextChapter && next!=null && next!=snapshot.chapterId) requestNext(snapshot.bookId,next)
                        else stop()
                    }
                },
                onError={message->if(generation==serial) fail(message)},
            )
        } catch(failure:Exception) { if(generation==serial) fail(failure.message ?: "系统听书引擎无法启动") }
    }

    fun pause() {
        if(mutable.value.status !in setOf(SpeechStatus.Loading,SpeechStatus.Speaking))return
        invalidate()
        mutable.value=mutable.value.copy(status=SpeechStatus.Paused,generation=serial)
    }

    fun resume() {
        if(mutable.value.status!=SpeechStatus.Paused)return
        val next=pendingNext
        if(next!=null)requestNext(next.first,next.second)
        else mutable.value.chapter?.let{play(it,mutable.value.segmentIndex)}
    }

    fun interrupt(message:String) {
        if(mutable.value.status !in setOf(SpeechStatus.Loading,SpeechStatus.Speaking))return
        pause()
        mutable.value=mutable.value.copy(message=message)
    }

    fun stop() {
        invalidate()
        pendingNext=null
        mutable.value=mutable.value.copy(status=SpeechStatus.Stopped,message=null,generation=serial)
    }

    fun replaceChapter(chapter:SpeechChapter) {
        val current=mutable.value
        if(current.chapter?.bookId!=chapter.bookId||current.chapter.chapterId!=chapter.chapterId)return
        if(current.chapter.textRevision==chapter.textRevision && current.chapter.segments==chapter.segments)return
        if(current.status in setOf(SpeechStatus.Loading,SpeechStatus.Speaking))play(chapter,current.segmentIndex)
        else {
            invalidate()
            mutable.value=current.copy(chapter=chapter.copy(segments=chapter.segments.toList()),
                segmentIndex=current.segmentIndex.coerceIn(0,chapter.segments.lastIndex.coerceAtLeast(0)),generation=serial)
        }
    }

    fun updateSettings(next:ReaderTtsSettings) {
        if(next==settings)return
        val engineChanged=readerTtsEngineSettingsChanged(settings,next)
        settings=next
        if(engineChanged && mutable.value.status in setOf(SpeechStatus.Loading,SpeechStatus.Speaking))pause()
    }

    fun retry() {
        if(mutable.value.status!=SpeechStatus.Error)return
        val next=pendingNext
        if(next!=null)requestNext(next.first,next.second)
        else mutable.value.chapter?.let{play(it,mutable.value.segmentIndex)}
    }

    private fun requestNext(bookId:Long,chapterId:Long) {
        invalidate()
        pendingNext=bookId to chapterId
        val generation=serial
        mutable.value=mutable.value.copy(status=SpeechStatus.Loading,message="正在加载下一章",generation=generation)
        nextJob=scope.launch {
            try {
                val chapter=source.load(bookId,chapterId)
                if(generation!=serial)return@launch
                require(chapter.bookId==bookId&&chapter.chapterId==chapterId){"听书章节身份不匹配"}
                pendingNext=null
                // Do not cancel this coroutine when play() invalidates the preceding generation.
                nextJob=null
                play(chapter,0)
            } catch(cancelled:CancellationException) {throw cancelled}
            catch(failure:Exception) {if(generation==serial)fail(failure.message ?: "下一章载入失败，可重试")}
        }
    }

    private fun fail(message:String) {
        invalidate()
        mutable.value=mutable.value.copy(status=SpeechStatus.Error,message=message,generation=serial)
    }

    private fun invalidate() {
        serial++
        nextJob?.cancel()
        nextJob=null
        engine.stop()
    }

    fun close() {stop();engine.close()}
}
