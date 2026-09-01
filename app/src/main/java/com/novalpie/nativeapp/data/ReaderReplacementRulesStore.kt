package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.ReaderReplacementOwner
import com.novalpie.nativeapp.model.ReaderReplacementRegexFlag
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementScope
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists local reader-rule controls only. The source chapter and shared glossary stay remote;
 * this store holds a reader's personal overrides and per-book visibility choices.
 */
class ReaderReplacementRulesStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadPersonalRules(novelId: Long): List<ReaderReplacementRule> =
        ArrayList(decodeRules(preferences.getString(personalRulesKey(novelId), null), novelId))

    fun savePersonalRules(novelId: Long, rules: List<ReaderReplacementRule>) {
        preferences.edit()
            .putString(personalRulesKey(novelId), encodeRules(rules.map { it.copy(novelId = novelId) }))
            .putLong(revisionKey(novelId), revision(novelId) + 1L)
            .apply()
    }

    fun loadHiddenSharedRuleIds(novelId: Long): Set<String> =
        decodeStringSet(preferences.getString(hiddenSharedRulesKey(novelId), null))

    fun saveHiddenSharedRuleIds(novelId: Long, ruleIds: Set<String>) {
        preferences.edit()
            .putString(hiddenSharedRulesKey(novelId), JSONArray(ruleIds.sorted()).toString())
            .putLong(revisionKey(novelId), revision(novelId) + 1L)
            .apply()
    }

    /** Safe default: public rules stay visible but never alter a new book until explicitly enabled. */
    fun loadDefaultSharedRulesEnabled(): Boolean =
        preferences.getBoolean(DEFAULT_SHARED_RULES_ENABLED_KEY, false)

    fun saveDefaultSharedRulesEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(DEFAULT_SHARED_RULES_ENABLED_KEY, enabled).apply()
    }

    /** Null means the book follows the device default; false and true are intentional overrides. */
    fun loadSharedRulesEnabledOverride(novelId: Long): Boolean? {
        val key = sharedRulesEnabledOverrideKey(novelId)
        return if (preferences.contains(key)) preferences.getBoolean(key, false) else null
    }

    fun saveSharedRulesEnabledOverride(novelId: Long, enabled: Boolean?) {
        val key = sharedRulesEnabledOverrideKey(novelId)
        preferences.edit().apply {
            if (enabled == null) remove(key) else putBoolean(key, enabled)
            putLong(revisionKey(novelId), revision(novelId) + 1L)
        }.apply()
    }

    fun revision(novelId: Long): Long = preferences.getLong(revisionKey(novelId), 0L)

    private fun decodeRules(raw: String?, novelId: Long): List<ReaderReplacementRule> = runCatching {
        val values = JSONArray(raw ?: "[]")
        buildList {
            for (index in 0 until values.length()) {
                values.optJSONObject(index)?.let { rule ->
                    decodeRule(rule, novelId)?.let(::add)
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun decodeRule(value: JSONObject, novelId: Long): ReaderReplacementRule? {
        val id = value.optString("id").trim()
        val source = value.optString("source")
        if (id.isBlank() || source.isBlank()) return null
        return ReaderReplacementRule(
            id = id,
            novelId = novelId,
            source = source,
            replacement = value.optString("replacement"),
            owner = ReaderReplacementOwner.Personal,
            sharedRuleId = value.optString("shared_rule_id").takeIf(String::isNotBlank),
            websiteRuleId = value.takeLongOrNull("website_rule_id"),
            isRegex = value.optBoolean("is_regex", false),
            regexFlags = decodeRegexFlags(value.optJSONArray("regex_flags")),
            isEnabled = value.optBoolean("is_enabled", true),
            order = value.optInt("order", 0),
            target = when (value.optString("target")) {
                "title" -> ReaderReplacementTarget.Title
                "both" -> ReaderReplacementTarget.Both
                else -> ReaderReplacementTarget.Content
            },
            scope = decodeScope(value.optJSONObject("scope")),
            createdAt = value.optString("created_at").takeIf(String::isNotBlank),
            updatedAt = value.optString("updated_at").takeIf(String::isNotBlank),
        )
    }

    private fun encodeRules(rules: List<ReaderReplacementRule>): String = JSONArray().apply {
        rules.forEach { rule ->
            put(
                JSONObject()
                    .put("id", rule.id)
                    .put("source", rule.source)
                    .put("replacement", rule.replacement)
                    .put("shared_rule_id", rule.sharedRuleId ?: JSONObject.NULL)
                    .put("website_rule_id", rule.websiteRuleId ?: JSONObject.NULL)
                    .put("is_regex", rule.isRegex)
                    .put("regex_flags", JSONArray(rule.regexFlags.map(ReaderReplacementRegexFlag::name).sorted()))
                    .put("is_enabled", rule.isEnabled)
                    .put("order", rule.order)
                    .put("target", rule.target.name.lowercase())
                    .put("scope", encodeScope(rule.scope))
                    .put("created_at", rule.createdAt ?: JSONObject.NULL)
                    .put("updated_at", rule.updatedAt ?: JSONObject.NULL),
            )
        }
    }.toString()

    private fun encodeScope(scope: ReaderReplacementScope): JSONObject = when (scope) {
        ReaderReplacementScope.WholeBook -> JSONObject().put("type", "whole_book")
        is ReaderReplacementScope.CurrentChapter -> JSONObject()
            .put("type", "current_chapter")
            .put("chapter_order", scope.chapterOrder)
        is ReaderReplacementScope.ChapterRange -> JSONObject()
            .put("type", "chapter_range")
            .put("start_order", scope.startOrder)
            .put("end_order", scope.endOrder)
    }

    private fun decodeScope(value: JSONObject?): ReaderReplacementScope = when (value?.optString("type")) {
        "current_chapter" -> ReaderReplacementScope.CurrentChapter(
            value.optInt("chapter_order", 1).coerceAtLeast(1),
        )
        "chapter_range" -> {
            val start = value.optInt("start_order", 1).coerceAtLeast(1)
            ReaderReplacementScope.ChapterRange(
                startOrder = start,
                endOrder = value.optInt("end_order", start).coerceAtLeast(start),
            )
        }
        else -> ReaderReplacementScope.WholeBook
    }

    private fun decodeRegexFlags(values: JSONArray?): Set<ReaderReplacementRegexFlag> = buildSet {
        if (values == null) return@buildSet
        for (index in 0 until values.length()) {
            runCatching { ReaderReplacementRegexFlag.valueOf(values.optString(index)) }
                .getOrNull()
                ?.let(::add)
        }
    }

    private fun decodeStringSet(raw: String?): Set<String> = runCatching {
        val values = JSONArray(raw ?: "[]")
        buildSet {
            for (index in 0 until values.length()) {
                values.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }.getOrDefault(emptySet())

    private fun JSONObject.takeLongOrNull(key: String): Long? = when (val value = opt(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun personalRulesKey(novelId: Long): String = "personal_rules_$novelId"
    private fun hiddenSharedRulesKey(novelId: Long): String = "hidden_shared_rules_$novelId"
    private fun sharedRulesEnabledOverrideKey(novelId: Long): String = "shared_rules_enabled_override_$novelId"
    private fun revisionKey(novelId: Long): String = "revision_$novelId"

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_reader_replacement_rules"
        private const val DEFAULT_SHARED_RULES_ENABLED_KEY = "default_shared_rules_enabled"
    }
}
