package com.novalpie.nativeapp.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/** A text entry selected from a local document or extracted from a ZIP archive. */
data class EditorBatchTextEntry(
    val displayName: String,
    val text: String
)

/**
 * Reads the text-like part of an editor ZIP import without writing it to disk.
 *
 * Android's storage picker can hand the app an arbitrary archive. Limits are
 * enforced while streaming so a compressed archive cannot expand into an
 * unbounded allocation before it reaches the editor document.
 */
object EditorBatchImporter {
    const val MAX_ARCHIVE_ENTRIES = 500
    const val MAX_ENTRY_BYTES = 10 * 1024 * 1024
    const val MAX_TOTAL_BYTES = 50 * 1024 * 1024
    const val MAX_TOTAL_CHARACTERS = 50_000_000

    fun isTextFile(name: String): Boolean {
        val normalized = name.substringBefore('?').substringBefore('#').lowercase()
        return normalized.endsWith(".txt") || normalized.endsWith(".md") || normalized.endsWith(".markdown")
    }

    fun isBatchFile(name: String): Boolean = isTextFile(name) || name.endsWith(".zip", ignoreCase = true)

    fun readArchive(input: InputStream, charset: Charset): List<EditorBatchTextEntry> {
        val result = mutableListOf<EditorBatchTextEntry>()
        var entryCount = 0
        var totalBytes = 0
        var totalCharacters = 0

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                if (entryCount > MAX_ARCHIVE_ENTRIES) {
                    throw IOException("压缩包条目超过 $MAX_ARCHIVE_ENTRIES 个")
                }
                if (entry.isDirectory || !isTextFile(entry.name)) {
                    zip.closeEntry()
                    continue
                }

                val bytes = readEntry(zip, entry.name) { read ->
                    totalBytes += read
                    if (totalBytes > MAX_TOTAL_BYTES) {
                        throw IOException("压缩包展开内容超过 ${MAX_TOTAL_BYTES / 1024 / 1024} MiB")
                    }
                }
                val text = String(bytes, charset).removePrefix("\uFEFF")
                totalCharacters += text.length
                if (totalCharacters > MAX_TOTAL_CHARACTERS) {
                    throw IOException("压缩包文本超过 ${MAX_TOTAL_CHARACTERS / 1_000_000}00 万字符")
                }
                result += EditorBatchTextEntry(safeDisplayName(entry.name), text)
                zip.closeEntry()
            }
        }

        if (result.isEmpty()) throw IOException("压缩包内没有可导入的 .txt 或 .md 文件")
        return result
    }

    private fun readEntry(
        zip: ZipInputStream,
        name: String,
        onBytesRead: (Int) -> Unit
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var entryBytes = 0
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            entryBytes += read
            if (entryBytes > MAX_ENTRY_BYTES) {
                throw IOException("压缩包文件过大：${safeDisplayName(name)}")
            }
            onBytesRead(read)
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun safeDisplayName(path: String): String = path
        .replace('\\', '/')
        .substringAfterLast('/')
        .replace(Regex("[\\r\\n\\t]"), " ")
        .trim()
        .ifBlank { "未命名文本" }
}
