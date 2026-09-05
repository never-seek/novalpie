package com.novalpie.nativeapp.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.novalpie.nativeapp.data.ReaderTtsSettings

/** Real system-engine smoke test, not a fake engine or an error-state acceptance test. */
class ReaderTtsDeviceTest {
    @Test fun nativeControllerSpeaksChineseAndReportsBothQueuedParagraphs() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        lateinit var controller: ReaderTtsController
        val complete=CountDownLatch(1)
        val seen=java.util.concurrent.CopyOnWriteArrayList<Int>()
        instrumentation.runOnMainSync {
            controller=ReaderTtsController(instrumentation.targetContext)
            controller.speak(
                segments=listOf("第一段：中文听书正常。","第二段：连续朗读没有丢掉。"),
                settings=ReaderTtsSettings(language="zh-CN"),
                onSegmentChanged={index,_->seen.add(index)},
                onFinished={complete.countDown()},
            )
        }
        try {
            val finished=complete.await(40,TimeUnit.SECONDS)
            assertTrue("原生听书未完成：${controller.failureMessage}",finished)
            assertEquals(listOf(0,1),seen.distinct())
            instrumentation.runOnMainSync { assertEquals(ReaderTtsState.Stopped,controller.state) }
        } finally { instrumentation.runOnMainSync { controller.shutdown() } }
    }

    @Test fun installedChineseEngineSynthesizesPcmAndActuallyCompletesPlayback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext
        val ready = CountDownLatch(1)
        val initialized = AtomicInteger(TextToSpeech.ERROR)
        lateinit var engine: TextToSpeech
        instrumentation.runOnMainSync {
            engine = TextToSpeech(app, { status -> initialized.set(status); ready.countDown() })
        }
        try {
            assertTrue("系统引擎没有初始化", ready.await(15, TimeUnit.SECONDS))
            assertEquals(TextToSpeech.SUCCESS, initialized.get())
            assertTrue("Android包可见性必须允许发现系统TTS引擎", engine.engines.isNotEmpty())
            val chinese = engine.voices.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language in setOf("zh","cmn") }
            assertNotNull("必须配置实际可用的中文系统音色", chinese)
            assertEquals(TextToSpeech.SUCCESS, engine.setVoice(chinese))
            val synthesized = CountDownLatch(1)
            val started = CountDownLatch(1)
            val played = CountDownLatch(1)
            val failure = AtomicInteger(0)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { if(id=="beta7-play") started.countDown() }
                override fun onDone(id: String?) { if(id=="beta7-file") synthesized.countDown(); if(id=="beta7-play") played.countDown() }
                @Deprecated("Framework callback")
                override fun onError(id: String?) { failure.incrementAndGet(); synthesized.countDown(); played.countDown() }
            })
            val output = File(app.cacheDir,"beta7-tts-chinese-smoke.wav")
            val text = "这是小说阅读器的中文听书测试。第一句话完整，第二句话继续。"
            assertEquals(TextToSpeech.SUCCESS,engine.synthesizeToFile(text,Bundle(),output,"beta7-file"))
            assertTrue("中文音频未生成",synthesized.await(20,TimeUnit.SECONDS))
            assertEquals(0,failure.get())
            assertTrue(output.length()>1000)
            val bytes = output.readBytes()
            assertEquals("RIFF",String(bytes,0,4,Charsets.US_ASCII))
            assertTrue("音频数据不能全为静音",bytes.drop(44).count { it.toInt()!=0 }>100)
            assertEquals(TextToSpeech.SUCCESS,engine.speak(text,TextToSpeech.QUEUE_FLUSH,Bundle(),"beta7-play"))
            assertTrue("引擎没有开始真实播放",started.await(15,TimeUnit.SECONDS))
            assertTrue("引擎没有完成真实播放",played.await(30,TimeUnit.SECONDS))
            assertEquals(0,failure.get())
        } finally {
            instrumentation.runOnMainSync { engine.stop();engine.shutdown() }
        }
    }
}
