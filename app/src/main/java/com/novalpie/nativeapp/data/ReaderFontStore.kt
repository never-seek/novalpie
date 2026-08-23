package com.novalpie.nativeapp.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.io.File
import java.util.UUID

/** Imports user-owned Android font files into app-private storage for the native reader. */
object ReaderFontStore {
    internal const val CUSTOM_FONT_PREFIX = "custom-font:"
    private const val DIRECTORY_NAME = "reader-fonts"
    private val supportedExtensions = setOf("ttf", "otf", "ttc")
    private val builtInFamilies = setOf("system", "serif", "sans", "monospace")

    fun isSupportedFamily(value: String): Boolean = value in builtInFamilies || customFontFileName(value) != null

    fun customFontFileName(value: String): String? {
        if (!value.startsWith(CUSTOM_FONT_PREFIX)) return null
        val filename = value.removePrefix(CUSTOM_FONT_PREFIX)
        val extension = filename.substringAfterLast('.', "").lowercase()
        return filename
            .takeIf { it.matches(Regex("[A-Za-z0-9._-]{1,160}")) }
            ?.takeIf { extension in supportedExtensions }
    }

    fun customFontKey(filename: String): String = CUSTOM_FONT_PREFIX + filename

    fun displayName(value: String): String? = customFontFileName(value)

    /** Copies a picker URI and returns the stable persisted family key. */
    fun import(context: Context, uri: Uri): Result<String> = runCatching {
        val sourceName = queryDisplayName(context, uri)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: throw IllegalArgumentException("无法识别字体文件名")
        val extension = sourceName.substringAfterLast('.', "").lowercase()
        require(extension in supportedExtensions) { "仅支持 TTF、OTF 或 TTC 字体" }
        val safeBase = sourceName.substringBeforeLast('.', sourceName)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "reader-font" }
            .take(80)
        val filename = "$safeBase-${UUID.randomUUID()}.$extension"
        val directory = File(context.filesDir, DIRECTORY_NAME)
        require(directory.exists() || directory.mkdirs()) { "无法创建字体存储目录" }
        val target = File(directory, filename)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().buffered().use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("无法读取字体文件")
        require(target.length() > 0L) { "字体文件为空" }
        customFontKey(filename)
    }

    fun fileFor(context: Context, value: String): File? {
        val filename = customFontFileName(value) ?: return null
        val directory = File(context.filesDir, DIRECTORY_NAME)
        val target = File(directory, filename)
        val directoryPath = directory.canonicalFile
        val targetPath = target.canonicalFile
        return targetPath
            .takeIf { it.parentFile == directoryPath && it.isFile && it.length() > 0L }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf("_display_name")
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor: Cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
