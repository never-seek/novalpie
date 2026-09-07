package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceIsolationProtocolTest {
    @Test fun localKeysAndJobsNeverCrossAnAccountSwitchAndClearIsAccountScoped() {
        var account: Long? = 1
        val store = WorkspaceLocalStore(ApplicationProvider.getApplicationContext()) { account }
        store.upsertApi(WorkspaceLocalApiConfig(1, "合成配置", "model", "https://fixture.test", "synthetic-test-only"))
        store.upsertJob(WorkspaceTranslationJob(2, 3, "测试书", translatorName = "合成配置"))
        account = 2
        assertTrue(store.loadApis().isEmpty()); assertTrue(store.loadJobs().isEmpty())
        store.clearAll()
        account = 1
        assertEquals(1, store.loadApis().size); assertEquals(1, store.loadJobs().size)
    }
    @Test fun explicitRefusalIsNotASharedConfigOrAnEmptyDashboard() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
            fun reject() { server.enqueue(MockResponse().setBody("""{"success":false,"message":"synthetic denial","data":{}}""")) }
            reject(); assertFalse(api.createWorkspaceApi("test", "model", "https://fixture.test", "synthetic-test-only", 2).success)
            reject(); assertTrue(runCatching { api.workspaceApiConfigs() }.isFailure)
        } finally { server.shutdown() }
    }
}
