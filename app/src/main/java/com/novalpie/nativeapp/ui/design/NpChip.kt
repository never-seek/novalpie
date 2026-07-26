package com.novalpie.nativeapp.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

/**
 * The app's one chip implementation.
 *
 * There were five: `NovelTagPill`, `NovelSourcePill`, `LibraryStatPill`, `BookDetailFactLabel` and
 * `CompactForumBadge`, each with its own padding, radius and colour choice. Because the colour was
 * chosen per call site rather than by meaning, a search result could show `上传` in grey next to
 * `已完结` and `奇幻` in a completely different colour, with none of it signifying anything.
 *
 * [NpChipTone] fixes that by making the colour carry the meaning. A reader can then learn the
 * palette once: neutral facts are grey, the source platform is blue, publication status is teal,
 * and a warning is red.
 */
enum class NpChipTone {
    /** Word counts, view counts, dates — quantitative metadata. */
    Neutral,

    /** The source platform: NovelPia, 上传. Identifies where a work came from. */
    Source,

    /** Publication status: 连载中, 已完结. */
    Status,

    /** Website tags and genres, the user's own vocabulary. */
    Tag,

    /** Something the user should notice: 19禁, banned account, failed job. */
    Warning,
}

private data class NpChipColors(val container: Color, val content: Color, val border: Color?)

@Composable
private fun chipColors(tone: NpChipTone): NpChipColors {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        NpChipTone.Neutral -> NpChipColors(
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurfaceVariant,
            border = null,
        )
        NpChipTone.Source -> NpChipColors(
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            border = null,
        )
        NpChipTone.Status -> NpChipColors(
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
            border = null,
        )
        // Tags are the densest chips on a card, so they are outlined rather than filled: a dozen
        // filled chips in a row turns a book card into a colour swatch.
        NpChipTone.Tag -> NpChipColors(
            container = Color.Transparent,
            content = scheme.onSurfaceVariant,
            border = scheme.outlineVariant,
        )
        NpChipTone.Warning -> NpChipColors(
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
            border = null,
        )
    }
}

/**
 * A non-interactive metadata chip.
 *
 * Deliberately not clickable. The old UI had seven `AssistChip(onClick = {})` instances that
 * rippled on touch and announced themselves as buttons to TalkBack while doing nothing; a chip
 * that is only a label should not pretend otherwise. Use a real button when an action is intended.
 */
@Composable
fun NpChip(
    label: String,
    tone: NpChipTone = NpChipTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val colors = chipColors(tone)
    val shape = RoundedCornerShape(NpChipRadius)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = colors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(colors.container)
            .then(
                if (colors.border != null) {
                    Modifier.border(BorderStroke(NovalPieSize.hairline, colors.border), shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = NovalPieSpacing.sm, vertical = NovalPieSpacing.xxs),
    )
}

/**
 * Wrapping row of chips.
 *
 * The audit counted 59 `LazyRow` chip rails against only 5 `FlowRow`s. A horizontally scrolling
 * rail clips its last chip mid-glyph with no scroll affordance, which is why the search filter row
 * showed a truncated "字…" at the screen edge — the user cannot tell there is more. Chips wrap
 * instead of scrolling, so nothing is ever hidden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NpChipRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = NovalPieSpacing.xs,
    verticalSpacing: androidx.compose.ui.unit.Dp = NovalPieSpacing.xs,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        content()
    }
}

private val NpChipRadius = NovalPieRadius.xs

@Preview(name = "Chip tones", showBackground = true)
@Composable
private fun NpChipPreview() {
    com.novalpie.nativeapp.ui.NovalPieTheme {
        NpChipRow(modifier = Modifier.padding(NovalPieSpacing.lg)) {
            NpChip("12.4万字", NpChipTone.Neutral)
            NpChip("NovelPia", NpChipTone.Source)
            NpChip("已完结", NpChipTone.Status)
            NpChip("奇幻", NpChipTone.Tag)
            NpChip("19禁", NpChipTone.Warning)
        }
    }
}
