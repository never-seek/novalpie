package com.novalpie.nativeapp.regression

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.core.*
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.books.BookDetailViewModel
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class RootBookIsolationRegressionTest {
    private class Sandbox(val scenario:(RecordedRequest)->MockResponse?) : AutoCloseable {
        val scheduler=StandardTestDispatcher()
        val app=ApplicationProvider.getApplicationContext<Application>()
        val store=ViewModelStore()
        val server=MockWebServer()
        val requests=ConcurrentLinkedQueue<String>()
        val writes=ConcurrentLinkedQueue<String>()
        val model:NovalPieViewModel
        val api:NovalPieApi
        private val oldBase:Any?
        private val oldProxy:Any?
        init {
            Dispatchers.setMain(scheduler)
            AuthSessionStore(app).saveToken("synthetic-local-only")
            server.dispatcher=object:okhttp3.mockwebserver.Dispatcher(){override fun dispatch(r:RecordedRequest):MockResponse {
                requests.add(r.requestUrl!!.encodedPath)
                if(r.method=="POST")writes.add(r.requestUrl!!.encodedPath)
                scenario(r)?.let{return it}
                val p=r.requestUrl!!.encodedPath
                val text=when {
                    p=="/api/reader/session-key"->"""{"session_id":"fixture","session_key":"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="}"""
                    p.endsWith("/users/me")||p=="/api/users/77"->"""{"id":2,"username":"Synthetic B","role":"user","auto_checkin":false}"""
                    p.matches(Regex("/api/novels/\\d+/detail"))->"""{"id":${p.split('/')[3]},"title":"Synthetic book","author":"QA"}"""
                    p.matches(Regex("/api/v2/novels/\\d+/chapters"))->"""{"chapters":[{"id":10,"title":"One","chapter_number":1},{"id":20,"title":"Two","chapter_number":2}]}"""
                    p.endsWith("/photo")->"{}"
                    p=="/api/favorites/status"->"""{"is_favorited":false}"""
                    p.endsWith("permissions/check")->"""{"permissions":{"title":true}}"""
                    else->"""{"data":[],"pagination":{"page":1,"total":0,"last_page":1}}"""
                }
                return MockResponse().setHeader("content-type","application/json").setBody(text)
            }}
            server.start()
            model=NovalPieViewModel(app);store.put("root",model)
            api=field("api") as NovalPieApi
            val base=api.javaClass.getDeclaredField("baseUrl").apply{isAccessible=true}
            val proxy=api.javaClass.getDeclaredField("proxySelectorProvider").apply{isAccessible=true}
            oldBase=base.get(api);oldProxy=proxy.get(api)
            base.set(api,server.url("/").toString().trimEnd('/'))
            val direct:()->ProxySelector?={FixedProxySelector(listOf(Proxy.NO_PROXY))}
            proxy.set(api,direct)
        }
        fun field(name:String):Any?=model.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(model)
        @Suppress("UNCHECKED_CAST") fun <T> state(name:String,value:T){(field(name+"\$delegate") as MutableState<T>).value=value}
        fun nav(vararg r:AppRoute){(field("navigator") as AppNavigator).replaceAll(r.toList())}
        fun book(transform:(BookDetailState)->BookDetailState){(field("bookFeature") as BookDetailViewModel).present(transform)}
        fun pumpUntil(label:String,condition:()->Boolean){
            val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(7)
            while(System.nanoTime()<until){scheduler.scheduler.runCurrent();if(condition())return;Thread.sleep(10)}
            throw AssertionError("Fixture did not settle: $label; requests=$requests")
        }
        fun pump(){repeat(20){scheduler.scheduler.runCurrent();Thread.sleep(10)}}
        override fun close(){
            store.clear();scheduler.scheduler.runCurrent();server.shutdown()
            api.javaClass.getDeclaredField("baseUrl").apply{isAccessible=true}.set(api,oldBase)
            api.javaClass.getDeclaredField("proxySelectorProvider").apply{isAccessible=true}.set(api,oldProxy)
            AuthSessionStore(app).clearToken()
            AppContainer.from(app).refreshEnvironmentFromStores()
            Dispatchers.resetMain()
        }
    }

    @Test fun returningFromBookBMustRestoreBookAContentIdentity(){
        Sandbox{null}.use { s ->
            s.model.openBook(2)
            s.pumpUntil("book A",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("A_DRAFT_MUST_NOT_DISAPPEAR")
            s.model.openBook(3)
            s.pumpUntil("book B",{s.model.bookDetailState.bookId==3L&&s.model.bookDetailState.book is LoadResult.Success})
            assertTrue(s.model.goBack());s.pump()
            assertEquals(AppRoute.BookDetail(2),s.model.currentRoute)
            assertEquals("Route restored A but shared detail still displays B",2L,s.model.bookDetailState.bookId)
            assertEquals("A draft must stay with A", "A_DRAFT_MUST_NOT_DISAPPEAR",s.model.bookDetailState.commentDraft)
        }
    }
    @Test fun earlierSuccessfulCommentMustNotEraseNewlyEditedDraft(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try { Sandbox{r->if(r.method=="POST"&&r.requestUrl!!.encodedPath=="/api/comments"){
            started.countDown();release.await(5,TimeUnit.SECONDS);MockResponse().setBody("{\"success\":true,\"id\":99,\"message\":\"posted\"}")
        }else null}.use { s ->
            s.model.openBook(2);s.pumpUntil("book loaded",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("ORIGINAL_SUBMITTED_DRAFT");s.model.submitBookComment();s.scheduler.scheduler.runCurrent()
            assertTrue(started.await(3,TimeUnit.SECONDS))
            s.model.updateBookCommentDraft("NEW_UNSUBMITTED_DRAFT")
            release.countDown();s.pumpUntil("comment finished",{!s.model.bookDetailState.actionLoading});s.pump()
            assertEquals("Old success must not erase text entered after request began", "NEW_UNSUBMITTED_DRAFT",s.model.bookDetailState.commentDraft)
        }}finally{release.countDown()}
    }

    @Test fun denialCheckpointWriteFailureMustStillLeaveReaderInExplicitDeniedState(){
        val failures=CopyOnWriteArrayList<Throwable>()
        val prior=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> failures.add(error) }
        try { Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content")
            MockResponse().setResponseCode(403).setBody("{\"message\":\"denied fixture\"}")else null}.use { s ->
            val cache=ReaderChapterCacheStore(s.app)
            assertTrue(cache.save(2,10,"india",true,null,ReaderContent("One","DENIED_FAULT_SENTINEL","synthetic")))
            val scope=readerCacheAccountScope(AuthSessionStore(s.app).loadToken())
            val folder=java.io.File(s.app.filesDir,"novalpie_reader_chapter_cache/accounts/$scope")
            val cached=folder.listFiles().orEmpty().firstOrNull{it.name.startsWith("2-10-")&&it.extension=="json"}
                ?: error("Expected synthetic cache file")
            val marker=java.io.File(folder,cached.name+".denied")
            assertTrue(marker.mkdir()) // Only this synthetic marker; models an unwritable checkpoint.
            try {
                s.model.openReader(2,10)
                s.pumpUntil("denied content or unhandled failure",{s.model.readerState.content is LoadResult.Error||failures.isNotEmpty()})
                assertTrue("Denial persistence failure must not escape uncaught: $failures",failures.isEmpty())
                assertTrue("Reader must show denial, not retain cached protected body",s.model.readerState.content is LoadResult.Error)
            } finally { marker.delete();cache.clearBook(2) }
        }} finally { Thread.setDefaultUncaughtExceptionHandler(prior) }
    }

    @Test fun inFlightAResultAfterNavigatingToBClearsOnlyAAndIsNotResentOnBack(){
        val started=CountDownLatch(1);val release=CountDownLatch(1);val writes=AtomicInteger()
        try {Sandbox{r->if(r.method=="POST"&&r.requestUrl!!.encodedPath=="/api/comments"){
            writes.incrementAndGet();started.countDown();release.await(10,TimeUnit.SECONDS)
            MockResponse().setBody("{\"success\":true,\"message\":\"posted A\"}")
        }else null}.use { s ->
            s.model.openBook(2);s.pumpUntil("A",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("submitted A");s.model.submitBookComment();s.scheduler.scheduler.runCurrent();assertTrue(started.await(3,TimeUnit.SECONDS))
            s.model.openBook(3);s.pumpUntil("B",{s.model.bookDetailState.bookId==3L&&s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("B unsent");release.countDown()
            val feature=s.field("bookFeature") as BookDetailViewModel
            s.pumpUntil("A write finished while B visible",{feature.retainedState(2)?.actionLoading==false})
            assertEquals(3L,s.model.bookDetailState.bookId);assertEquals("B unsent",s.model.bookDetailState.commentDraft)
            assertTrue(s.model.goBack());s.pump();assertEquals(2L,s.model.bookDetailState.bookId)
            assertFalse(s.model.bookDetailState.actionLoading);assertEquals("",s.model.bookDetailState.commentDraft);assertEquals(1,writes.get())
        }}finally{release.countDown()}
    }
    @Test fun lateACallbacksCannotEditOrSubmitB(){
        Sandbox{null}.use {s->
            s.model.openBook(2);s.pumpUntil("A",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCatalogQuery("A query",2);s.model.updateBookCommentDraft("A draft",2)
            s.model.openBook(3);s.pumpUntil("B",{s.model.bookDetailState.bookId==3L&&s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("B draft",3)
            s.model.updateBookCommentDraft("stale A input",2);s.model.updateBookCatalogQuery("stale A query",2)
            s.model.replyToBookComment(ChapterComment(9,bookId=2,authorName="A author",content="A"),2)
            s.model.submitBookComment(2);s.model.toggleBookDetailFavorite(2);s.pump()
            assertEquals("B draft",s.model.bookDetailState.commentDraft);assertEquals("",s.model.bookCatalogQuery)
            assertFalse(s.writes.contains("/api/comments"));assertFalse(s.writes.contains("/api/favorites"))
            s.model.goBack();s.pump();assertEquals("A query",s.model.bookCatalogQuery);assertEquals("A draft",s.model.bookDetailState.commentDraft)
        }
    }
    @Test fun changingReplyTargetDuringAWriteDoesNotLoseTheNewTarget(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try {Sandbox{r->if(r.method=="POST"&&r.requestUrl!!.encodedPath=="/api/comments"){
            started.countDown();release.await(10,TimeUnit.SECONDS);MockResponse().setBody("{\"success\":true}")
        }else null}.use {s->
            s.model.openBook(2);s.pumpUntil("A",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("original");s.model.submitBookComment();s.scheduler.scheduler.runCurrent();assertTrue(started.await(3,TimeUnit.SECONDS))
            // Reply controls remain available while the text field itself is disabled.
            s.model.replyToBookComment(ChapterComment(19,bookId=2,authorName="new target",content="reply"))
            release.countDown();s.pumpUntil("finished",{!s.model.bookDetailState.actionLoading})
            assertEquals(19L,s.model.bookDetailState.replyingToCommentId);assertEquals("new target",s.model.bookDetailState.replyingToName)
        }}finally{release.countDown()}
    }
    @Test fun lateDeniedOpeningBodyDoesNotDiscardAnAuthorizedSuccessorOrItsDraft(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try {Sandbox{r->when(r.requestUrl!!.encodedPath){
            "/api/chapters/10/content"->{started.countDown();release.await(12,TimeUnit.SECONDS);MockResponse().setResponseCode(403).setBody("{\"message\":\"denied opening chapter\"}")}
            "/api/chapters/20/content"->MockResponse().setBody("{\"title\":\"Allowed 20\",\"content\":\"AUTHORIZED_SUCCESSOR_BODY\"}")
            else->null
        }}.use {s->
            val cache=ReaderChapterCacheStore(s.app)
            try {
                cache.save(2,10,"india",true,null,ReaderContent("One","OPTIMISTIC_10","fixture"))
                s.model.openReader(2,10)
                s.pumpUntil("cached opening and directory",{s.model.readerState.contentFromCache&&s.model.readerState.chapters is LoadResult.Success})
                assertTrue(started.await(3,TimeUnit.SECONDS));s.model.loadNextReaderChapter(10)
                s.pumpUntil("real successor GET",{s.model.readerState.chapterContents.any{it.chapterId==20L}})
                s.model.recordVisibleReaderChapter(20,"Allowed 20");s.model.updateReaderCommentDraft(20,"successor draft")
                release.countDown();s.pumpUntil("opening denied",{cache.load(2,10,"india",true)==null});s.pump()
                assertEquals(listOf(20L),s.model.readerState.chapterContents.map{it.chapterId})
                assertTrue((s.model.readerState.content as LoadResult.Success).value.content.contains("AUTHORIZED_SUCCESSOR_BODY"))
                assertEquals(20L,s.model.readerState.visibleChapterId);assertEquals("successor draft",s.model.readerState.chapterCommentStates[20]?.draft)
            }finally{cache.clearBook(2)}
        }}finally{release.countDown()}
    }
    @Test fun older403CannotRevokeAYoungerAuthorizedReadOfTheSameChapter(){
        val started=CountDownLatch(1);val release=CountDownLatch(1);val calls=AtomicInteger()
        try {Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content"){
            if(calls.incrementAndGet()==1){started.countDown();release.await(12,TimeUnit.SECONDS);MockResponse().setResponseCode(403).setBody("{\"message\":\"old denial\"}")}
            else MockResponse().setBody("{\"title\":\"Fresh\",\"content\":\"YOUNGER_AUTHORIZED_BODY\"}")
        }else null}.use {s->
            val cache=ReaderChapterCacheStore(s.app)
            try {
                s.model.openReader(2,10);s.scheduler.scheduler.runCurrent();assertTrue(started.await(3,TimeUnit.SECONDS))
                s.model.loadReader(2,10)
                s.pumpUntil("fresh read cached",{cache.load(2,10,"india",true)?.content?.content?.contains("YOUNGER_AUTHORIZED_BODY")==true})
                release.countDown();s.pump()
                assertTrue(cache.load(2,10,"india",true)?.content?.content?.contains("YOUNGER_AUTHORIZED_BODY")==true)
                assertTrue((s.model.readerState.content as LoadResult.Success).value.content.contains("YOUNGER_AUTHORIZED_BODY"))
            }finally{cache.clearBook(2)}
        }}finally{release.countDown()}
    }
}
