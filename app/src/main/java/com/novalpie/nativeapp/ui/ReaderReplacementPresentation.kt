package com.novalpie.nativeapp.ui

import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException
import com.novalpie.nativeapp.model.ReaderReplacementOwner
import com.novalpie.nativeapp.model.ReaderReplacementRegexFlag
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementScope
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.Chapter

internal data class ReaderReplacementValidation(
    val isValid: Boolean,
    val message: String? = null,
)

internal data class ReaderReplacementApplyResult(
    val text: String,
    val invalidRuleIds: List<String> = emptyList(),
)

internal data class EffectiveReaderText(
    val title: String,
    val content: String,
    val ttsContent: String,
    val revision: Long,
    val invalidRuleIds: List<String>,
)

enum class ReaderReplacementRuleSource {
    Personal,
    All,
}

/**
 * The server glossary can represent only an enabled whole-book rule applied to both title and
 * body.  Richer native scopes deliberately remain device-local rather than silently changing
 * their meaning on the website.
 */
internal sealed interface ReaderReplacementRemoteSyncAction {
    data object None : ReaderReplacementRemoteSyncAction

    data class Create(
        val rule: ReaderReplacementRule,
    ) : ReaderReplacementRemoteSyncAction

    data class Update(
        val serverRuleId: Long,
        val replacement: String,
    ) : ReaderReplacementRemoteSyncAction

    data class Replace(
        val serverRuleId: Long,
        val rule: ReaderReplacementRule,
    ) : ReaderReplacementRemoteSyncAction

    data class Delete(
        val serverRuleId: Long,
    ) : ReaderReplacementRemoteSyncAction
}

data class ReaderReplacementState(
    val novelId: Long = 0L,
    /** Chooses which list is being managed; it never by itself alters rendered reader text. */
    val source: ReaderReplacementRuleSource = ReaderReplacementRuleSource.Personal,
    val personalRules: List<ReaderReplacementRule> = emptyList(),
    val sharedRules: LoadResult<List<ReaderReplacementRule>> = LoadResult.Idle,
    val hiddenSharedRuleIds: Set<String> = emptySet(),
    val defaultSharedRulesEnabled: Boolean = false,
    val sharedRulesEnabledOverride: Boolean? = null,
    val revision: Long = 0L,
    /** Only user-driven rule changes interrupt an active TTS queue. */
    val ttsRevision: Long = 0L,
    val actionMessage: String? = null,
) {
    val availableSharedRules: List<ReaderReplacementRule>
        get() = (sharedRules as? LoadResult.Success)?.value.orEmpty()
    val sharedRulesEnabled: Boolean
        get() = readerSharedRulesEnabled(defaultSharedRulesEnabled, sharedRulesEnabledOverride)
}

internal fun readerSharedRulesEnabled(
    defaultEnabled: Boolean,
    bookOverride: Boolean?,
): Boolean = bookOverride ?: defaultEnabled

/** Copy for compact settings cards; it must describe the effective source mode, not availability. */
internal fun readerReplacementModeSummary(state: ReaderReplacementState): String {
    val personalCount = state.personalRules.size
    val sharedCount = state.availableSharedRules.size
    val enabledSharedCount = state.availableSharedRules.count { it.id !in state.hiddenSharedRuleIds }
    return if (state.sharedRulesEnabled) {
        "我的 $personalCount 条 · 已启用公共 $enabledSharedCount 条"
    } else {
        "仅我的 $personalCount 条 · 公共 $sharedCount 条可选"
    }
}

internal fun readerReplacementModeTag(state: ReaderReplacementState): String =
    if (state.sharedRulesEnabled) {
        "公共已启用 ${state.availableSharedRules.count { it.id !in state.hiddenSharedRuleIds }}"
    } else {
        "公共未应用"
    }

/** Immutable export text result; source image placeholders are preserved by the body transformer. */
internal data class ReaderDownloadReplacementText(
    val title: String,
    val body: String,
)

/**
 * Captures the rule collections and per-book public policy at the instant a native export starts.
 * This deliberately does not retain Compose state or a ViewModel reference, so a later edit cannot
 * make one download switch rules halfway through its chapters.
 */
