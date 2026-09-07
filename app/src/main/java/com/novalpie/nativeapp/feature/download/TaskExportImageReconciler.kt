package com.novalpie.nativeapp.feature.download

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Only ambiguous duplicate chapters need a source read; task-owned snapshots are reused on retry. */
internal class TaskExportImageReconciler(private val directory: File,
    private val sourceImages: suspend (Int) -> List<String>) {
    suspend fun reconcile(number: Int, body: String): String {
        require(number > 0)
        val urls = Regex("\\[图片(?:[:：]|\\s)*?(.*?)\\]").findAll(body).mapNotNull { normalizedExportImageUrl(it.groupValues[1]) }.toList()
        if (urls.size == urls.distinct().size) return body
        val hash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }
        val record = File(directory, "$number.json")
        val cached = if (record.isFile) runCatching { JSONObject(record.readText()) }.getOrNull() else null
        val expected = if (cached?.optInt("schema") == 1 && cached.optString("bodyHash") == hash)
            cached.getJSONArray("images").let { a -> (0 until a.length()).map { a.getString(it) } }
        else sourceImages(number)
        val result = reconcileExportImageOccurrences(body, expected)
        check(directory.isDirectory || directory.mkdirs()) { "无法保存插图校对检查点" }
        val data = JSONObject().put("schema", 1).put("bodyHash", hash).put("images", JSONArray(expected))
            .put("exportOccurrences", urls.size).put("retainedOccurrences", expected.size).put("removed", result.removed).toString()
        record.writeText(data)
        check(record.readText() == data) { "插图校对检查点保存失败" }
        return result.body
    }
}
