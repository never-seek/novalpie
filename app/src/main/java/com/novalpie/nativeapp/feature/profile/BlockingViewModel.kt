package com.novalpie.nativeapp.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.LoadResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal interface BlockingRepository {
    suspend fun list(page:Int):BlockedUsersPage
    suspend fun status(id:Long,book:Boolean):Boolean
    suspend fun set(id:Long,book:Boolean,blocked:Boolean)
}

internal class WebsiteBlockingRepository(private val api:NovalPieApi):BlockingRepository {
    override suspend fun list(page:Int)=api.blockedUsers(page)
    override suspend fun status(id:Long,book:Boolean)=if(book)api.bookBlocked(id)else api.userBlocked(id)
    override suspend fun set(id:Long,book:Boolean,blocked:Boolean){if(book)api.setBookBlocked(id,blocked)else api.setUserBlocked(id,blocked)}
}

internal data class BlockingUiState(
    val page:Int=1,
    val users:LoadResult<BlockedUsersPage> = LoadResult.Idle,
    val busyId:Long?=null,
    val message:String?=null,
)

/** List actions own their identity, error and page; leaving a row cannot apply its late response elsewhere. */
internal class BlockingViewModel(private val repository:BlockingRepository,scope:CoroutineScope?=null):ViewModel() {
    private val work=scope ?: viewModelScope
    var state by mutableStateOf(BlockingUiState())
        private set
    private var generation=0L
    private var loading:Job?=null
    fun load(page:Int=state.page) {
        if(state.busyId!=null)return
        val id=++generation
        loading?.cancel()
        state=state.copy(page=page.coerceAtLeast(1),users=LoadResult.Loading,message=null)
        val target=state.page
        loading=work.launch {
            try{val result=repository.list(target);if(id==generation)state=state.copy(users=LoadResult.Success(result))}
            catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(id==generation)state=state.copy(users=LoadResult.Error(failure.message ?: "屏蔽列表加载失败"))}
        }
    }
    fun unblock(userId:Long) {
        val page=(state.users as? LoadResult.Success)?.value ?: return
        if(state.busyId!=null||page.users.none{it.id==userId})return
        val id=generation
        state=state.copy(busyId=userId,message=null)
        work.launch {
            try {
                repository.set(userId,false,false)
                if(id==generation){state=state.copy(busyId=null);load(if(page.users.size==1&&state.page>1)state.page-1 else state.page)}
            } catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(id==generation)state=state.copy(message=failure.message ?: "解除失败，请刷新状态后重试")}
            finally{if(id==generation)state=state.copy(busyId=null)}
        }
    }
    fun dispose(){generation++;loading?.cancel();work.cancel()}
}

internal data class BlockTargetState(val blocked:Boolean?=null,val busy:Boolean=false,val message:String?=null)
internal class BlockTargetViewModel(private val repository:BlockingRepository,private val id:Long,private val book:Boolean,scope:CoroutineScope?=null):ViewModel() {
    private val work=scope ?: viewModelScope
    var state by mutableStateOf(BlockTargetState())
        private set
    private var generation=0L
    fun refresh() {
        if(state.busy)return
        val serial=++generation
        work.launch {
            try{val value=repository.status(id,book);if(serial==generation)state=BlockTargetState(blocked=value)}
            catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(serial==generation)state=BlockTargetState(message=failure.message ?: "无法获取屏蔽状态")}
        }
    }
    fun toggle() {
        val before=state.blocked ?: return
        if(state.busy)return
        val serial=++generation
        state=state.copy(busy=true,message=null)
        work.launch {
            try{repository.set(id,book,!before);if(serial==generation)state=BlockTargetState(blocked=!before)}
            catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(serial==generation)state=BlockTargetState(message="操作结果未确认，请刷新状态后再操作")}
        }
    }
    fun dispose(){generation++;work.cancel()}
}
