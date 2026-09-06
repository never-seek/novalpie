package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.novalpie.nativeapp.model.NovelCard

/** Measure the short identity labels once per visible row; never truncate an author's/book's name. */
@Composable
internal fun compactLibraryRowTextSlots(books:List<NovelCard>,cardWidth:Dp):CompactLibraryBookCardTextSlots {
    val density=LocalDensity.current
    val variant=LocalChineseVariant.current
    val measurer=rememberTextMeasurer(cacheSize=16)
    val width=with(density){cardWidth.roundToPx().coerceAtLeast(1)}
    val titleStyle=MaterialTheme.typography.titleSmall.copy(fontWeight=FontWeight.Bold)
    val authorStyle=MaterialTheme.typography.labelMedium
    return remember(books,width,density.fontScale,variant,titleStyle,authorStyle) {
        CompactLibraryBookCardTextSlots(
            titleLines=books.maxOfOrNull{measurer.measure(convertChineseVariantText(it.title,variant),titleStyle,constraints=Constraints(maxWidth=width)).lineCount}?.coerceAtLeast(2) ?: 2,
            authorLines=books.maxOfOrNull{measurer.measure(convertChineseVariantText(it.author ?: "未知作者",variant),authorStyle,constraints=Constraints(maxWidth=width)).lineCount}?.coerceAtLeast(1) ?: 1,
        )
    }
}