internal data class ReaderDownloadReplacementSnapshot private constructor(
    private val applyRules: Boolean,
    private val state: ReaderReplacementState,
) {
    fun transform(
        chapterOrder: Int,
        title: String,
        body: String,
    ): ReaderDownloadReplacementText {
        if (!applyRules) return ReaderDownloadReplacementText(title = title, body = body)
        val titleRules = readerReplacementRulesForChapter(
            state = state,
            chapterOrder = chapterOrder,
            target = ReaderReplacementTarget.Title,
        )
        val bodyRules = readerReplacementRulesForChapter(
            state = state,
            chapterOrder = chapterOrder,
            target = ReaderReplacementTarget.Content,
        )
        return ReaderDownloadReplacementText(
            title = applyReaderReplacementRules(
                original = title,
                rules = titleRules,
                chapterOrder = chapterOrder,
                target = ReaderReplacementTarget.Title,
            ).text,
            body = applyReaderReplacementRulesToDownloadBody(
                original = body,
                rules = bodyRules,
                chapterOrder = chapterOrder,
            ).text,
        )
    }

    companion object {
        fun capture(applyRules: Boolean, state: ReaderReplacementState): ReaderDownloadReplacementSnapshot {
            val frozen = state.copy(
                personalRules = state.personalRules.toList(),
                sharedRules = LoadResult.Success(state.availableSharedRules.toList()),
                hiddenSharedRuleIds = state.hiddenSharedRuleIds.toSet(),
            )
            return ReaderDownloadReplacementSnapshot(applyRules = applyRules, state = frozen)
        }
    }
}

internal fun readerDownloadReplacementSnapshot(
    applyRules: Boolean,
    state: ReaderReplacementState,
): ReaderDownloadReplacementSnapshot = ReaderDownloadReplacementSnapshot.capture(applyRules, state)

/**
 * Remote glossary responses arrive after the chapter body. Advance the display revision whenever
 * they change either rule collection so Compose rebuilds the effective chapter text.
 */
internal fun readerReplacementRevisionAfterRulesLoad(
    current: ReaderReplacementState,
    personalRules: List<ReaderReplacementRule>,
    sharedRules: LoadResult<List<ReaderReplacementRule>>,
): Long = if (
    current.personalRules == personalRules && current.sharedRules == sharedRules
) {
    current.revision
} else {
    current.revision + 1L
}

/** Local edits must remain visible even when a previous remote load advanced only memory state. */
internal fun readerReplacementNextLocalRevision(
    currentRevision: Long,
    persistedRevision: Long,
): Long = maxOf(currentRevision + 1L, persistedRevision)

/** A chapter scope is only trustworthy once that chapter appears in the fetched directory. */
internal fun readerChapterOrderForId(
    chapterId: Long,
    chapters: List<Chapter>,
): Int? {
    val index = chapters.indexOfFirst { it.id == chapterId }
    if (index < 0) return null
    return chapters[index].number ?: index + 1
}

internal fun readerReplacementStateHasScopedRules(state: ReaderReplacementState): Boolean =
    state.personalRules.any { rule ->
        rule.isEnabled && rule.scope != ReaderReplacementScope.WholeBook
    } || state.availableSharedRules.any { rule ->
        rule.isEnabled &&
            rule.id !in state.hiddenSharedRuleIds &&
            rule.scope != ReaderReplacementScope.WholeBook
    }

internal fun readerReplacementRulesForChapter(
    state: ReaderReplacementState,
    chapterOrder: Int?,
    target: ReaderReplacementTarget,
): List<ReaderReplacementRule> = effectiveReaderReplacementRules(
    // List navigation and render policy are separate. Users may inspect the entire public glossary
    // without unexpectedly changing the current chapter; only the explicit per-book policy applies
    // shared rules to reader text/TTS/download snapshots.
    sharedRules = if (state.sharedRulesEnabled) {
        state.availableSharedRules
    } else {
        emptyList()
    },
    personalRules = state.personalRules,
    hiddenSharedRuleIds = state.hiddenSharedRuleIds,
    chapterOrder = chapterOrder,
    target = target,
)

/**
 * The website's “全部规则” list is a reading view, not a shared-only list: it contains the
 * reader's own rules plus the other available rules.  Keep the personal override visible and
 * suppress the duplicate shared source so what the user sees matches what will be rendered.
 */
internal fun readerReplacementRulesForDisplay(
    source: ReaderReplacementRuleSource,
    personalRules: List<ReaderReplacementRule>,
    sharedRules: List<ReaderReplacementRule>,
): List<ReaderReplacementRule> = when (source) {
    ReaderReplacementRuleSource.Personal -> personalRules.sortedWith(readerReplacementRuleComparator)
    ReaderReplacementRuleSource.All -> {
        val personalSources = personalRules.map { it.source.trim() }.toSet()
        sharedRules
            .filterNot { it.source.trim() in personalSources }
            .sortedWith(readerReplacementRuleComparator) +
            personalRules.sortedWith(readerReplacementRuleComparator)
    }
}

