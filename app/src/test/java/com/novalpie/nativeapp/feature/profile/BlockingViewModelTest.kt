package com.novalpie.nativeapp.feature.profile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.novalpie.nativeapp.model.LoadResult
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockingViewModelTest {
    @Test fun latePageCannotReplaceTheUsersFromTheLatestPage()=runTest {
        val first=CompletableDeferred<BlockedUsersPage>()
        val repository=object:BlockingRepository {
            override suspend fun list(page:Int)=if(page==1)withContext(NonCancellable){first.await()}else BlockedUsersPage(listOf(BlockedUser(2,"第二页")),2,21,2)
            override suspend fun status(id:Long,book:Boolean)=false
            override suspend fun set(id:Long,book:Boolean,blocked:Boolean){}
        }
        val model=BlockingViewModel(repository,backgroundScope)
        model.load(1);runCurrent();model.load(2);runCurrent()
        first.complete(BlockedUsersPage(listOf(BlockedUser(1,"过时第一页面")),1,21,2));runCurrent()
        assertEquals(2,model.state.page)
        assertEquals(2L,(model.state.users as LoadResult.Success).value.users.single().id)
    }

    @Test fun aFailedWriteMustRefreshTheStatusInsteadOfBlindlyRepeatingTheMutation()=runTest {
        var writes=0
        val repository=object:BlockingRepository {
            override suspend fun list(page:Int)=BlockedUsersPage(emptyList(),page,0,0)
            override suspend fun status(id:Long,book:Boolean)=false
            override suspend fun set(id:Long,book:Boolean,blocked:Boolean){writes++;error("response lost")}
        }
        val model=BlockTargetViewModel(repository,42,false,backgroundScope)
        model.refresh();runCurrent();model.toggle();runCurrent();model.toggle();runCurrent()
        assertEquals(1,writes)
        assertNull(model.state.blocked)
        assertNotNull(model.state.message)
        model.refresh();runCurrent()
        assertEquals(false,model.state.blocked)
    }
}
