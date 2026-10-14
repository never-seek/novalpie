package com.novalpie.nativeapp.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Single-owner, backup-based transaction. Callers serialize access to their own file. Renames
 * never rely on overwriting an existing target (Android and Windows QA behave differently there).
 * An interrupted/uncommitted write is rolled back on the next read; no prior bytes are discarded.
 */
internal class OwnedAtomicFile(private val file: File) {
    private val backup = File(file.path + ".bak")
    private fun recover() {
        if (!backup.isFile) return
        if (file.exists() && !file.delete()) throw IOException("无法恢复文件检查点")
        if (!backup.renameTo(file)) throw IOException("无法恢复文件备份")
    }
    fun openRead(): FileInputStream {
        recover()
        return file.inputStream()
    }
    fun startWrite(): FileOutputStream {
        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) throw IOException("无法创建检查点目录") }
        recover()
        if (file.isFile && !file.renameTo(backup)) throw IOException("无法保留旧检查点")
        return try { file.outputStream() } catch (error: Throwable) {
            if (backup.isFile) backup.renameTo(file)
            throw error
        }
    }
    fun finishWrite(stream: FileOutputStream) {
        stream.fd.sync()
        stream.close()
        if (backup.exists() && !backup.delete()) throw IOException("无法完成检查点保存")
    }
    fun failWrite(stream: FileOutputStream) {
        runCatching { stream.close() }
        if (file.exists() && !file.delete()) throw IOException("无法回滚未完成检查点")
        if (backup.isFile && !backup.renameTo(file)) throw IOException("无法恢复旧检查点")
    }
}
