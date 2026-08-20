package com.novalpie.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * A non-2xx response from the site, carrying whatever explanation the server gave.
 *
 * The API layer used to read the error body in full and then throw it away:
 *
 * ```
 * val responseBody = response.body?.string().orEmpty()
 * if (!response.isSuccessful) throw IOException("NovalPie API ${response.code}: $path")
 * ```
 *
 * so a `422 {"message":"章节标题重复，请修改后重试"}` reached the user as
 * `保存章节请求失败: 服务返回错误 422` — a number, with the one sentence that would have told them
 * what to fix already parsed and discarded. The same applied to every 400/403/409/429 in the app.
 *
 * [message] keeps the `NovalPie API <code>:` prefix so existing status-code handling still works,
 * and appends the server text when there is any.
 */
class NovalPieApiException(
    val statusCode: Int,
    val path: String,
    val serverMessage: String?,
) : IOException(buildMessage(statusCode, path, serverMessage)) {

    companion object {
        private fun buildMessage(statusCode: Int, path: String, serverMessage: String?): String {
            val base = "NovalPie API $statusCode: $path"
            return if (serverMessage.isNullOrBlank()) base else "$base - $serverMessage"
        }

        /**
         * Pulls a human-readable explanation out of an error body.
         *
         * The site is inconsistent about where it puts the text — the same tolerance the response
         * normalizers need — so several shapes are accepted: a flat `message`/`error`/`detail`/`msg`,
         * the same keys nested under `data`, and Laravel-style `errors: {field: ["..."]}`.
         */
        fun extractServerMessage(body: String): String? {
            val trimmed = body.trim()
            if (trimmed.isEmpty()) return null

            if (trimmed.startsWith("{")) {
                val json = runCatching { JSONObject(trimmed) }.getOrNull()
                if (json != null) {
                    directMessage(json)?.let { return it }
                    json.optJSONObject("data")?.let { data -> directMessage(data)?.let { return it } }
                    firstValidationMessage(json.optJSONObject("errors"))?.let { return it }
                    firstValidationMessage(json.optJSONObject("data")?.optJSONObject("errors"))
                        ?.let { return it }
                }
            }

            // A short plain-text body is still better than nothing. Long bodies are almost always
            // an HTML error page, which would be worse than showing the status code alone.
            if (!trimmed.startsWith("{") && !trimmed.startsWith("<") && trimmed.length <= 120) {
                return trimmed
            }
            return null
        }

        private fun directMessage(json: JSONObject): String? =
            listOf("message", "error", "detail", "msg", "error_message", "errorMessage")
                .asSequence()
                .mapNotNull { key -> json.optString(key).takeIf { it.isNotBlank() && it != "null" } }
                .firstOrNull()

        private fun firstValidationMessage(errors: JSONObject?): String? {
            if (errors == null) return null
            for (key in errors.keys()) {
                when (val value = errors.opt(key)) {
                    is String -> value.takeIf { it.isNotBlank() }?.let { return it }
                    is JSONArray -> (0 until value.length())
                        .asSequence()
                        .mapNotNull { index -> value.optString(index).takeIf { it.isNotBlank() } }
                        .firstOrNull()
                        ?.let { return it }
                }
            }
            return null
        }
    }
}
