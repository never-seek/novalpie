package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import com.novalpie.nativeapp.model.WorkspaceTranslationJob
import org.json.JSONArray
import org.json.JSONObject

class WorkspaceLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadApis(): List<WorkspaceLocalApiConfig> = parseArray(KEY_APIS).mapNotNull { item ->
        val id = item.longOrNull("id") ?: return@mapNotNull null
        WorkspaceLocalApiConfig(
            id = id,
            name = item.stringOrEmpty("name"),
            model = item.stringOrEmpty("model"),
            endpoint = item.stringOrEmpty("endpoint"),
            apiKey = item.stringOrEmpty("api_key"),
            concurrency = item.intOrNull("concurrency") ?: 10,
            sharedToServer = item.booleanOrFalse("shared_to_server"),
            serverId = item.longOrNull("server_id")
        )
    }

    fun upsertApi(config: WorkspaceLocalApiConfig) {
        val next = loadApis().associateByTo(linkedMapOf()) { it.id }
        next[config.id] = config
        saveArray(KEY_APIS, next.values.map(::apiToJson))
    }

    fun deleteApi(id: Long) {
        saveArray(KEY_APIS, loadApis().filterNot { it.id == id }.map(::apiToJson))
    }

    fun loadJobs(): List<WorkspaceTranslationJob> = parseArray(KEY_JOBS).mapNotNull { item ->
        val id = item.longOrNull("id") ?: return@mapNotNull null
        val bookId = item.longOrNull("book_id") ?: return@mapNotNull null
        WorkspaceTranslationJob(
            id = id,
            bookId = bookId,
            bookTitle = item.stringOrEmpty("book_title"),
            translatorId = item.longOrNull("translator_id"),
            translatorName = item.stringOrEmpty("translator_name"),
            chapterCount = item.intOrNull("chapter_count") ?: 0,
            completedChapters = item.intOrNull("completed_chapters") ?: 0,
            status = item.stringOrEmpty("status").ifBlank { "pending" },
            createdAt = item.stringOrNull("created_at"),
            updatedAt = item.stringOrNull("updated_at")
        )
    }

    fun upsertJob(job: WorkspaceTranslationJob) {
        val next = loadJobs().associateByTo(linkedMapOf()) { it.id }
        next[job.id] = job
        saveArray(KEY_JOBS, next.values.map(::jobToJson))
    }

    fun deleteJob(id: Long) {
        saveArray(KEY_JOBS, loadJobs().filterNot { it.id == id }.map(::jobToJson))
    }

    fun clearAll() {
        prefs.edit().remove(KEY_APIS).remove(KEY_JOBS).apply()
    }

    private fun parseArray(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optJSONObject(index) }
    }

    private fun saveArray(key: String, values: List<JSONObject>) {
        val array = JSONArray()
        values.forEach(array::put)
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun apiToJson(config: WorkspaceLocalApiConfig): JSONObject = JSONObject()
        .put("id", config.id)
        .put("name", config.name)
        .put("model", config.model)
        .put("endpoint", config.endpoint)
        .put("api_key", config.apiKey)
        .put("concurrency", config.concurrency)
        .put("shared_to_server", config.sharedToServer)
        .apply { config.serverId?.let { put("server_id", it) } }

    private fun jobToJson(job: WorkspaceTranslationJob): JSONObject = JSONObject()
        .put("id", job.id)
        .put("book_id", job.bookId)
        .put("book_title", job.bookTitle)
        .put("translator_name", job.translatorName)
        .put("chapter_count", job.chapterCount)
        .put("completed_chapters", job.completedChapters)
        .put("status", job.status)
        .apply {
            job.translatorId?.let { put("translator_id", it) }
            job.createdAt?.let { put("created_at", it) }
            job.updatedAt?.let { put("updated_at", it) }
        }

    private fun JSONObject.stringOrNull(key: String): String? =
        opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.stringOrEmpty(key: String): String = stringOrNull(key).orEmpty()

    private fun JSONObject.longOrNull(key: String): Long? =
        opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()?.toLongOrNull()

    private fun JSONObject.intOrNull(key: String): Int? =
        opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()?.toIntOrNull()

    private fun JSONObject.booleanOrFalse(key: String): Boolean = when (val value = opt(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> false
    }

    companion object {
        private const val PREFS_NAME = "novalpie_native_workspace"
        private const val KEY_APIS = "api_configs"
        private const val KEY_JOBS = "translation_jobs"
    }
}
