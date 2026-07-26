package com.novalpie.nativeapp.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The app's type scale.
 *
 * The previous theme never passed `typography` to `MaterialTheme`, so the whole app ran on
 * Material's baseline Roboto scale. Two things follow from that, and both are visible.
 *
 * First, the baseline line heights are tuned for Latin text. Chinese glyphs fill their em box
 * far more completely than Latin lowercase, so body copy set at Material's 1.43 ratio reads as
 * cramped, and it matters most in the reader where paragraphs run long. Every style below uses
 * a deliberately looser ratio, and `LineHeightStyle` with `Trim.None` keeps the first and last
 * lines from losing their leading -- without that, a one-line label sits optically high.
 *
 * Second, the display styles were unreachable dead weight: nothing in a phone-sized reading app
 * needs 57sp. The scale is compressed at the top so headline/title steps are actually
 * distinguishable at the sizes the app uses, which is what the audit meant by most secondary
 * screens showing their title twice in two styles about 24dp apart -- the two styles barely
 * differed.
 *
 * FontFamily.Default resolves to the platform CJK face, which is correct: bundling a Chinese
 * font would add megabytes for a worse result than the system's own.
 */
private val cjkLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun novalPieTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = 0.0,
): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    // Chinese text needs no tracking; Latin-derived positive tracking makes CJK look gappy.
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = cjkLineHeight,
)

internal val NovalPieTypography = Typography(
    // Display: only reachable on the largest hero surfaces. Kept small enough to be usable.
    displayLarge = novalPieTextStyle(40, 48, FontWeight.Bold),
    displayMedium = novalPieTextStyle(34, 42, FontWeight.Bold),
    displaySmall = novalPieTextStyle(28, 36, FontWeight.Bold),

    // Headline: screen titles.
    headlineLarge = novalPieTextStyle(26, 34, FontWeight.Bold),
    headlineMedium = novalPieTextStyle(22, 30, FontWeight.Bold),
    headlineSmall = novalPieTextStyle(20, 28, FontWeight.SemiBold),

    // Title: section headers and card titles. The three steps are clearly separated so a
    // header and a subheader cannot be mistaken for each other.
    titleLarge = novalPieTextStyle(18, 26, FontWeight.SemiBold),
    titleMedium = novalPieTextStyle(16, 24, FontWeight.SemiBold),
    titleSmall = novalPieTextStyle(14, 21, FontWeight.SemiBold),

    // Body: reading copy. 1.6-1.65 line-height ratio, which is the usual recommendation for
    // long-form Chinese text and noticeably looser than Material's default.
    bodyLarge = novalPieTextStyle(16, 26),
    bodyMedium = novalPieTextStyle(14, 23),
    bodySmall = novalPieTextStyle(13, 21),

    // Label: buttons, chips, metadata. Note labelSmall is 12sp, not Material's 11sp -- 11sp
    // Chinese is genuinely hard to read, and this app uses labelSmall for the facts chips that
    // carry word counts, statuses and tags.
    labelLarge = novalPieTextStyle(15, 22, FontWeight.Medium),
    labelMedium = novalPieTextStyle(13, 18, FontWeight.Medium),
    labelSmall = novalPieTextStyle(12, 17, FontWeight.Medium),
)
