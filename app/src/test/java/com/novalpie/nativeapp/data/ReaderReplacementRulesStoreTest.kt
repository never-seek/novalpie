package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ReaderReplacementOwner
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementScope
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderReplacementRulesStoreTest {
    private lateinit var context: Context
    private lateinit var store: ReaderReplacementRulesStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReaderReplacementRulesStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = ReaderReplacementRulesStore(context)
    }

    @Test
    fun personalRulesRoundTripWithOrderAndScope() {
        val rules = listOf(
            rule(id = "first", order = 1, scope = ReaderReplacementScope.WholeBook),
            rule(id = "second", order = 2, scope = ReaderReplacementScope.ChapterRange(3, 5)),
        )

        store.savePersonalRules(novelId = 12L, rules = rules)

        val loaded = store.loadPersonalRules(12L)
        assertEquals(rules.map(ReaderReplacementRule::id), loaded.map(ReaderReplacementRule::id))
        assertEquals(rules.map(ReaderReplacementRule::source), loaded.map(ReaderReplacementRule::source))
        assertEquals(rules.map(ReaderReplacementRule::replacement), loaded.map(ReaderReplacementRule::replacement))
        assertEquals(rules.map(ReaderReplacementRule::order), loaded.map(ReaderReplacementRule::order))
        assertEquals(listOf("whole", "range:3:5"), loaded.map { scopeLabel(it.scope) })
    }

    @Test
    fun hiddenSharedRulesAreStoredPerBook() {
        store.saveHiddenSharedRuleIds(12L, setOf("shared-a", "shared-b"))
        store.saveHiddenSharedRuleIds(13L, setOf("shared-c"))

        assertEquals(setOf("shared-a", "shared-b"), store.loadHiddenSharedRuleIds(12L))
        assertEquals(setOf("shared-c"), store.loadHiddenSharedRuleIds(13L))
    }

    @Test
    fun savingChangesIncrementsOnlyThatBooksRevision() {
        assertEquals(0L, store.revision(12L))

        store.savePersonalRules(12L, listOf(rule(id = "rule")))
        store.saveHiddenSharedRuleIds(13L, setOf("shared"))

        assertEquals(1L, store.revision(12L))
        assertEquals(1L, store.revision(13L))
    }

    @Test
    fun websiteRuleIdSurvivesLocalPersistenceSoFutureEditsUpdateInsteadOfCreate() {
        val remoteBacked = rule(id = "local-1").copy(websiteRuleId = 42L)

        store.savePersonalRules(12L, listOf(remoteBacked))

        assertEquals(42L, store.loadPersonalRules(12L).single().websiteRuleId)
    }

    @Test
    fun sharedRulePolicyKeepsDeviceDefaultSeparateFromPerBookOverrides() {
        assertFalse(store.loadDefaultSharedRulesEnabled())
        assertNull(store.loadSharedRulesEnabledOverride(12L))

        store.saveDefaultSharedRulesEnabled(true)
        store.saveSharedRulesEnabledOverride(12L, false)

        assertTrue(store.loadDefaultSharedRulesEnabled())
        assertEquals(false, store.loadSharedRulesEnabledOverride(12L))
        assertNull(store.loadSharedRulesEnabledOverride(13L))
    }

    @Test
    fun clearingABookOverrideRestoresTheDeviceDefaultPolicy() {
        store.saveDefaultSharedRulesEnabled(true)
        store.saveSharedRulesEnabledOverride(12L, false)

        store.saveSharedRulesEnabledOverride(12L, null)

        assertNull(store.loadSharedRulesEnabledOverride(12L))
        assertTrue(store.loadDefaultSharedRulesEnabled())
    }

    private fun rule(
        id: String,
        order: Int = 0,
        scope: ReaderReplacementScope = ReaderReplacementScope.WholeBook,
    ) = ReaderReplacementRule(
        id = id,
        novelId = 12L,
        source = "原名$id",
        replacement = "译名$id",
        owner = ReaderReplacementOwner.Personal,
        order = order,
        scope = scope,
        target = ReaderReplacementTarget.Content,
    )

    private fun scopeLabel(scope: ReaderReplacementScope): String = when (scope) {
        ReaderReplacementScope.WholeBook -> "whole"
        is ReaderReplacementScope.CurrentChapter -> "chapter:${scope.chapterOrder}"
        is ReaderReplacementScope.ChapterRange -> "range:${scope.startOrder}:${scope.endOrder}"
    }
}