internal fun readerReplacementSaveSyncAction(
    previous: ReaderReplacementRule?,
    saved: ReaderReplacementRule,
): ReaderReplacementRemoteSyncAction {
    val previousServerRuleId = previous?.let(::readerReplacementPersonalServerRuleId)
    if (!saved.canSyncReaderReplacementToWebsite()) {
        return previousServerRuleId?.let(ReaderReplacementRemoteSyncAction::Delete)
            ?: ReaderReplacementRemoteSyncAction.None
    }
    if (previousServerRuleId == null) return ReaderReplacementRemoteSyncAction.Create(saved)

    return if (previous.hasSameWebsiteReplacementSource(saved)) {
        ReaderReplacementRemoteSyncAction.Update(
            serverRuleId = previousServerRuleId,
            replacement = saved.replacement,
        )
    } else {
        ReaderReplacementRemoteSyncAction.Replace(
            serverRuleId = previousServerRuleId,
            rule = saved,
        )
    }
}

internal fun readerReplacementDeleteSyncAction(
    rule: ReaderReplacementRule,
): ReaderReplacementRemoteSyncAction = readerReplacementPersonalServerRuleId(rule)
    ?.let(ReaderReplacementRemoteSyncAction::Delete)
    ?: ReaderReplacementRemoteSyncAction.None

internal fun validateReaderReplacementRule(rule: ReaderReplacementRule): ReaderReplacementValidation {
    val source = rule.source.trim()
    if (source.isEmpty()) return ReaderReplacementValidation(false, "替换前不能为空")
    if (source.startsWith("@js:", ignoreCase = true)) {
        return ReaderReplacementValidation(false, "替换规则不支持脚本执行")
    }
    when (val scope = rule.scope) {
        ReaderReplacementScope.WholeBook -> Unit
        is ReaderReplacementScope.CurrentChapter -> {
            if (scope.chapterOrder < 1) return ReaderReplacementValidation(false, "章节范围无效")
        }
        is ReaderReplacementScope.ChapterRange -> {
            if (scope.startOrder < 1 || scope.endOrder < scope.startOrder) {
                return ReaderReplacementValidation(false, "章节范围无效")
            }
        }
    }
    if (!rule.isRegex) return ReaderReplacementValidation(true)
    if (source.length > MAX_REPLACEMENT_REGEX_LENGTH) {
        return ReaderReplacementValidation(false, "正则表达式过长")
    }
    return try {
        Pattern.compile(source, regexFlagsToPatternFlags(rule.regexFlags))
        ReaderReplacementValidation(true)
    } catch (_: PatternSyntaxException) {
        ReaderReplacementValidation(false, "正则表达式格式无效")
    }
}

internal fun effectiveReaderReplacementRules(
    sharedRules: List<ReaderReplacementRule>,
    personalRules: List<ReaderReplacementRule>,
    hiddenSharedRuleIds: Set<String>,
    chapterOrder: Int?,
    target: ReaderReplacementTarget,
): List<ReaderReplacementRule> {
    val activePersonal = personalRules.filter { rule ->
        rule.isEnabled && ruleAppliesTo(rule, chapterOrder, target) && validateReaderReplacementRule(rule).isValid
    }
    val personalSources = activePersonal.map { it.source.trim() }.toSet()
    val activeShared = sharedRules.filter { rule ->
        rule.isEnabled &&
            rule.id !in hiddenSharedRuleIds &&
            rule.source.trim() !in personalSources &&
            ruleAppliesTo(rule, chapterOrder, target) &&
            validateReaderReplacementRule(rule).isValid
    }
    return activeShared.sortedWith(readerReplacementRuleComparator) +
        activePersonal.sortedWith(readerReplacementRuleComparator)
}

internal fun applyReaderReplacementRules(
    original: String,
    rules: List<ReaderReplacementRule>,
    chapterOrder: Int? = 1,
    target: ReaderReplacementTarget = ReaderReplacementTarget.Content,
): ReaderReplacementApplyResult {
    var transformed = original
    val invalidRuleIds = mutableListOf<String>()
    rules.forEach { rule ->
        if (!rule.isEnabled || !ruleAppliesTo(rule, chapterOrder, target)) return@forEach
        val validation = validateReaderReplacementRule(rule)
        if (!validation.isValid) {
            invalidRuleIds += rule.id
            return@forEach
        }
        transformed = if (rule.isRegex) {
            runCatching {
                Pattern.compile(rule.source.trim(), regexFlagsToPatternFlags(rule.regexFlags))
                    .matcher(transformed)
                    .replaceAll(rule.replacement)
            }.getOrElse {
                invalidRuleIds += rule.id
                transformed
            }
        } else {
            transformed.replace(rule.source, rule.replacement, ignoreCase = false)
        }
    }
    return ReaderReplacementApplyResult(
        text = transformed,
        invalidRuleIds = invalidRuleIds.distinct(),
    )
}

