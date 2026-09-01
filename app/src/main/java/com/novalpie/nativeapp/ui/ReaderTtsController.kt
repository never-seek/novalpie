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
    Paused,
    Stopped,
    Error,
}

internal data class ReaderTtsVoiceOption(
    val name: String,
    val label: String,
    val languageTag: String,
)

/** A recoverable requested-voice mismatch that a reader UI may present without stopping TTS. */
internal data class ReaderTtsVoiceFallback(
    val requestedVoiceName: String,
    val languageTag: String,
    val message: String = "已选语音不可用，已改用当前语言的默认声音",
)

/** An Android framework TTS object is not usable until at least one speech engine is installed. */
internal fun readerTtsEngineAvailable(engineCount: Int): Boolean = engineCount > 0

/** Only these settings require flushing Android's already queued utterances. */
internal fun readerTtsEngineSettingsChanged(
    previous: ReaderTtsSettings,
    next: ReaderTtsSettings,
): Boolean = previous.rate != next.rate ||
    previous.pitch != next.pitch ||
    previous.volume != next.volume ||
    previous.language != next.language ||
    previous.voice != next.voice ||
    previous.pauseBetweenSegmentsMs != next.pauseBetweenSegmentsMs

/**
 * Splits reader paragraphs into bounded utterances. Android engines commonly reject or truncate
 * text around four thousand UTF-16 code units, so a whole chapter must never be sent as one
 * utterance. Sentence boundaries are preferred within one display paragraph; paragraphs remain
 * separate so playback highlighting and follow-scroll retain a precise visual anchor.
 */
