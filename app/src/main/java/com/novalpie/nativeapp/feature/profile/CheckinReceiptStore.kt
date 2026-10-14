package com.novalpie.nativeapp.feature.profile

import android.content.Context
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Stable account/day receipts contain no session token or profile content. */
internal enum class CheckinReceipt { Attempted, Rejected, Confirmed }
internal interface CheckinReceiptStore {
    fun get(accountId:Long,date:String):CheckinReceipt?
    fun put(accountId:Long,date:String,value:CheckinReceipt)
    fun clear(accountId:Long,date:String)
}
internal class MemoryCheckinReceiptStore:CheckinReceiptStore {
    private val values=mutableMapOf<Pair<Long,String>,CheckinReceipt>()
    override fun get(accountId:Long,date:String)=values[accountId to date]
    override fun put(accountId:Long,date:String,value:CheckinReceipt){values[accountId to date]=value}
    override fun clear(accountId:Long,date:String){values.remove(accountId to date)}
}
internal class StoredCheckinReceiptStore(context:Context):CheckinReceiptStore {
    private val preferences=context.applicationContext.getSharedPreferences("novalpie_checkin_receipts",Context.MODE_PRIVATE)
    private fun key(id:Long,date:String):String {require(id>0&&date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")));return "$id:$date"}
    override fun get(accountId:Long,date:String):CheckinReceipt?=runCatching {
        preferences.getString(key(accountId,date),null)?.let {stored->CheckinReceipt.entries.firstOrNull {it.name==stored} ?: CheckinReceipt.Attempted}
    }.getOrDefault(CheckinReceipt.Attempted)
    override fun put(accountId:Long,date:String,value:CheckinReceipt) {
        check(preferences.edit().putString(key(accountId,date),value.name).commit()){ "签到回执未能保存；不会自动重复提交" }
    }
    override fun clear(accountId:Long,date:String){check(preferences.edit().remove(key(accountId,date)).commit()){ "签到回执未能更新" }}
}
internal fun checkinSiteDay(nowMillis:Long):String {
    val calendar=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply{timeInMillis=nowMillis}
    return String.format(Locale.US,"%tF",calendar)
}
internal fun checkinAlreadyCompletedMessage(value:String?):Boolean {
    val text=value.orEmpty().lowercase()
    return Regex("(?:今日|今天|当日|當日).{0,8}(?:已|已经|已經).{0,3}(?:签到|簽到)|(?:已|已经|已經)(?:签到|簽到)(?:过|過)?(?:了)?$").containsMatchIn(text)||
        Regex("already (?:checked[ -]?in|signed[ -]?in)(?: today)?").containsMatchIn(text)
}
