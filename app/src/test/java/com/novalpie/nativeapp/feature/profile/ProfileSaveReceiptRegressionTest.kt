package com.novalpie.nativeapp.feature.profile
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35])
class ProfileSaveReceiptRegressionTest {
 @Test fun rejectedSecondSettingsWriteMustBeReportedAsPartialNotAllSaved()=runBlocking {
  MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("{\"success\":true}"));s.enqueue(MockResponse().setBody("{\"success\":false,\"message\":\"settings rejected\"}"))
   val result=runCatching{WebsiteProfileRepository(NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))).save(UserProfile(1,"QA",autoCheckin=true,showCheckin=false))}
   assertTrue(result.isFailure);assertTrue(result.exceptionOrNull()!!.message!!.contains("资料已保存"));assertEquals(2,s.requestCount)
  }
 }
 @Test fun transportFailureInSecondWriteCannotBeSwallowedAfterTheFirstWasAcknowledged()=runBlocking {
  MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("{\"success\":true}"));s.enqueue(MockResponse().setResponseCode(503).setBody("{\"message\":\"offline\"}"))
   val result=runCatching{WebsiteProfileRepository(NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))).save(UserProfile(1,"QA",autoCheckin=true,showCheckin=true))}
   assertTrue(result.isFailure);assertEquals(2,s.requestCount)
  }
 }
 @Test fun rejectedFirstWriteNeverSubmitsTheSecondWrite()=runBlocking {
  MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("{\"success\":false,\"message\":\"no permission\"}"))
   val result=runCatching{WebsiteProfileRepository(NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))).save(UserProfile(1,"QA",autoCheckin=true,showCheckin=true))}
   assertTrue(result.isFailure);assertEquals(1,s.requestCount)
  }
 }

 @Test fun rejectedOuterEnvelopeCannotBeHiddenByAnInnerObjectWithoutSuccess()=runBlocking {
  MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("{\"success\":true}"));s.enqueue(MockResponse().setBody("{\"success\":false,\"message\":\"blocked\",\"data\":{}}"))
   val result=runCatching{WebsiteProfileRepository(NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))).save(UserProfile(1,"QA",autoCheckin=true,showCheckin=true))}
   assertTrue(result.exceptionOrNull() is ProfileSavePartialFailure);assertEquals(2,s.requestCount)
  }
 }
 @Test fun unspecifiedCheckinSettingIsNotResetToADefaultDuringPartialUpdate()=runBlocking {
  MockWebServer().use{s->s.start();repeat(2){s.enqueue(MockResponse().setBody("{\"success\":true}"))}
   WebsiteProfileRepository(NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))).save(UserProfile(1,"QA",autoCheckin=true))
   val first=s.takeRequest();val second=s.takeRequest();val body=org.json.JSONObject(second.body.readUtf8())
   assertTrue(body.getBoolean("auto_checkin"));assertFalse(body.has("show_checkin"));assertEquals("PATCH",second.method)
  }
 }
}
