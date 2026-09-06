package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ReaderReplacementRule
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReplacementAccountIsolationTest {
    @Test fun missingUpgradeIdentityRequiresExplicitRecoveryAndNeverPublishesOldRules() {
        val prefs = context.getSharedPreferences(ReaderReplacementRulesStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("personal_rules_1", """[{"id":"personal:7","source":"A","replacement":"B","website_rule_id":7}]""").commit()
        var account: Long? = null
        val store = ReaderReplacementRulesStore(context) { account }
        assertTrue(store.loadPersonalRules(1).isEmpty())
        account = 33
        assertTrue(store.loadPersonalRules(1).isEmpty())
        assertTrue(store.hasUnassignedLegacyRules(1))
        assertEquals(1, store.importUnassignedLegacyRules(1))
        val restored = store.loadPersonalRules(1).single()
        assertFalse(restored.isEnabled)
        assertNull(restored.websiteRuleId)
        assertEquals(0, store.importUnassignedLegacyRules(1))
    }
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    @Test fun switchingAccountsCannotReadOrPublishThePreviousUsersPersonalRules() {
        var account: Long? = 11
        val store = ReaderReplacementRulesStore(context) { account }
        store.savePersonalRules(1, listOf(ReaderReplacementRule("mine", 1, "Alice", "我的名字")))
        store.saveHiddenSharedRuleIds(1, setOf("shared:7"))
        store.saveSharedRulesEnabledOverride(1, true)
        account = 22
        assertTrue(store.loadPersonalRules(1).isEmpty())
        assertTrue(store.loadHiddenSharedRuleIds(1).isEmpty())
        assertNull(store.loadSharedRulesEnabledOverride(1))
        account = null
        assertTrue(store.loadPersonalRules(1).isEmpty())
        account = 11
        assertEquals("我的名字", store.loadPersonalRules(1).single().replacement)
        assertEquals(setOf("shared:7"), store.loadHiddenSharedRuleIds(1))
        assertEquals(true, store.loadSharedRulesEnabledOverride(1))
    }

    @Test fun beta6DataMigratesOnlyToTheRetainedAccountAndResetCannotReplayLegacyValues() {
        val prefs = context.getSharedPreferences(ReaderReplacementRulesStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val rules = JSONArray().put(JSONObject().put("id", "legacy").put("source", "Alice").put("replacement", "旧规则"))
        prefs.edit().putString("personal_rules_1", rules.toString()).putBoolean("shared_rules_enabled_override_1", true).commit()
        var account: Long? = 11
        val store = ReaderReplacementRulesStore(context) { account }
        assertEquals("legacy", store.loadPersonalRules(1).single().id)
        store.saveSharedRulesEnabledOverride(1, null)
        assertNull(store.loadSharedRulesEnabledOverride(1))
        account = 22
        assertTrue(store.loadPersonalRules(1).isEmpty())
        assertNull(store.loadSharedRulesEnabledOverride(1))
        account = 11
        val reopened = ReaderReplacementRulesStore(context) { account }
        assertNull(reopened.loadSharedRulesEnabledOverride(1))
        assertTrue("旧版副本必须保留可恢复", prefs.contains("personal_rules_1"))
    }
}
