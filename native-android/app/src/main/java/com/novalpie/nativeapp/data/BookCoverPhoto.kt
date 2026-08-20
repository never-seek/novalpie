package com.novalpie.nativeapp.data

/** The source card image and its full-screen image are separate resources for layered covers. */
internal data class BookCoverPhoto(
    val previewUrl: String? = null,
    val originalUrl: String? = null,
)
