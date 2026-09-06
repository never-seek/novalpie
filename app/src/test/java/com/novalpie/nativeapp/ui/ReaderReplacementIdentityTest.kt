package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Test

class ReaderReplacementIdentityTest {
    private fun rule(id: String, source: String, target: String, regex: Boolean = false, shared: Boolean = false) =
        ReaderReplacementRule(id, 1, source, target, isRegex = regex, owner = if (shared) ReaderReplacementOwner.Shared else ReaderReplacementOwner.Personal)

    @Test fun literalAndRegexWithIdenticalSpellingAreNotTheSameRule() {
        val local = rule("local", "a.b", "字面")
        val remote = rule("personal:5", "a.b", "正则", regex = true).copy(websiteRuleId = 5)
        assertEquals(2, mergeReaderReplacementPersonalRules(listOf(local), listOf(remote)).size)
        val active = effectiveReaderReplacementRules(listOf(remote.copy(id = "shared:5", owner = ReaderReplacementOwner.Shared)), listOf(local), emptySet(), 1, ReaderReplacementTarget.Content)
        assertEquals(2, active.size)
    }

    @Test fun aNewRemoteTargetUpdatesTheMirrorWithoutOverwritingLocalScopes() {
        val remote = rule("personal:8", "旧名", "新译名").copy(websiteRuleId = 8, updatedAt = "2026-09-06 10:00:00")
        val local = remote.copy(replacement = "旧译名", updatedAt = "2026-09-01 10:00:00", isEnabled = false,
            scope = ReaderReplacementScope.CurrentChapter(3), websiteSource = "旧名", websiteReplacement = "旧译名")
        val merged = mergeReaderReplacementPersonalRules(listOf(local), listOf(remote)).single()
        assertEquals("新译名", merged.replacement)
        assertFalse(merged.isEnabled)
        assertEquals(ReaderReplacementScope.CurrentChapter(3), merged.scope)
    }

    @Test fun aLegacyOrUnsyncedLocalEditMustNeverBeGuessedAwayFromTimestamps() {
        val remote = rule("personal:8", "旧名", "别人设备译名").copy(websiteRuleId = 8, updatedAt = "2026-09-06 10:00:00")
        val legacy = remote.copy(replacement = "本机译名", updatedAt = "2026-09-01 10:00:00")
        assertEquals("本机译名", mergeReaderReplacementPersonalRules(listOf(legacy), listOf(remote)).single().replacement)
        val pending = legacy.copy(websiteSource = "旧名", websiteReplacement = "上次同步译名")
        assertEquals("本机译名", mergeReaderReplacementPersonalRules(listOf(pending), listOf(remote)).single().replacement)
    }

    @Test fun newestSharedDuplicateWinsInsteadOfAnEarlierSourceConsumingTheText() {
        val old = rule("shared:10", "Alice", "旧公共名", shared = true).copy(updatedAt = "2026-09-01 00:00:00")
        val recent = old.copy(id = "shared:11", replacement = "新公共名", updatedAt = "2026-09-06 00:00:00")
        val active = effectiveReaderReplacementRules(listOf(old, recent), emptyList(), emptySet(), 1, ReaderReplacementTarget.Content)
        assertEquals("新公共名", applyReaderReplacementRules("Alice", active).text)
        assertEquals(1, active.size)
    }

    @Test fun userRuleOrderWinsForOverlappingSequentialRules() {
        val first = rule("z", "A", "B").copy(order = 1)
        val second = rule("a", "B", "C").copy(order = 2)
        assertEquals("C", applyReaderReplacementRules("A", listOf(second, first)).text)
    }
}
