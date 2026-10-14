package com.novalpie.nativeapp.feature.profile
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class StoredCheckinReceiptRegressionTest {
 @Test fun storedReceiptsSurviveARecreatedStoreAndStayAccountAndDayScoped(){
  val context=ApplicationProvider.getApplicationContext<Context>()
  val store=StoredCheckinReceiptStore(context);store.put(1,"2026-10-05",CheckinReceipt.Confirmed)
  assertEquals(CheckinReceipt.Confirmed,StoredCheckinReceiptStore(context).get(1,"2026-10-05"))
  assertNull(store.get(2,"2026-10-05"));assertNull(store.get(1,"2026-10-06"))
 }
 @Test fun corruptReceiptCannotBecomePermissionToReplayAnUnknownWrite(){
  val context=ApplicationProvider.getApplicationContext<Context>()
  context.getSharedPreferences("novalpie_checkin_receipts",0).edit().putInt("1:2026-10-05",7).commit()
  assertEquals(CheckinReceipt.Attempted,StoredCheckinReceiptStore(context).get(1,"2026-10-05"))
 }
 @Test fun stationCalendarUsesChinaMidnightInsteadOfThePhonesLocalDay(){
  val utc=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply{set(2026,9,5,16,0,0);set(java.util.Calendar.MILLISECOND,0)}
  assertEquals("2026-10-06",checkinSiteDay(utc.timeInMillis));assertEquals("2026-10-05",checkinSiteDay(utc.timeInMillis-1))
 }
}
