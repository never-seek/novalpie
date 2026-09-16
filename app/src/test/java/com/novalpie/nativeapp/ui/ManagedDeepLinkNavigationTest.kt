package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.AuthSessionStore
import com.novalpie.nativeapp.data.FixedProxySelector
import com.novalpie.nativeapp.data.NovalPieApi
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ManagedDeepLinkNavigationTest {
    @Test fun obsoleteEditorEntryCannotPullTheUserBackAfterAToBToA() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath
                val body = when {
                    path.endsWith("/permissions/check") -> {
                        started.countDown(); release.await(10, TimeUnit.SECONDS)
                        """{"permissions":{"title":true,"author_name":true}}"""
                    }
                    path.matches(Regex("/api/novels/[12]/detail")) -> """{"id":${path.split('/')[3]},"title":"book","author":"author"}"""
                    else -> """{"data":[],"pagination":{"total":0,"page":1,"last_page":1}}"""
                }
                return MockResponse().setHeader("content-type", "application/json").setBody(body)
            }
        }
        server.start()
        val store = ViewModelStore()
        var api: NovalPieApi? = null
        var oldBase: Any? = null
        var oldProxy: Any? = null
        val auth = AuthSessionStore(ApplicationProvider.getApplicationContext<Application>())
        val previousToken = auth.loadToken()
        try {
            val app = ApplicationProvider.getApplicationContext<Application>()
            // Synthetic sandbox token only; all requests are redirected to the local fake server.
            auth.saveToken("fixture-only-not-a-real-token")
            val model = NovalPieViewModel(app)
            store.put("root", model)
            api = model.javaClass.getDeclaredField("api").apply { isAccessible = true }.get(model) as NovalPieApi
            val baseField = api.javaClass.getDeclaredField("baseUrl").apply { isAccessible = true }
            val proxyField = api.javaClass.getDeclaredField("proxySelectorProvider").apply { isAccessible = true }
            oldBase = baseField.get(api); oldProxy = proxyField.get(api)
            baseField.set(api, server.url("/").toString().trimEnd('/'))
            val direct: () -> ProxySelector? = { FixedProxySelector(listOf(Proxy.NO_PROXY)) }
            proxyField.set(api, direct)
            val before = model.viewModelScope.coroutineContext[Job]!!.children.toSet()
            model.openDeepLink("https://novalpie.cc/book-edit/info/1")
            val entry = (model.viewModelScope.coroutineContext[Job]!!.children.toSet() - before).single()
            dispatcher.scheduler.runCurrent()
            assertTrue("Permission read must be in flight", started.await(5, TimeUnit.SECONDS))
            model.openBook(2); model.openBook(1)
            release.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
            while (!entry.isCompleted && System.nanoTime() < deadline) {
                dispatcher.scheduler.runCurrent(); Thread.sleep(10)
            }
            dispatcher.scheduler.runCurrent()
            assertTrue("Permission continuation must have settled", entry.isCompleted)
            assertEquals(AppRoute.BookDetail(1), model.currentRoute)
        } finally {
            release.countDown(); store.clear(); dispatcher.scheduler.runCurrent()
            server.shutdown()
            api?.let { value ->
                value.javaClass.getDeclaredField("baseUrl").apply { isAccessible = true }.set(value, oldBase)
                value.javaClass.getDeclaredField("proxySelectorProvider").apply { isAccessible = true }.set(value, oldProxy)
            }
            if (previousToken == null) auth.clearToken() else auth.saveToken(previousToken)
            com.novalpie.nativeapp.core.AppContainer.from(ApplicationProvider.getApplicationContext<Application>()).refreshEnvironmentFromStores()
            Dispatchers.resetMain()
        }
    }
}
