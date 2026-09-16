package com.novalpie.nativeapp.data

import android.content.Context

/** Local controls for native book downloads; these never change the server-side download policy. */
data class DownloadSettings(
    val imageConcurrency: Int = DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY,
    val compressImages: Boolean = false,
    val imageQuality: Int = DEFAULT_DOWNLOAD_IMAGE_QUALITY,
    val zipCompressionLevel: Int = DEFAULT_DOWNLOAD_ZIP_COMPRESSION_LEVEL,
)

class DownloadSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): DownloadSettings = DownloadSettings(
        imageConcurrency = normalizeDownloadImageConcurrency(
            prefs.getInt(KEY_IMAGE_CONCURRENCY, DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY),
        ),
        compressImages = prefs.getBoolean(KEY_COMPRESS_IMAGES, false),
        imageQuality = normalizeDownloadImageQuality(
            prefs.getInt(KEY_IMAGE_QUALITY, DEFAULT_DOWNLOAD_IMAGE_QUALITY),
        ),
        zipCompressionLevel = normalizeDownloadZipCompressionLevel(
            prefs.getInt(KEY_ZIP_COMPRESSION_LEVEL, DEFAULT_DOWNLOAD_ZIP_COMPRESSION_LEVEL),
        ),
    )

    fun save(settings: DownloadSettings) {
        prefs.edit()
            .putInt(KEY_IMAGE_CONCURRENCY, normalizeDownloadImageConcurrency(settings.imageConcurrency))
            .putBoolean(KEY_COMPRESS_IMAGES, settings.compressImages)
            .putInt(KEY_IMAGE_QUALITY, normalizeDownloadImageQuality(settings.imageQuality))
            .putInt(KEY_ZIP_COMPRESSION_LEVEL, normalizeDownloadZipCompressionLevel(settings.zipCompressionLevel))
            .apply()
    }

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_download_settings"
        private const val KEY_IMAGE_CONCURRENCY = "image_concurrency"
        private const val KEY_COMPRESS_IMAGES = "compress_images"
        private const val KEY_IMAGE_QUALITY = "image_quality"
        private const val KEY_ZIP_COMPRESSION_LEVEL = "zip_compression_level"
    }
}

const val MIN_DOWNLOAD_IMAGE_CONCURRENCY: Int = 1
const val MAX_DOWNLOAD_IMAGE_CONCURRENCY: Int = 256
const val DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY: Int = 8

const val MIN_DOWNLOAD_IMAGE_QUALITY: Int = 1
const val MAX_DOWNLOAD_IMAGE_QUALITY: Int = 95
const val DEFAULT_DOWNLOAD_IMAGE_QUALITY: Int = 75

const val MIN_DOWNLOAD_ZIP_COMPRESSION_LEVEL: Int = 0
const val MAX_DOWNLOAD_ZIP_COMPRESSION_LEVEL: Int = 9
const val DEFAULT_DOWNLOAD_ZIP_COMPRESSION_LEVEL: Int = 0

/** Keep a user-entered worker count positive without allowing an accidental huge worker pool. */
fun normalizeDownloadImageConcurrency(value: Int): Int = value.coerceIn(
    MIN_DOWNLOAD_IMAGE_CONCURRENCY,
    MAX_DOWNLOAD_IMAGE_CONCURRENCY,
)

fun normalizeDownloadImageQuality(value: Int): Int = value.coerceIn(
    MIN_DOWNLOAD_IMAGE_QUALITY,
    MAX_DOWNLOAD_IMAGE_QUALITY,
)

fun normalizeDownloadZipCompressionLevel(value: Int): Int = value.coerceIn(
    MIN_DOWNLOAD_ZIP_COMPRESSION_LEVEL,
    MAX_DOWNLOAD_ZIP_COMPRESSION_LEVEL,
)

/** Parses the compact custom field; blank, fractional and non-positive values are rejected. */
fun parseDownloadImageConcurrency(value: String): Int? = value.trim()
    .toIntOrNull()
    ?.takeIf { it > 0 }
    ?.let(::normalizeDownloadImageConcurrency)

fun parseDownloadImageQuality(value: String): Int? = value.trim()
    .toIntOrNull()
    ?.takeIf { it in MIN_DOWNLOAD_IMAGE_QUALITY..MAX_DOWNLOAD_IMAGE_QUALITY }
    ?.let(::normalizeDownloadImageQuality)

fun parseDownloadZipCompressionLevel(value: String): Int? = value.trim()
    .toIntOrNull()
    ?.takeIf { it in MIN_DOWNLOAD_ZIP_COMPRESSION_LEVEL..MAX_DOWNLOAD_ZIP_COMPRESSION_LEVEL }
    ?.let(::normalizeDownloadZipCompressionLevel)
