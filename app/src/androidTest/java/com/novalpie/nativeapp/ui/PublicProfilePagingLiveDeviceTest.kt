package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.feature.profile.*
import com.novalpie.nativeapp.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Read-only source data with a small server page size so the actual paging control is exercised. */
class PublicProfilePagingLiveDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    @Test fun loadOlderButtonReadsTheNextSourcePageAndKeepsEarlierActivities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = PublicProfileViewModel(WebsitePublicProfileRepository(AppContainer.from(context).api, activityPageSize = 2))
        try {
            compose.setContent { MaterialTheme {
                UserProfileDetailScreen(model.state, true, { model.enter(100002, refresh = true) },
                    { model.retry(PublicUserProfilePanel.Activities) }, { model.retry(PublicUserProfilePanel.Books) },
                    { model.retry(PublicUserProfilePanel.CheckinStats) }, { model.retry(PublicUserProfilePanel.CheckinRecords) },
                    { model.retry(PublicUserProfilePanel.CheckinSettings) },
                    { tab -> model.present { it.copy(selectedTab = tab) } }, { filter -> model.present { it.copy(activityFilter = filter) } },
                    {}, {}, { _, _ -> }, {}, currentUserId = 100164,
                    onLoadMoreActivities = model::loadMoreActivities)
            } }
            compose.runOnIdle { model.enter(100002) }
            compose.waitUntil(60000) { model.state.activities is LoadResult.Success && model.state.activityPage == 1 }
            val firstIds = (model.state.activities as LoadResult.Success).value.map { "${it.type}:${it.id}" }.toSet()
            assertTrue(model.state.activityFeed?.hasMore == true)
            compose.onNodeWithTag("public-profile-list").performScrollToNode(hasText("加载更早动态"))
            compose.onNodeWithText("加载更早动态").performClick()
            compose.waitUntil(60000) { model.state.activityPage >= 2 }
            val all = (model.state.activities as LoadResult.Success).value.map { "${it.type}:${it.id}" }.toSet()
            assertTrue(all.containsAll(firstIds))
            assertTrue(all.size > firstIds.size)
            val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
            File(folder, "public-activity-paging.json").writeText(JSONObject().put("userId", 100002).put("serverPageSize", 2)
                .put("page", model.state.activityPage).put("beforeCount", firstIds.size).put("afterCount", all.size).put("keptEarlier", true).toString(2))
        } finally { compose.runOnIdle { model.close() } }
    }
}
