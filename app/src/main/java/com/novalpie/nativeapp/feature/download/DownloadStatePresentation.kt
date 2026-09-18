package com.novalpie.nativeapp.feature.download

import com.novalpie.nativeapp.ui.NativeBookDownloadFormat
import com.novalpie.nativeapp.ui.NativeDownloadReplacementMode
import com.novalpie.nativeapp.ui.NativeEpubDownloadState

internal fun nativeDownloadStateFromTask(task: DownloadTask, live: DownloadUiState): NativeEpubDownloadState =
    NativeEpubDownloadState(
        bookId = task.bookId,
        format = if (task.format == DownloadFormat.Epub) NativeBookDownloadFormat.Epub else NativeBookDownloadFormat.Txt,
        replacementMode = if (task.applyReplacement) NativeDownloadReplacementMode.EffectiveReaderRules else NativeDownloadReplacementMode.Source,
        busy = live.busy, paused = task.phase == DownloadPhase.Paused,
        progress = com.novalpie.nativeapp.data.NativeEpubExportProgress(
            completedChapters = task.completedChapters, completedImages = task.completedAssets,
            totalChapters = task.totalChapters, totalImages = task.totalAssets, failedImages = task.failedAssets,
            statusLog = live.logs.lastOrNull(),
        ),
        message = downloadStatusText(live),
        canRetry = !live.busy && task.phase in setOf(DownloadPhase.Failed, DownloadPhase.NeedsRetry, DownloadPhase.Cancelled, DownloadPhase.Paused),
        completedUri = task.destinationUri.takeIf { task.phase == DownloadPhase.Completed && !live.busy },
        logs = live.logs,
        awaitingFailureDecision = live.awaitingFailureDecision,
        failedImageCount = live.failedImageCount,
        totalImageCount = live.totalImageCount,
    )

internal fun nativeDownloadCompletedState(task: DownloadTask): NativeEpubDownloadState =
    nativeDownloadStateFromTask(task, DownloadUiState(task = task))
