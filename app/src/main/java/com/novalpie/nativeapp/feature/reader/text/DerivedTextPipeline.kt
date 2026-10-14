package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import com.novalpie.nativeapp.ui.applyReaderReplacementRules

internal data class DerivedTextResult(
    val original: String,
    val markup: String,
    val invalidRuleIds: List<String>,
)

/**
 * Shared reader/export transformation. Authored markup is never round-tripped through an HTML
 * serializer: tags, attributes, URLs and image occurrences retain their exact original bytes.
 * Replacement output is text, with a narrow compatibility exception for line-break syntax.
 */
internal object DerivedTextPipeline {
    /** Input has already been decoded/parsed. Never decode entities or interpret output markup. */
    fun transformVisibleText(original: String, rules: List<ReaderReplacementRule>, chapterOrder: Int?): String {
        if (rules.isEmpty()) return original
        val result = StringBuilder(original.length)
        var cursor = 0
        val urls = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
        fun append(until: Int) {
            if (until > cursor) result.append(breakTag.replace(
                applyReaderReplacementRules(original.substring(cursor, until), rules, chapterOrder, ReaderReplacementTarget.Content).text, "\n"))
        }
        urls.findAll(original).forEach { url -> append(url.range.first); result.append(url.value); cursor = url.range.last + 1 }
        append(original.length)
        return result.toString()
    }

    fun transform(original:String,rules:List<ReaderReplacementRule>,chapterOrder:Int?):DerivedTextResult {
        if(rules.isEmpty())return DerivedTextResult(original,original,emptyList())
        return transformNodes(original,0,true) {text->
            applyReaderReplacementRules(text,rules,chapterOrder,ReaderReplacementTarget.Content)
        }
    }

    /** Original markup/URLs are opaque; output is safely encoded for the same source grammar. */
    fun transformMarkupTextNodes(original:String,transform:(String)->String):String =
        transformNodes(original,0,true) {text->com.novalpie.nativeapp.ui.ReaderReplacementApplyResult(transform(text))}.markup

    /** Export formatting escapes the complete result afterwards, so escape replacement text only once. */
    fun transformLiteralTextNodes(original:String,transform:(String)->String):String =
        transformNodes(original,0,false) {text->com.novalpie.nativeapp.ui.ReaderReplacementApplyResult(transform(text))}.markup

    /** TXT keeps literal replacement characters in plain sources, but preserves source HTML grammar. */
    fun transformTxtTextNodes(original:String,transform:(String)->String):String =
        transformNodes(original,0,htmlTag.containsMatchIn(original),lineBreakAsHtml=false) {text->com.novalpie.nativeapp.ui.ReaderReplacementApplyResult(transform(text))}.markup

    /** Visit source structure before visible Markdown formatting, never exposing attributes to rules. */
    fun renderSourceTextNodes(original:String,renderText:(String)->String,renderProtected:(String)->String):String =
        transformNodes(original,0,false,renderProtected) {text->com.novalpie.nativeapp.ui.ReaderReplacementApplyResult(renderText(text))}.markup

