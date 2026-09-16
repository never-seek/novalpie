package com.novalpie.nativeapp.feature.books

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

internal data class ManagedBookMutationResponse(
    val message: String?,
    val failedFields: List<String>,
    val errors: List<String>,
)

/** A transport success is not proof that a destructive/book-management write was accepted. */
internal fun confirmedManagedBookMutation(raw: Any): ManagedBookMutationResponse {
    val layers = mutableListOf<JSONObject>()
    fun collect(value: JSONObject, depth: Int = 0) {
        if (depth > 16) throw IOException("书籍操作回执层级异常，请先核对网站；未自动重发")
        layers += value
        listOf("data", "result").forEach { key -> value.optJSONObject(key)?.let { collect(it, depth + 1) } }
    }
    (raw as? JSONObject)?.let { collect(it) }
    fun flag(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is Number -> when (value.toDouble()) { 1.0 -> true; 0.0 -> false; else -> null }
        is String -> when (value.trim().lowercase()) {
            "true", "1", "ok", "success" -> true
            "false", "0", "error", "failed" -> false
            else -> null
        }
        else -> null
    }
    fun message(value: JSONObject): String? = listOf("message", "msg", "detail")
        .asSequence().mapNotNull { value.opt(it) as? String }.firstOrNull { it.isNotBlank() }
    val flags = layers.flatMap { layer -> listOf("success", "ok", "status").mapNotNull { flag(layer.opt(it)) } } + listOfNotNull(flag(raw))
    val rejected = layers.firstOrNull { layer -> listOf("success", "ok", "status").any { flag(layer.opt(it)) == false } }
    if (false in flags) throw IllegalStateException(rejected?.let(::message)
        ?: layers.firstNotNullOfOrNull(::message) ?: "服务器拒绝书籍操作")
    if (true !in flags) throw IOException("书籍操作回执未确认，请先刷新核对网站；未自动重发")
    fun strings(key: String): List<String> = layers.flatMap { layer ->
        val values = layer.opt(key) as? JSONArray
        if (values == null) emptyList() else (0 until values.length()).mapNotNull { values.opt(it) as? String }
    }.filter(String::isNotBlank).distinct()
    return ManagedBookMutationResponse(layers.firstNotNullOfOrNull(::message), strings("failed_fields"), strings("errors"))
}
