package com.novalpie.nativeapp.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.data.ReaderTtsSettingsStore
import java.util.Locale

internal enum class ReaderTtsState {
    Loading,
    Speaking,
    Stopped,
    Error,
}

internal data class ReaderTtsVoiceOption(
    val name: String,
    val label: String,
    val languageTag: String,
)

/** An Android framework TTS object is not usable until at least one speech engine is installed. */
internal fun readerTtsEngineAvailable(engineCount: Int): Boolean = engineCount > 0

/**
 * Splits reader paragraphs into bounded utterances. Android engines commonly reject or truncate
 * text around four thousand UTF-16 code units, so a whole chapter must never be sent as one
 * utterance. Sentence boundaries are preferred; an unusually long sentence is hard-split safely.
 */
internal fun readerTtsSegments(
    paragraphs: List<String>,
    maxLength: Int = DEFAULT_TTS_SEGMENT_LENGTH,
): List<String> {
    val limit = maxLength.coerceAtLeast(80)
    val result = mutableListOf<String>()
    paragraphs.forEach { paragraph ->
        val normalized = paragraph.replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return@forEach
        val sentences = splitReaderTtsSentences(normalized)
        val pieces = if (sentences.isEmpty()) listOf(normalized) else sentences
        pieces.forEach { sentence ->
            if (sentence.length > limit) {
                sentence.chunked(limit).forEach { chunk ->
                    chunk.trim().takeIf(String::isNotBlank)?.let(result::add)
                }
            } else {
                result += sentence
            }
        }
    }
    return result
}

internal const val DEFAULT_TTS_SEGMENT_LENGTH = 3500

private fun splitReaderTtsSentences(value: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    value.forEachIndexed { index, character ->
        current.append(character)
        val isBoundary = character in "。！？!?；;…"
        val next = value.getOrNull(index + 1)
        if (isBoundary && (next == null || next !in "。！？!?；;…")) {
            current.toString().trim().takeIf(String::isNotBlank)?.let(result::add)
            current.clear()
        }
    }
    current.toString().trim().takeIf(String::isNotBlank)?.let(result::add)
    return result
}

