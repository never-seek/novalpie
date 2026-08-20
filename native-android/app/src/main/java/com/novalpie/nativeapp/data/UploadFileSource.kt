package com.novalpie.nativeapp.data

import java.io.InputStream

class UploadFileSource(
    val fileName: String,
    val sizeBytes: Long,
    val contentType: String? = null,
    val openStream: () -> InputStream
)
