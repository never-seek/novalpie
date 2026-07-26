package com.novalpie.nativeapp.ui

import java.net.URI

enum class WorkspaceTab(val label: String) {
    Overview("\u6982\u89c8"),
    Apis("API \u7ba1\u7406"),
    Cookies("Cookie \u7ba1\u7406"),
    Queue("\u4efb\u52a1\u961f\u5217")
}

data class WorkspaceApiDraft(
    val id: Long? = null,
    val serverId: Long? = null,
    val name: String = "",
    val model: String = "deepseek-chat",
    val endpoint: String = "https://api.deepseek.com",
    val apiKey: String = "",
    val concurrency: String = "10",
    val shareToServer: Boolean = false
)

data class WorkspaceCookieDraft(
    val id: Long? = null,
    val configKey: String = "",
    val description: String = "",
    val cookieRaw: String = "",
    val proxyIp: String = "",
    val isActive: Boolean = true
)

internal fun maskWorkspaceApiKey(apiKey: String?): String {
    val value = apiKey?.trim().orEmpty()
    if (value.isEmpty()) return "\u672a\u914d\u7f6e"
    if (value.length <= 8) return "********"
    return value.take(4) + "******" + value.takeLast(4)
}

internal fun validateWorkspaceApiDraft(draft: WorkspaceApiDraft): String? {
    if (draft.name.isBlank()) return "API \u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a"
    if (draft.model.isBlank()) return "\u6a21\u578b\u4e0d\u80fd\u4e3a\u7a7a"
    if (!isHttpUrl(draft.endpoint)) return "API \u7aef\u70b9\u5fc5\u987b\u662f http(s) URL"
    if (draft.apiKey.isBlank()) return "API Key \u4e0d\u80fd\u4e3a\u7a7a"
    val concurrency = draft.concurrency.toIntOrNull()
    if (concurrency == null || concurrency !in 1..100) return "\u5e76\u53d1\u6570\u5fc5\u987b\u4ecb\u4e8e 1 \u5230 100"
    return null
}

internal fun validateWorkspaceCookieDraft(draft: WorkspaceCookieDraft): String? {
    if (draft.id == null && draft.configKey.isBlank()) return "\u914d\u7f6e\u952e\u540d\u4e0d\u80fd\u4e3a\u7a7a"
    if (draft.id == null && draft.cookieRaw.isBlank()) return "Cookie \u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a"
    if (draft.proxyIp.isNotBlank() && !isProxyValue(draft.proxyIp)) {
        return "\u4ee3\u7406\u683c\u5f0f\u5e94\u4e3a IP:PORT \u6216 http(s)://..."
    }
    return null
}

private fun isHttpUrl(value: String): Boolean {
    val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
    return uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
}

private fun isProxyValue(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
        return !uri.host.isNullOrBlank() && uri.port in 1..65535
    }
    return HOST_PORT_REGEX.matches(normalized) && normalized.substringAfter(':').substringBefore(':').toIntOrNull() in 1..65535
}

private val HOST_PORT_REGEX = Regex("^[^:\\s]+:\\d{1,5}(?::[^:\\s]+:[^:\\s]+)?$")
