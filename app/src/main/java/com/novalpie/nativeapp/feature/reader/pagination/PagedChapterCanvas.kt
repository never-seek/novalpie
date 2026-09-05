package com.novalpie.nativeapp.feature.reader.pagination

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * A page is a measured document partition, not a scrolled list. Text fragments share the complete
 * paragraph layout, retaining justification/indent/spans in both directions. Clip boundaries are
 * exact measured line boundaries, never an opaque overlay covering potentially visible glyphs.
 * Image rendering/long-press remains a normal composable supplied by the reader feature.
 */
@Composable
internal fun PagedChapterCanvas(
    measured: MeasuredChapterDocument,
    pageIndex: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
    image: @Composable (ChapterDocumentBlock.Image, Modifier) -> Unit,
) {
    val page = measured.plan.pages[pageIndex.coerceIn(measured.plan.pages.indices)]
    val density = LocalDensity.current
    val accessibleText = page.fragments.filterIsInstance<PageFragment.Text>().joinToString("\n") { slice ->
        measured.textLayouts.getValue(slice.blockId).layoutInput.text.text.substring(slice.startOffset, slice.endOffset)
    }
    Box(modifier = modifier.semantics { text = AnnotatedString(accessibleText) }) {
        Canvas(Modifier.fillMaxSize()) {
            page.fragments.filterIsInstance<PageFragment.Text>().forEach { slice ->
                clipRect(left = 0f, top = slice.yPx, right = size.width, bottom = slice.yPx + slice.heightPx) {
                    drawText(
                        textLayoutResult = measured.textLayouts.getValue(slice.blockId),
                        color = textColor,
                        topLeft = Offset(0f, slice.yPx - slice.sourceTopPx),
                    )
                }
            }
        }
        page.fragments.filterIsInstance<PageFragment.Image>().forEach { slice ->
            val block = measured.document.blocks.first { it.id == slice.blockId } as ChapterDocumentBlock.Image
            image(
                block,
                Modifier.offset {
                    IntOffset(((measured.plan.key.widthPx - slice.widthPx) / 2f).roundToInt(), slice.yPx.roundToInt())
                }.requiredSize(with(density) { slice.widthPx.toDp() }, with(density) { slice.heightPx.toDp() }),
            )
        }
    }
}