/**
 * Native source exports keep image placeholders inside the chapter body. Apply replacements only
 * to prose spans so a user rule can never rewrite an image URL/marker and silently drop artwork
 * from the generated EPUB or TXT.
 */
internal fun applyReaderReplacementRulesToDownloadBody(
    original: String,
    rules: List<ReaderReplacementRule>,
    chapterOrder: Int?,
): ReaderReplacementApplyResult {
    val output = StringBuilder(original.length)
    val invalidRuleIds = mutableListOf<String>()
    var cursor = 0
    readerDownloadImageMarker.findAll(original).forEach { marker ->
        val prose = original.substring(cursor, marker.range.first)
        val transformed = applyReaderReplacementRules(
            original = prose,
            rules = rules,
            chapterOrder = chapterOrder,
            target = ReaderReplacementTarget.Content,
        )
        output.append(transformed.text)
        invalidRuleIds += transformed.invalidRuleIds
        output.append(marker.value)
        cursor = marker.range.last + 1
    }
    val tail = applyReaderReplacementRules(
        original = original.substring(cursor),
        rules = rules,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Content,
    )
    output.append(tail.text)
    invalidRuleIds += tail.invalidRuleIds
    return ReaderReplacementApplyResult(output.toString(), invalidRuleIds.distinct())
}

internal fun effectiveReaderText(
    title: String,
    content: String,
    rules: List<ReaderReplacementRule>,
    chapterOrder: Int?,
    revision: Long,
): EffectiveReaderText {
    val transformedTitle = applyReaderReplacementRules(
        original = title,
        rules = rules,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Title,
    )
    val transformedContent = applyReaderReplacementRules(
        original = content,
        rules = rules,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Content,
    )
    return EffectiveReaderText(
        title = transformedTitle.text,
        content = transformedContent.text,
        ttsContent = transformedContent.text,
        revision = revision,
        invalidRuleIds = (transformedTitle.invalidRuleIds + transformedContent.invalidRuleIds).distinct(),
    )
}

internal fun effectiveReaderChapterContent(
    chapter: ReaderChapterContent,
    chapterOrder: Int?,
    replacementState: ReaderReplacementState,
): ReaderChapterContent {
    val titleRules = readerReplacementRulesForChapter(
        state = replacementState,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Title,
    )
    val contentRules = readerReplacementRulesForChapter(
        state = replacementState,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Content,
    )
    val titleResult = applyReaderReplacementRules(
        original = chapter.title ?: chapter.content.title.orEmpty(),
        rules = titleRules,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Title,
    )
    val contentResult = applyReaderReplacementRules(
        original = chapter.content.content,
        rules = contentRules,
        chapterOrder = chapterOrder,
        target = ReaderReplacementTarget.Content,
    )
    return chapter.copy(
        title = titleResult.text,
        content = chapter.content.copy(
            title = titleResult.text,
            content = contentResult.text,
        ),
    )
}

internal fun cloneSharedReaderReplacementRule(
    sharedRule: ReaderReplacementRule,
    newId: String,
): ReaderReplacementRule = sharedRule.copy(
    id = newId,
    owner = ReaderReplacementOwner.Personal,
    sharedRuleId = sharedRule.id,
    websiteRuleId = null,
    order = 0,
)

internal fun selectedTextReplacementSource(value: String): String = value.trim()

internal fun readerReplacementSourceFromClipboard(value: CharSequence?): String =
    selectedTextReplacementSource(value?.toString().orEmpty())

/**
 * A reader-scoped clipboard listener must not open the rule editor for unrelated copies.  Accept
 * only text that is actually present in the effective visible chapter window (including titles).
 */
internal fun readerReplacementPrefillSource(
    clipboardText: CharSequence?,
    contents: List<ReaderChapterContent>,
): String? {
    val source = readerReplacementSourceFromClipboard(clipboardText)
    if (source.isBlank()) return null
    val belongsToReader = contents.any { chapter ->
        chapter.title.orEmpty().contains(source) ||
            chapter.content.title.orEmpty().contains(source) ||
            // The effective source retains all display text even when its renderer later decodes
            // HTML/Markdown. A raw containment check keeps this helper platform-independent and
            // avoids treating unrelated clipboard content as a chapter selection.
            chapter.content.content.contains(source)
    }
    return source.takeIf { belongsToReader }
}

