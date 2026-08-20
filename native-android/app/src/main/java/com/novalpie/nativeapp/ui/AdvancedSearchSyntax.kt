package com.novalpie.nativeapp.ui

/**
 * A compact native counterpart to the source site's advanced search grammar.
 *
 * It deliberately resolves only syntax that the mobile source was observed to
 * turn into `/api/search` parameters. Unknown text remains a normal keyword so
 * a future source-side search feature is not silently discarded.
 */
internal data class AdvancedSearchParseResult(
    val keyword: String = "",
    val scope: String? = null,
    val requiredTags: List<String> = emptyList(),
    val tagsAny: List<String> = emptyList(),
    val tagsExpression: String? = null,
    val blockedTags: List<String> = emptyList(),
    val blockedTerms: List<String> = emptyList(),
    val platform: String? = null,
    val type: String? = null,
    val status: String? = null,
    val adultFilter: String? = null,
    val matchType: String? = null,
    val minWordCount: Long? = null,
    val maxWordCount: Long? = null,
    val errors: List<String> = emptyList()
)

internal data class ResolvedSearchRequest(
    val keyword: String,
    val sortBy: String,
    val sortOrder: String,
    val scope: String,
    val matchType: String,
    val adultFilter: String,
    val source: String,
    val platform: String? = null,
    val type: String? = null,
    val status: String? = null,
    val minWordCount: Long? = null,
    val maxWordCount: Long? = null,
    val requiredTags: List<String> = emptyList(),
    val tagsAny: List<String> = emptyList(),
    val tagsExpression: String? = null,
    val blockedTags: List<String> = emptyList(),
    val blockedTerms: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

internal fun resolveSearchRequest(keyword: String, options: SearchOptions): ResolvedSearchRequest {
    val baseMin = searchMinWordCount(options.wordCountRange)
    val baseMax = searchMaxWordCount(options.wordCountRange)
    if (!options.advancedSyntaxEnabled) {
        return ResolvedSearchRequest(
            keyword = keyword.trim(),
            sortBy = options.sortBy,
            sortOrder = options.sortOrder,
            scope = options.scope,
            matchType = options.matchType,
            adultFilter = options.adultFilter,
            source = options.source,
            minWordCount = baseMin,
            maxWordCount = baseMax,
            requiredTags = options.requiredTags,
            blockedTags = options.blockedTags
        )
    }

    val parsed = parseAdvancedSearchSyntax(keyword)
    return ResolvedSearchRequest(
        keyword = parsed.keyword,
        sortBy = options.sortBy,
        sortOrder = options.sortOrder,
        scope = parsed.scope ?: options.scope,
        matchType = parsed.matchType ?: options.matchType,
        adultFilter = parsed.adultFilter ?: options.adultFilter,
        // The source uses `platform`, not its basic-form `source`, for syntax mode.
        source = if (parsed.platform == null) options.source else "",
        platform = parsed.platform,
        type = parsed.type,
        status = parsed.status,
        minWordCount = parsed.minWordCount ?: baseMin,
        maxWordCount = parsed.maxWordCount ?: baseMax,
        requiredTags = normalizeSearchTagList(options.requiredTags + parsed.requiredTags),
        tagsAny = normalizeSearchTagList(parsed.tagsAny),
        tagsExpression = parsed.tagsExpression,
        blockedTags = normalizeSearchTagList(options.blockedTags + parsed.blockedTags),
        blockedTerms = normalizeSearchTerms(parsed.blockedTerms),
        errors = parsed.errors
    )
}

internal fun parseAdvancedSearchSyntax(source: String): AdvancedSearchParseResult {
    val tokens = tokenizeAdvancedSearch(source)
    val keyword = mutableListOf<String>()
    val requiredTags = mutableListOf<String>()
    val tagsAny = mutableListOf<String>()
    val blockedTags = mutableListOf<String>()
    val blockedTerms = mutableListOf<String>()
    val errors = mutableListOf<String>()
    var scope: String? = null
    var tagsExpression: String? = null
    var platform: String? = null
    var type: String? = null
    var status: String? = null
    var adultFilter: String? = null
    var matchType: String? = null
    var minWordCount: Long? = null
    var maxWordCount: Long? = null
    var index = 0

    fun appendTag(value: String, target: MutableList<String>) {
        parseSearchTagInput(value).forEach(target::add)
    }

    fun parseTag(value: String, blocked: Boolean) {
        val target = if (blocked) blockedTags else requiredTags
        when {
            value.isBlank() -> errors += "标签条件缺少内容"
            value.contains('|') -> {
                if (blocked) {
                    errors += "屏蔽标签不支持 OR 表达式"
                } else {
                    value.split('|').forEach(tagsAny::add)
                }
            }

            else -> appendTag(value, target)
        }
    }

    while (index < tokens.size) {
        val token = tokens[index]
        when {
            token.equals("NOT", ignoreCase = true) -> {
                val next = tokens.getOrNull(index + 1)
                when {
                    next == null -> errors += "NOT 后缺少条件"
                    next.startsWith("tag:", ignoreCase = true) -> {
                        parseTag(next.substringAfter(':'), blocked = true)
                        index += 1
                    }

                    next.startsWith("-tag:", ignoreCase = true) -> {
                        parseTag(next.substringAfter(':'), blocked = true)
                        index += 1
                    }

                    else -> {
                        blockedTerms += next
                        index += 1
                    }
                }
            }

            token.startsWith("-tag:", ignoreCase = true) -> {
                parseTag(token.substringAfter(':'), blocked = true)
            }

            token.startsWith("tag:(", ignoreCase = true) -> {
                val (expression, endIndex) = consumeTagExpression(tokens, index)
                if (expression == null) {
                    errors += "标签表达式缺少右括号"
                } else {
                    tagsExpression = expression
                    index = endIndex
                }
            }

            token.startsWith("tag:", ignoreCase = true) -> {
                parseTag(token.substringAfter(':'), blocked = false)
            }

            // Source-side syntax treats both explicit AND and OR as separators for ordinary
            // terms and successive tag directives. Parenthesized tag expressions are consumed
            // above as `tags_expr`, so an operator reaching here must never leak into q.
            token.equals("AND", ignoreCase = true) || token.equals("OR", ignoreCase = true) -> Unit

            token.startsWith("#") && token.length > 1 -> appendTag(token.drop(1), requiredTags)

            token.startsWith("in:", ignoreCase = true) -> {
                scope = parseScope(token.substringAfter(':')) ?: run {
                    errors += "不支持的搜索范围：${token.substringAfter(':')}"
                    null
                }
            }

            token.startsWith("@", ignoreCase = true) && token.contains(':') -> {
                val field = token.substringBefore(':').drop(1)
                val fieldScope = parseScope(field)
                if (fieldScope == null) {
                    keyword += token
                } else {
                    scope = fieldScope
                    token.substringAfter(':').takeIf(String::isNotBlank)?.let(keyword::add)
                }
            }

            token.startsWith("word:", ignoreCase = true) -> {
                val wordRange = parseWordRange(token.substringAfter(':'))
                if (wordRange == null) errors += "无法识别字数条件：$token"
                else {
                    minWordCount = wordRange.first ?: minWordCount
                    maxWordCount = wordRange.second ?: maxWordCount
                }
            }

            token.startsWith("word>=", ignoreCase = true) || token.startsWith("word>", ignoreCase = true) -> {
                val value = token.substringAfter('>').removePrefix("=")
                val parsed = parseWordAmount(value)
                if (parsed == null) errors += "无法识别字数条件：$token" else minWordCount = parsed
            }

            token.startsWith("word<=", ignoreCase = true) || token.startsWith("word<", ignoreCase = true) -> {
                val value = token.substringAfter('<').removePrefix("=")
                val parsed = parseWordAmount(value)
                if (parsed == null) errors += "无法识别字数条件：$token" else maxWordCount = parsed
            }

            token.startsWith("platform:", ignoreCase = true) -> {
                platform = token.substringAfter(':').trim().takeIf(String::isNotBlank)
            }

            token.startsWith("type:", ignoreCase = true) -> {
                type = token.substringAfter(':').trim().takeIf(String::isNotBlank)
            }

            token.startsWith("status:", ignoreCase = true) -> {
                status = token.substringAfter(':').trim().takeIf(String::isNotBlank)
            }

            token.startsWith("adult:", ignoreCase = true) -> {
                val value = token.substringAfter(':').trim().lowercase()
                adultFilter = when (value) {
                    "only", "adult", "adult_only" -> "adult_only"
                    "all" -> "all"
                    "unrestricted", "safe", "全年龄" -> "unrestricted"
                    else -> {
                        errors += "不支持的成人筛选：$value"
                        null
                    }
                }
            }

            token.startsWith("match:", ignoreCase = true) -> {
                val value = token.substringAfter(':').trim().lowercase()
                matchType = when (value) {
                    "ai" -> "ai"
                    "strict", "fuzzy_strict" -> "fuzzy_strict"
                    "loose", "fuzzy_loose" -> "fuzzy_loose"
                    "exact" -> "exact"
                    else -> {
                        errors += "不支持的匹配模式：$value"
                        null
                    }
                }
            }

            else -> keyword += token
        }
        index += 1
    }

    if (minWordCount != null && maxWordCount != null && minWordCount > maxWordCount) {
        errors += "字数范围下限不能大于上限"
    }

    return AdvancedSearchParseResult(
        keyword = keyword.joinToString(" ").trim(),
        scope = scope,
        requiredTags = normalizeSearchTagList(requiredTags),
        tagsAny = normalizeSearchTagList(tagsAny),
        tagsExpression = tagsExpression,
        blockedTags = normalizeSearchTagList(blockedTags),
        blockedTerms = normalizeSearchTerms(blockedTerms),
        platform = platform,
        type = type,
        status = status,
        adultFilter = adultFilter,
        matchType = matchType,
        minWordCount = minWordCount,
        maxWordCount = maxWordCount,
        errors = errors.distinct()
    )
}

private fun consumeTagExpression(tokens: List<String>, startIndex: Int): Pair<String?, Int> {
    val values = mutableListOf<String>()
    var depth = 0
    var index = startIndex
    while (index < tokens.size) {
        val token = if (index == startIndex) tokens[index].substringAfter(':') else tokens[index]
        values += token
        depth += token.count { it == '(' }
        depth -= token.count { it == ')' }
        if (depth == 0) {
            var expressionEnd = index
            if (tokens.getOrNull(index + 1).equals("OR", ignoreCase = true)) {
                val alternative = tokens.getOrNull(index + 2)
                if (!alternative.isNullOrBlank() && !isAdvancedDirective(alternative)) {
                    values += "OR"
                    values += alternative
                    expressionEnd += 2
                }
            }
            return values.joinToString(" ") to expressionEnd
        }
        index += 1
    }
    return null to startIndex
}

private fun isAdvancedDirective(token: String): Boolean =
    token.equals("NOT", ignoreCase = true) ||
        token.startsWith("tag:", ignoreCase = true) ||
        token.startsWith("-tag:", ignoreCase = true) ||
        token.startsWith("in:", ignoreCase = true) ||
        token.startsWith("@") ||
        token.startsWith("word", ignoreCase = true) ||
        token.startsWith("platform:", ignoreCase = true) ||
        token.startsWith("type:", ignoreCase = true) ||
        token.startsWith("status:", ignoreCase = true) ||
        token.startsWith("adult:", ignoreCase = true) ||
        token.startsWith("match:", ignoreCase = true)

private fun parseScope(value: String): String? = when (value.trim().lowercase()) {
    "title" -> "title"
    "author" -> "author"
    "tags", "tag" -> "tags"
    else -> null
}

private fun parseWordRange(value: String): Pair<Long?, Long?>? {
    val pieces = value.split("..", limit = 2)
    if (pieces.size != 2) return null
    val min = pieces[0].trim().takeIf(String::isNotBlank)?.let(::parseWordAmount) ?: run {
        if (pieces[0].isBlank()) null else return null
    }
    val max = pieces[1].trim().takeIf(String::isNotBlank)?.let(::parseWordAmount) ?: run {
        if (pieces[1].isBlank()) null else return null
    }
    return min to max
}

private fun parseWordAmount(value: String): Long? {
    val normalized = value.trim().lowercase().replace(",", "")
    val multiplier = when {
        normalized.endsWith("万") || normalized.endsWith("w") -> 10_000.0
        normalized.endsWith("k") -> 1_000.0
        else -> 1.0
    }
    val number = normalized.removeSuffix("万").removeSuffix("w").removeSuffix("k").toDoubleOrNull() ?: return null
    return (number * multiplier).toLong().takeIf { it >= 0 }
}

private fun tokenizeAdvancedSearch(source: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    source.forEach { char ->
        when {
            quote != null -> {
                if (char == quote) quote = null else current.append(char)
            }

            char == '\'' || char == '"' -> quote = char
            char.isWhitespace() -> {
                if (current.isNotEmpty()) {
                    tokens += current.toString()
                    current.clear()
                }
            }

            else -> current.append(char)
        }
    }
    if (current.isNotEmpty()) tokens += current.toString()
    return tokens
}

private fun normalizeSearchTerms(values: List<String>): List<String> =
    values
        .map(String::trim)
        .filter(String::isNotBlank)
        .fold(emptyList()) { terms, value ->
            if (terms.any { it.equals(value, ignoreCase = true) }) terms else terms + value
        }
