package com.novalpie.nativeapp.feature.reader.tts

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.ui.ReaderTtsController
import com.novalpie.nativeapp.ui.ReaderTtsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Bridges the tested Android rolling queue to application-level playback state. Main thread only. */
internal class AndroidSpeechEngine(context: Context, private val scope: CoroutineScope): SpeechEngine {
    private val application = context.applicationContext
    private var controller: ReaderTtsController? = null
    private var errorObserver: Job? = null
    private var serial = 0L
    val voiceOptions get() = controller?.voiceOptions.orEmpty()
    val voiceFallback get() = controller?.voiceFallback

    override fun start(
        segments: List<String>, startIndex: Int, settings: ReaderTtsSettings,
        onSegment: (Int) -> Unit, onFinished: () -> Unit, onError: (String) -> Unit,
    ) {
        stop()
        val generation=serial
        val engine=controller ?: ReaderTtsController(application).also { controller=it }
        errorObserver=scope.launch {
            snapshotFlow { engine.state to engine.failureMessage }.collect { (state,message) ->
                if(generation==serial && state==ReaderTtsState.Error) onError(message ?: "系统听书引擎播放失败")
            }
        }
        engine.speak(
            segments=segments.drop(startIndex),settings=settings,
            onSegmentChanged={index,_->if(generation==serial)onSegment(startIndex+index)},
            onFinished={if(generation==serial)onFinished()},
        )
    }

    override fun stop() {
        serial++
        errorObserver?.cancel()
        errorObserver=null
        controller?.stop()
    }

    override fun close() {
        stop()
        controller?.shutdown()
        controller=null
    }
}
