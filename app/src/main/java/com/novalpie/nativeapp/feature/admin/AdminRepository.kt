package com.novalpie.nativeapp.feature.admin

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*

/** Form drafts are in-memory only; never persist administrator cookies or print their values. */
sealed class AdminEditDraft {
    class Cookie(val config: AdminCookieConfig, val raw: String?) : AdminEditDraft()
    class Rule(val rule: AdminBaseUrlRule) : AdminEditDraft()
    class Shop(val item: AdminShopItem) : AdminEditDraft()
    final override fun toString(): String = "AdminEditDraft(redacted)"
}

internal sealed class AdminCommand(val section: AdminSection, val label: String, val successMessage: String) {
    class ReviewSettings(val upload: Boolean, val delete: Boolean) : AdminCommand(AdminSection.Review, "更新审核设置", "审核设置已更新")
    class Review(val id: Long, val action: String) : AdminCommand(AdminSection.Review, "处理审核请求", "审核请求已处理")
    class ReviewAll(val query: AdminReviewQuery) : AdminCommand(AdminSection.Review, "批量处理审核", "审核请求已批量处理")
    class KeyStatus(val id: Long, val status: String) : AdminCommand(AdminSection.Keys, "更新 Key 状态", "Key 状态已更新")
    class DeleteKey(val id: Long) : AdminCommand(AdminSection.Keys, "删除 Key", "Key 已删除")
    class SaveCookie(val config: AdminCookieConfig, val raw: String?, val fromEditor: Boolean = true) : AdminCommand(AdminSection.Scraper, "保存 Cookie 配置", "Cookie 配置已保存")
    class DeleteCookie(val id: Long) : AdminCommand(AdminSection.Scraper, "删除 Cookie 配置", "Cookie 配置已删除")
    class SaveRule(val rule: AdminBaseUrlRule, val fromEditor: Boolean = true) : AdminCommand(AdminSection.Keys, "保存 BaseURL 规则", "BaseURL 规则已保存")
    class DeleteRule(val id: Long) : AdminCommand(AdminSection.Keys, "删除 BaseURL 规则", "BaseURL 规则已删除")
    class SaveShop(val item: AdminShopItem, val fromEditor: Boolean = true) : AdminCommand(AdminSection.Shop, "保存商品", "商品已保存")
    class DeleteShop(val id: Long) : AdminCommand(AdminSection.Shop, "删除商品", "商品已删除")
    final override fun toString() = "AdminCommand(section=$section, payload=redacted)"

    fun editDraft(): AdminEditDraft? = when (this) {
        is SaveCookie -> if (fromEditor) AdminEditDraft.Cookie(config, raw) else null
        is SaveRule -> if (fromEditor) AdminEditDraft.Rule(rule) else null
        is SaveShop -> if (fromEditor) AdminEditDraft.Shop(item) else null
        else -> null
    }
}

internal interface AdminRepository {
    suspend fun overview(days: Int): AdminOverviewStats
    suspend fun reviewSettings(): AdminReviewSettings
    suspend fun reviews(query: AdminReviewQuery): List<AdminReviewRequest>
    suspend fun keys(): List<AdminKeyItem>
    suspend fun rules(): List<AdminBaseUrlRule>
    suspend fun logs(query: AdminOperationLogQuery): AdminOperationLogPage
    suspend fun cookies(): List<AdminCookieConfig>
    suspend fun scheduler(lines: Int): AdminSchedulerLogs
    suspend fun shop(query: AdminShopQuery): List<AdminShopItem>
    suspend fun mutate(command: AdminCommand): UserCheckinAction
}

internal class WebsiteAdminRepository(private val api: NovalPieApi) : AdminRepository {
    override suspend fun overview(days: Int) = api.adminOverview(days)
    override suspend fun reviewSettings() = api.adminReviewSettings()
    override suspend fun reviews(query: AdminReviewQuery) = api.adminReviewRequests(query.type.trim(), query.status.trim(), query.keyword.trim())
    override suspend fun keys() = api.adminKeys()
    override suspend fun rules() = api.adminBaseUrlRules()
    override suspend fun logs(query: AdminOperationLogQuery) = api.adminOperationLogs(page = query.page, action = query.action.trim(), status = query.status.trim(),
        userId = query.userId.trim(), novelId = query.novelId.trim(), keyword = query.keyword.trim(), startDate = query.startDate.trim(), endDate = query.endDate.trim())
    override suspend fun cookies() = api.adminCookieConfigs()
    override suspend fun scheduler(lines: Int) = api.adminSchedulerLogs(lines)
    override suspend fun shop(query: AdminShopQuery) = api.adminShopItems(type = query.type.trim(), active = query.isActive, keyword = query.keyword.trim())
    override suspend fun mutate(command: AdminCommand): UserCheckinAction = when (command) {
        is AdminCommand.ReviewSettings -> api.adminUpdateReviewSettings(command.upload, command.delete)
        is AdminCommand.Review -> api.adminReviewAction(command.id, command.action)
        is AdminCommand.ReviewAll -> api.adminApproveAllReviews(command.query.type.trim(), command.query.status.trim(), command.query.keyword.trim())
        is AdminCommand.KeyStatus -> api.adminUpdateKeyStatus(command.id, command.status)
        is AdminCommand.DeleteKey -> api.adminDeleteKey(command.id)
        is AdminCommand.SaveCookie -> api.adminSaveCookieConfig(command.config, command.raw)
        is AdminCommand.DeleteCookie -> api.adminDeleteCookieConfig(command.id)
        is AdminCommand.SaveRule -> api.adminSaveBaseUrlRule(command.rule)
        is AdminCommand.DeleteRule -> api.adminDeleteBaseUrlRule(command.id)
        is AdminCommand.SaveShop -> api.adminSaveShopItem(command.item)
        is AdminCommand.DeleteShop -> api.adminDeleteShopItem(command.id)
    }
}
