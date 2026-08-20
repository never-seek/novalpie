package com.novalpie.nativeapp.ui

import android.os.Build
import android.icu.text.Transliterator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.novalpie.nativeapp.model.ChineseVariant

/** Current display conversion for ordinary Compose text. Rich annotated text keeps its source spans. */
internal val LocalChineseVariant = staticCompositionLocalOf { ChineseVariant.Original }

internal fun ChineseVariant.displayLabel(): String = when (this) {
    ChineseVariant.Original -> "原文模式"
    ChineseVariant.Traditional -> "繁体模式"
    ChineseVariant.Simplified -> "简体模式"
}

internal fun ChineseVariant.description(): String = when (this) {
    ChineseVariant.Original -> "保留源站原始文字，不做本地转换"
    ChineseVariant.Traditional -> "本地转换为繁体中文，源站数据不会被修改"
    ChineseVariant.Simplified -> "本地转换为简体中文，源站数据不会被修改"
}

/**
 * The website applies OpenCC to the rendered DOM, not to its API payload. Android ICU provides the
 * same script-level transformations on Android 10+, while a small compatibility map keeps the
 * setting safe on the app's Android 6 minimum without ever changing source data.
 */
internal fun convertChineseVariantText(text: String, variant: ChineseVariant): String {
    if (text.isBlank() || variant == ChineseVariant.Original) return text
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val transliteratorId = when (variant) {
            ChineseVariant.Traditional -> "Simplified-Traditional"
            ChineseVariant.Simplified -> "Traditional-Simplified"
            ChineseVariant.Original -> return text
        }
        runCatching { Transliterator.getInstance(transliteratorId).transliterate(text) }
            .getOrNull()
            ?.let { return it }
    }
    return fallbackChineseVariantText(text, variant)
}

private fun fallbackChineseVariantText(text: String, variant: ChineseVariant): String {
    val table = when (variant) {
        ChineseVariant.Traditional -> simplifiedToTraditional
        ChineseVariant.Simplified -> traditionalToSimplified
        ChineseVariant.Original -> emptyMap()
    }
    return buildString(text.length) {
        text.forEach { character -> append(table[character] ?: character) }
    }
}

private val simplifiedToTraditional = mapOf(
    '书' to '書', '画' to '畫', '读' to '讀', '说' to '說', '话' to '話', '评' to '評',
    '论' to '論', '网' to '網', '页' to '頁', '码' to '碼', '号' to '號', '后' to '後',
    '发' to '發', '现' to '現', '开' to '開', '关' to '關', '换' to '換', '显' to '顯',
    '示' to '示', '组' to '組', '织' to '織', '历' to '歷', '史' to '史', '签' to '簽',
    '标' to '標', '题' to '題', '节' to '節', '点' to '點', '击' to '擊', '载' to '載',
    '线' to '線', '续' to '續', '关' to '關', '键' to '鍵', '设' to '設', '置' to '置',
    '简' to '簡', '体' to '體', '繁' to '繁', '个' to '個', '条' to '條', '项' to '項',
    '务' to '務', '户' to '戶', '页' to '頁', '图' to '圖', '片' to '片', '记' to '記',
    '录' to '錄', '请' to '請', '输' to '輸', '删' to '刪', '除' to '除', '选' to '選',
    '择' to '擇', '确' to '確', '认' to '認', '错' to '錯', '误' to '誤', '数' to '數',
    '据' to '據', '资' to '資', '讯' to '訊', '页' to '頁', '览' to '覽', '统' to '統',
    '计' to '計', '别' to '別', '级' to '級', '转' to '轉', '换' to '換', '测' to '測'
)

private val traditionalToSimplified = simplifiedToTraditional.entries.associate { (simplified, traditional) ->
    traditional to simplified
}

@Composable
internal fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val variant = LocalChineseVariant.current
    val converted = remember(text, variant) { convertChineseVariantText(text, variant) }
    MaterialText(
        text = converted,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
internal fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
