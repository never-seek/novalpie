package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileProtocolRegressionTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi
    @Before fun setUp() { server = MockWebServer(); server.start(); api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/')) }
    @After fun tearDown() { server.shutdown() }
    private fun reject() { server.enqueue(MockResponse().setBody("""{"success":false,"message":"合成权限拒绝","data":{}}""")) }
    @Test fun rejectedProfileSaveIsNotReturnedAsTheSavedDraft() = runBlocking {
        reject()
        assertTrue(runCatching { api.updateCurrentUser(UserProfile(1, "合成资料")) }.isFailure)
        assertEquals(1, server.requestCount)
    }
    @Test fun rejectedEquipmentAndPurchaseCannotLoseTheirOuterFailure() = runBlocking {
        reject(); assertFalse(api.setCurrentUserEquipment(1, "equip").success)
        reject(); assertFalse(api.purchaseShopItem(1).success)
    }
}
