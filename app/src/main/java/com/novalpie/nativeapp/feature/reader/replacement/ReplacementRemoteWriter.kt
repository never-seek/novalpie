package com.novalpie.nativeapp.feature.reader.replacement

import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.ui.ReaderReplacementRemoteSyncAction
import com.novalpie.nativeapp.ui.readerReplacementSourceIdentity
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.encodeWebsiteReaderReplacementSource

internal interface ReplacementRemoteRepository {
    suspend fun personal(bookId: Long): List<ReaderReplacementRule>
    suspend fun create(rule: ReaderReplacementRule): ReaderReplacementRule
    suspend fun update(id: Long, replacement: String): ReaderReplacementRule
    suspend fun delete(id: Long)
}

internal class WebsiteReplacementRemoteRepository(private val api: NovalPieApi) : ReplacementRemoteRepository {
    override suspend fun personal(bookId: Long) = api.personalGlossaries(bookId)
    override suspend fun create(rule: ReaderReplacementRule) = api.createPersonalGlossary(rule)
    override suspend fun update(id: Long, replacement: String) = api.updatePersonalGlossary(id, replacement)
    override suspend fun delete(id: Long) { api.deletePersonalGlossary(id) }
}

/** Create and checkpoint a changed source before deleting the previous owned contribution. */
internal class ReplacementRemoteWriter(private val repository: ReplacementRemoteRepository) {
    suspend fun execute(action: ReaderReplacementRemoteSyncAction, checkpoint: (ReaderReplacementRule) -> Unit): ReaderReplacementRule? {
        val result = when (action) {
            ReaderReplacementRemoteSyncAction.None -> null
            is ReaderReplacementRemoteSyncAction.Create -> createOrReconcile(action.rule)
            is ReaderReplacementRemoteSyncAction.Update -> repository.update(action.serverRuleId, action.replacement).also {
                check(it.websiteRuleId == action.serverRuleId && it.replacement == action.replacement) {
                    "服务器返回的修改结果与提交不一致，请刷新确认"
                }
            }
            is ReaderReplacementRemoteSyncAction.Replace -> {
                val created = createOrReconcile(action.rule)
                val newId = created.websiteRuleId ?: error("新规则未返回ID，旧公共规则未删除")
                check(newId != action.serverRuleId) { "服务器未生成新规则，旧公共规则未删除" }
                val pending = created.copy(websiteCleanupRuleId = action.serverRuleId)
                checkpoint(pending)
                repository.delete(action.serverRuleId)
                created.copy(websiteCleanupRuleId = null)
            }
            is ReaderReplacementRemoteSyncAction.Delete -> { repository.delete(action.serverRuleId); null }
        }
        if (result != null) checkpoint(result)
        return result
    }

    suspend fun cleanup(rule: ReaderReplacementRule, checkpoint: (ReaderReplacementRule) -> Unit): ReaderReplacementRule {
        val id = rule.websiteCleanupRuleId ?: return rule
        check(id != rule.websiteRuleId) { "不能删除当前生效的规则" }
        repository.delete(id)
        return rule.copy(websiteCleanupRuleId = null).also(checkpoint)
    }

    private suspend fun createOrReconcile(rule: ReaderReplacementRule): ReaderReplacementRule {
        val identity = readerReplacementSourceIdentity(rule)
        val existing = repository.personal(rule.novelId).filter { readerReplacementSourceIdentity(it) == identity }
        val identical = existing.firstOrNull { it.replacement == rule.replacement }
        if (identical != null) return identical
        check(existing.isEmpty()) { "网站已有同原文的个人规则，请编辑已有规则；没有重复发布" }
        return repository.create(rule).also {
            check(it.websiteRuleId != null && readerReplacementSourceIdentity(it) == identity && it.replacement == rule.replacement) {
                "服务器返回的规则与提交内容不一致，请刷新确认；未删除旧规则"
            }
        }
    }
}

/** Reconcile storage independently of the currently visible book; later local edits remain intact. */
internal fun bindReaderReplacementAcknowledgement(
    current: List<ReaderReplacementRule>, saved: ReaderReplacementRule, remote: ReaderReplacementRule,
): List<ReaderReplacementRule> = current.map { rule -> if (rule.id != saved.id) rule else rule.copy(
    websiteRuleId = remote.websiteRuleId ?: rule.websiteRuleId,
    websiteSource = remote.websiteSource ?: encodeWebsiteReaderReplacementSource(saved),
    websiteReplacement = remote.websiteReplacement ?: remote.replacement,
    websiteCleanupRuleId = remote.websiteCleanupRuleId,
    createdAt = remote.createdAt ?: rule.createdAt,
    updatedAt = remote.updatedAt ?: rule.updatedAt,
) }

internal fun readerReplacementSyncBaseline(previous: ReaderReplacementRule?, stored: ReaderReplacementRule?): ReaderReplacementRule? =
    if (stored?.websiteSource != null) stored else previous ?: stored
