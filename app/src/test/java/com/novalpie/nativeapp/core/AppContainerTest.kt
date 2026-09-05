package com.novalpie.nativeapp.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.AuthSessionStore
import com.novalpie.nativeapp.data.ProxySettings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerTest {
    private lateinit var app: Application
    @Before fun before() {
        app=ApplicationProvider.getApplicationContext()
        AuthSessionStore(app).clearToken()
    }
    @Test fun dependenciesAreSingleInstanceAndSessionChangesAdvanceRequestIdentity() {
        val container=AppContainer(app)
        assertSame(container.api,container.api)
        assertSame(container.searchRepository,container.searchRepository)
        val generation=container.environment.revision
        container.environment.setToken("test-session-a")
        assertEquals(generation+1,container.environment.revision)
        container.environment.setToken("test-session-a")
        assertEquals(generation+1,container.environment.revision)
        assertEquals("test-session-a",AuthSessionStore(app).loadToken())
        container.environment.setToken(null)
        assertNull(AuthSessionStore(app).loadToken())
        assertEquals(generation+2,container.environment.revision)
    }
    @Test fun rotatingProxyInvalidatesRequestsWithoutDiscardingTheAuthSession() {
        val container=AppContainer(app)
        container.environment.setToken("test-session-a")
        val generation=container.environment.revision
        val prior=container.environment.proxy
        container.environment.setProxy(prior.copy(port=if(prior.port==4567)4568 else 4567))
        assertEquals(generation+1,container.environment.revision)
        assertEquals("test-session-a",container.environment.token)
        container.environment.setProxy(prior)
    }
}
