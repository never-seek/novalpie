package com.novalpie.nativeapp.ui

import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.data.ReaderTtsSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.*
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowTextToSpeech
import java.time.Duration
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [ReaderTtsInitializationTest.ColdEngine::class])
@LooperMode(LooperMode.Mode.PAUSED)
class ReaderTtsInitializationTest {
    @Implements(TextToSpeech::class)
    class ColdEngine : ShadowTextToSpeech() {
        @Implementation fun getEngines(): List<TextToSpeech.EngineInfo> = listOf(TextToSpeech.EngineInfo().apply { name = "fixture.tts" })
    }
    private fun fixture(): Pair<ReaderTtsController, ColdEngine> {
        ShadowTextToSpeech.addLanguageAvailability(Locale.SIMPLIFIED_CHINESE)
        val controller = ReaderTtsController(ApplicationProvider.getApplicationContext())
        val shadow = Shadow.extract<ColdEngine>(ShadowTextToSpeech.getLastTextToSpeechInstance())
        controller.speak(listOf("测试首句。"), ReaderTtsSettings())
        return controller to shadow
    }
    @Test fun twelveSecondColdInitKeepsTheOriginalRequest() {
        val (controller, engine) = fixture()
        try {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(12))
            assertEquals(ReaderTtsState.Loading, controller.state)
            engine.onInitListener.onInit(TextToSpeech.SUCCESS)
            assertTrue(engine.spokenTextList.contains("测试首句。"))
        } finally { controller.shutdown() }
    }
    @Test fun stopDuringInitializationDoesNotSpeakWhenTheEngineArrives() {
        val (controller, engine) = fixture()
        controller.stop()
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(30))
        assertEquals(ReaderTtsState.Stopped, controller.state)
        assertTrue(engine.spokenTextList.isEmpty())
        controller.shutdown()
    }
    @Test fun unavailableEngineTimesOutAndLateInitCannotResurrectDiscardedSpeech() {
        val (controller, engine) = fixture()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(21))
        assertEquals(ReaderTtsState.Error, controller.state)
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        assertTrue(engine.spokenTextList.isEmpty())
        controller.shutdown()
    }
    @Test fun shutdownWhileBindingReleasesTheEngineAndIgnoresItsLateCallback() {
        val (controller, engine) = fixture()
        controller.shutdown()
        assertTrue("尚在绑定的引擎也必须释放", engine.isShutdown)
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        controller.speak(listOf("过期内容"), ReaderTtsSettings())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(30))
        assertTrue(engine.spokenTextList.isEmpty())
        assertEquals(ReaderTtsState.Stopped, controller.state)
    }
}
