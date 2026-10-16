package com.novalpie.nativeapp.feature.library

import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteDestinationViewModelTest {
    private class Repository : FavoriteDestinationRepository {
        val calls = mutableListOf<Pair<Long,Long>>()
        val names = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        var createGate: CompletableDeferred<Unit>? = null
        var groupGate: CompletableDeferred<Unit>? = null
        override suspend fun groups(): List<FavoriteGroup> { groupGate?.let { withContext(NonCancellable) { it.await() } }; return listOf(FavoriteGroup(17, "自选收藏夹")) }
        override suspend fun create(name: String): FavoriteGroup { names += name; createGate?.await(); return FavoriteGroup(23, name) }
        override suspend fun add(bookId: Long, groupId: Long): FavoriteStatus {
            calls += bookId to groupId; gate?.await(); if (fail) error("网络失败")
            return FavoriteStatus(true, groupId)
        }
    }
    @Test fun customDestinationIsUsedInsteadOfDefaultAndDuplicateTapsDoNotPostTwice() = runTest {
        val repo = Repository().apply { gate = CompletableDeferred() }
        val saved = mutableListOf<FavoriteStatus>()
        val vm = FavoriteDestinationViewModel(repo, backgroundScope, { _, status -> saved += status })
        vm.open(42); runCurrent(); vm.choose(17); vm.choose(0); runCurrent()
        assertEquals(listOf(42L to 17L), repo.calls); assertTrue(vm.state.busy)
        repo.gate!!.complete(Unit); runCurrent()
        assertEquals(17L, saved.single().groupId); assertFalse(vm.state.visible)
    }
    @Test fun newlyCreatedFolderCanBeUsedImmediatelyAndNotCreatedTwiceAfterAddFailure() = runTest {
        val repo = Repository().apply { fail = true }
        val vm = FavoriteDestinationViewModel(repo, backgroundScope, { _, _ -> })
        vm.open(42); runCurrent(); vm.startCreate(); vm.updateName("  新收藏夹  "); vm.createAndChoose(); runCurrent()
        assertEquals(listOf("新收藏夹"), repo.names); assertTrue(vm.state.visible); assertNotNull(vm.state.error)
        assertFalse(vm.state.creating); assertFalse(vm.state.busy)
        repo.fail = false; vm.choose(23); runCurrent()
        assertEquals(1, repo.names.size); assertEquals(listOf(42L to 23L,42L to 23L),repo.calls)
    }
    @Test fun cancelAndUnknownFolderNeverMutateFavorites() = runTest {
        val repo = Repository(); val vm = FavoriteDestinationViewModel(repo, backgroundScope, { _, _ -> })
        vm.open(42); runCurrent(); vm.choose(99); vm.dismiss(); vm.choose(0); runCurrent()
        assertTrue(repo.calls.isEmpty()); assertFalse(vm.state.visible)
    }
    @Test fun accountChangeDoesNotPublishAnOldFavoriteResponse() = runTest {
        val repo = Repository().apply { gate = CompletableDeferred() }
        val saved = mutableListOf<FavoriteStatus>()
        val vm = FavoriteDestinationViewModel(repo, backgroundScope, { _, status -> saved += status })
        vm.open(42); runCurrent(); vm.choose(17); runCurrent(); vm.invalidate()
        repo.gate!!.complete(Unit); runCurrent()
        assertTrue(saved.isEmpty()); assertFalse(vm.state.visible)
    }
    @Test fun aLateCreateResponseCannotLeakFoldersAcrossAccountsOrAddTheBook() = runTest {
        val repo = Repository().apply { createGate = CompletableDeferred() }
        val observed = mutableListOf<List<FavoriteGroup>>()
        val vm = FavoriteDestinationViewModel(repo, backgroundScope, { _, _ -> }, { observed += it })
        vm.open(42); runCurrent(); vm.startCreate(); vm.updateName("旧账号收藏夹"); vm.createAndChoose(); runCurrent()
        vm.invalidate(); repo.createGate!!.complete(Unit); runCurrent()
        assertTrue(repo.calls.isEmpty()); assertEquals(1, observed.size); assertFalse(vm.state.visible)
    }

    @Test fun aLateListDoesNotHideTheCreatedFolderAfterAnAddFailure() = runTest {
        val repo = Repository().apply { fail = true; groupGate = CompletableDeferred() }
        val vm = FavoriteDestinationViewModel(repo, backgroundScope, { _, _ -> })
        vm.open(42); runCurrent(); vm.startCreate(); vm.updateName("新夹"); vm.createAndChoose(); runCurrent()
        repo.groupGate!!.complete(Unit); runCurrent()
        assertNotNull(vm.state.error)
        assertTrue((vm.state.groups as LoadResult.Success).value.any { it.id == 23L })
    }

}
