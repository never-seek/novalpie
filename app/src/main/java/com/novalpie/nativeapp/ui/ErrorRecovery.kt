package com.novalpie.nativeapp.ui

internal fun retryActionLabel(surface: String): String {
    val normalized = surface.trim()
    return if (normalized.isBlank()) "重试" else "重试$normalized"
}
