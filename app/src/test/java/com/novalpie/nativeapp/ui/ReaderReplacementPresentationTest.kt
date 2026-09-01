package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ReaderReplacementOwner
import com.novalpie.nativeapp.model.ReaderReplacementRegexFlag
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementScope
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderReplacementPresentationTest {
    @Test
    fun personalRuleOverridesSharedRuleWithTheSameSource() {
        val shared = rule(
            id = "shared-alice",
            source = "Alice",
            replacement = "共享译名",
            owner = ReaderReplacementOwner.Shared,
        )
        val personal = rule(
            id = "personal-alice",
            source = "Alice",
            replacement = "我的译名",
        )

        val effective = effectiveReaderReplacementRules(
            sharedRules = listOf(shared),
            personalRules = listOf(personal),
            hiddenSharedRuleIds = emptySet(),
            chapterOrder = 1,
            target = ReaderReplacementTarget.Content,
        )

        assertEquals("我的译名", applyReaderReplacementRules("Alice", effective).text)
    }

    @Test
    fun allRulesViewKeepsPersonalRulesAndAddsOnlyNonOverriddenSharedRules() {
        val personal = rule(
            id = "personal-alice",
            source = "Alice",
            replacement = "我的译名",
        )
        val sharedAlice = rule(
            id = "shared-alice",
            source = "Alice",
            replacement = "公共译名",
            owner = ReaderReplacementOwner.Shared,
        )
        val sharedBob = rule(
            id = "shared-bob",
            source = "Bob",
            replacement = "鲍勃",
            owner = ReaderReplacementOwner.Shared,
        )

        assertEquals(
            listOf(sharedBob, personal),
            readerReplacementRulesForDisplay(
                source = ReaderReplacementRuleSource.All,
                personalRules = listOf(personal),
                sharedRules = listOf(sharedAlice, sharedBob),
            ),
        )
    }

    @Test
    fun personalModeDoesNotApplySharedRulesUntilTheReaderChoosesAllRules() {
        val shared = rule(
            id = "shared-alice",
            source = "Alice",
            replacement = "公共译名",
            owner = ReaderReplacementOwner.Shared,
        )
        val personalMode = ReaderReplacementState(
            source = ReaderReplacementRuleSource.Personal,
            sharedRules = LoadResult.Success(listOf(shared)),
        )
        val allMode = personalMode.copy(
            source = ReaderReplacementRuleSource.All,
            sharedRulesEnabledOverride = true,
        )

        assertEquals(
            emptyList<ReaderReplacementRule>(),
            readerReplacementRulesForChapter(
                state = personalMode,
                chapterOrder = 1,
                target = ReaderReplacementTarget.Content,
            ),
        )
        assertEquals(
            listOf(shared),
            readerReplacementRulesForChapter(
                state = allMode,
                chapterOrder = 1,
                target = ReaderReplacementTarget.Content,
            ),
        )
    }

    @Test
    fun simplePersonalRuleCreatesAWebsiteGlossaryEntry() {
        val saved = rule(
            id = "local-a",
            source = "Alice",
            replacement = "艾莉丝",
        )

        assertEquals(
            ReaderReplacementRemoteSyncAction.Create(saved),
            readerReplacementSaveSyncAction(previous = null, saved = saved),
        )
    }

    @Test
    fun changingAnExistingWebsiteRuleToChapterScopeRemovesItsGlobalServerCopy() {
        val previous = rule(
            id = "personal:42",
            source = "Alice",
            replacement = "艾莉丝",
        )
        val scoped = previous.copy(scope = ReaderReplacementScope.CurrentChapter(3))

        assertEquals(
            ReaderReplacementRemoteSyncAction.Delete(serverRuleId = 42L),
            readerReplacementSaveSyncAction(previous = previous, saved = scoped),
        )
    }

    @Test
    fun changingAWebsiteRuleSourceReplacesRatherThanMutatingTheImmutableSourceField() {
        val previous = rule(
            id = "personal:42",
            source = "Alice",
            replacement = "艾莉丝",
        )
        val saved = previous.copy(source = "Alicia")

        assertEquals(
            ReaderReplacementRemoteSyncAction.Replace(serverRuleId = 42L, rule = saved),
            readerReplacementSaveSyncAction(previous = previous, saved = saved),
        )
    }

    @Test
    fun deletingAnImportedPersonalGlossaryUsesItsServerId() {
        val rule = rule(
            id = "personal:42",
            source = "Alice",
            replacement = "艾莉丝",
        )

        assertEquals(
            ReaderReplacementRemoteSyncAction.Delete(serverRuleId = 42L),
            readerReplacementDeleteSyncAction(rule),
        )
    }

    @Test
    fun hiddenSharedRuleDoesNotChangeText() {
        val shared = rule(
            id = "shared-ad",
            source = "广告",
            replacement = "",
            owner = ReaderReplacementOwner.Shared,
        )

        val effective = effectiveReaderReplacementRules(
            sharedRules = listOf(shared),
            personalRules = emptyList(),
            hiddenSharedRuleIds = setOf("shared-ad"),
            chapterOrder = 1,
            target = ReaderReplacementTarget.Content,
        )

        assertEquals("广告正文", applyReaderReplacementRules("广告正文", effective).text)
    }

    @Test
    fun chapterRangeRuleDoesNotLeakOutsideItsRange() {
        val rangeRule = rule(
            id = "range",
            source = "由男",
            replacement = "由乃",
            scope = ReaderReplacementScope.ChapterRange(startOrder = 2, endOrder = 4),
        )

        val inside = effectiveReaderReplacementRules(
            sharedRules = emptyList(),
            personalRules = listOf(rangeRule),
            hiddenSharedRuleIds = emptySet(),
            chapterOrder = 3,
            target = ReaderReplacementTarget.Content,
        )
        val outside = effectiveReaderReplacementRules(
            sharedRules = emptyList(),
            personalRules = listOf(rangeRule),
            hiddenSharedRuleIds = emptySet(),
            chapterOrder = 5,
            target = ReaderReplacementTarget.Content,
        )

        assertEquals("由乃", applyReaderReplacementRules("由男", inside, chapterOrder = 3).text)
        assertEquals("由男", applyReaderReplacementRules("由男", outside, chapterOrder = 5).text)
    }

    @Test
    fun literalReplacementKeepsDollarCharactersLiteral() {
        val result = applyReaderReplacementRules(
            "原文",
            listOf(rule(id = "literal", source = "原文", replacement = "$1")),
        )

        assertEquals("$1", result.text)
    }

    @Test
    fun regexRuleSupportsCaptureGroupReplacement() {
        val regexRule = rule(
            id = "regex",
            source = "(甲)(乙)",
            replacement = "$2-$1",
            isRegex = true,
        )

        assertEquals("乙-甲", applyReaderReplacementRules("甲乙", listOf(regexRule)).text)
    }

    @Test
    fun malformedRegexIsReportedAndDoesNotChangeText() {
        val result = applyReaderReplacementRules(
            "正文",
            listOf(rule(id = "bad", source = "(", replacement = "x", isRegex = true)),
        )

        assertEquals("正文", result.text)
        assertEquals(listOf("bad"), result.invalidRuleIds)
    }

    @Test
    fun localScriptRulesAreRejectedForEveryRuleSource() {
        val validation = validateReaderReplacementRule(
            rule(id = "script", source = "@js:return text", replacement = "x"),
        )

        assertFalse(validation.isValid)
        assertTrue(validation.message.orEmpty().contains("脚本"))
    }

    @Test
    fun cloneSharedRuleCreatesAnEditablePersonalOverride() {
        val shared = rule(
            id = "shared-a",
            source = "Alice",
            replacement = "艾丽丝",
            owner = ReaderReplacementOwner.Shared,
        )

        val clone = cloneSharedReaderReplacementRule(shared, newId = "personal-a")

        assertEquals(ReaderReplacementOwner.Personal, clone.owner)
        assertEquals("shared-a", clone.sharedRuleId)
        assertEquals("Alice", clone.source)
        assertEquals("艾丽丝", clone.replacement)
    }

    @Test
    fun effectiveReaderTextUsesTheSameTransformedContentForDisplayAndTts() {
        val effective = effectiveReaderText(
            title = "广告标题",
            content = "广告正文",
            rules = listOf(
                rule(id = "remove", source = "广告", replacement = "").copy(
                    target = ReaderReplacementTarget.Both,
                ),
            ),
            chapterOrder = 1,
            revision = 7L,
        )

        assertEquals("标题", effective.title)
        assertEquals("正文", effective.content)
        assertEquals(effective.content, effective.ttsContent)
        assertEquals(7L, effective.revision)
    }

    @Test
    fun chapterRangeEditorScopeRequiresAnOrderedPositiveRange() {
        assertEquals(
            ReaderReplacementScope.ChapterRange(startOrder = 2, endOrder = 5),
            readerReplacementScopeForEditor(
                selectedScope = ReaderReplacementScope.ChapterRange(1, 1),
                rangeStart = "2",
                rangeEnd = "5",
            ),
        )
        assertEquals(
            null,
            readerReplacementScopeForEditor(
                selectedScope = ReaderReplacementScope.ChapterRange(1, 1),
                rangeStart = "5",
                rangeEnd = "2",
            ),
        )
    }

    @Test
    fun remotePersonalRulesFillGapsWithoutOverwritingLocalRules() {
        val local = rule(
            id = "local-alice",
            source = "Alice",
            replacement = "我的译名",
        )
        val remoteAlice = rule(
            id = "personal:11",
            source = "Alice",
            replacement = "远端译名",
        )
        val remoteBob = rule(
            id = "personal:12",
            source = "Bob",
            replacement = "鲍勃",
        )

        assertEquals(
            listOf(local, remoteBob),
            mergeReaderReplacementPersonalRules(
                localRules = listOf(local),
                remoteRules = listOf(remoteAlice, remoteBob),
            ),
        )
    }

    @Test
    fun importedWebsiteRuleBindsToExistingLocalRuleWithTheSameSource() {
        val local = rule(
            id = "local-alice",
            source = "Alice",
            replacement = "我的译名",
        )
        val remote = rule(
            id = "personal:11",
            source = "Alice",
            replacement = "网站译名",
        ).copy(websiteRuleId = 11L)

        val merged = mergeReaderReplacementPersonalRules(
            localRules = listOf(local),
            remoteRules = listOf(remote),
        )

        assertEquals("我的译名", merged.single().replacement)
        assertEquals(11L, merged.single().websiteRuleId)
    }

    @Test
    fun copiedReaderSelectionBecomesTrimmedReplacementSource() {
        assertEquals(
            "错别字",
            readerReplacementSourceFromClipboard("  错别字\n"),
        )
        assertEquals("", readerReplacementSourceFromClipboard(null))
    }

    @Test
    fun copiedSelectionPrefillsReplacementOnlyWhenItBelongsToTheVisibleReaderText() {
        val contents = listOf(
            ReaderChapterContent(
                chapterId = 1L,
                title = "Chapter Alice",
                content = ReaderContent(
                    title = "Chapter Alice",
                    content = "<p>Alice enters the room.</p>",
                    source = "novelpia",
                ),
            ),
        )

        assertEquals("Alice", readerReplacementPrefillSource("  Alice  ", contents))
        assertEquals("Chapter Alice", readerReplacementPrefillSource("Chapter Alice", contents))
        assertEquals(null, readerReplacementPrefillSource("outside clipboard", contents))
    }

    @Test
    fun personalRuleModeDoesNotMislabelAvailablePublicRulesAsEnabled() {
        val state = ReaderReplacementState(
            source = ReaderReplacementRuleSource.Personal,
            personalRules = listOf(rule(id = "mine", source = "A", replacement = "甲")),
            sharedRules = LoadResult.Success(
                listOf(
                    rule(id = "shared-a", source = "B", replacement = "乙", owner = ReaderReplacementOwner.Shared),
                    rule(id = "shared-b", source = "C", replacement = "丙", owner = ReaderReplacementOwner.Shared),
                ),
            ),
        )

        assertEquals("仅我的 1 条 · 公共 2 条可选", readerReplacementModeSummary(state))
        assertEquals("公共未应用", readerReplacementModeTag(state))
        assertEquals(
            "我的 1 条 · 已启用公共 2 条",
            readerReplacementModeSummary(state.copy(sharedRulesEnabledOverride = true)),
        )
    }

    @Test
    fun sharedRulePolicyUsesBookOverrideBeforeTheDeviceDefault() {
        assertFalse(readerSharedRulesEnabled(defaultEnabled = false, bookOverride = null))
        assertTrue(readerSharedRulesEnabled(defaultEnabled = true, bookOverride = null))
        assertFalse(readerSharedRulesEnabled(defaultEnabled = true, bookOverride = false))
        assertTrue(readerSharedRulesEnabled(defaultEnabled = false, bookOverride = true))
    }

    @Test
    fun perBookPublicRulePolicyChangesOnlyTheEffectiveReaderText() {
        val personal = rule(id = "mine", source = "Alice", replacement = "艾莉丝")
        val shared = rule(
            id = "shared",
            source = "Bob",
            replacement = "鲍勃",
            owner = ReaderReplacementOwner.Shared,
        )
        val base = ReaderReplacementState(
            personalRules = listOf(personal),
            sharedRules = LoadResult.Success(listOf(shared)),
            defaultSharedRulesEnabled = false,
        )
        fun render(state: ReaderReplacementState): String = effectiveReaderText(
            title = "",
            content = "Alice meets Bob",
            rules = readerReplacementRulesForChapter(
                state = state,
                chapterOrder = 1,
                target = ReaderReplacementTarget.Content,
            ),
            chapterOrder = 1,
            revision = state.revision,
        ).content

        assertEquals("艾莉丝 meets Bob", render(base))
        assertEquals("艾莉丝 meets 鲍勃", render(base.copy(sharedRulesEnabledOverride = true)))
        assertEquals(
            "艾莉丝 meets Bob",
            render(base.copy(sharedRulesEnabledOverride = true, hiddenSharedRuleIds = setOf("shared"))),
        )
    }

    @Test
    fun downloadReplacementTransformsOnlyTextAndKeepsSourceImageMarkersUntouched() {
        val rules = listOf(rule(id = "replace", source = "Alice", replacement = "艾莉丝"))
        val body = "Alice speaks.\n[图片: https://images.example.test/Alice.png]\nAlice leaves."

        assertEquals(
            "艾莉丝 speaks.\n[图片: https://images.example.test/Alice.png]\n艾莉丝 leaves.",
            applyReaderReplacementRulesToDownloadBody(body, rules, chapterOrder = 1).text,
        )
    }

    @Test
    fun downloadSnapshotFreezesTheEffectiveRulesAtDownloadStart() {
        val shared = rule(
            id = "shared",
            source = "Bob",
            replacement = "鲍勃",
            owner = ReaderReplacementOwner.Shared,
        )
        val personal = rule(id = "mine", source = "Alice", replacement = "艾莉丝")
            .copy(target = ReaderReplacementTarget.Both)
        val state = ReaderReplacementState(
            personalRules = listOf(personal),
            sharedRules = LoadResult.Success(listOf(shared)),
            sharedRulesEnabledOverride = true,
        )

        val snapshot = readerDownloadReplacementSnapshot(applyRules = true, state = state)
        val transformed = snapshot.transform(chapterOrder = 1, title = "Alice", body = "Alice meets Bob")

        assertEquals("艾莉丝", transformed.title)
        assertEquals("艾莉丝 meets 鲍勃", transformed.body)
        assertEquals(
            "Alice meets Bob",
            readerDownloadReplacementSnapshot(applyRules = false, state = state)
                .transform(chapterOrder = 1, title = "Alice", body = "Alice meets Bob")
                .body,
        )
    }

    @Test
    fun loadedSharedRulesAdvanceTheDisplayRevision() {
        val current = ReaderReplacementState(
            novelId = 12L,
            sharedRules = LoadResult.Loading,
            revision = 4L,
        )
        val shared = listOf(
            rule(
                id = "shared:1",
                source = "Alice",
                replacement = "艾丽丝",
                owner = ReaderReplacementOwner.Shared,
            ),
        )

        assertEquals(
            5L,
            readerReplacementRevisionAfterRulesLoad(
                current = current,
                personalRules = current.personalRules,
                sharedRules = LoadResult.Success(shared),
            ),
        )
    }

    @Test
    fun localMutationKeepsRevisionAheadOfARecentRemoteLoad() {
        assertEquals(
            8L,
            readerReplacementNextLocalRevision(
                currentRevision = 7L,
                persistedRevision = 3L,
            ),
        )
    }

    @Test
    fun unknownChapterOrderKeepsScopedRulesOutOfTheInitialBody() {
        val wholeBook = rule(
            id = "whole",
            source = "Alice",
            replacement = "艾丽丝",
        )
        val currentChapter = rule(
            id = "chapter-100",
            source = "Bob",
            replacement = "鲍勃",
            scope = ReaderReplacementScope.CurrentChapter(100),
        )
        val state = ReaderReplacementState(personalRules = listOf(wholeBook, currentChapter))

        assertEquals(
            listOf(wholeBook),
            readerReplacementRulesForChapter(
                state = state,
                chapterOrder = null,
                target = ReaderReplacementTarget.Content,
            ),
        )
        assertEquals(
            null,
            readerChapterOrderForId(
                chapterId = 1000L,
                chapters = listOf(Chapter(id = 1L, title = "第一章", number = 1)),
            ),
        )
    }

    private fun rule(
        id: String,
        source: String,
        replacement: String,
        owner: ReaderReplacementOwner = ReaderReplacementOwner.Personal,
        isRegex: Boolean = false,
        scope: ReaderReplacementScope = ReaderReplacementScope.WholeBook,
    ) = ReaderReplacementRule(
        id = id,
        novelId = 12L,
        source = source,
        replacement = replacement,
        owner = owner,
        isRegex = isRegex,
        regexFlags = setOf(ReaderReplacementRegexFlag.IgnoreCase),
        scope = scope,
        target = ReaderReplacementTarget.Content,
    )
}