/** Returns a valid editor scope, keeping malformed range input out of persisted rule data. */
internal fun readerReplacementScopeForEditor(
    selectedScope: ReaderReplacementScope,
    rangeStart: String,
    rangeEnd: String,
): ReaderReplacementScope? = when (selectedScope) {
    ReaderReplacementScope.WholeBook,
    is ReaderReplacementScope.CurrentChapter,
    -> selectedScope
    is ReaderReplacementScope.ChapterRange -> {
        val startOrder = rangeStart.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        val endOrder = rangeEnd.trim().toIntOrNull()?.takeIf { it >= startOrder } ?: return null
        ReaderReplacementScope.ChapterRange(startOrder, endOrder)
    }
}

/**
 * The website only stores source/replacement pairs. Keep a richer local rule authoritative when
 * both stores name the same source, then append remote-only entries so another device's simple
 * glossary changes are still immediately useful.
 */
internal fun mergeReaderReplacementPersonalRules(
    localRules: List<ReaderReplacementRule>,
    remoteRules: List<ReaderReplacementRule>,
): List<ReaderReplacementRule> {
    val remoteBySource = remoteRules.associateBy { it.source.trim() }
    val enrichedLocalRules = localRules.map { local ->
        val remote = remoteBySource[local.source.trim()]
        if (
            local.websiteRuleId == null &&
            remote?.websiteRuleId != null &&
            local.canSyncReaderReplacementToWebsite() &&
            local.hasSameWebsiteReplacementSource(remote)
        ) {
            local.copy(
                websiteRuleId = remote.websiteRuleId,
                createdAt = remote.createdAt ?: local.createdAt,
                updatedAt = remote.updatedAt ?: local.updatedAt,
            )
        } else {
            local
        }
    }
    val localIds = enrichedLocalRules.map(ReaderReplacementRule::id).toSet()
    val localSources = enrichedLocalRules.map { it.source.trim() }.toSet()
    return enrichedLocalRules + remoteRules.filter { remote ->
        remote.id !in localIds && remote.source.trim() !in localSources
    }
}

private val readerReplacementRuleComparator = compareBy<ReaderReplacementRule> { it.order }
    .thenByDescending { it.source.length }
    .thenBy { it.id }

private fun ruleAppliesTo(
    rule: ReaderReplacementRule,
    chapterOrder: Int?,
    target: ReaderReplacementTarget,
): Boolean {
    val targetMatches = rule.target == ReaderReplacementTarget.Both || rule.target == target
    if (!targetMatches) return false
    return when (val scope = rule.scope) {
        ReaderReplacementScope.WholeBook -> true
        is ReaderReplacementScope.CurrentChapter -> chapterOrder == scope.chapterOrder
        is ReaderReplacementScope.ChapterRange -> chapterOrder != null &&
            chapterOrder in scope.startOrder..scope.endOrder
    }
}

private fun regexFlagsToPatternFlags(flags: Set<ReaderReplacementRegexFlag>): Int = buildList {
    if (ReaderReplacementRegexFlag.IgnoreCase in flags) add(Pattern.CASE_INSENSITIVE)
    if (ReaderReplacementRegexFlag.Multiline in flags) add(Pattern.MULTILINE)
    if (ReaderReplacementRegexFlag.DotMatchesAll in flags) add(Pattern.DOTALL)
}.fold(0) { accumulator, flag -> accumulator or flag }

private val readerDownloadImageMarker = Regex("\\[图片(?:[:：]|\\s)*?.*?\\]")

private fun ReaderReplacementRule.canSyncReaderReplacementToWebsite(): Boolean =
    owner == ReaderReplacementOwner.Personal &&
        isEnabled &&
        // The webpage applies its glossary during body rendering. Title/both rules are useful
        // native extensions, but must stay local instead of being misrepresented remotely.
        target == ReaderReplacementTarget.Content &&
        scope == ReaderReplacementScope.WholeBook

private fun ReaderReplacementRule.hasSameWebsiteReplacementSource(
    other: ReaderReplacementRule,
): Boolean = source == other.source &&
    isRegex == other.isRegex &&
    regexFlags == other.regexFlags

private fun readerReplacementPersonalServerRuleId(rule: ReaderReplacementRule): Long? =
    rule.websiteRuleId ?: rule.id.removePrefix("personal:")
        .takeIf { rule.id.startsWith("personal:") }
        ?.toLongOrNull()

private const val MAX_REPLACEMENT_REGEX_LENGTH = 512
