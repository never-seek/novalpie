package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.ReaderReplacementRegexFlag
import com.novalpie.nativeapp.model.ReaderReplacementRule

/**
 * NovelPia stores regular-expression glossary sources in the same compact form used by its
 * Nuxt reader: `re:/pattern/gim`. Native reader rules keep the human-editable pattern and flags
 * separately, so the wire representation must be decoded before it reaches the renderer.
 */
internal data class WebsiteReaderReplacementSource(
    val source: String,
    val isRegex: Boolean,
    val regexFlags: Set<ReaderReplacementRegexFlag> = emptySet(),
)

internal fun decodeWebsiteReaderReplacementSource(value: String): WebsiteReaderReplacementSource {
    if (!value.startsWith(WEBSITE_REGEX_PREFIX) || value.length <= WEBSITE_REGEX_PREFIX.length + 1) {
        return WebsiteReaderReplacementSource(source = value, isRegex = false)
    }

    val encoded = value.removePrefix(WEBSITE_REGEX_PREFIX)
    if (!encoded.startsWith('/')) {
        return WebsiteReaderReplacementSource(source = value, isRegex = false)
    }

    var escaped = false
    var closingSlash = -1
    for (index in 1 until encoded.length) {
        when {
            escaped -> escaped = false
            encoded[index] == '\\' -> escaped = true
            encoded[index] == '/' -> {
                closingSlash = index
                break
            }
        }
    }
    if (closingSlash <= 1) return WebsiteReaderReplacementSource(source = value, isRegex = false)

    val suffix = encoded.substring(closingSlash + 1)
    if (suffix.any { !it.isLetter() }) return WebsiteReaderReplacementSource(source = value, isRegex = false)

    val flags = buildSet {
        if ('i' in suffix.lowercase()) add(ReaderReplacementRegexFlag.IgnoreCase)
        if ('m' in suffix.lowercase()) add(ReaderReplacementRegexFlag.Multiline)
        if ('s' in suffix.lowercase()) add(ReaderReplacementRegexFlag.DotMatchesAll)
    }
    return WebsiteReaderReplacementSource(
        source = encoded.substring(1, closingSlash).replace("\\/", "/"),
        isRegex = true,
        regexFlags = flags,
    )
}

/** Serializes the local regex fields back to the exact source string accepted by the website. */
internal fun encodeWebsiteReaderReplacementSource(rule: ReaderReplacementRule): String {
    if (!rule.isRegex) return rule.source
    val escapedPattern = buildString {
        var previousBackslashEscapesCurrent = false
        rule.source.forEach { character ->
            if (character == '/' && !previousBackslashEscapesCurrent) append('\\')
            append(character)
            previousBackslashEscapesCurrent = if (character == '\\') {
                !previousBackslashEscapesCurrent
            } else {
                false
            }
        }
    }
    val flags = buildString {
        append('g')
        if (ReaderReplacementRegexFlag.IgnoreCase in rule.regexFlags) append('i')
        if (ReaderReplacementRegexFlag.Multiline in rule.regexFlags) append('m')
        if (ReaderReplacementRegexFlag.DotMatchesAll in rule.regexFlags) append('s')
    }
    return "$WEBSITE_REGEX_PREFIX/$escapedPattern/$flags"
}

private const val WEBSITE_REGEX_PREFIX = "re:"
