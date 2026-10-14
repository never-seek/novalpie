package com.novalpie.nativeapp.feature.books

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.BookDetailState
import com.novalpie.nativeapp.ui.apiFailureMessage
import kotlinx.coroutines.*

internal interface BookDetailRepository {
    suspend fun book(id:Long):NovelCard
    suspend fun cover(id:Long):String?
    suspend fun chapters(id:Long):List<Chapter>
    suspend fun comments(id:Long):List<ChapterComment>
    suspend fun favorite(id:Long):FavoriteStatus
    suspend fun permissions(id:Long):BookEditPermissions
}
internal class WebsiteBookDetailRepository(private val api:NovalPieApi):BookDetailRepository {
    override suspend fun book(id:Long)=api.bookDetail(id)
    override suspend fun cover(id:Long)=api.bookCoverPhoto(id)
    override suspend fun chapters(id:Long)=api.chapters(id)
    override suspend fun comments(id:Long)=api.bookComments(id,1,30)
    override suspend fun favorite(id:Long)=api.favoriteStatus(id)
    override suspend fun permissions(id:Long)=api.managedBookPermissions(id)
}

internal enum class BookActionLane { Comments, Favorite, NewChapters }
internal data class BookActionTicket(val bookId: Long, val ownerEpoch: Long, val operation: Long, val lane: BookActionLane)

