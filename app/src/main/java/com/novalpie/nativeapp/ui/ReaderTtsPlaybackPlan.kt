package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
/**
 * Immutable session state for the reader's rolling Android TTS queue. The Android adapter owns
 * engine calls; this plan only decides which generation-scoped utterances are allowed to advance.
 */
internal data class ReaderTtsPlaybackPlan private constructor(
    val segments: List<String>,
    val generation: Int,
    val currentSegmentIndex: Int,
    val queuedThroughIndex: Int,
    val pendingQueueIndexes: List<Int>,
    val status: ReaderTtsPlaybackStatus,
    private val lookahead: Int,
) {
    val currentText: String? get() = segments.getOrNull(currentSegmentIndex)

    val isTerminal: Boolean get() = status != ReaderTtsPlaybackStatus.Active

    /**
     * A queued utterance remains live only until its index has been completed. Android can deliver
     * delayed callbacks from the same generation, so list bounds alone are not sufficient here.
     */
    fun acceptsUtterance(generation: Int, utteranceIndex: Int): Boolean =
        !isTerminal &&
            this.generation == generation &&
            utteranceIndex in currentSegmentIndex..queuedThroughIndex

    /** Marks the commands in [pendingQueueIndexes] as handed to the Android engine. */
    fun consumePendingQueue(): ReaderTtsPlaybackPlan =
        if (pendingQueueIndexes.isEmpty()) this else copy(pendingQueueIndexes = emptyList())

    /**
     * Advances only when the active utterance from this exact playback generation has completed.
     * Advancing frees one queue slot, which is exposed through [pendingQueueIndexes].
     */
    fun onUtteranceDone(generation: Int, utteranceIndex: Int): ReaderTtsPlaybackPlan {
        if (!acceptsUtterance(generation, utteranceIndex) || utteranceIndex != currentSegmentIndex) {
            return this
        }

        val nextIndex = utteranceIndex + 1
        if (nextIndex !in segments.indices) {
            return copy(
                pendingQueueIndexes = emptyList(),
                status = ReaderTtsPlaybackStatus.Finished,
            )
        }

        val newQueuedThroughIndex = minOf(segments.lastIndex, nextIndex + lookahead)
        val additions = if (queuedThroughIndex < newQueuedThroughIndex) {
            (queuedThroughIndex + 1..newQueuedThroughIndex).toList()
        } else {
            emptyList()
        }
        return copy(
            currentSegmentIndex = nextIndex,
            queuedThroughIndex = newQueuedThroughIndex,
            pendingQueueIndexes = additions,
        )
    }

    /** Ignores stale callbacks and makes all queued utterances invalid after an engine error. */
    fun onError(generation: Int): ReaderTtsPlaybackPlan {
        if (this.generation != generation || isTerminal) return this
        return errorState()
    }

    /** An error callback must be tied to a still-live utterance, not merely this generation. */
    fun onUtteranceError(generation: Int, utteranceIndex: Int): ReaderTtsPlaybackPlan =
        if (!acceptsUtterance(generation, utteranceIndex)) this else errorState()

    /**
     * Android has no true pause primitive: stop its queue, retain the active segment and queue it
     * again on resume.  The original generation stays invalid, so a delayed callback can never
     * advance the resumed reader.
     */
    fun pause(): ReaderTtsPlaybackPlan =
        if (status != ReaderTtsPlaybackStatus.Active) this else copy(
            queuedThroughIndex = currentSegmentIndex,
            pendingQueueIndexes = emptyList(),
            status = ReaderTtsPlaybackStatus.Paused,
        )

    /** Resume from the active segment in a fresh generation and rebuild the rolling look-ahead. */
    fun resume(generation: Int): ReaderTtsPlaybackPlan {
        if (status != ReaderTtsPlaybackStatus.Paused || currentSegmentIndex !in segments.indices) return this
        val queuedThrough = minOf(segments.lastIndex, currentSegmentIndex + lookahead)
        return copy(
            generation = generation,
            queuedThroughIndex = queuedThrough,
            pendingQueueIndexes = (currentSegmentIndex..queuedThrough).toList(),
            status = ReaderTtsPlaybackStatus.Active,
        )
    }

    fun stop(): ReaderTtsPlaybackPlan =
        if (status == ReaderTtsPlaybackStatus.Stopped) this else copy(
            queuedThroughIndex = currentSegmentIndex,
            pendingQueueIndexes = emptyList(),
            status = ReaderTtsPlaybackStatus.Stopped,
        )

    private fun errorState(): ReaderTtsPlaybackPlan = copy(
        queuedThroughIndex = currentSegmentIndex - 1,
        pendingQueueIndexes = emptyList(),
        status = ReaderTtsPlaybackStatus.Error,
    )

    companion object {
        fun start(
            segments: List<String>,
            generation: Int,
            lookahead: Int = DEFAULT_TTS_QUEUE_LOOKAHEAD,
        ): ReaderTtsPlaybackPlan {
            val playableSegments = segments.filter(String::isNotBlank)
            val normalizedLookahead = lookahead.coerceAtLeast(0)
            if (playableSegments.isEmpty()) {
                return ReaderTtsPlaybackPlan(
                    segments = emptyList(),
                    generation = generation,
                    currentSegmentIndex = -1,
                    queuedThroughIndex = -1,
                    pendingQueueIndexes = emptyList(),
                    status = ReaderTtsPlaybackStatus.Stopped,
                    lookahead = normalizedLookahead,
                )
            }

            val queuedThroughIndex = minOf(playableSegments.lastIndex, normalizedLookahead)
            return ReaderTtsPlaybackPlan(
                segments = playableSegments,
                generation = generation,
                currentSegmentIndex = 0,
                queuedThroughIndex = queuedThroughIndex,
                pendingQueueIndexes = (0..queuedThroughIndex).toList(),
                status = ReaderTtsPlaybackStatus.Active,
                lookahead = normalizedLookahead,
            )
        }

        fun nextChapter(
            segments: List<String>,
            generation: Int,
            lookahead: Int = DEFAULT_TTS_QUEUE_LOOKAHEAD,
        ): ReaderTtsPlaybackPlan = start(segments, generation, lookahead)
    }
}

