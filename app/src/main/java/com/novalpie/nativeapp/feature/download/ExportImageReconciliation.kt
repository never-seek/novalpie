package com.novalpie.nativeapp.feature.download

/** Source download TXT can append an extra inventory copy after authored inline illustrations. */
internal data class ExportImageReconciliation(val body: String, val removed: Int, val sourceOnlyOccurrences: Int = 0)

internal fun normalizedExportImageUrl(raw: String): String? {
    val value = raw.trim().replace(Regex("\\s+"), "").trimStart(':', '：').removePrefix("图片://")
    if (value.isBlank()) return null
    return when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("http://", true) || value.startsWith("https://", true) || value.startsWith("./") -> value
        else -> "https://${value.trimStart('/')}"
    }
}

internal fun reconcileExportImageOccurrences(body: String, authoritativeImages: List<String>): ExportImageReconciliation {
    val pattern = Regex("\\[图片(?:[:：]|\\s)*?(.*?)\\]")
    val matches = pattern.findAll(body).filter { normalizedExportImageUrl(it.groupValues[1]) != null }.toList()
    val urls = matches.map { normalizedExportImageUrl(it.groupValues[1])!! }
    if (urls.size == urls.distinct().size) return ExportImageReconciliation(body, 0)
    val expected = authoritativeImages.mapNotNull(::normalizedExportImageUrl)
    require(expected.isNotEmpty() && urls.toSet().containsAll(expected.toSet())) {
        "导出图片与正文图片无法对应，未删除任何插图；请刷新章节后重试"
    }
    val quotas = expected.groupingBy { it }.eachCount()
    val counts = urls.groupingBy { it }.eachCount()
    require(quotas.all { (url, count) -> (counts[url] ?: 0) >= count }) { "导出文件缺少正文插图，未生成成功包" }
    val used = mutableMapOf<String, Int>()
    val retained = mutableListOf<String>()
    var sourceOnly = 0
    var cursor = 0
    var removed = 0
    val output = StringBuilder()
    matches.forEachIndexed { index, match ->
        output.append(body, cursor, match.range.first)
        val url = urls[index]
        val seen = used[url] ?: 0
        val quota = quotas[url]
        if (quota == null) {
            // Source export may still contain an older illustration omitted by today's reader.
            // It is not a proven duplicate: preserve every such reference and surface the count.
            output.append(match.value); sourceOnly++
        } else if (seen < quota) {
            output.append(match.value); retained += url; used[url] = seen + 1
        } else removed++
        cursor = match.range.last + 1
    }
    output.append(body, cursor, body.length)
    val retainedCounts = retained.groupingBy { it }.eachCount()
    require(quotas.all { (url, count) -> (retainedCounts[url] ?: 0) == count }) { "导出插图保留数量与正文不一致，未生成成功包" }
    return ExportImageReconciliation(output.toString(), removed, sourceOnly)
}
