package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import com.novalpie.nativeapp.model.WorkspaceTranslationJob
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceLocalStoreTest {
    private lateinit var store: WorkspaceLocalStore

    @Before
    fun setUp() {
        store = WorkspaceLocalStore(ApplicationProvider.getApplicationContext())
        store.clearAll()
    }

    @Test
    fun apiConfigsRoundTripAndUpsertById() {
        store.upsertApi(
            WorkspaceLocalApiConfig(
                id = 1,
                name = "Local",
                model = "deepseek-chat",
                endpoint = "https://api.deepseek.com",
                apiKey = "sk-local",
                concurrency = 8
            )
        )
        store.upsertApi(
            WorkspaceLocalApiConfig(
                id = 1,
                name = "Local Updated",
                model = "deepseek-chat",
                endpoint = "https://api.deepseek.com/v1",
                apiKey = "sk-next",
                concurrency = 12,
                sharedToServer = true,
                serverId = 9
            )
        )

        assertEquals(1, store.loadApis().size)
        assertEquals("Local Updated", store.loadApis().single().name)
        assertEquals("sk-next", store.loadApis().single().apiKey)
        assertEquals(9L, store.loadApis().single().serverId)

        store.deleteApi(1)
        assertEquals(emptyList<WorkspaceLocalApiConfig>(), store.loadApis())
    }

    @Test
    fun translationJobsRoundTripUpdateAndDelete() {
        store.upsertJob(
            WorkspaceTranslationJob(
                id = 100,
                bookId = 354491,
                bookTitle = "Book",
                translatorId = 1,
                translatorName = "Local",
                chapterCount = 20,
                status = "pending"
            )
        )
        store.upsertJob(store.loadJobs().single().copy(completedChapters = 5, status = "paused"))

        assertEquals(1, store.loadJobs().size)
        assertEquals(5, store.loadJobs().single().completedChapters)
        assertEquals("paused", store.loadJobs().single().status)

        store.deleteJob(100)
        assertEquals(emptyList<WorkspaceTranslationJob>(), store.loadJobs())
    }
}
