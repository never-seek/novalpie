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

/** Each source panel resolves independently under the same book identity and request generation. */
internal class BookDetailViewModel(
    private val repository:BookDetailRepository,
    scope:CoroutineScope?=null,
    private val progress:(Long)->ReaderProgress?={null},
    private val onComments:(Long,List<ChapterComment>)->Unit={_,_->},
):ViewModel() {
    private val work=scope ?: viewModelScope
    var state by mutableStateOf(BookDetailState())
        private set
    var revision=0L
        private set
    private var request:Job?=null
    fun present(transform:(BookDetailState)->BookDetailState){state=transform(state)}
    fun load(id:Long,authenticated:Boolean,retainComposer:Boolean=false,message:String?=null) {
        require(id>0)
        val serial=++revision
        request?.cancel()
        val old=state.takeIf{it.bookId==id}
        state=BookDetailState(bookId=id,book=LoadResult.Loading,chapters=LoadResult.Loading,
            comments=old?.comments?.takeIf{retainComposer&&it is LoadResult.Success} ?: LoadResult.Loading,
            bookReferences=if(retainComposer)old?.bookReferences.orEmpty()else emptyMap(),favoriteStatus=LoadResult.Loading,
            managementPermissions=if(authenticated)LoadResult.Loading else LoadResult.Success(BookEditPermissions()),readerProgress=progress(id),
            commentDraft=if(retainComposer)old?.commentDraft.orEmpty()else "",replyingToCommentId=if(retainComposer)old?.replyingToCommentId else null,
            replyingToName=if(retainComposer)old?.replyingToName else null,actionMessage=message)
        request=work.launch {
            supervisorScope {
                val bookResult=async { fetch("书籍详情"){repository.book(id)} }
                launch {
                    val result=bookResult.await()
                    if(serial==revision)state=state.copy(book=if(result is LoadResult.Success)LoadResult.Success(result.value.copy(
                        fullCoverUrl=(state.book as? LoadResult.Success)?.value?.fullCoverUrl ?: result.value.fullCoverUrl))else result,readerProgress=progress(id))
                }
                launch {
                    val cover=try{repository.cover(id)}catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){null}
                    if(serial==revision&&!cover.isNullOrBlank()) {
                        val existing=(state.book as? LoadResult.Success)?.value
                        if(existing!=null)state=state.copy(book=LoadResult.Success(existing.copy(fullCoverUrl=cover)))
                        else {
                            // Await only this sibling's book result; other panels keep publishing.
                            val resolved=(bookResult.await() as? LoadResult.Success)?.value
                            if(serial==revision&&resolved?.id==id)state=state.copy(book=LoadResult.Success(resolved.copy(fullCoverUrl=cover)))
                        }
                    }
                }
                launch {val result=fetch("目录"){repository.chapters(id)};if(serial==revision)state=state.copy(chapters=result)}
                launch {
                    val result=fetch("评论区"){repository.comments(id)}
                    if(serial==revision) {
                        state=state.copy(comments=result,bookReferences=if(result is LoadResult.Success)emptyMap()else state.bookReferences)
                        if(result is LoadResult.Success)onComments(id,result.value)
                    }
                }
                launch {val result=fetch("收藏状态"){repository.favorite(id)};if(serial==revision)state=state.copy(favoriteStatus=result)}
                if(authenticated)launch {val result=fetch("管理权限"){repository.permissions(id)};if(serial==revision)state=state.copy(managementPermissions=result)}
            }
        }
    }
    private suspend fun <T> fetch(label:String,load:suspend()->T):LoadResult<T> = try {LoadResult.Success(load())}
        catch(cancelled:CancellationException){throw cancelled}catch(failure:Exception){LoadResult.Error(apiFailureMessage(label,failure))}
    fun environmentChanged(){revision++;request?.cancel();state=BookDetailState()}
    fun close(){revision++;request?.cancel();work.cancel()}
}