/** Account-owned book snapshots; read generations and write operations have independent lifetimes. */
internal class BookDetailViewModel(
    private val repository:BookDetailRepository,
    scope:CoroutineScope?=null,
    private val progress:(Long)->ReaderProgress?={null},
    private val onComments:(Long,List<ChapterComment>)->Unit={_,_->},
):ViewModel() {
    private val work=scope ?: viewModelScope
    private var visible by mutableStateOf(BookDetailState())
    val state:BookDetailState get()=visible
    var revision=0L
        private set
    private var ownerEpoch=0L
    private var operation=0L
    private var request:Job?=null
    private val retained=linkedMapOf<Long,BookDetailState>()
    private data class Composer(val draft:String,val replyId:Long?,val replyName:String?,val query:String)
    private val composers=mutableMapOf<Long,Composer>()
    private val pending=mutableMapOf<Pair<Long,BookActionLane>,BookActionTicket>()
    private val dirtyComments=mutableSetOf<Long>()
    private val commentEpochs=mutableMapOf<Long,Long>()
    private val favoriteEpochs=mutableMapOf<Long,Long>()

    private fun remember(value:BookDetailState) {
        if(value.bookId<=0)return
        composers[value.bookId]=Composer(value.commentDraft,value.replyingToCommentId,value.replyingToName,value.catalogQuery)
        retained.remove(value.bookId);retained[value.bookId]=value
        // Large catalog/comment lists are bounded; unsent composers stay independent of this LRU.
        while(retained.size>12) {
            val evict=retained.keys.firstOrNull {id->id!=visible.bookId&&pending.keys.none{it.first==id}} ?: break
            retained.remove(evict)
        }
    }
    private fun publish(value:BookDetailState) { visible=value;remember(value) }
    fun present(transform:(BookDetailState)->BookDetailState){publish(transform(state))}
    fun retainedState(id:Long):BookDetailState? = state.takeIf{it.bookId==id} ?: retained[id]
    private fun base(id:Long):BookDetailState {
        retainedState(id)?.let{return it}
        val c=composers[id]
        return BookDetailState(bookId=id,commentDraft=c?.draft.orEmpty(),replyingToCommentId=c?.replyId,
            replyingToName=c?.replyName,catalogQuery=c?.query.orEmpty())
    }
    private fun BookDetailState.busy(lane:BookActionLane,value:Boolean):BookDetailState=when(lane) {
        BookActionLane.Comments->copy(actionLoading=value)
        BookActionLane.Favorite->copy(favoriteLoading=value)
        BookActionLane.NewChapters->copy(requestNewChapterLoading=value)
    }
    fun beginAction(lane:BookActionLane):BookActionTicket? {
        val id=state.bookId
        val busy=when(lane) {
            BookActionLane.Comments->state.actionLoading
            BookActionLane.Favorite->state.favoriteLoading
            BookActionLane.NewChapters->state.requestNewChapterLoading
        }
        if(id<=0||busy||pending.containsKey(id to lane))return null
        val ticket=BookActionTicket(id,ownerEpoch,++operation,lane)
        pending[id to lane]=ticket;publish(state.busy(lane,true).copy(actionMessage=null))
        return ticket
    }
    fun ownsAction(ticket:BookActionTicket)=ticket.ownerEpoch==ownerEpoch&&pending[ticket.bookId to ticket.lane]==ticket
    fun finishAction(ticket:BookActionTicket, invalidateComments:Boolean=false, transform:(BookDetailState)->BookDetailState):Boolean {
        if(!ownsAction(ticket))return false
        val value=transform(base(ticket.bookId)).busy(ticket.lane,false)
        require(value.bookId==ticket.bookId){"A book write cannot change its content identity"}
        pending.remove(ticket.bookId to ticket.lane)
        if(invalidateComments) {
            dirtyComments.add(ticket.bookId);commentEpochs[ticket.bookId]=(commentEpochs[ticket.bookId] ?: 0L)+1
        }
        if(ticket.lane==BookActionLane.Favorite)favoriteEpochs[ticket.bookId]=(favoriteEpochs[ticket.bookId] ?: 0L)+1
        if(state.bookId==ticket.bookId)publish(value) else remember(value)
        return true
    }
    fun restore(id:Long,authenticated:Boolean) {
        // Render A immediately, invalidate B's late reads, then refresh independently in the background.
        load(id,authenticated,retainComposer=true)
    }
    fun load(id:Long,authenticated:Boolean,retainComposer:Boolean=false,message:String?=null) {
        require(id>0)
        remember(state)
        val old=base(id)
        val serial=++revision
        val commentEpoch=commentEpochs[id] ?: 0L
        val favoriteEpoch=favoriteEpochs[id] ?: 0L
        request?.cancel()
        publish(old.copy(bookId=id,
            book=old.book.takeIf{it is LoadResult.Success} ?: LoadResult.Loading,
            chapters=old.chapters.takeIf{it is LoadResult.Success} ?: LoadResult.Loading,
            comments=old.comments.takeIf{(retainComposer||old.bookId==id)&&it is LoadResult.Success} ?: LoadResult.Loading,
            favoriteStatus=old.favoriteStatus.takeIf{it is LoadResult.Success} ?: LoadResult.Loading,
            managementPermissions=if(authenticated)LoadResult.Loading else LoadResult.Success(BookEditPermissions()),
            readerProgress=progress(id),actionMessage=message ?: old.actionMessage,
            actionLoading=pending.containsKey(id to BookActionLane.Comments),
            favoriteLoading=pending.containsKey(id to BookActionLane.Favorite),
            requestNewChapterLoading=pending.containsKey(id to BookActionLane.NewChapters)))
        request=work.launch {
            supervisorScope {
                val bookResult=async { fetch("书籍详情"){repository.book(id).also {require(it.id==id){"书籍详情身份与请求不一致"}}} }
                launch {
                    val result=bookResult.await()
                    if(serial==revision)publish(state.copy(book=if(result is LoadResult.Success)LoadResult.Success(result.value.copy(
                        fullCoverUrl=(state.book as? LoadResult.Success)?.value?.fullCoverUrl ?: result.value.fullCoverUrl))else result,
                        readerProgress=progress(id)))
                }
                launch {
                    val cover=try{repository.cover(id)}catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){null}
                    if(serial==revision&&!cover.isNullOrBlank()) {
                        val existing=(state.book as? LoadResult.Success)?.value
                        if(existing!=null)publish(state.copy(book=LoadResult.Success(existing.copy(fullCoverUrl=cover))))
                        else {
                            val resolved=(bookResult.await() as? LoadResult.Success)?.value
                            if(serial==revision&&resolved?.id==id)publish(state.copy(book=LoadResult.Success(resolved.copy(fullCoverUrl=cover))))
                        }
                    }
                }
                launch {val result=fetch("目录"){repository.chapters(id)};if(serial==revision)publish(state.copy(chapters=result))}
                launch {
                    val result=fetch("评论区"){repository.comments(id)}
                    if(serial==revision&&commentEpoch==(commentEpochs[id] ?: 0L)) {
                        publish(state.copy(comments=result,bookReferences=if(result is LoadResult.Success)emptyMap()else state.bookReferences))
                        if(result is LoadResult.Success){dirtyComments.remove(id);onComments(id,result.value)}
                    }
                }
                launch {val result=fetch("收藏状态"){repository.favorite(id)};if(serial==revision&&favoriteEpoch==(favoriteEpochs[id] ?: 0L))publish(state.copy(favoriteStatus=result))}
                if(authenticated)launch {val result=fetch("管理权限"){repository.permissions(id)};if(serial==revision)publish(state.copy(managementPermissions=result))}
            }
        }
    }
    private suspend fun <T> fetch(label:String,load:suspend()->T):LoadResult<T> = try {LoadResult.Success(load())}
        catch(cancelled:CancellationException){throw cancelled}catch(failure:Exception){LoadResult.Error(apiFailureMessage(label,failure))}
    fun environmentChanged(){ownerEpoch++;revision++;request?.cancel();pending.clear();retained.clear();composers.clear();dirtyComments.clear();commentEpochs.clear();favoriteEpochs.clear();visible=BookDetailState()}
    fun close(){environmentChanged();work.cancel()}
}
