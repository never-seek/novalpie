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
    @Test fun websiteProfileRepositorySaveUpdatesBothProfileAndCheckinSettings() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":1,"username":"新名字","show_checkin":true,"auto_checkin":true}}"""))
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"show_checkin":true,"auto_checkin":true}}"""))
        val repo = com.novalpie.nativeapp.feature.profile.WebsiteProfileRepository(api)
        val saved = repo.save(UserProfile(1, "新名字", showCheckin = true, autoCheckin = true))
        assertEquals("新名字", saved.name)
        val req1 = server.takeRequest()
        assertEquals("PATCH", req1.method)
        assertEquals("/api/users/me", req1.path)
        val req2 = server.takeRequest()
        assertEquals("PATCH", req2.method)
        assertEquals("/api/users/me/checkins/settings", req2.path)
    }
}