internal fun readerTtsSegments(
    paragraphs: List<String>,
    maxLength: Int = DEFAULT_TTS_SEGMENT_LENGTH,
): List<String> {
    val limit = maxLength.coerceAtLeast(1)
    val result = mutableListOf<String>()

    fun flushCurrentGroup(currentGroup: StringBuilder) {
        currentGroup.toString().takeIf(String::isNotBlank)?.let(result::add)
    }

    paragraphs.forEach { paragraph ->
        val normalized = paragraph.replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return@forEach
        // Queueing keeps adjacent utterances gap-free, while keeping a display paragraph's
        // utterances together preserves a reliable highlight and auto-scroll anchor.
        var currentGroup = StringBuilder()
        val sentences = splitReaderTtsSentences(normalized)
        val pieces = if (sentences.isEmpty()) listOf(normalized) else sentences
        pieces.forEach { sentence ->
            if (sentence.length > limit) {
                flushCurrentGroup(currentGroup)
                sentence.chunked(limit)
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .forEach(result::add)
                currentGroup = StringBuilder()
            } else {
                if (currentGroup.isNotEmpty() && currentGroup.length + sentence.length > limit) {
                    flushCurrentGroup(currentGroup)
                    currentGroup = StringBuilder()
                }
                currentGroup.append(sentence)
            }
        }
        flushCurrentGroup(currentGroup)
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
    private var settings: ReaderTtsSettings = ReaderTtsSettings()
    private var onSegmentChanged: ((Int, String) -> Unit)? = null
    private var onFinished: (() -> Unit)? = null
    private var playbackPlan: ReaderTtsPlaybackPlan? = null
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
    var voiceFallback by mutableStateOf<ReaderTtsVoiceFallback?>(null)
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
                handler.post { handleUtteranceStarted(utteranceId) }
            }

            override fun onDone(utteranceId: String?) {
                handler.post { handleUtteranceDone(utteranceId) }
            }

            override fun onError(utteranceId: String?) {
                handler.post { handleUtteranceError(utteranceId) }
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
        when (state) {
            ReaderTtsState.Speaking -> pause()
            ReaderTtsState.Paused -> resume()
            ReaderTtsState.Loading -> stop()
            ReaderTtsState.Stopped,
            ReaderTtsState.Error,
            -> speak(segments, settings, onSegmentChanged, onFinished)
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
        voiceFallback = null
        val normalized = readerTtsSegments(segments)
        if (normalized.isEmpty()) {
            fail("当前章节没有可朗读的正文", stopEngine = true)
            return
        }
        if (!initialized) {
            pendingSegments = normalized
            pendingSettings = settings
            pendingOnSegmentChanged = onSegmentChanged
            pendingOnFinished = onFinished
            failureMessage = null
            state = ReaderTtsState.Loading
            armInitializationTimeout()
            return
        }

        stop(clearCallbacks = false)
        this.settings = settings
        this.onSegmentChanged = onSegmentChanged
        this.onFinished = onFinished
        failureMessage = null
        if (!applyEngineSettings(this.settings)) {
            this.onFinished = null
            return
        }
        generation += 1
        playbackPlan = ReaderTtsPlaybackPlan.start(normalized, generation)
        state = ReaderTtsState.Loading
        enqueuePendingSegments(playbackPlan ?: return)
    }

    /** Compatibility overload for callers that provide one unbounded text value. */
    fun speak(
        text: String,
        settings: ReaderTtsSettings,
        onFinished: (() -> Unit)? = null,
    ) = speak(readerTtsSegments(listOf(text)), settings, onFinished = onFinished)

    fun stop() = stop(clearCallbacks = true)

    /**
     * Android cannot safely mutate utterances already added with QUEUE_ADD. Store the latest
     * settings and pause at the current segment; Resume gives every remaining utterance a fresh
     * generation after applying the selected voice/rate/pitch.
     */
    fun updateSettingsForResume(nextSettings: ReaderTtsSettings) {
        val requiresQueueFlush = readerTtsEngineSettingsChanged(settings, nextSettings)
        settings = nextSettings
        if (
            requiresQueueFlush &&
            (state == ReaderTtsState.Speaking || state == ReaderTtsState.Loading)
        ) {
            pause()
        }
    }

    /**
     * Framework TextToSpeech only exposes stop, so pause keeps the immutable playback plan and
     * resumes from its current utterance under a fresh generation.  Retaining the callbacks here
     * is intentional: the reader highlight and auto-next callback belong to the paused session.
     */
    fun pause() {
        val activePlan = playbackPlan
        if (activePlan?.status != ReaderTtsPlaybackStatus.Active) {
            stop()
            return
        }
        generation += 1
        clearInitializationTimeout()
        handler.removeCallbacksAndMessages(null)
        if (initialized) engine.stop()
        playbackPlan = activePlan.pause()
        failureMessage = null
        state = ReaderTtsState.Paused
    }

    fun resume() {
        val pausedPlan = playbackPlan
        if (pausedPlan?.status != ReaderTtsPlaybackStatus.Paused) return
        if (!initialized) {
            fail("未检测到可用的系统听书引擎")
            return
        }
        if (!applyEngineSettings(settings)) return
        generation += 1
        val resumed = pausedPlan.resume(generation)
        if (resumed.status != ReaderTtsPlaybackStatus.Active) {
            stop()
            return
        }
        playbackPlan = resumed
        failureMessage = null
        state = ReaderTtsState.Loading
        enqueuePendingSegments(resumed)
    }

    private fun stop(clearCallbacks: Boolean) {
        generation += 1
        clearInitializationTimeout()
        handler.removeCallbacksAndMessages(null)
        if (initialized) engine.stop()
        pendingSegments = null
        pendingSettings = null
        pendingOnSegmentChanged = null
        pendingOnFinished = null
        playbackPlan = playbackPlan?.stop()
        playbackPlan = null
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

    private fun applyRequestedVoice(requestedSettings: ReaderTtsSettings, locale: Locale) {
        val requestedVoiceName = requestedSettings.voice?.takeIf(String::isNotBlank) ?: return
        val requestedVoice = engine.voices.orEmpty().firstOrNull { it.name == requestedVoiceName }
        if (requestedVoice != null && engine.setVoice(requestedVoice) != TextToSpeech.ERROR) return

        // A stale persisted voice can survive engine updates. Re-selecting the language restores
        // its default voice, while the in-memory setting prevents the stale name being retried.
        engine.setLanguage(locale)
        settings = requestedSettings.copy(voice = null)
        voiceFallback = ReaderTtsVoiceFallback(
            requestedVoiceName = requestedVoiceName,
            languageTag = locale.toLanguageTag(),
        )
    }

    private fun applyEngineSettings(requestedSettings: ReaderTtsSettings): Boolean {
        val locale = Locale.forLanguageTag(requestedSettings.language.ifBlank { "zh-CN" })
        val languageResult = engine.setLanguage(locale)
        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            fail("未安装或不支持 ${requestedSettings.language.ifBlank { "zh-CN" }} 的语音数据")
            return false
        }
        applyRequestedVoice(requestedSettings, locale)
        engine.setSpeechRate(settings.rate.coerceIn(ReaderTtsSettingsStore.MIN_RATE, ReaderTtsSettingsStore.MAX_RATE))
        engine.setPitch(settings.pitch.coerceIn(ReaderTtsSettingsStore.MIN_PITCH, ReaderTtsSettingsStore.MAX_PITCH))
        return true
    }

    private fun enqueuePendingSegments(plan: ReaderTtsPlaybackPlan) {
        val pendingIndexes = plan.pendingQueueIndexes
        if (pendingIndexes.isEmpty()) return

        pendingIndexes.forEachIndexed { pendingPosition, index ->
            val queueMode = if (pendingPosition == 0) {
                TextToSpeech.QUEUE_FLUSH
            } else {
                TextToSpeech.QUEUE_ADD
            }
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.volume.coerceIn(0f, 1f))
            }
            val speakResult = engine.speak(
                plan.segments[index],
                queueMode,
                params,
                speechUtteranceId(plan.generation, index),
            )
            if (speakResult == TextToSpeech.ERROR) {
                playbackPlan = plan.onError(plan.generation)
                fail("系统听书引擎未能开始朗读", stopEngine = true)
                return
            }

            val pause = settings.pauseBetweenSegmentsMs
                .coerceIn(0, ReaderTtsSettingsStore.MAX_PAUSE_MS)
                .toLong()
            if (pause > 0L && index < plan.segments.lastIndex) {
                val pauseResult = engine.playSilentUtterance(
                    pause,
                    TextToSpeech.QUEUE_ADD,
                    pauseUtteranceId(plan.generation, index),
                )
                if (pauseResult == TextToSpeech.ERROR) {
                    playbackPlan = plan.onError(plan.generation)
                    fail("系统听书引擎未能维持朗读队列", stopEngine = true)
                    return
                }
            }
        }
        playbackPlan = plan.consumePendingQueue()
    }

    private fun handleUtteranceStarted(utteranceId: String?) {
        val token = parseUtteranceId(utteranceId) ?: return
        if (token.isPause) return
        val plan = playbackPlan ?: return
        if (!plan.acceptsUtterance(token.generation, token.index)) return

        currentSegmentIndex = token.index
        currentSegmentText = plan.segments[token.index].takeIf { settings.enableHighlight }
        onSegmentChanged?.invoke(token.index, plan.segments[token.index])
        state = ReaderTtsState.Speaking
    }

    private fun handleUtteranceDone(utteranceId: String?) {
        val token = parseUtteranceId(utteranceId) ?: return
        if (token.isPause) return
        val plan = playbackPlan ?: return
        val advancedPlan = plan.onUtteranceDone(token.generation, token.index)
        if (advancedPlan == plan) return

        playbackPlan = advancedPlan
        if (advancedPlan.isTerminal) {
            finish()
        } else {
            enqueuePendingSegments(advancedPlan)
        }
    }

    private fun handleUtteranceError(utteranceId: String?) {
        val token = parseUtteranceId(utteranceId) ?: return
        val plan = playbackPlan ?: return
        val erroredPlan = plan.onUtteranceError(token.generation, token.index)
        if (erroredPlan == plan) return

        playbackPlan = erroredPlan
        fail("系统听书引擎无法朗读当前内容", stopEngine = true)
    }

    private fun finish() {
        clearInitializationTimeout()
        playbackPlan = null
        currentSegmentText = null
        state = ReaderTtsState.Stopped
        failureMessage = null
        val callback = onFinished
        onFinished = null
        onSegmentChanged = null
        callback?.invoke()
    }

    private fun fail(message: String, stopEngine: Boolean = false) {
        clearInitializationTimeout()
        handler.removeCallbacksAndMessages(null)
        playbackPlan = null
        if (stopEngine && initialized) engine.stop()
        generation += 1
        pendingSegments = null
        pendingSettings = null
        pendingOnSegmentChanged = null
        pendingOnFinished = null
        currentSegmentIndex = -1
        currentSegmentText = null
        onSegmentChanged = null
        onFinished = null
        voiceFallback = null
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

    private data class QueuedUtterance(
        val generation: Int,
        val index: Int,
        val isPause: Boolean,
    )

    private fun parseUtteranceId(utteranceId: String?): QueuedUtterance? {
        val match = utteranceId?.let(UTTERANCE_ID_PATTERN::matchEntire) ?: return null
        return QueuedUtterance(
            generation = match.groupValues[1].toIntOrNull() ?: return null,
            index = match.groupValues[2].toIntOrNull() ?: return null,
            isPause = match.groupValues[3].isNotEmpty(),
        )
    }

    private companion object {
        const val UTTERANCE_PREFIX = "novalpie-reader-utterance"
        const val TTS_INITIALIZATION_TIMEOUT_MS = 5_000L
        const val PAUSE_SUFFIX = ":pause"
        val UTTERANCE_ID_PATTERN = Regex(
            "^${Regex.escape(UTTERANCE_PREFIX)}:(\\d+):(\\d+)(${Regex.escape(PAUSE_SUFFIX)})?$",
        )

        fun speechUtteranceId(generation: Int, index: Int): String =
            "$UTTERANCE_PREFIX:$generation:$index"

        fun pauseUtteranceId(generation: Int, index: Int): String =
            "${speechUtteranceId(generation, index)}$PAUSE_SUFFIX"
    }
}
