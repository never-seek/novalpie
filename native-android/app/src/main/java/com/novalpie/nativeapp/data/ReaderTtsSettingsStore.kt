package com.novalpie.nativeapp.data

import android.content.Context

/** The website's `novel_tts_settings` preferences, kept local to the native reader. */
data class ReaderTtsSettings(
    val rate: Float = 1f,
    val pitch: Float = 1f,
    val volume: Float = 1f,
    val language: String = "zh-CN",
    val voice: String? = null,
    val enableHighlight: Boolean = true,
    val enableAutoScroll: Boolean = true,
    val enableAutoNextChapter: Boolean = false,
    val pauseBetweenSegmentsMs: Int = 0,
)

class ReaderTtsSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ReaderTtsSettings = ReaderTtsSettings(
        rate = prefs.getFloat(KEY_RATE, 1f).coerceIn(MIN_RATE, MAX_RATE),
        pitch = prefs.getFloat(KEY_PITCH, 1f).coerceIn(MIN_PITCH, MAX_PITCH),
        volume = prefs.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f),
        language = prefs.getString(KEY_LANGUAGE, "zh-CN").orEmpty().ifBlank { "zh-CN" },
        voice = prefs.getString(KEY_VOICE, null).takeIf { !it.isNullOrBlank() },
        enableHighlight = prefs.getBoolean(KEY_HIGHLIGHT, true),
        enableAutoScroll = prefs.getBoolean(KEY_AUTO_SCROLL, true),
        enableAutoNextChapter = prefs.getBoolean(KEY_AUTO_NEXT, false),
        pauseBetweenSegmentsMs = prefs.getInt(KEY_PAUSE, 0).coerceIn(0, MAX_PAUSE_MS),
    )

    fun save(value: ReaderTtsSettings) {
        prefs.edit()
            .putFloat(KEY_RATE, value.rate.coerceIn(MIN_RATE, MAX_RATE))
            .putFloat(KEY_PITCH, value.pitch.coerceIn(MIN_PITCH, MAX_PITCH))
            .putFloat(KEY_VOLUME, value.volume.coerceIn(0f, 1f))
            .putString(KEY_LANGUAGE, value.language.ifBlank { "zh-CN" })
            .putString(KEY_VOICE, value.voice?.takeIf { it.isNotBlank() })
            .putBoolean(KEY_HIGHLIGHT, value.enableHighlight)
            .putBoolean(KEY_AUTO_SCROLL, value.enableAutoScroll)
            .putBoolean(KEY_AUTO_NEXT, value.enableAutoNextChapter)
            .putInt(KEY_PAUSE, value.pauseBetweenSegmentsMs.coerceIn(0, MAX_PAUSE_MS))
            .apply()
    }

    fun reset() = prefs.edit().clear().apply()

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_reader_tts_settings"
        const val MIN_RATE = 0.25f
        const val MAX_RATE = 3f
        const val MIN_PITCH = 0.25f
        const val MAX_PITCH = 2f
        const val MAX_PAUSE_MS = 5000
        private const val KEY_RATE = "rate"
        private const val KEY_PITCH = "pitch"
        private const val KEY_VOLUME = "volume"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_VOICE = "voice"
        private const val KEY_HIGHLIGHT = "enable_highlight"
        private const val KEY_AUTO_SCROLL = "enable_auto_scroll"
        private const val KEY_AUTO_NEXT = "enable_auto_next_chapter"
        private const val KEY_PAUSE = "pause_between_segments_ms"
    }
}