internal enum class ReaderTtsPlaybackStatus {
    Active,
    Paused,
    Finished,
    Stopped,
    Error,
}

internal const val DEFAULT_TTS_QUEUE_LOOKAHEAD = 2

/**
 * A chapter route changes before its body arrives. Keep the continuation dormant until the
 * requested route has real text, otherwise Android TTS receives an empty list and the first
 * sentence of the next chapter is lost.
 */
internal fun readerTtsContinuationShouldStart(
    pendingChapterId: Long?,
    currentChapterId: Long,
    hasReadableBody: Boolean,
): Boolean = pendingChapterId != null &&
    pendingChapterId == currentChapterId &&
    hasReadableBody

internal fun readerTtsContinuationShouldCancelForLoadFailure(
    pendingChapterId: Long?,
    currentChapterId: Long,
    bodyLoadFailed: Boolean,
): Boolean = pendingChapterId != null &&
    pendingChapterId == currentChapterId &&
    bodyLoadFailed

/** A continuous reader window may already have spoken prefetched chapters. Do not replay them. */
internal fun readerTtsAutoNextChapterId(
    spokenChapterIds: List<Long>,
    routeChapterId: Long,
    chapters: List<Chapter>,
): Long? = adjacentReaderChapters(
    currentChapterId = spokenChapterIds.lastOrNull() ?: routeChapterId,
    chapters = chapters,
).next?.id

internal enum class ReaderTtsToggleAction {
    CancelPendingContinuation,
    StartPlayback,
    PausePlayback,
    ResumePlayback,
    StopPlayback,
    IgnoreUntilBodyLoads,
}

internal fun readerTtsToggleAction(
    pendingChapterId: Long?,
    hasReadableBody: Boolean,
    playbackState: ReaderTtsState,
): ReaderTtsToggleAction = when {
    pendingChapterId != null -> ReaderTtsToggleAction.CancelPendingContinuation
    !hasReadableBody -> ReaderTtsToggleAction.IgnoreUntilBodyLoads
    playbackState == ReaderTtsState.Speaking -> ReaderTtsToggleAction.PausePlayback
    playbackState == ReaderTtsState.Paused -> ReaderTtsToggleAction.ResumePlayback
    playbackState == ReaderTtsState.Loading -> ReaderTtsToggleAction.StopPlayback
    else -> ReaderTtsToggleAction.StartPlayback
}
