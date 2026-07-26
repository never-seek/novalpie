package com.novalpie.nativeapp.ui

import java.net.URI

enum class WorkspaceTab(val label: String) {
    Overview("概览"),
    Apis("API 管理"),
    Cookies("Cookie 管理"),
    Queue("任务队列")
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
    if (value.isEmpty()) return "未配置"
    if (value.length <= 8) return "********"
    return value.take(4) + "******" + value.takeLast(4)
}

internal fun validateWorkspaceApiDraft(draft: WorkspaceApiDraft): String? {
    if (draft.name.isBlank()) return "API 名称不能为空"
    if (draft.model.isBlank()) return "模型不能为空"
    if (!isHttpUrl(draft.endpoint)) return "API 端点必须是 http(s) URL"
    if (draft.apiKey.isBlank()) return "API Key 不能为空"
    val concurrency = draft.concurrency.toIntOrNull()
    if (concurrency == null || concurrency !in 1..100) return "并发数必须介于 1 到 100"
    return null
}

internal fun validateWorkspaceCookieDraft(draft: WorkspaceCookieDraft): String? {
    if (draft.id == null && draft.configKey.isBlank()) return "配置键名不能为空"
    if (draft.id == null && draft.cookieRaw.isBlank()) return "Cookie 内容不能为空"
    if (draft.proxyIp.isNotBlank() && !isProxyValue(draft.proxyIp)) {
        return "代理格式应为 IP:PORT 或 http(s)://..."
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
