package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.NovalPieApiException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Turns a failure into something a reader can act on.
 *
 * Two problems are addressed here. Server explanations were discarded, so a `422` carrying
 * `章节标题重复，请修改后重试` surfaced as `服务返回错误 422` — a number, when the one sentence that
 * would have told the user what to change had already been parsed and dropped. And when the failure
 * was *not* an HTTP status, the raw exception text was shown verbatim, producing Chinese-plus-English
 * hybrids such as
 * `书架请求失败: failed to connect to /10.0.2.2 (port 7890) from /192.168.1.5 ... after 12000ms`.
 */
fun apiFailureMessage(label: String, throwable: Throwable): String =
    "${visibleFailureLabel(label)}请求失败: ${visibleFailureDetail(throwable)}"

private fun visibleFailureLabel(label: String): String =
    label.trim().removeSuffix("/API").ifBlank { "请求" }

private fun visibleFailureDetail(throwable: Throwable): String {
    // Prefer what the server actually said.
    if (throwable is NovalPieApiException) {
        unavailableNewChapterDetail(throwable)?.let { return it }
        throwable.serverMessage?.takeIf { it.isNotBlank() }?.let { return it }
        return statusExplanation(throwable.statusCode)
    }

    // Network-level failures. The exception text is diagnostic detail meant for developers -- host
    // addresses, port numbers, millisecond budgets -- and says nothing useful to a reader.
    networkFailureDetail(throwable)?.let { return it }

    val detail = throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName

    // Call sites that still throw plain exceptions carrying the "NovalPie API <code>" prefix.
    Regex("""NovalPie API (\d+)""").find(detail)?.groupValues?.getOrNull(1)?.let { status ->
        return "服务返回错误 $status"
    }
    return detail
}

/** The live source keeps this website action visible while its API route is temporarily unavailable. */
private fun unavailableNewChapterDetail(failure: NovalPieApiException): String? {
    if (!failure.path.matches(Regex("/api(?:/v2)?/novels/\\d+/(?:chapters/request|chapter-requests)"))) return null
    val serverText = failure.serverMessage.orEmpty()
    val unavailable = failure.statusCode in setOf(405, 501) ||
        serverText.contains("not implemented", ignoreCase = true) ||
        serverText.contains("method is not supported", ignoreCase = true)
    return "源站暂未开放该功能，请使用网页详情重试".takeIf { unavailable }
}

private fun networkFailureDetail(throwable: Throwable): String? {
    // Causes matter here: OkHttp usually wraps the real reason.
    var current: Throwable? = throwable
    var depth = 0
    while (current != null && depth < 5) {
        when (current) {
            is SocketTimeoutException -> return "网络连接超时，请稍后重试"
            is UnknownHostException -> return "无法解析服务器地址，请检查网络连接"
            is ConnectException -> return "无法连接服务器，请检查网络连接"
            is SSLException -> return "安全连接失败，请检查网络环境"
        }
        current = current.cause
        depth++
    }
    return null
}

/**
 * Plain-language fallback for the status codes this app actually meets, used only when the server
 * sent no explanation of its own.
 */
private fun statusExplanation(statusCode: Int): String = when (statusCode) {
    400 -> "请求内容有误，请检查后重试"
    401 -> "登录已失效，请重新登录"
    403 -> "没有访问权限"
    404 -> "内容不存在或已被删除"
    409 -> "内容已存在或状态冲突"
    422 -> "内容未通过校验，请修改后重试"
    429 -> "操作过于频繁，请稍后再试"
    in 500..599 -> "服务暂时不可用，请稍后重试（$statusCode）"
    else -> "服务返回错误 $statusCode"
}
