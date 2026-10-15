package com.novalpie.nativeapp.feature.books

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.NovalPieApiException
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ForumActionResult
import com.novalpie.nativeapp.ui.apiFailureMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Numbers in this protocol are chapter numbers, never chapter IDs or current filtered indexes. */
internal fun retranslationChapterNumbers(input:String,available:List<Int>):List<Int> {
    val known=available.filter { it>0 }.distinct().sorted()
    require(known.isNotEmpty()) { "章节目录尚未加载，请先刷新目录" }
    require(known.size<=100_000) { "章节数量过大，请分批选择" }
    if(input.isBlank())return known
    val picked=sortedSetOf<Int>()
    for(part in input.replace('，',',').split(',')) {
        val token=part.trim()
        val range=Regex("([0-9]+)\\s*-\\s*([0-9]+)").matchEntire(token)
        if(range!=null) {
            val start=range.groupValues[1].toIntOrNull() ?: error("章节号过大")
            val end=range.groupValues[2].toIntOrNull() ?: error("章节号过大")
            require(start>0&&end>=start&&end.toLong()-start<100_000) { "无效的章节范围：$token" }
            for(n in start..end)picked.add(n)
        } else {
            require(token.matches(Regex("[0-9]+"))) { "无效的章节号：$token" }
            val number=token.toIntOrNull() ?: error("章节号过大")
            require(number>0) { "章节号必须大于 0" };picked.add(number)
        }
        require(picked.size<=100_000) { "章节数量过大，请分批选择" }
    }
    val unknown=picked-known.toSet()
    require(unknown.isEmpty()) { "目录中没有第 ${unknown.take(5).joinToString("、")} 章" }
    return picked.toList()
}

internal interface ChapterRetranslationRepository {
    suspend fun chapters(bookId:Long):List<Chapter>
    suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult
}
internal class WebsiteChapterRetranslationRepository(private val api:NovalPieApi):ChapterRetranslationRepository {
    override suspend fun chapters(bookId:Long)=api.chapters(bookId)
    override suspend fun submit(bookId:Long,numbers:List<Int>)=api.retranslateNovelChapters(bookId,numbers)
}
internal data class ChapterRetranslationState(
    val bookId:Long=0,val bookTitle:String="",val open:Boolean=false,val range:String="",
    val chapterNumbers:List<Int> = emptyList(),val loading:Boolean=false,val submitting:Boolean=false,
    val confirmed:Boolean=false,val uncertain:Boolean=false,val message:String?=null,
    val currentChapterMissing:Boolean=false,
) {
    val selection:Result<List<Int>> get()=runCatching {
        require(!currentChapterMissing) { "目录中无法确认当前章节，不能自动改成全书重翻；请刷新目录" }
        retranslationChapterNumbers(range,chapterNumbers)
    }
}

