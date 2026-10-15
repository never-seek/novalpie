package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterRetranslationTest {
    private val catalog=listOf(Chapter(901,"EP1",number=1),Chapter(903,"EP3",number=3),Chapter(904,"EP4",number=4))
    @Test fun rangeUsesActualNumbersAndRejectsLooseJavascriptParsing() {
        assertEquals(listOf(1,3,4),retranslationChapterNumbers("",listOf(4,1,3)))
        assertEquals(listOf(1,3,4),retranslationChapterNumbers("3-4, 1, 3",listOf(1,3,4)))
        for(bad in listOf("1abc","2","0","4-3","1,","1-2147483647","99999999999999"))
            assertTrue(bad,runCatching{retranslationChapterNumbers(bad,listOf(1,3,4))}.isFailure)
    }
    private open class Repo:ChapterRetranslationRepository {
        var calls=0;var numbers:List<Int> = emptyList()
        override suspend fun chapters(bookId:Long)=emptyList<Chapter>()
        override suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult {
            calls++;this.numbers=numbers;return ForumActionResult(true)
        }
    }
    @Test fun repeatedSubmitWritesOnlyOnceAndPrefillsCurrentOrdinal()=runTest {
        val repo=Repo();val c=ChapterRetranslationController(repo,backgroundScope){0}
        c.open(1,"Book",catalog,3);c.submit();c.submit();runCurrent()
        assertEquals(listOf(3),repo.numbers);assertEquals(1,repo.calls)
        assertTrue(c.state.confirmed);assertFalse(c.state.submitting)
    }
    @Test fun serverRejectedPersonalModelIsShownWithoutPretendingItWasQueued()=runTest {
        val repo=object:Repo(){override suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult {
            calls++;return ForumActionResult(false,"本人 API 暂不可用")
        }}
        val c=ChapterRetranslationController(repo,backgroundScope){0};c.open(1,"Book",catalog);c.submit();runCurrent()
        assertEquals(1,repo.calls);assertFalse(c.state.confirmed);assertTrue(c.state.message!!.contains("API"))
    }
    @Test fun transportFailureIsUncertainAndDoesNotBlindlyReplayAfterReopen()=runTest {
        val repo=object:Repo(){override suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult {calls++;throw IOException("lost ack")}}
        val c=ChapterRetranslationController(repo,backgroundScope){0};c.open(1,"A",catalog);c.submit();runCurrent()
        assertTrue(c.state.uncertain);c.dismiss();c.open(1,"A",catalog);c.submit();runCurrent()
        assertEquals(1,repo.calls);assertTrue(c.state.uncertain)
    }
    @Test fun switchingBookDoesNotTakeLateAcknowledgementOrLoseItsOwnRange()=runTest {
        val response=CompletableDeferred<ForumActionResult>()
        val repo=object:Repo(){override suspend fun submit(bookId:Long,numbers:List<Int>)=response.await()}
        val c=ChapterRetranslationController(repo,backgroundScope){0};c.open(1,"A",catalog);c.submit();runCurrent()
        c.open(2,"B",catalog,4);response.complete(ForumActionResult(true));runCurrent()
        assertEquals(2L,c.state.bookId);assertEquals("4",c.state.range);assertFalse(c.state.confirmed);assertFalse(c.state.submitting)
    }
    @Test fun changedAccountBeforeQueuedWriteNeverPostsForOldAccount()=runTest {
        var revision=0L;val repo=Repo()
        val c=ChapterRetranslationController(repo,backgroundScope){revision}
        c.open(1,"A",catalog);c.submit();revision++;c.environmentChanged();runCurrent()
        assertEquals(0,repo.calls);assertFalse(c.state.open)
    }
    @Test fun reopeningDuringSubmissionDoesNotLeaveAPermanentlyBusyDialog()=runTest {
        val response=CompletableDeferred<ForumActionResult>()
        val repo=object:Repo(){override suspend fun submit(bookId:Long,numbers:List<Int>)=response.await()}
        val c=ChapterRetranslationController(repo,backgroundScope){0};c.open(1,"A",catalog);c.submit();runCurrent()
        c.dismiss();c.open(1,"A",catalog);assertTrue(c.state.submitting)
        response.complete(ForumActionResult(true));runCurrent()
        assertFalse(c.state.submitting);assertTrue(c.state.confirmed)
        c.dismiss();c.open(1,"A",catalog);assertTrue(c.state.confirmed)
    }
    @Test fun lateCatalogPrefillsCurrentChapterInsteadOfAccidentallySelectingWholeBook()=runTest {
        val reply=CompletableDeferred<List<Chapter>>()
        val repo=object:Repo(){override suspend fun chapters(bookId:Long)=reply.await()}
        val c=ChapterRetranslationController(repo,backgroundScope){0}
        c.open(1,"A",null,currentChapterId=903);c.submit();runCurrent();assertEquals(0,repo.calls)
        reply.complete(catalog);runCurrent()
        assertEquals("3",c.state.range);assertEquals(listOf(3),c.state.selection.getOrThrow())
        c.open(1,"A",catalog,currentChapterId=999);runCurrent()
        assertTrue(c.state.selection.isFailure);c.submit();runCurrent();assertEquals(0,repo.calls)
    }
    @Test fun transportRevisionCannotEraseAnInFlightPaidRequestOrItsAcknowledgement()=runTest {
        var revision=0L;val answer=CompletableDeferred<ForumActionResult>()
        val repo=object:Repo(){override suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult {
            calls++;return answer.await()
        }}
        val c=ChapterRetranslationController(repo,backgroundScope){revision};c.open(1,"A",catalog);c.submit();runCurrent()
        revision++
        c.transportChanged()
        // The controller must preserve account-owned writes across a proxy change.
        c.open(1,"A",catalog);c.submit();runCurrent()
        assertEquals(1,repo.calls)
        answer.complete(ForumActionResult(true));runCurrent()
        assertTrue("A same-account late acknowledgement remains authoritative",c.state.confirmed)
        assertFalse(c.state.submitting)
    }
    @Test fun transportRevisionKeepsUnknownWriteResultUntilExplicitUserConfirmation()=runTest {
        var revision=0L;val answer=CompletableDeferred<ForumActionResult>()
        val repo=object:Repo(){override suspend fun submit(bookId:Long,numbers:List<Int>):ForumActionResult {
            calls++;return answer.await()
        }}
        val c=ChapterRetranslationController(repo,backgroundScope){revision};c.open(1,"A",catalog);c.submit();runCurrent()
        revision++;c.transportChanged();answer.completeExceptionally(IOException("response lost after proxy change"));runCurrent()
        c.open(1,"A",catalog);c.submit();runCurrent()
        assertTrue(c.state.uncertain);assertEquals(1,repo.calls)
    }
}
