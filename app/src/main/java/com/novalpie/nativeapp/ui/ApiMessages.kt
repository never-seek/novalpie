package com.novalpie.nativeapp.ui

fun apiFailureMessage(label: String, throwable: Throwable): String {
    val detail = throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName
    return "${visibleFailureLabel(label)}请求失败: ${visibleFailureDetail(detail)}"
}

private fun visibleFailureLabel(label: String): String =
    label.trim().removeSuffix("/API").ifBlank { "请求" }

private fun visibleFailureDetail(detail: String): String {
    val status = Regex("""NovalPie API (\d+)""").find(detail)?.groupValues?.getOrNull(1)
    return if (status == null) detail else "服务返回错误 $status"
}
