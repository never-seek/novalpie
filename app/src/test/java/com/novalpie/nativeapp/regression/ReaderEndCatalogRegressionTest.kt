package com.novalpie.nativeapp.regression

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.core.*
import com.novalpie.nativeapp.data.*
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
import java.net.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ReaderEndCatalogRegressionTest {
    @Test fun failedCatalogRefreshWithNoRetainedDirectoryKeepsCatalogRetryReachable() {
        val dispatcher=StandardTestDispatcher();Dispatchers.setMain(dispatcher)
        val app=ApplicationProvider.getApplicationContext<Application>();val holder=ViewModelStore()
        MockWebServer().use{s->s.start();s.dispatcher=object:okhttp3.mockwebserver.Dispatcher(){override fun dispatch(r:RecordedRequest)=MockResponse().setResponseCode(503)}
            val vm=NovalPieViewModel(app);holder.put("root",vm)
            val api=vm.javaClass.getDeclaredField("api").apply{isAccessible=true}.get(vm) as NovalPieApi
            val base=api.javaClass.getDeclaredField("baseUrl").apply{isAccessible=true};val old=base.get(api)
            val proxy=api.javaClass.getDeclaredField("proxySelectorProvider").apply{isAccessible=true};val oldProxy=proxy.get(api)
            try {
                base.set(api,s.url("/").toString().trimEnd('/'));proxy.set(api,{FixedProxySelector(listOf(Proxy.NO_PROXY))})
                (vm.javaClass.getDeclaredField("navigator").apply{isAccessible=true}.get(vm) as AppNavigator)
                    .replaceAll(listOf(AppRoute.Home,AppRoute.Reader(2,10)))
                @Suppress("UNCHECKED_CAST")
                val state=vm.javaClass.getDeclaredField("readerState\$delegate").apply{isAccessible=true}.get(vm) as MutableState<ReaderState>
                state.value=ReaderState(bookId=2,chapterId=10,chapters=LoadResult.Error("offline"))
                vm.refreshReaderCatalog()
                val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8)
                while(System.nanoTime()<until){dispatcher.scheduler.runCurrent();if(!vm.readerState.catalogRefreshing)break;Thread.sleep(10)}
                assertTrue(vm.readerState.chapters is LoadResult.Error)
                assertNull("A next-chapter retry cannot repair an absent catalog; keep the actual catalog retry visible",vm.readerState.nextChapterError)
            }finally{holder.clear();dispatcher.scheduler.runCurrent();base.set(api,old);proxy.set(api,oldProxy);Dispatchers.resetMain()}
        }
    }
    @Test fun lastContinuousWindowConfirmsEndWithoutSecondManualRetryOrLosingCatalog() {
        val dispatcher=StandardTestDispatcher();Dispatchers.setMain(dispatcher)
        val app=ApplicationProvider.getApplicationContext<Application>();val holder=ViewModelStore()
        MockWebServer().use{s->s.start();s.dispatcher=object:okhttp3.mockwebserver.Dispatcher(){override fun dispatch(r:RecordedRequest):MockResponse {
            val body=if(r.requestUrl!!.encodedPath=="/api/v2/novels/2/chapters")
                """{"chapters":[{"id":10,"chapter_number":1,"title":"One"},{"id":20,"chapter_number":2,"title":"Two"},{"id":30,"chapter_number":3,"title":"End"}]}"""
                else """{"data":[]}"""
            return MockResponse().setBody(body).setBodyDelay(150,TimeUnit.MILLISECONDS)
        }}
        val vm=NovalPieViewModel(app);holder.put("root",vm)
        val apiField=vm.javaClass.getDeclaredField("api").apply{isAccessible=true};val api=apiField.get(vm) as NovalPieApi
        val base=api.javaClass.getDeclaredField("baseUrl").apply{isAccessible=true};val old=base.get(api)
        val proxy=api.javaClass.getDeclaredField("proxySelectorProvider").apply{isAccessible=true};val oldProxy=proxy.get(api)
        try {
            base.set(api,s.url("/").toString().trimEnd('/'));proxy.set(api,{FixedProxySelector(listOf(Proxy.NO_PROXY))})
            val nav=vm.javaClass.getDeclaredField("navigator").apply{isAccessible=true}.get(vm) as AppNavigator
            nav.replaceAll(listOf(AppRoute.Home,AppRoute.Reader(2,10)))
            val catalog=listOf(Chapter(10,"One",1),Chapter(20,"Two",2),Chapter(30,"End",3))
            val content=ReaderContent("End","Last paragraph","qa")
            val state=ReaderState(bookId=2,chapterId=10,visibleChapterId=30,content=LoadResult.Success(content),
                chapters=LoadResult.Success(catalog),chapterContents=listOf(ReaderChapterContent(30,"End",content)))
            @Suppress("UNCHECKED_CAST")
            val reader=vm.javaClass.getDeclaredField("readerState\$delegate").apply{isAccessible=true}.get(vm) as MutableState<ReaderState>
            reader.value=state;vm.loadNextReaderChapter(30)
            assertTrue("Refreshing end confirmation must retain the existing directory",vm.readerState.chapters is LoadResult.Success)
            val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8)
            while(System.nanoTime()<until) {dispatcher.scheduler.runCurrent();if(vm.readerState.nextChapterExhausted)break;Thread.sleep(10)}
            assertTrue("End detection must use the actual window edge, not the evicted entry chapter",vm.readerState.nextChapterExhausted)
            assertFalse(vm.readerState.nextChapterWaitingForCatalog)
        }finally{holder.clear();dispatcher.scheduler.runCurrent();base.set(api,old);proxy.set(api,oldProxy);Dispatchers.resetMain()}
        }
    }
}