/** Native segmented TTS adapter. It owns only the application context and the engine lifecycle. */
internal class ReaderTtsController(
    context: android.content.Context,
) : TextToSpeech.OnInitListener {
    private val handler = Handler(Looper.getMainLooper())
    private var initialized = false
    private var initStatusBeforeEngineAssignment: Int? = null
    private var pendingSegments: List<String>? = null
    private var pendingSettings: ReaderTtsSettings? = null
    private var pendingOnSegmentChanged: ((Int, String) -> Unit)? = null
    private var pendingOnFinished: (() -> Unit)? = null
    private var segments: List<String> = emptyList()
    private var settings: ReaderTtsSettings = ReaderTtsSettings()
    private var onSegmentChanged: ((Int, String) -> Unit)? = null
    private var onFinished: (() -> Unit)? = null
    private var activeUtteranceId: String? = null
    private var generation = 0
    private var initializationTimeout: Runnable? = null

    var state by mutableStateOf(ReaderTtsState.Stopped)
        private set
    var currentSegmentIndex by mutableIntStateOf(-1)
        private set
    var currentSegmentText by mutableStateOf<String?>(null)
        private set
    var voiceOptions by mutableStateOf<List<ReaderTtsVoiceOption>>(emptyList())
        private set
    var failureMessage by mutableStateOf<String?>(null)
        private set

    // Some Android/MuMu TTS engines can invoke OnInit during the TextToSpeech constructor. Keep
    // every state delegate initialized before constructing the engine, otherwise the callback can
    // reach `state = ...` while its backing MutableState is still null.
    private lateinit var engine: TextToSpeech

    init {
        // A few engines invoke OnInit synchronously from the constructor. The callback is queued
        // until the lateinit engine has been assigned so neither the state delegates nor engine
        // itself can be observed half-constructed.
        engine = TextToSpeech(context.applicationContext, this)
        initStatusBeforeEngineAssignment?.let { status ->
            initStatusBeforeEngineAssignment = null
            handleInit(status)
        }
    }

    override fun onInit(status: Int) {
        if (!::engine.isInitialized) {
            initStatusBeforeEngineAssignment = status
            return
        }
        handleInit(status)
    }

    private fun handleInit(status: Int) {
        clearInitializationTimeout()
        initialized = status == TextToSpeech.SUCCESS && readerTtsEngineAvailable(
            runCatching { engine.engines.size }.getOrDefault(0),
        )
        if (!initialized) {
            pendingSegments = null
            pendingSettings = null
            pendingOnSegmentChanged = null
            pendingOnFinished = null
            fail("未检测到可用的系统听书引擎")
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                handler.post {
                    if (isCurrentUtterance(utteranceId)) state = ReaderTtsState.Speaking
                }
            }

            override fun onDone(utteranceId: String?) {
                handler.post { handleUtteranceDone(utteranceId) }
            }

            override fun onError(utteranceId: String?) {
                handler.post {
                    if (isCurrentUtterance(utteranceId)) {
                        fail("系统听书引擎无法朗读当前内容")
                        onFinished = null
                    }
                }
            }
        })
        voiceOptions = engine.voices.orEmpty()
            .map { voice ->
                ReaderTtsVoiceOption(
                    name = voice.name,
                    label = "${voice.locale.displayName} · ${voice.name}",
                    languageTag = voice.locale.toLanguageTag(),
                )
            }
            .distinctBy(ReaderTtsVoiceOption::name)
            .sortedBy { it.label }

        val queuedSegments = pendingSegments
        val queuedSettings = pendingSettings
        val queuedSegmentCallback = pendingOnSegmentChanged
        val queuedFinishedCallback = pendingOnFinished
        pendingSegments = null
        pendingSettings = null
        pendingOnSegmentChanged = null
        pendingOnFinished = null
        if (!queuedSegments.isNullOrEmpty() && queuedSettings != null) {
            speak(queuedSegments, queuedSettings, queuedSegmentCallback, queuedFinishedCallback)
        }
    }

    fun toggle(
        segments: List<String>,
        settings: ReaderTtsSettings,
        onSegmentChanged: ((Int, String) -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
    ) {
        if (state == ReaderTtsState.Speaking || state == ReaderTtsState.Loading) {
            stop()
        } else {
            speak(segments, settings, onSegmentChanged, onFinished)
        }
    }

    /** Compatibility overload for callers that have not yet split their text. */
    fun toggle(
        text: String,
        settings: ReaderTtsSettings,
        onFinished: (() -> Unit)? = null,
    ) = toggle(readerTtsSegments(listOf(text)), settings, onFinished = onFinished)

    fun speak(
        segments: List<String>,
        settings: ReaderTtsSettings,
        onSegmentChanged: ((Int, String) -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
    ) {
        val normalized = readerTtsSegments(segments)
        if (normalized.isEmpty()) {
            fail("当前章节没有可朗读的正文")
            return
        }
        if (!initialized) {
            if (state == ReaderTtsState.Error) {
                fail(failureMessage ?: "系统听书引擎不可用")
                return
            }
            pendingSegments = normalized
            pendingSettings = settings
            pendingOnSegmentChanged = onSegmentChanged
            pendingOnFinished = onFinished
            state = ReaderTtsState.Loading
            armInitializationTimeout()
            return
        }

        stop(clearCallbacks = false)
        this.segments = normalized
        this.settings = settings
        this.onSegmentChanged = onSegmentChanged
        this.onFinished = onFinished
        failureMessage = null
        val locale = Locale.forLanguageTag(settings.language.ifBlank { "zh-CN" })
        val languageResult = engine.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            fail("未安装或不支持 ${settings.language.ifBlank { "zh-CN" }} 的语音数据")
            this.onFinished = null
            return
        }
        settings.voice?.takeIf(String::isNotBlank)?.let { voiceName ->
            engine.voices?.firstOrNull { it.name == voiceName }?.let(engine::setVoice)
        }
        engine.setSpeechRate(settings.rate.coerceIn(ReaderTtsSettingsStore.MIN_RATE, ReaderTtsSettingsStore.MAX_RATE))
        engine.setPitch(settings.pitch.coerceIn(ReaderTtsSettingsStore.MIN_PITCH, ReaderTtsSettingsStore.MAX_PITCH))
        generation += 1
        state = ReaderTtsState.Loading
        startSegment(0, generation)
    }

    /** Compatibility overload for callers that provide one unbounded text value. */
    fun speak(
        text: String,
        settings: ReaderTtsSettings,
        onFinished: (() -> Unit)? = null,
    ) = speak(readerTtsSegments(listOf(text)), settings, onFinished = onFinished)

    fun stop() = stop(clearCallbacks = true)

    private fun stop(clearCallbacks: Boolean) {
        generation += 1
        clearInitializationTimeout()
        handler.removeCallbacksAndMessages(null)
        if (initialized) engine.stop()
        activeUtteranceId = null
        pendingSegments = null
        pendingSettings = null
        pendingOnSegmentChanged = null
        pendingOnFinished = null
        segments = emptyList()
        currentSegmentIndex = -1
        currentSegmentText = null
        if (clearCallbacks) {
            onSegmentChanged = null
            onFinished = null
        }
        state = ReaderTtsState.Stopped
        failureMessage = null
    }

    fun shutdown() {
        stop()
        if (initialized) engine.shutdown()
        initialized = false
    }

    private fun startSegment(index: Int, token: Int) {
        if (token != generation || index !in segments.indices) {
            if (token == generation) finish()
            return
        }
        currentSegmentIndex = index
        currentSegmentText = segments[index].takeIf { settings.enableHighlight }
        onSegmentChanged?.invoke(index, segments[index])
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.volume.coerceIn(0f, 1f))
        }
        val utteranceId = "$UTTERANCE_PREFIX-$token-$index"
        activeUtteranceId = utteranceId
        val result = engine.speak(segments[index], TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            fail("系统听书引擎未能开始朗读")
            onFinished = null
        }
    }

    private fun handleUtteranceDone(utteranceId: String?) {
        if (!isCurrentUtterance(utteranceId)) return
        val next = currentSegmentIndex + 1
        if (next !in segments.indices) {
            finish()
            return
        }
        val token = generation
        val delay = settings.pauseBetweenSegmentsMs.coerceIn(0, ReaderTtsSettingsStore.MAX_PAUSE_MS).toLong()
        if (delay == 0L) startSegment(next, token)
        else handler.postDelayed({ startSegment(next, token) }, delay)
    }

    private fun finish() {
        activeUtteranceId = null
        state = ReaderTtsState.Stopped
        currentSegmentText = null
        val callback = onFinished
        onFinished = null
        onSegmentChanged = null
        callback?.invoke()
    }

    private fun fail(message: String) {
        activeUtteranceId = null
        currentSegmentText = null
        failureMessage = message
        state = ReaderTtsState.Error
    }

    /**
     * Some emulator images expose the framework TTS API but do not provide an engine. In that
     * state Android never calls OnInit, so a reader action must time out into a visible error
     * instead of leaving the toolbar permanently on "Preparing".
     */
    private fun armInitializationTimeout() {
        clearInitializationTimeout()
        val timeout = Runnable {
            if (initialized || pendingSegments.isNullOrEmpty()) return@Runnable
            pendingSegments = null
            pendingSettings = null
            pendingOnSegmentChanged = null
            pendingOnFinished = null
            fail("系统听书引擎启动超时，请在系统设置中启用文字转语音后重试")
        }
        initializationTimeout = timeout
        handler.postDelayed(timeout, TTS_INITIALIZATION_TIMEOUT_MS)
    }

    private fun clearInitializationTimeout() {
        initializationTimeout?.let(handler::removeCallbacks)
        initializationTimeout = null
    }

    private fun isCurrentUtterance(utteranceId: String?): Boolean =
        utteranceId != null && utteranceId == activeUtteranceId

    private companion object {
        const val UTTERANCE_PREFIX = "novalpie-reader-utterance"
        const val TTS_INITIALIZATION_TIMEOUT_MS = 5_000L
    }
}
