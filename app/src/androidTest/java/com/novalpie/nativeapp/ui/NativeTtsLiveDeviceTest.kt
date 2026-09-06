package com.novalpie.nativeapp.ui

import android.app.NotificationManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.ReaderProgressStore
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.feature.reader.tts.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Opt-in live source test. Only IDs/counts/status are exported; never prose or session material. */
class NativeTtsLiveDeviceTest {
    @Test fun actualSourceTailContinuesAtNextChapterFirstSegmentAndSavesBackgroundProgress() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val bookId = args.getString("bookId")?.toLongOrNull() ?: error("Explicit bookId is required")
        val chapterId = args.getString("chapterId")?.toLongOrNull() ?: error("Explicit chapterId is required")
        require(bookId > 0 && chapterId > 0)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val container = AppContainer.from(context)
        val notifications = context.getSystemService(NotificationManager::class.java)
        val scenario = ActivityScenario.launch(ReaderTestActivity::class.java)
        val started = android.os.SystemClock.elapsedRealtime()
        val firstSegments = ConcurrentHashMap<Long, Int>()
        var observer: Job? = null
        try {
            val source = withTimeout(60000) { WebsiteSpeechChapterSource(context, container.api).load(bookId, chapterId) }
            val nextId = source.nextChapterId ?: error("Source chapter has no next chapter")
            require(source.segments.isNotEmpty())
            observer = launch(Dispatchers.Default) {
                container.playback.state.collect { state ->
                    if (state.status == SpeechStatus.Speaking) state.chapter?.let { firstSegments.putIfAbsent(it.chapterId, state.segmentIndex) }
                }
            }
            val startingSegment = (source.segments.lastIndex - 1).coerceAtLeast(0)
            scenario.onActivity { activity ->
                ReaderPlaybackService.start(activity, source, ReaderTtsSettings(language = "zh-CN", rate = 1.4f, enableAutoNextChapter = true), startingSegment)
            }
            withTimeout(25000) { container.playback.state.first { it.chapter?.chapterId == chapterId && it.status in setOf(SpeechStatus.Speaking, SpeechStatus.Error) } }
            assertEquals(SpeechStatus.Speaking, container.playback.state.value.status)
            instrumentation.uiAutomation.executeShellCommand("input keyevent 3").close()
            val next = withTimeout(90000) {
                container.playback.state.first { (it.chapter?.chapterId == nextId && it.status == SpeechStatus.Speaking) || it.status == SpeechStatus.Error }
            }
            assertEquals(SpeechStatus.Speaking, next.status)
            assertEquals(nextId, next.chapter?.chapterId)
            withTimeout(5000) { while (firstSegments[nextId] == null) delay(20) }
            assertEquals("续章必须从真实首句开始", 0, firstSegments[nextId])
            withTimeout(5000) { while (ReaderProgressStore(context).load(bookId)?.chapterId != nextId) delay(20) }
            val local = ReaderProgressStore(context).load(bookId)!!
            assertEquals(next.chapter?.chapterNumber, local.chapterNumber)

            instrumentation.uiAutomation.executeShellCommand("input keyevent 223").close()
            delay(700)
            assertEquals(SpeechStatus.Speaking, container.playback.state.value.status)
            instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
            withTimeout(5000) { while (notifications.activeNotifications.firstOrNull { it.id == 7101 }?.notification?.actions?.firstOrNull()?.title?.toString() != "暂停") delay(50) }
            notifications.activeNotifications.first { it.id == 7101 }.notification.actions[0].actionIntent.send()
            withTimeout(5000) { container.playback.state.first { it.status == SpeechStatus.Paused } }
            val pausedAt = container.playback.state.value.segmentIndex
            withTimeout(5000) { while (notifications.activeNotifications.firstOrNull { it.id == 7101 }?.notification?.actions?.firstOrNull()?.title?.toString() != "继续") delay(50) }
            notifications.activeNotifications.first { it.id == 7101 }.notification.actions[0].actionIntent.send()
            val resumed = withTimeout(15000) { container.playback.state.first { it.status == SpeechStatus.Speaking } }
            assertEquals(nextId, resumed.chapter?.chapterId)
            assertEquals(pausedAt, resumed.segmentIndex)

            val report = JSONObject().put("bookId", bookId).put("initialChapterId", chapterId).put("nextChapterId", nextId)
                .put("startedAtSegment", startingSegment).put("nextFirstSegment", firstSegments[nextId])
                .put("nextSegmentCount", next.chapter?.segments?.size).put("localChapterNumber", local.chapterNumber)
                .put("pauseResumeSegment", pausedAt).put("background", true).put("screenOff", true)
                .put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started)
            val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
            File(folder, "live-tts.json").writeText(report.toString(2))
        } finally {
            observer?.cancel()
            instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
            instrumentation.runOnMainSync { container.playback.stop() }
            scenario.close()
        }
    }
}
