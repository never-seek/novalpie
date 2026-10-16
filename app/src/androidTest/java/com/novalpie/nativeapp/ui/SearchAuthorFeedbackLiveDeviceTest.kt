package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Live read-only regression for feedback 2007; never clears the user's saved filters. */
class SearchAuthorFeedbackLiveDeviceTest {
    @Test fun koreanAuthorIsSearchableWithoutTheUsersSavedSourceOrTagRestrictions() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveNovalPie") == "1")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val api = AppContainer.from(context).also { it.refreshEnvironmentFromStores() }.api
        val keyword = "싸늘한감자튀김"
        val books = api.search(keyword, scope = "author", adultFilter = "all", source = "", matchType = "fuzzy_strict")
        assertTrue("Current source returned no author results; inspect source availability before calling this an App defect", books.isNotEmpty())
        assertTrue(books.all { it.author?.contains(keyword) == true })
        val report = JSONObject().put("feedbackId", 2007).put("scope", "author").put("resultCount", books.size)
            .put("bookIds", JSONArray(books.map { it.id })).put("userSettingsChanged", false)
        File(context.getExternalFilesDir(null), "author-feedback-verified.json").writeText(report.toString(2))
    }
}
