package com.novalpie.nativeapp.feature.messages

import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

internal open class FakeMessagesRepository : MessagesRepository {
    override suspend fun page(query: MessageQuery, page: Int, pageSize: Int) = MessagePage(emptyList(), MessagePagination())
    override suspend fun stats() = MessageStats()
    override suspend fun detail(id: Long) = SiteMessage(id, 1, "合成通知")
    override suspend fun markRead(ids: List<Long>) = MessageActionResult(true)
    override suspend fun markAllRead() = MessageActionResult(true)
    override suspend fun star(id: Long, starred: Boolean) = MessageActionResult(true)
    override suspend fun delete(ids: List<Long>) = MessageActionResult(true)
    override suspend fun conversation(targetId: Long, page: Int, pageSize: Int) = emptyList<DirectMessage>()
    override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) = MessageActionResult(true)
    override suspend fun settings() = MessageSettings()
    override suspend fun saveSettings(settings: MessageSettings) = MessageActionResult(true)
}

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {
    @Test fun settingsDraftSurvivesAFailedRefreshAndItsRetry() = runTest {
        var fail = false
        val model = MessageSettingsViewModel(object : FakeMessagesRepository() {
            override suspend fun settings(): MessageSettings { if (fail) throw IOException("offline"); return MessageSettings() }
        }, backgroundScope)
        model.load(); runCurrent(); model.edit { it.copy(autoReadAfterDays = 9) }
        fail = true; model.load(); runCurrent()
        assertEquals(9, model.state.draft.autoReadAfterDays)
        fail = false; model.load(); runCurrent()
        assertEquals(9, model.state.draft.autoReadAfterDays)
    }

    @Test fun slowStatsDoNotHoldTheInboxAndPagingUsesSubmittedNotDraftQuery() = runTest {
        val stats = CompletableDeferred<MessageStats>()
        val queries = mutableListOf<Pair<MessageQuery, Int>>()
        val repository = object : FakeMessagesRepository() {
            override suspend fun stats() = stats.await()
            override suspend fun page(query: MessageQuery, page: Int, pageSize: Int): MessagePage {
                queries += query to page
                return MessagePage(listOf(SiteMessage(page.toLong(), 1, "合成通知$page")), MessagePagination(page, pageSize, 2, 2))
            }
        }
        val model = MessageInboxViewModel(repository, backgroundScope)
        model.editKeyword("已提交"); model.submit(); runCurrent()
        assertTrue(model.state.messages is LoadResult.Success)
        assertTrue(model.state.stats is LoadResult.Loading)
        model.editKeyword("还未搜索的新输入"); model.loadMore(); runCurrent()
        assertEquals("已提交", queries.last().first.keyword)
        assertEquals(2, queries.last().second)
        assertEquals("还未搜索的新输入", model.state.query.keyword)
        assertEquals(2, (model.state.messages as LoadResult.Success).value.size)
        stats.complete(MessageStats(totalCount = 2)); runCurrent()
    }

    @Test fun filterChangeDropsOldResponsesAndRefreshPreservesLoadedPages() = runTest {
        val late = CompletableDeferred<MessagePage>()
        val repository = object : FakeMessagesRepository() {
            override suspend fun page(query: MessageQuery, page: Int, pageSize: Int): MessagePage =
                if (query.messageType == 1) withContext(NonCancellable) { late.await() }
                else MessagePage(listOf(SiteMessage(page.toLong(), 2, "新分类$page")), MessagePagination(page, pageSize, 2, 2))
        }
        val model = MessageInboxViewModel(repository, backgroundScope)
        model.filter { it.copy(messageType = 1) }; runCurrent()
        model.filter { it.copy(messageType = 2) }; runCurrent()
        late.complete(MessagePage(listOf(SiteMessage(50, 1, "旧分类")), MessagePagination())); runCurrent()
        assertEquals(2, (model.state.messages as LoadResult.Success).value.single().type)
        model.loadMore(); runCurrent(); model.refresh(); runCurrent()
        assertEquals(listOf(1L, 2L), (model.state.messages as LoadResult.Success).value.map { it.id })
    }

    @Test fun deleteCompletingOnAnotherDetailDoesNotReplaceThatDetailOrDuplicateWrite() = runTest {
        val reply = CompletableDeferred<MessageActionResult>()
        val deleted = mutableListOf<Long>()
        var calls = 0
        val model = MessageDetailViewModel(object : FakeMessagesRepository() {
            override suspend fun delete(ids: List<Long>): MessageActionResult { calls++; return reply.await() }
        }, backgroundScope, onDeleted = { deleted += it })
        model.load(1); runCurrent(); model.delete(); model.delete(); runCurrent()
        model.load(2); runCurrent(); reply.complete(MessageActionResult(true)); runCurrent()
        assertEquals(2L, model.state.messageId)
        assertEquals(2L, (model.state.detail as LoadResult.Success).value.id)
        assertEquals(listOf(1L), deleted)
        assertEquals(1, calls)
    }

    @Test fun environmentChangeClearsDraftsAndDiscardsAnUncancellableOldSend() = runTest {
        val reply = CompletableDeferred<MessageActionResult>()
        val model = ConversationViewModel(object : FakeMessagesRepository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) = withContext(NonCancellable) { reply.await() }
        }, backgroundScope)
        model.load(2, "旧账号会话"); runCurrent(); model.edit("旧账号草稿"); model.send(1, "旧本人"); runCurrent()
        model.environmentChanged(); model.load(2, "新账号会话"); runCurrent(); model.edit("新账号草稿")
        reply.complete(MessageActionResult(true)); runCurrent()
        assertEquals("新账号草稿", model.state.draft)
        assertFalse(model.state.sending)
    }

    @Test fun conversationCanReturnToDraftAndLoadOlderMessagesWithoutDuplicatingIds() = runTest {
        val model = ConversationViewModel(object : FakeMessagesRepository() {
            override suspend fun conversation(targetId: Long, page: Int, pageSize: Int) =
                if (page == 1) (100L..199L).map { DirectMessage(it, "合成文本") }
                else listOf(DirectMessage(99, "更早"), DirectMessage(100, "重叠更新"))
        }, backgroundScope)
        model.load(2, "甲"); runCurrent(); model.edit("保存草稿")
        model.load(3, "乙"); runCurrent(); model.load(2, "甲"); runCurrent()
        assertEquals("保存草稿", model.state.draft)
        model.loadMore(); runCurrent()
        assertEquals(101, (model.state.messages as LoadResult.Success).value.size)
        assertFalse(model.state.hasMore)
        assertEquals(99L, (model.state.messages as LoadResult.Success).value.first().id)
    }

    @Test fun refreshingConversationKeepsAnUnsentDraft() = runTest {
        val model = ConversationViewModel(FakeMessagesRepository(), backgroundScope)
        model.load(2, "测试甲"); runCurrent()
        model.edit("仅合成草稿，不发送到真实服务")
        model.load(2, "测试甲"); runCurrent()
        assertEquals("仅合成草稿，不发送到真实服务", model.state.draft)
    }

    @Test fun oldSendCannotNavigateBackOrEraseAnotherRecipientsDraft() = runTest {
        val reply = CompletableDeferred<MessageActionResult>()
        val repository = object : FakeMessagesRepository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) = reply.await()
        }
        val model = ConversationViewModel(repository, backgroundScope)
        model.load(2, "测试甲"); runCurrent(); model.edit("第一条"); model.send(1, "测试本人"); runCurrent()
        model.load(3, "测试乙"); runCurrent(); model.edit("另一会话的草稿")
        reply.complete(MessageActionResult(true)); runCurrent()
        assertEquals(3L, model.state.targetUserId)
        assertEquals("另一会话的草稿", model.state.draft)
    }

    @Test fun successfulSendDoesNotEraseTextTypedWhileSending() = runTest {
        val reply = CompletableDeferred<MessageActionResult>()
        val model = ConversationViewModel(object : FakeMessagesRepository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) = reply.await()
        }, backgroundScope)
        model.load(2, "测试甲"); runCurrent(); model.edit("第一条"); model.send(1, "本人"); runCurrent()
        model.edit("接着写的第二条")
        reply.complete(MessageActionResult(true)); runCurrent()
        assertEquals("接着写的第二条", model.state.draft)
        assertFalse(model.state.sending)
    }

    @Test fun unsuccessfulAcknowledgementIsNotSentAndNeverAutomaticallyRetried() = runTest {
        var sends = 0
        val model = ConversationViewModel(object : FakeMessagesRepository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String): MessageActionResult {
                sends++; return MessageActionResult(false, "测试拒绝")
            }
        }, backgroundScope)
        model.load(2, null); runCurrent(); model.edit("保留草稿"); model.send(1, "本人"); runCurrent()
        assertEquals("保留草稿", model.state.draft)
        assertTrue(model.state.actionMessage.orEmpty().contains("测试拒绝"))
        assertEquals(1, sends)
    }

    @Test fun transportFailureRetainsDraftAndDoesNotReplay() = runTest {
        var sends = 0
        val model = ConversationViewModel(object : FakeMessagesRepository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String): MessageActionResult {
                sends++; throw IOException("synthetic connection loss")
            }
        }, backgroundScope)
        model.load(2, null); runCurrent(); model.edit("待确认草稿"); model.send(1, "本人"); runCurrent()
        assertEquals("待确认草稿", model.state.draft)
        assertEquals(1, sends)
        assertFalse(model.state.sending)
    }

    @Test fun settingsSaveCapturesTheClickSnapshotNotLaterUiEdits() = runTest {
        val saved = mutableListOf<MessageSettings>()
        val reply = CompletableDeferred<MessageActionResult>()
        val model = MessageSettingsViewModel(object : FakeMessagesRepository() {
            override suspend fun saveSettings(settings: MessageSettings): MessageActionResult { saved += settings; return reply.await() }
        }, backgroundScope)
        model.load(); runCurrent(); model.edit { it.copy(enableEmail = true) }
        model.save()
        model.edit { it.copy(enableEmail = false, autoReadAfterDays = 3) }
        runCurrent(); reply.complete(MessageActionResult(true)); runCurrent()
        assertTrue(saved.single().enableEmail)
        val acknowledged = (model.state.settings as LoadResult.Success).value
        assertTrue(acknowledged.enableEmail)
        assertNull(acknowledged.autoReadAfterDays)
        assertEquals(3, model.state.draft.autoReadAfterDays)
        assertFalse(model.state.draft.enableEmail)
    }
}