    private fun transformNodes(original:String,depth:Int,encodeMarkup:Boolean,
        renderProtected:(String)->String={it},lineBreakAsHtml:Boolean=encodeMarkup,
        transform:(String)->com.novalpie.nativeapp.ui.ReaderReplacementApplyResult):DerivedTextResult {
        val html = htmlTag.containsMatchIn(original)
        val result = StringBuilder(original.length)
        val invalid = linkedSetOf<String>()
        var cursor = 0
        var index = 0

        fun appendText(until: Int) {
            if (until <= cursor) return
            val source = original.substring(cursor, until)
            val decoded = decodeEntities(source)
            val applied = transform(decoded)
            invalid += applied.invalidRuleIds
            if (applied.text == decoded) {
                result.append(source)
            } else {
                val withBreaks = breakTag.replace(applied.text, "\n")
                val safe = if(encodeMarkup&&(html||decoded!=source||htmlTag.containsMatchIn(withBreaks)))
                    escapeMarkupText(withBreaks) else withBreaks
                result.append(if(lineBreakAsHtml&&html)safe.replace("\n","<br/>")else safe)
            }
        }

        while (index < original.length) {
            var protectedEnd: Int? = null
            var transformedLink: String? = null
            if (original[index] == '<') {
                if (original.startsWith("<!--", index)) {
                    protectedEnd = original.indexOf("-->", index + 4).takeIf { it >= 0 }?.plus(3) ?: original.length
                } else {
                    val tag = htmlTag.matchAt(original, index)
                    if (tag != null) {
                        protectedEnd = tag.range.last + 1
                        val name = tag.value.drop(1).takeWhile(Char::isLetter).lowercase()
                        if (name in setOf("script", "style")) {
                            val close = original.indexOf("</$name", protectedEnd, ignoreCase = true)
                            protectedEnd = if (close >= 0) original.indexOf('>', close).takeIf { it >= 0 }?.plus(1) ?: original.length else original.length
                        }
                    }
                }
            } else if (original[index] == '[' || original.startsWith("![", index)) {
                val marker = imageMarker.matchAt(original, index)
                if (marker != null) protectedEnd = marker.range.last + 1
                else {
                    val image = original[index] == '!'
                    val openLabel = if (image) index + 1 else index
                    val closeLabel = balancedEnd(original, openLabel, '[', ']')
                    if (closeLabel != null && original.getOrNull(closeLabel + 1) == '(') {
                        val closeUrl = balancedEnd(original, closeLabel + 1, '(', ')')
                        if (closeUrl != null) {
                            protectedEnd = closeUrl + 1
                            if (!image && depth < 8) {
                                val label = transformNodes(original.substring(openLabel + 1, closeLabel),depth+1,encodeMarkup,renderProtected,lineBreakAsHtml,transform)
                                invalid += label.invalidRuleIds
                                transformedLink = "[${label.markup}]" + renderProtected(original.substring(closeLabel + 1,protectedEnd))
                            }
                        }
                    }
                }
            } else if (original.startsWith("https://", index, true) || original.startsWith("http://", index, true)) {
                protectedEnd = index
                while (protectedEnd < original.length && !original[protectedEnd].isWhitespace() && original[protectedEnd] !in "<>\"'") protectedEnd++
            }

            if (protectedEnd != null) {
                appendText(index)
                result.append(transformedLink ?: renderProtected(original.substring(index,protectedEnd)))
                cursor = protectedEnd
                index = protectedEnd
            } else index++
        }
        appendText(original.length)
        return DerivedTextResult(original, result.toString(), invalid.toList())
    }

    private fun balancedEnd(value: String, start: Int, open: Char, close: Char): Int? {
        var nesting = 0
        var index = start
        while (index < value.length) {
            val character = value[index]
            if (character == '\\') { index += 2; continue }
            if (character == open) nesting++
            if (character == close && --nesting == 0) return index
            index++
        }
        return null
    }

    fun decodeEntities(value: String): String = entities.replace(value) { match ->
        when (val body = match.groupValues[1]) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "nbsp" -> "\u00a0"
            else -> {
                val code = if (body.startsWith("#x", true)) body.drop(2).toIntOrNull(16) else body.removePrefix("#").toIntOrNull()
                if (code != null && Character.isValidCodePoint(code) && code !in 0xD800..0xDFFF) String(Character.toChars(code)) else match.value
            }
        }
    }

    private fun escapeMarkupText(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private val entities = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos|nbsp);")
    private val breakTag = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val htmlTag = Regex("""</?[A-Za-z][A-Za-z0-9:-]*(?:\s+(?:[^>"']|"[^"]*"|'[^']*')*)?\s*/?>""")
    private val imageMarker = Regex("""\[\[\s*img\s*:\s*\d+\s*]]|\[图片[^]\r\n]*]""", RegexOption.IGNORE_CASE)
}
