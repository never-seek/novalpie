package com.novalpie.nativeapp.feature.reader.pagination

import org.junit.Assert.*
import org.junit.Test

class PagePlannerTest {
    private fun key(height: Int = 100, revision: String = "source:rules:1") = PageLayoutKey(
        bookId = 10, chapterId = 20, documentRevision = revision,
        widthPx = 200, heightPx = height, typographyRevision = "test-font-20", imageRevision = "images:0",
    )

    private fun paragraph(id: String, count: Int, lineHeight: Float = 20f, gap: Float = 0f) = MeasuredChapterBlock.Paragraph(
        id = id, textLength = count * 5, lines = (0 until count).map { index ->
            MeasuredTextLine(index * 5, (index + 1) * 5, index * lineHeight, (index + 1) * lineHeight)
        }, spaceAfterPx = gap,
    )

    @Test fun longParagraphSplitsOnlyAtMeasuredLineBoundariesWithoutLosingCharacters() {
        val plan = planChapterPages(key(), listOf(paragraph("p", 13)))
        assertEquals(3, plan.pages.size)
        val fragments = plan.pages.flatMap { it.fragments }.filterIsInstance<PageFragment.Text>()
        assertEquals(listOf(0, 25, 50), fragments.map { it.startOffset })
        assertEquals(listOf(25, 50, 65), fragments.map { it.endOffset })
        assertEquals(listOf(0, 5, 10), fragments.map { it.firstLine })
        assertTrue(fragments.all { it.yPx + it.heightPx <= 100f })
    }

    @Test fun aParagraphThatFitsOnePageIsKeptTogetherWhenThePreviousPageHasInsufficientSpace() {
        val plan = planChapterPages(key(), listOf(paragraph("a", 4), paragraph("b", 2)))
        assertEquals(2, plan.pages.size)
        assertEquals(listOf("a"), plan.pages.first().fragments.map { it.blockId })
        assertEquals(0f, plan.pages.last().fragments.single().yPx, 0.001f)
    }

    @Test fun longIllustrationFitsTheReadableViewportAndItsFollowingTextIsNotSkipped() {
        val image = MeasuredChapterBlock.Image("image", 1200f, 6000f)
        val plan = planChapterPages(key(), listOf(paragraph("a", 1), image, paragraph("b", 1)))
        assertEquals(3, plan.pages.size)
        val fragment = plan.pages[1].fragments.single() as PageFragment.Image
        assertEquals(20f, fragment.widthPx, 0.001f)
        assertEquals(100f, fragment.heightPx, 0.001f)
        assertEquals("b", plan.pages.last().fragments.single().blockId)
    }

    @Test fun paragraphGapsDoNotCreateBlankPagesOrRepeatTheTail() {
        val plan = planChapterPages(key(), listOf(paragraph("a", 5, gap = 35f), paragraph("b", 1, gap = 500f)))
        assertEquals(2, plan.pages.size)
        assertEquals(20f, plan.pages.last().usedHeightPx, 0.001f)
        assertEquals(listOf("a", "b"), plan.pages.flatMap { it.fragments }.map { it.blockId })
    }

    @Test fun pageAnchorsRestoreInsideTheSameTextAfterFontOrImageRelayout() {
        val old = planChapterPages(key(), listOf(paragraph("p", 13)))
        val anchor = old.pages[1].startAnchor
        val new = planChapterPages(key(height = 60, revision = "2"), listOf(paragraph("p", 13)))
        assertEquals(1, new.pageForAnchor(anchor))
        assertEquals(2, new.pageForAnchor(ReaderAnchor(10, 20, "p", 30)))
        assertEquals(4, new.pageForAnchor(ReaderAnchor(10, 20, "p", 999)))
    }

    @Test fun advancingAndReturningVisitExactlyTheSamePagesAndGateChapterRequests() {
        val plan = planChapterPages(key(), listOf(paragraph("p", 13)))
        val navigator = ReaderPageNavigator(plan)
        assertEquals(PageMove.Page(1), navigator.next())
        assertEquals(PageMove.Page(2), navigator.next())
        assertEquals(PageMove.Page(1), navigator.previous())
        assertEquals(PageMove.Page(0), navigator.previous())
        val request = navigator.previous() as PageMove.Chapter
        assertEquals(ChapterDirection.Previous, request.direction)
        assertEquals(PageMove.Busy, navigator.previous())
        val prior = planChapterPages(key().copy(chapterId = 19), listOf(paragraph("prior", 9)))
        assertTrue(navigator.acceptChapter(request.requestId, prior))
        assertEquals(1, navigator.pageIndex)
        assertFalse(navigator.acceptChapter(request.requestId, plan))
        val next = navigator.next() as PageMove.Chapter
        assertEquals(ChapterDirection.Next, next.direction)
        assertEquals(PageMove.Busy, navigator.next())
        assertTrue(navigator.acceptChapter(next.requestId, plan))
        assertEquals(0, navigator.pageIndex)
    }

    @Test fun aFailedChapterRequestCanRetryButLateCallbacksCannotCommitTwice() {
        val plan = planChapterPages(key(), listOf(paragraph("p", 1)))
        val navigator = ReaderPageNavigator(plan)
        val first = navigator.next() as PageMove.Chapter
        navigator.failChapter(first.requestId)
        val retry = navigator.next() as PageMove.Chapter
        assertNotEquals(first.requestId, retry.requestId)
        assertFalse(navigator.acceptChapter(first.requestId, plan.copy(key = key().copy(chapterId = 21))))
        assertTrue(navigator.acceptChapter(retry.requestId, plan.copy(key = key().copy(chapterId = 21))))
    }

    @Test fun outdatedLayoutJobsAreRejectedAfterNewSettingsOrNavigation() {
        val gate = PageLayoutGeneration()
        val old = gate.request(key())
        val new = gate.request(key(height = 60))
        assertFalse(gate.isCurrent(old))
        assertTrue(gate.isCurrent(new))
        gate.invalidate()
        assertFalse(gate.isCurrent(new))
    }

    @Test fun emptyDocumentHasAnExplicitEmptyPageInsteadOfAnIndexCrash() {
        val plan = planChapterPages(key(), emptyList())
        assertEquals(1, plan.pages.size)
        assertTrue(plan.pages.single().fragments.isEmpty())
    }

    @Test(expected = ReaderViewportTooSmall::class)
    fun aSingleGlyphLineTooTallForTheViewportIsReportedRatherThanSilentlyClipped() {
        planChapterPages(key(height = 10), listOf(paragraph("p", 1)))
    }
}
