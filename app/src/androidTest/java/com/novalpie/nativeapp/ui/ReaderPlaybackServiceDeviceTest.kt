package com.novalpie.nativeapp.ui

import android.app.NotificationManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.feature.reader.tts.ReaderPlaybackService
import com.novalpie.nativeapp.feature.reader.tts.SpeechChapter
import com.novalpie.nativeapp.feature.reader.tts.SpeechStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Exercises system service/notification lifecycle using only synthetic, non-network book text. */
class ReaderPlaybackServiceDeviceTest {
    @Test fun backgroundAndScreenOffPlaybackCanBePausedResumedAndStoppedFromTheMediaNotification()= runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val playback=AppContainer.from(context).playback
        val notifications=context.getSystemService(NotificationManager::class.java)
        val scenario=ActivityScenario.launch(ComponentActivity::class.java)
        try {
            scenario.onActivity { activity ->
                ReaderPlaybackService.start(activity,
                    SpeechChapter(900001,900002,"Beta7受控听书测试","后台播放测试",List(20){"这是后台听书的第${it+1}段，屏幕关闭后应该继续朗读，通知栏可以暂停和继续。"}),
                    ReaderTtsSettings(language="zh-CN"),
                )
            }
            withTimeout(15000){playback.state.first{it.status==SpeechStatus.Speaking}}
            instrumentation.uiAutomation.executeShellCommand("input keyevent 3").close()
            SystemClock.sleep(1200)
            assertEquals(SpeechStatus.Speaking,playback.state.value.status)
            instrumentation.uiAutomation.executeShellCommand("input keyevent 223").close()
            SystemClock.sleep(1500)
            assertEquals("熄屏不应停止播放",SpeechStatus.Speaking,playback.state.value.status)
            instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
            val pause=notifications.activeNotifications.firstOrNull{it.id==7101}?.notification
            assertNotNull("必须有系统媒体通知",pause)
            assertEquals("暂停",pause!!.actions[0].title.toString())
            pause.actions[0].actionIntent.send()
            withTimeout(5000){playback.state.first{it.status==SpeechStatus.Paused}}
            withTimeout(5000) {
                while(notifications.activeNotifications.firstOrNull{it.id==7101}?.notification?.actions?.firstOrNull()?.title?.toString()!="继续")kotlinx.coroutines.delay(100)
            }
            val resume=notifications.activeNotifications.first{it.id==7101}.notification
            assertEquals("继续",resume.actions[0].title.toString())
            resume.actions[0].actionIntent.send()
            withTimeout(10000){playback.state.first{it.status==SpeechStatus.Speaking}}
            notifications.activeNotifications.first{it.id==7101}.notification.actions[1].actionIntent.send()
            withTimeout(5000){playback.state.first{it.status==SpeechStatus.Stopped}}
            withTimeout(5000) {while(notifications.activeNotifications.any{it.id==7101})kotlinx.coroutines.delay(100)}
            assertFalse(notifications.activeNotifications.any{it.id==7101})
        } finally {
            instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
            instrumentation.runOnMainSync {playback.stop()}
            scenario.close()
        }
    }
}
