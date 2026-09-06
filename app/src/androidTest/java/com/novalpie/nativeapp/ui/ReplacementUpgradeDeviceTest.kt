package com.novalpie.nativeapp.ui

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.AuthSessionStore
import com.novalpie.nativeapp.data.ReaderReplacementRulesStore
import com.novalpie.nativeapp.data.decodeAuthTokenProfile
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Verifies this device's retained Beta6 data in-place without exporting rules or login material. */
class ReplacementUpgradeDeviceTest {
    @Test fun retainedAccountCanReadItsLegacyRulesAndAnotherAccountCannot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val retained = AuthSessionStore(context).loadToken()?.let { decodeAuthTokenProfile(it, nowEpochSeconds = 0)?.id }
        assertNotNull("需要原安装保留的登录身份", retained)
        val prefs = context.getSharedPreferences(ReaderReplacementRulesStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val legacy = prefs.all.filterKeys { it.matches(Regex("personal_rules_[0-9]+")) }
        val current = ReaderReplacementRulesStore(context)
        var originalCount = 0
        var importedCount = 0
        for ((key, value) in legacy) {
            val book = key.substringAfterLast('_').toLong()
            val rows = JSONArray(value as String)
            val loaded = current.loadPersonalRules(book)
            originalCount += rows.length()
            importedCount += loaded.size
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val match = loaded.firstOrNull { it.id == row.getString("id") }
                assertNotNull("旧规则ID必须保留", match)
                assertEquals(row.optString("source"), match!!.source)
                assertEquals(row.optString("replacement"), match.replacement)
                assertEquals(row.optBoolean("is_enabled", true), match.isEnabled)
            }
            val legacyOverride = "shared_rules_enabled_override_$book"
            if (prefs.contains(legacyOverride)) assertEquals(prefs.getBoolean(legacyOverride, false), current.loadSharedRulesEnabledOverride(book))
        }
        val foreign = ReaderReplacementRulesStore(context) { Long.MAX_VALUE }
        assertTrue(legacy.keys.all { foreign.loadPersonalRules(it.substringAfterLast('_').toLong()).isEmpty() })
        val report = JSONObject().put("legacyBookCount", legacy.size).put("legacyRuleCount", originalCount)
            .put("loadedRuleCount", importedCount).put("foreignAccountEmpty", true).put("legacyBackupRetained", legacy.keys.all(prefs::contains))
        val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        File(folder, "replacement-upgrade.json").writeText(report.toString(2))
    }
}
