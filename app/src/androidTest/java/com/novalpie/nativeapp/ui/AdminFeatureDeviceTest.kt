package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.feature.admin.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real admin forms/confirmation, synthetic repository only; never changes real administrator data. */
class AdminFeatureDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    @Test fun rejectedSaveAndCancelledConfirmationKeepTheFormThenConfirmedRetryWorks() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val calls = mutableListOf<AdminCommand>()
        val original = AdminShopItem(900011, "合成商品", type = "frame")
        val repository = object : AdminRepository {
            override suspend fun overview(days: Int) = AdminOverviewStats()
            override suspend fun reviewSettings() = AdminReviewSettings()
            override suspend fun reviews(query: AdminReviewQuery) = emptyList<AdminReviewRequest>()
            override suspend fun keys() = emptyList<AdminKeyItem>()
            override suspend fun rules() = emptyList<AdminBaseUrlRule>()
            override suspend fun logs(query: AdminOperationLogQuery) = AdminOperationLogPage()
            override suspend fun cookies() = emptyList<AdminCookieConfig>()
            override suspend fun scheduler(lines: Int) = AdminSchedulerLogs()
            override suspend fun shop(query: AdminShopQuery) = listOf(original)
            override suspend fun mutate(command: AdminCommand): UserCheckinAction {
                calls += command
                return if (calls.size == 1) UserCheckinAction(false, "受控拒绝，草稿保留") else UserCheckinAction(true, "受控保存完成")
            }
        }
        val model = AdminViewModel(repository, { true }, scope)
        try {
            compose.runOnIdle { model.load(AdminSection.Shop) }
            compose.setContent { MaterialTheme { AdminScreen(
                state = model.state, onRefresh = { model.load() }, onSectionSelected = model::load,
                onOverviewDaysChange = {}, onReviewQueryChange = {}, onApplyReviewQuery = {}, onResetReviewQuery = {}, onApproveAllReviews = {},
                onOperationLogQueryChange = {}, onApplyOperationLogQuery = {}, onResetOperationLogQuery = {}, onOperationLogPageChange = {},
                onShopQueryChange = {}, onApplyShopQuery = {}, onResetShopQuery = {}, onToggleReviewSetting = {}, onReviewAction = { _, _ -> },
                onUpdateKeyStatus = { _, _ -> }, onDeleteKey = {}, onSaveCookie = { _, _ -> }, onToggleCookie = {}, onDeleteCookie = {},
                onSaveRule = {}, onSetRuleAction = { _, _ -> }, onDeleteRule = {}, onSaveShopItem = { model.mutate(AdminCommand.SaveShop(it)) },
                onToggleShopItem = {}, onDeleteShopItem = {}) } }
            compose.onNodeWithTag("admin-scroll").performScrollToNode(hasText("编辑"))
            compose.onNodeWithText("编辑").performClick()
            compose.onNode(hasText("名称") and hasSetTextAction()).performTextReplacement("没有丢失的新名称")
            compose.onNodeWithText("保存").performClick()
            compose.onNodeWithText("取消").performClick() // Returns to the edited form, not a lost draft.
            compose.onNodeWithText("没有丢失的新名称").assertExists()
            compose.runOnIdle { assertTrue(calls.isEmpty()) }
            compose.onNodeWithText("保存").performClick()
            compose.onNodeWithText("确认").performClick()
            compose.waitUntil(5000) { model.state.actionError && !model.state.actionLoading }
            compose.onNodeWithTag("admin-scroll").performScrollToNode(hasText("继续编辑未保存的内容"))
            compose.onNodeWithText("受控拒绝，草稿保留").assertIsDisplayed()
            compose.onNodeWithText("继续编辑未保存的内容").performClick()
            compose.onNodeWithText("没有丢失的新名称").assertExists()
            compose.onNodeWithText("保存").performClick()
            compose.onNodeWithText("确认").performClick()
            compose.waitUntil(5000) { model.state.actionMessage == "受控保存完成" }
            compose.runOnIdle {
                assertEquals(2, calls.size)
                assertEquals("没有丢失的新名称", (calls.last() as AdminCommand.SaveShop).item.name)
                assertFalse(model.state.actionError); assertNull(model.state.failedEditDraft)
            }
        } finally { model.close(); scope.cancel() }
    }
}
