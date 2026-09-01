package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsPlaybackPlanTest {
    @Test
    fun startsWithTheCurrentUtteranceAndTwoQueuedLookaheadItems() {
        val plan = ReaderTtsPlaybackPlan.start(
            segments = listOf("甲。", "乙。", "丙。", "丁。"),
            generation = 5,
        )

        assertEquals("甲。", plan.currentText)
        assertEquals(listOf(0, 1, 2), plan.pendingQueueIndexes)
    }

    @Test
    fun completingAnUtteranceRefillsOnlyTheFreedLookaheadSlot() {
        val plan = ReaderTtsPlaybackPlan.start(
            segments = listOf("甲。", "乙。", "丙。", "丁。"),
            generation = 5,
        ).consumePendingQueue()

        val advanced = plan.onUtteranceDone(generation = 5, utteranceIndex = 0)

        assertEquals("乙。", advanced.currentText)
        assertEquals(listOf(3), advanced.pendingQueueIndexes)
    }

    @Test
    fun staleGenerationCompletionCannotAdvanceQueue() {
        val state = ReaderTtsPlaybackPlan.start(listOf("甲。", "乙。"), generation = 5)

        assertEquals(state, state.onUtteranceDone(generation = 4, utteranceIndex = 0))
    }

    @Test
    fun sameGenerationCallbacksRejectAnAlreadyCompletedUtterance() {
        val advanced = ReaderTtsPlaybackPlan.start(
            segments = listOf("甲。", "乙。", "丙。"),
            generation = 5,
        ).consumePendingQueue().onUtteranceDone(generation = 5, utteranceIndex = 0)

        assertFalse(advanced.acceptsUtterance(generation = 5, utteranceIndex = 0))
        assertEquals(advanced, advanced.onUtteranceError(generation = 5, utteranceIndex = 0))
        assertTrue(advanced.acceptsUtterance(generation = 5, utteranceIndex = 1))
    }

    @Test
    fun errorClearsQueuedPlaybackAndReturnsTerminalState() {
        val state = ReaderTtsPlaybackPlan.start(listOf("甲。", "乙。", "丙。"), 1).onError(1)

        assertTrue(state.isTerminal)
        assertEquals(emptyList<Int>(), state.pendingQueueIndexes)
        assertEquals(-1, state.queuedThroughIndex)
    }

    @Test
    fun voiceFallbackStatePreservesTheRequestedVoiceForNonfatalUiFeedback() {
        val fallback = ReaderTtsVoiceFallback(
            requestedVoiceName = "stale-voice",
            languageTag = "zh-CN",
        )

        assertEquals("stale-voice", fallback.requestedVoiceName)
        assertEquals("zh-CN", fallback.languageTag)
        assertTrue(fallback.message.contains("默认声音"))
    }

    @Test
    fun nextChapterStartsWithItsFirstActualUtterance() {
        assertEquals(
            "下一章首句。",
            ReaderTtsPlaybackPlan.nextChapter(listOf("下一章首句。"), 2).currentText,
        )
    }

    @Test
    fun continuationStartsOnlyAfterTheRequestedChapterBodyIsReady() {
        assertFalse(
            readerTtsContinuationShouldStart(
                pendingChapterId = 22L,
                currentChapterId = 22L,
                hasReadableBody = false,
            ),
        )
        assertTrue(
            readerTtsContinuationShouldStart(
                pendingChapterId = 22L,
                currentChapterId = 22L,
                hasReadableBody = true,
            ),
        )
        assertFalse(
            readerTtsContinuationShouldStart(
                pendingChapterId = 22L,
                currentChapterId = 23L,
                hasReadableBody = true,
            ),
        )
    }

    @Test
    fun autoNextContinuesAfterTheLastChapterAlreadySpokenInContinuousScroll() {
        val chapters = listOf(
            Chapter(id = 10L, title = "第一章"),
            Chapter(id = 11L, title = "第二章"),
            Chapter(id = 12L, title = "第三章"),
            Chapter(id = 13L, title = "第四章"),
        )

        assertEquals(
            13L,
            readerTtsAutoNextChapterId(
                spokenChapterIds = listOf(10L, 11L, 12L),
                routeChapterId = 10L,
                chapters = chapters,
            ),
        )
    }

    @Test
    fun tappingTtsWhileAutoNextIsPendingCancelsTheContinuation() {
        assertEquals(
            ReaderTtsToggleAction.CancelPendingContinuation,
            readerTtsToggleAction(
                pendingChapterId = 22L,
                hasReadableBody = false,
                playbackState = ReaderTtsState.Stopped,
            ),
        )
        assertEquals(
            ReaderTtsToggleAction.StartPlayback,
            readerTtsToggleAction(
                pendingChapterId = null,
                hasReadableBody = true,
                playbackState = ReaderTtsState.Stopped,
            ),
        )
        assertEquals(
            ReaderTtsToggleAction.IgnoreUntilBodyLoads,
            readerTtsToggleAction(
                pendingChapterId = null,
                hasReadableBody = false,
                playbackState = ReaderTtsState.Stopped,
            ),
        )
    }

    @Test
    fun ttsPrimaryActionPausesThenResumesInsteadOfDiscardingTheQueue() {
        assertEquals(
            ReaderTtsToggleAction.PausePlayback,
            readerTtsToggleAction(
                pendingChapterId = null,
                hasReadableBody = true,
                playbackState = ReaderTtsState.Speaking,
            ),
        )
        assertEquals(
            ReaderTtsToggleAction.ResumePlayback,
            readerTtsToggleAction(
                pendingChapterId = null,
                hasReadableBody = true,
                playbackState = ReaderTtsState.Paused,
            ),
        )
    }

    @Test
    fun pauseKeepsTheCurrentUtteranceAndResumeRequeuesItInANewGeneration() {
        val active = ReaderTtsPlaybackPlan.start(
            segments = listOf("甲。", "乙。", "丙。"),
            generation = 5,
        ).consumePendingQueue().onUtteranceDone(generation = 5, utteranceIndex = 0)

        val paused = active.pause()
        val resumed = paused.resume(generation = 6)

        assertEquals(ReaderTtsPlaybackStatus.Paused, paused.status)
        assertEquals("乙。", paused.currentText)
        assertEquals(ReaderTtsPlaybackStatus.Active, resumed.status)
        assertEquals(6, resumed.generation)
        assertEquals("乙。", resumed.currentText)
        assertEquals(listOf(1, 2), resumed.pendingQueueIndexes)
        assertFalse(resumed.acceptsUtterance(generation = 5, utteranceIndex = 1))
    }

    @Test
    fun engineSettingChangesPauseAPlayingQueueButDisplayOnlyTogglesDoNot() {
        val current = ReaderTtsSettings()

        assertTrue(readerTtsEngineSettingsChanged(current, current.copy(rate = 1.25f)))
        assertTrue(readerTtsEngineSettingsChanged(current, current.copy(voice = "new-voice")))
        assertTrue(readerTtsEngineSettingsChanged(current, current.copy(pauseBetweenSegmentsMs = 120)))
        assertFalse(readerTtsEngineSettingsChanged(current, current.copy(enableHighlight = false)))
        assertFalse(readerTtsEngineSettingsChanged(current, current.copy(enableAutoScroll = false)))
    }

    @Test
    fun failedAutoNextBodyCancelsThePendingContinuation() {
        assertTrue(
            readerTtsContinuationShouldCancelForLoadFailure(
                pendingChapterId = 22L,
                currentChapterId = 22L,
                bodyLoadFailed = true,
            ),
        )
        assertFalse(
            readerTtsContinuationShouldCancelForLoadFailure(
                pendingChapterId = 22L,
                currentChapterId = 22L,
                bodyLoadFailed = false,
            ),
        )
    }
}
