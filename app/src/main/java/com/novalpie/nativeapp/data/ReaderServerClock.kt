package com.novalpie.nativeapp.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Source TLS Date corrects request timestamps in-memory, never the device's OS clock. */
internal class ReaderServerClock {
    private data class Sample(val serverMillis:Long,val elapsedMillis:Long)
    @Volatile private var sample:Sample?=null
    fun observe(date:String?,elapsedMillis:Long):Boolean {
        val value=date?.takeIf{it.length<=64} ?: return false
        val parsed=runCatching {SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US).apply {
            isLenient=false;timeZone=TimeZone.getTimeZone("GMT")
        }.parse(value)?.time}.getOrNull() ?: return false
        if(parsed !in 1_577_836_800_000L..4_102_444_800_000L)return false
        sample=Sample(parsed,elapsedMillis)
        return true
    }
    fun epochSeconds(localMillis:Long,elapsedMillis:Long):Long {
        val snapshot=sample ?: return localMillis/1000
        val age=elapsedMillis-snapshot.elapsedMillis
        return if(age in 0..21_600_000L)(snapshot.serverMillis+age)/1000 else localMillis/1000
    }
}