/** Independent owner/session lifetime: late A responses cannot mutate B or a switched account. */
internal class ChapterRetranslationController(
    private val repository:ChapterRetranslationRepository,private val scope:CoroutineScope,
    private val environmentRevision:()->Long,
) {
    var state by mutableStateOf(ChapterRetranslationState());private set
    private var generation=0L
    private var ownerEpoch=0L
    private val unresolved=mutableSetOf<Long>()
    private val inFlight=mutableSetOf<Long>()
    private val receipts=mutableMapOf<Long,String>()
    fun open(bookId:Long,title:String,catalog:List<Chapter>?,currentNumber:Int?=null,currentChapterId:Long?=null) {
        if(bookId<=0)return
        val id=++generation;val environment=environmentRevision()
        val number=currentNumber ?: catalog?.firstOrNull{it.id==currentChapterId}?.number
        state=ChapterRetranslationState(bookId,title,open=true,range=number?.toString().orEmpty(),
            chapterNumbers=catalog.orEmpty().mapNotNull {it.number}.distinct().sorted(),
            submitting=bookId in inFlight,uncertain=bookId in unresolved,
            confirmed=bookId in receipts,message=receipts[bookId],currentChapterMissing=currentChapterId!=null&&number==null)
        if(state.chapterNumbers.isNotEmpty()&&!state.currentChapterMissing)return
        state=state.copy(loading=true)
        scope.launch {
            val result=runCatching { repository.chapters(bookId) }
            if(id!=generation||environment!=environmentRevision())return@launch
            val loaded=result.getOrNull().orEmpty()
            val current=loaded.firstOrNull{it.id==currentChapterId}?.number
            state=state.copy(loading=false,chapterNumbers=loaded.mapNotNull{it.number}.distinct().sorted(),
                range=current?.toString() ?: state.range,currentChapterMissing=currentChapterId!=null&&current==null,
                message=result.exceptionOrNull()?.let {apiFailureMessage("加载重翻目录",it)})
        }
    }
    fun updateRange(value:String) {
        if(!state.submitting&&value!=state.range) {
            receipts.remove(state.bookId)
            state=state.copy(range=value,confirmed=false,message=null)
        }
    }
    fun dismiss() { generation++;state=state.copy(open=false) }
    fun environmentChanged() { generation++;ownerEpoch++;unresolved.clear();inFlight.clear();receipts.clear();state=ChapterRetranslationState() }
    fun transportChanged() {
        generation++
        // GETs may be invalidated, but an already-written task still belongs to this account.
        if(state.loading)state=state.copy(loading=false,message="网络配置已变化，请重新打开重翻窗口加载目录")
    }
    fun confirmCheckedForRetry() { unresolved.remove(state.bookId);state=state.copy(uncertain=false,message=null) }
    fun submit() {
        val before=state
        if(!before.open||before.loading||before.submitting||before.confirmed||before.uncertain)return
        val numbers=before.selection.getOrElse {state=state.copy(message=it.message);return}
        val environment=environmentRevision();val owner=ownerEpoch;inFlight.add(before.bookId)
        state=state.copy(submitting=true,message=null)
        scope.launch {
            var written=false
            try {
                // Listing metadata can omit approval_status even for an allowed account.
                // The same v2 POST as the website is the authoritative permission decision.
                if(owner!=ownerEpoch)return@launch
                if(environment!=environmentRevision()) {
                    if(state.bookId==before.bookId)state=state.copy(message="网络配置已变化，本次尚未提交；请重新确认后提交")
                    return@launch
                }
                written=true
                val result=repository.submit(before.bookId,numbers)
                if(owner!=ownerEpoch)return@launch
                val message=if(result.success) "${result.message ?: "重翻任务已提交"}\n已提交 ${numbers.size} 章，完成后请刷新正文；提交成功不代表翻译已完成。" else result.message ?: "重翻请求被拒绝"
                if(result.success)receipts[before.bookId]=message
                if(state.bookId==before.bookId)state=state.copy(confirmed=result.success,message=message)
            } catch(cancelled:CancellationException) {
                // The API also uses cancellation to invalidate a response after a proxy/token
                // revision. Once sent, cancellation is NOT proof that the server rejected a POST.
                if(written&&owner==ownerEpoch) {
                    unresolved.add(before.bookId)
                    if(state.bookId==before.bookId)state=state.copy(uncertain=true,
                        message="提交响应已中断，结果尚未确认；请先核对任务，避免重复消耗 API。")
                }
                throw cancelled
            }
            catch(error:Exception) {
                if(owner!=ownerEpoch)return@launch
                val duplicate=(error as? NovalPieApiException)?.statusCode==409
                val uncertain=written&&((error as? NovalPieApiException)?.statusCode?.let {it>=500} ?: true)
                if(uncertain)unresolved.add(before.bookId)
                val message=if(duplicate) "服务器提示已有任务处理中，不会重复提交。" else if(uncertain)
                    "提交结果尚未确认，请先在网页或工作区确认任务，避免重复消耗 API。" else apiFailureMessage("重翻章节",error)
                if(duplicate)receipts[before.bookId]=message
                if(state.bookId==before.bookId)state=state.copy(confirmed=duplicate,uncertain=uncertain,message=message)
            } finally {
                if(owner==ownerEpoch)inFlight.remove(before.bookId)
                if(state.bookId==before.bookId&&owner==ownerEpoch)state=state.copy(submitting=false)
            }
        }
    }
}
