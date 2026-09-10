package com.novalpie.nativeapp.model

/** Confirmed batch boundaries only. An in-flight POST must never be replayed automatically. */
data class UploadBatchCheckpoint(
    val signature: String,
    val novelId: Long?,
    val nextBatch: Int,
    val totalBatches: Int,
    val inFlight: Boolean,
)
