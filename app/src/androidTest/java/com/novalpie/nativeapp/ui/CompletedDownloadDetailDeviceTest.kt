package com.novalpie.nativeapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.feature.download.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** One tiny synthetic TXT in system Downloads; real detail/re-entry button, no download points. */
class CompletedDownloadDetailDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun completedFileCanBeOpenedFromBookDetailEvenAfterReenteringTheBook() = runBlocking {
        val context = compose.activity
        val container = AppContainer.from(context)
        val account = container.api.currentUser().id ?: error("Existing account required")
        val id = "beta7-open-${UUID.randomUUID()}"
        val task = DownloadTask(id, account, 353686, "Beta7打开验收", DownloadFormat.Txt)
        val input = File(context.cacheDir, "$id.txt").apply { writeText("Beta7 synthetic open-file check.") }
        var uri: Uri? = null
        val model = ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        try {
            uri = Uri.parse(AndroidDownloadPublisher(context).publish(task, input) {})
            container.downloadStore.save(task.copy(phase = DownloadPhase.Completed, destinationUri = uri.toString()))
            compose.runOnIdle { model.openBook(353686) }
            compose.waitUntil(30000) { model.nativeEpubDownloadState.completedUri == uri.toString() &&
                model.bookDetailState.book is com.novalpie.nativeapp.model.LoadResult.Success }
            compose.onNodeWithText("打开", useUnmergedTree = true).assertIsDisplayed()
            // Verify the exact file can actually be opened by the same URI used by the action.
            assertEquals("Beta7 synthetic open-file check.", context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            compose.runOnIdle { model.loadBookDetail(353686) }
            compose.waitUntil(30000) { model.nativeEpubDownloadState.completedUri == uri.toString() &&
                model.bookDetailState.book is com.novalpie.nativeapp.model.LoadResult.Success }
            compose.onNodeWithText("打开", useUnmergedTree = true).performClick()
            // Android owns the chooser; returning with Back must not mutate the completed record.
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").close()
            assertTrue(container.downloadStore.recover(account).tasks.any { it.id == id && it.destinationUri == uri.toString() })
        } finally {
            uri?.let { context.contentResolver.delete(it, null, null) }
            input.delete()
            val records = File(context.noBackupFilesDir, "download-tasks").canonicalFile
            for (suffix in listOf(".json", ".json.bak", ".json.new")) {
                val record = File(records, id + suffix).canonicalFile
                check(record.parentFile == records && record.name.startsWith("beta7-open-"))
                record.delete()
            }
        }
    }
}
