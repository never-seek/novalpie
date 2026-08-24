package com.novalpie.nativeapp.data

import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * Keeps a large native EPUB off the MediaStore/FUSE stream while it is being assembled.
 * Publication into Downloads happens only after the archive has been closed successfully.
 */
internal fun nativeEpubGenerationFile(workDirectory: File, bookId: Long): File {
    require(bookId > 0) { "书籍 ID 无效" }
    return File(workDirectory, "novalpie-$bookId.epub.part")
}

/** Copies a finished private download and fails closed if the byte count changes mid-copy. */
internal fun copyNativeDownloadFile(source: File, output: OutputStream): Long {
    if (!source.isFile) throw IOException("下载临时文件不存在")
    val expected = source.length()
    var copied = 0L
    source.inputStream().buffered(NATIVE_DOWNLOAD_COPY_BUFFER_BYTES).use { input ->
        val buffer = ByteArray(NATIVE_DOWNLOAD_COPY_BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            output.write(buffer, 0, read)
            copied += read
        }
    }
    output.flush()
    if (copied != expected) {
        throw IOException("下载文件复制不完整：预期 $expected 字节，实际 $copied 字节")
    }
    return copied
}

private const val NATIVE_DOWNLOAD_COPY_BUFFER_BYTES = 1024 * 1024
