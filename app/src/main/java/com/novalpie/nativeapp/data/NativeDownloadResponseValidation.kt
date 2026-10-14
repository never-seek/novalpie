package com.novalpie.nativeapp.data

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream

/** Inspect only a bounded prefix, then rewind it; valid TXT bytes (including BOM) remain untouched. */
internal fun validatedNovelTextStream(input: InputStream, contentType: String?): InputStream {
    val stream = BufferedInputStream(input)
    val header = ByteArray(1024)
    stream.mark(header.size)
    var size = 0
    while (size < header.size) {
        val n = stream.read(header, size, header.size - size)
        if (n < 0) break
        if (n > 0) size += n
    }
    stream.reset()
    if (size == 0) throw IOException("下载内容为空")
    val text = String(header, 0, size, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t')
    val mime = contentType?.substringBefore(';')?.trim()?.lowercase()
    val htmlPage = Regex("^(?:<!doctype\\s+html\\b|<html\\b|<head\\b|<body\\b)", RegexOption.IGNORE_CASE).containsMatchIn(text)
    val jsonResponse = (text.startsWith("{") || text.startsWith("[")) &&
        (mime == "application/json" || mime?.endsWith("+json") == true)
    if (htmlPage || jsonResponse) throw IOException("下载服务返回了错误页面或接口数据，未保存为小说正文，请重试")
    return stream
}
