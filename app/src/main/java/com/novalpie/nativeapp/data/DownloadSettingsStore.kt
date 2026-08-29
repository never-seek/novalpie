package com.novalpie.nativeapp.data

import android.content.Context

/** Local controls for native book downloads; these never change the server-side download policy. */
data class DownloadSettings(
    val imageConcurrency: Int = DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY,
)

class DownloadSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): DownloadSettings = DownloadSettings(
        imageConcurrency = normalizeDownloadImageConcurrency(
            prefs.getInt(KEY_IMAGE_CONCURRENCY, DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY),
        ),
    )

    fun save(settings: DownloadSettings) {
        prefs.edit()
            .putInt(KEY_IMAGE_CONCURRENCY, normalizeDownloadImageConcurrency(settings.imageConcurrency))
            .apply()
    }

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_download_settings"
        private const val KEY_IMAGE_CONCURRENCY = "image_concurrency"
    }
}

const val MIN_DOWNLOAD_IMAGE_CONCURRENCY: Int = 1
const val MAX_DOWNLOAD_IMAGE_CONCURRENCY: Int = 256
const val DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY: Int = 8

/** Keep a user-entered worker count positive without allowing an accidental huge worker pool. */
fun normalizeDownloadImageConcurrency(value: Int): Int = value.coerceIn(
    MIN_DOWNLOAD_IMAGE_CONCURRENCY,
    MAX_DOWNLOAD_IMAGE_CONCURRENCY,
)

/** Parses the compact custom field; blank, fractional and non-positive values are rejected. */
fun parseDownloadImageConcurrency(value: String): Int? = value.trim()
    .toIntOrNull()
    ?.takeIf { it > 0 }
    ?.let(::normalizeDownloadImageConcurrency)
