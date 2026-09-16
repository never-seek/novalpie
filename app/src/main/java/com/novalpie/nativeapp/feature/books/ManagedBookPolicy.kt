package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.model.ManagedBookAccessPolicy
import org.json.JSONObject

/** Missing permissions are unknown, not an instruction to replace an author's policy with none. */
internal fun managedBookPolicy(source: JSONObject): ManagedBookAccessPolicy? {
    fun value(camel: String, snake: String): Any? = listOf(camel, snake).firstNotNullOfOrNull { key -> source.opt(key)?.takeUnless { it == JSONObject.NULL } }
    val allow = when (val raw = value("allowDownload", "allow_download")) {
        is Boolean -> raw
        is Number -> when (raw.toInt()) { 0 -> false; 1 -> true; else -> null }
        is String -> when (raw.lowercase()) { "true", "1" -> true; "false", "0" -> false; else -> null }
        else -> null
    } ?: return null
    val downloadType = value("downloadThresholdType", "download_threshold_type")?.toString() ?: return null
    val readType = value("readThresholdType", "read_threshold_type")?.toString() ?: return null
    fun threshold(type: String, camel: String, snake: String): Int? {
        if (type !in setOf("none", "points_min", "points_pay")) return null
        val number = value(camel, snake)?.toString()?.toIntOrNull() ?: return null
        if (number < 0 || (type == "none" && number != 0)) return null
        return number
    }
    val downloadValue = threshold(downloadType,"downloadThresholdValue","download_threshold_value") ?: return null
    val readValue = threshold(readType,"readThresholdValue","read_threshold_value") ?: return null
    return ManagedBookAccessPolicy(allow, downloadType, downloadValue, readType, readValue)
}
