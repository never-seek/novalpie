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
class RootStateRegressionTest {
    private class Sandbox(val scenario:(RecordedRequest)->MockResponse?) : AutoCloseable {
        val scheduler=StandardTestDispatcher()
        val app=ApplicationProvider.getApplicationContext<Application>()
        val store=ViewModelStore()
        val server=MockWebServer()
        val requests=ConcurrentLinkedQueue<String>()
        val model:NovalPieViewModel
        val api:NovalPieApi
        private val oldBase:Any?
        private val oldProxy:Any?
        init {
            Dispatchers.setMain(scheduler)
            AuthSessionStore(app).saveToken("synthetic-local-only")
            server.dispatcher=object:okhttp3.mockwebserver.Dispatcher(){override fun dispatch(r:RecordedRequest):MockResponse {
                requests.add(r.requestUrl!!.encodedPath)
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
    @Test fun cachedOtherAccountBodyMustNotSurvive403InRealRootLoader(){
        Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content")MockResponse().setResponseCode(403).setBody("{\"message\":\"synthetic account B denied\"}")else null}.use { s ->
            val cache=ReaderChapterCacheStore(s.app)
            try {
                assertTrue(cache.save(2,10,"india",true,null,ReaderContent("One","ACCOUNT_A_PRIVATE_SENTINEL","synthetic")))
                s.model.openReader(2,10)
                // Catalog now intentionally resolves before the body request. Waiting for it is
                // not evidence that the 403 has been handled; observe the actual rejected body.
                s.pumpUntil("chapter request rejected",{s.requests.contains("/api/chapters/10/content")&&s.model.readerState.content is LoadResult.Error})
                s.pump()
                val text=(s.model.readerState.content as? LoadResult.Success)?.value?.content.orEmpty()
                println("403 cache root result=${s.model.readerState.content} cache=${s.model.readerState.contentFromCache}")
                assertFalse("A cached body is displayed on a denied B request",text.contains("ACCOUNT_A_PRIVATE_SENTINEL"))
            }finally{cache.clearBook(2)}
        }
    }
    @Test fun forumInternalBookLinkMustReturnToOriginatingPost(){
        Sandbox{null}.use { s ->
            s.model.openForumPost(1)
            assertEquals(AppRoute.ForumPostDetail(1),s.model.currentRoute)
            s.model.openDeepLink("https://novalpie.cc/book/2")
            s.model.goBack()
            println("internal link back route=${s.model.currentRoute}")
            assertEquals(AppRoute.ForumPostDetail(1),s.model.currentRoute)
        }
    }
    @Test fun bookReactionExplicitRejectionMustNotSaySynchronized(){
        Sandbox{r->if(r.method=="POST"&&r.requestUrl!!.encodedPath.endsWith("/likes"))MockResponse().setBody("{\"success\":false}")else null}.use { s ->
            s.model.openBook(2);s.pumpUntil("book loaded",{s.model.bookDetailState.book is LoadResult.Success})
            s.book{it.copy(comments=LoadResult.Success(listOf(ChapterComment(9,bookId=2,content="fixture"))))}
            s.model.likeBookComment(ChapterComment(9,bookId=2,content="fixture"))
            s.pumpUntil("reaction complete",{s.requests.contains("/api/comments/9/likes")&&!s.model.bookDetailState.actionLoading})
            println("rejected reaction notice=${s.model.bookDetailState.actionMessage}")
            assertFalse(s.model.bookDetailState.actionMessage.orEmpty().contains("已同步"))
        }
    }
    @Test fun completedBookWriteOnChildRouteMustClearBusyWhenReturning(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try{Sandbox{r->if(r.method=="POST"&&r.requestUrl!!.encodedPath=="/api/comments"){
            started.countDown();release.await(4,TimeUnit.SECONDS);MockResponse().setBody("{\"success\":false,\"message\":\"fixture rejected\"}")
        }else null}.use { s ->
            s.model.openBook(2);s.pumpUntil("book",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("draft must remain");s.model.submitBookComment();s.scheduler.scheduler.runCurrent()
            assertTrue(started.await(3,TimeUnit.SECONDS))
            s.model.openUserProfile(77);release.countDown();s.pump()
            s.model.goBack();s.pump()
            println("retained book actionLoading=${s.model.bookDetailState.actionLoading} draft=${s.model.bookDetailState.commentDraft}")
            assertFalse(s.model.bookDetailState.actionLoading)
            assertEquals("draft must remain",s.model.bookDetailState.commentDraft)
        }}finally{release.countDown()}
    }
    @Test fun olderSameBookChapterEditorResponseMustNotOverwriteNewerDraft(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try{Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content"){
            started.countDown();release.await(4,TimeUnit.SECONDS);MockResponse().setBody("{\"title\":\"OLD\",\"content\":\"OLD_BODY\"}")
        }else null}.use { s ->
            s.nav(AppRoute.Home,AppRoute.BookChapters(2))
            s.state("bookChapterManagerState",BookChapterManagerState(bookId=2,chapters=LoadResult.Success(listOf(Chapter(10,"old",1)))))
            s.model.openManagedChapterEditor(Chapter(10,"old",1));s.scheduler.scheduler.runCurrent()
            assertTrue(started.await(3,TimeUnit.SECONDS))
            s.nav(AppRoute.Home,AppRoute.BookChapters(2))
            s.model.updateManagedChapterDraft(ManagedChapterDraft(chapterId=20,insertAt=2,title="NEW",content="NEW_BODY"))
            release.countDown();s.pumpUntil("old response settled",{!s.model.bookChapterManagerState.editorLoading})
            println("same-book late editor=${s.model.bookChapterManagerState.editor}")
            assertEquals(20L,s.model.bookChapterManagerState.editor?.chapterId)
            assertEquals("NEW_BODY",s.model.bookChapterManagerState.editor?.content)
        }}finally{release.countDown()}
    }
    @Test fun realFinalChapterAppendMustConfirmEndInsteadOfLeavingEndSentinelIdle(){
        Sandbox{null}.use { s ->
            s.nav(AppRoute.Home,AppRoute.Reader(2,20))
            s.state("readerState",ReaderState(bookId=2,chapterId=20,content=LoadResult.Success(ReaderContent("Two","body","synthetic")),
                chapterContents=listOf(ReaderChapterContent(20,"Two",ReaderContent("Two","body","synthetic"))),
                chapters=LoadResult.Success(listOf(Chapter(10,"One",1),Chapter(20,"Two",2)))))
            s.model.loadNextReaderChapter(20)
            println("last chapter end flags exhausted=${s.model.readerState.nextChapterExhausted} waiting=${s.model.readerState.nextChapterWaitingForCatalog} confirmation=${s.model.readerState.nextChapterEndConfirmationRequested}")
            assertTrue("Last-chapter guard returns before original end confirmation",s.model.readerState.nextChapterExhausted||s.model.readerState.nextChapterWaitingForCatalog||s.model.readerState.nextChapterEndConfirmationRequested)
        }
    }
    @Test fun ordinaryNativeBookNavigationKeepsForumOriginPositiveControl(){
        Sandbox{null}.use {s->
            s.model.openForumPost(1)
            s.model.openBook(2)
            s.model.goBack()
            assertEquals(AppRoute.ForumPostDetail(1),s.model.currentRoute)
        }
    }
    @Test fun sameAccountTemporaryNetworkFailureCanUseOwnCachePositiveControl(){
        Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content")MockResponse().setResponseCode(503).setBody("{\"message\":\"temporary\"}")else null}.use {s->
            val cache=ReaderChapterCacheStore(s.app)
            try {
                assertTrue(cache.save(2,10,"india",true,null,ReaderContent("One","OWN_OFFLINE_SENTINEL","synthetic")))
                s.model.openReader(2,10)
                s.pumpUntil("network/cache resolved",{s.requests.contains("/api/chapters/10/content")&&s.model.readerState.content is LoadResult.Success})
                assertTrue((s.model.readerState.content as LoadResult.Success).value.content.contains("OWN_OFFLINE_SENTINEL"))
                assertTrue(s.model.readerState.contentFromCache)
            }finally{cache.clearBook(2)}
        }
    }
    @Test fun rejectedBookWriteWithoutRouteChangeClearsBusyAndRetainsDraftPositiveControl(){
        Sandbox{r->if(r.method=="POST"&&r.requestUrl!!.encodedPath=="/api/comments")MockResponse().setBody("{\"success\":false,\"message\":\"fixture rejected\"}")else null}.use {s->
            s.model.openBook(2);s.pumpUntil("book",{s.model.bookDetailState.book is LoadResult.Success})
            s.model.updateBookCommentDraft("keep fixture draft")
            s.model.submitBookComment()
            s.pumpUntil("write rejected",{s.requests.contains("/api/comments")&&!s.model.bookDetailState.actionLoading})
            assertEquals("keep fixture draft",s.model.bookDetailState.commentDraft)
            assertTrue(s.model.bookDetailState.actionMessage.orEmpty().contains("fixture rejected"))
        }
    }
    @Test fun currentChapterEditorResponseAppliesNormallyPositiveControl(){
        Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content")MockResponse().setBody("{\"title\":\"CURRENT\",\"content\":\"CURRENT_BODY\"}")else null}.use {s->
            s.nav(AppRoute.Home,AppRoute.BookChapters(2))
            s.state("bookChapterManagerState",BookChapterManagerState(bookId=2))
            s.model.openManagedChapterEditor(Chapter(10,"current",1))
            s.pumpUntil("editor loaded",{s.requests.contains("/api/chapters/10/content")&&!s.model.bookChapterManagerState.editorLoading})
            assertEquals(10L,s.model.bookChapterManagerState.editor?.chapterId)
            assertEquals("CURRENT_BODY",s.model.bookChapterManagerState.editor?.content)
        }
    }
    @Test fun managedChapterRejectedSaveKeepsDraft(){
        Sandbox{r->if(r.method=="PATCH"&&r.requestUrl!!.encodedPath=="/api/users/me/chapters/10")
            MockResponse().setBody("{\"success\":false,\"message\":\"fixture rejected\"}") else null}.use{s->
            s.nav(AppRoute.Home,AppRoute.BookChapters(2))
            val draft=ManagedChapterDraft(chapterId=10,title="NEW",content="UNSAVED")
            s.state("bookChapterManagerState",BookChapterManagerState(bookId=2,editor=draft))
            s.model.saveManagedChapterDraft()
            s.pumpUntil("save rejected",{s.requests.contains("/api/users/me/chapters/10")&&!s.model.bookChapterManagerState.actionLoading})
            assertEquals(draft,s.model.bookChapterManagerState.editor)
            assertTrue(s.model.bookChapterManagerState.actionMessage.orEmpty().contains("fixture rejected"))
        }
    }
    @Test fun cancelDiscardKeepsDraftAndConfirmedDiscardClosesIt(){
        Sandbox{null}.use{s->
            s.nav(AppRoute.Home,AppRoute.BookChapters(2))
            val draft=ManagedChapterDraft(chapterId=10,title="NEW",content="UNSAVED")
            s.state("bookChapterManagerState",BookChapterManagerState(bookId=2,editor=draft))
            s.model.dismissManagedChapterEditor()
            assertTrue(s.model.bookChapterManagerState.confirmDiscardEditor)
            s.model.cancelDiscardManagedChapterEditor()
            assertEquals(draft,s.model.bookChapterManagerState.editor)
            assertFalse(s.model.bookChapterManagerState.confirmDiscardEditor)
            s.model.dismissManagedChapterEditor();s.model.confirmDiscardManagedChapterEditor()
            assertNull(s.model.bookChapterManagerState.editor)
            assertFalse(s.model.bookChapterManagerState.confirmDiscardEditor)
        }
    }
    @Test fun catalogMustNotWaitForSlowBodyWhenOfflineCacheIsAlreadyVisible(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try {Sandbox{r->if(r.requestUrl!!.encodedPath=="/api/chapters/10/content") {
            started.countDown();release.await(12,TimeUnit.SECONDS);MockResponse().setBody("{\"title\":\"One\",\"content\":\"FIRST_NEW\"}")
        }else null}.use{s->
            ReaderChapterCacheStore(s.app).save(2,10,"india",true,null,ReaderContent("One","CACHED_FIRST","fixture"))
            s.model.openReader(2,10)
            s.pumpUntil("cached body",{started.count==0L&&s.model.readerState.contentFromCache})
            s.pump()
            assertTrue("The fast independent catalog is still trapped behind a slow chapter body",s.model.readerState.chapters is LoadResult.Success)
            release.countDown()
        }}finally{release.countDown()}
    }
    @Test fun lateFirstBodyMustPreserveAppendedChapterVisibleIdentityAndCommentDraft(){
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        try {Sandbox{r->when(r.requestUrl!!.encodedPath) {
            "/api/chapters/10/content"->{started.countDown();release.await(12,TimeUnit.SECONDS);MockResponse().setBody("{\"title\":\"One\",\"content\":\"FIRST_NEW\"}")}
            "/api/chapters/20/content"->MockResponse().setBody("{\"title\":\"Two\",\"content\":\"SECOND_BODY\"}")
            else->null
        }}.use{s->
            ReaderChapterCacheStore(s.app).save(2,10,"india",true,null,ReaderContent("One","CACHED_FIRST","fixture"))
            s.model.openReader(2,10)
            s.pumpUntil("cached body",{started.count==0L&&s.model.readerState.contentFromCache})
            // Model a separately recovered catalog. This is an actual continuation/root load,
            // not a copy of its merge algorithm or a ReaderScreen-only state fixture.
            s.state("readerState",s.model.readerState.copy(chapters=LoadResult.Success(listOf(Chapter(10,"One",1),Chapter(20,"Two",2)))))
            s.model.loadNextReaderChapter(10)
            s.pumpUntil("append",{s.model.readerState.chapterContents.any{it.chapterId==20L}})
            s.model.recordVisibleReaderChapter(20,"Two");s.model.updateReaderCommentDraft(20,"KEEP_CHAPTER20_DRAFT")
            release.countDown()
            s.pumpUntil("first refresh finished",{(s.model.readerState.content as? LoadResult.Success)?.value?.content=="FIRST_NEW"})
            s.pump()
            assertEquals(listOf(10L,20L),s.model.readerState.chapterContents.map{it.chapterId})
            assertEquals(20L,s.model.readerState.visibleChapterId)
            assertEquals("KEEP_CHAPTER20_DRAFT",s.model.readerState.chapterCommentStates[20]?.draft)
            assertEquals(20L,ReaderProgressStore(s.app).load(2)?.chapterId)
        }}finally{release.countDown()}
    }
    @Test fun dirtyManagedChapterCancelMustNotSilentlyDiscardChanges(){
        Sandbox{null}.use {s->
            s.nav(AppRoute.Home,AppRoute.BookChapters(2))
            s.state("bookChapterManagerState",BookChapterManagerState(bookId=2,editor=ManagedChapterDraft(chapterId=10,title="OLD",content="OLD")))
            s.model.updateManagedChapterDraft(ManagedChapterDraft(chapterId=10,title="NEW",content="UNSAVED"))
            s.model.dismissManagedChapterEditor()
            assertNotNull("Dirty editor was discarded without confirmation",s.model.bookChapterManagerState.editor)
        }
    }
}
