package com.novalpie.nativeapp.ui

const val WEBSITE_UPLOAD_CHUNK_BYTES = 5L * 1024L * 1024L
const val WEBSITE_SERVER_EPUB_THRESHOLD_BYTES = 50L * 1024L * 1024L

enum class UploadParseMode { LOCAL, SERVER_CHUNKED }

data class UploadBookDraft(
    val title: String = "",
    val titleTranslation: String = "",
    val author: String = "",
    val description: String = "",
    val language: String = "ja",
    val spans: String = "balanced",
    val isAdult: Boolean = false,
    val source: String = "",
    val sourceUrl: String = "",
    val tagsText: String = "",
    val submitType: String = "chinese",
    val coverUrl: String = "",
    val chapterCount: Int = 0
)

fun validateUploadBookDraft(draft: UploadBookDraft): String? = when {
    draft.title.isBlank() -> "请输入书名"
    draft.author.isBlank() -> "请输入作者"
    draft.chapterCount <= 0 -> "请先选择并解析 EPUB 文件"
    draft.submitType !in setOf("chinese", "personal", "shared") -> "提交方式无效"
    else -> null
}

fun normalizeUploadTags(raw: String): List<String> = raw
    .split(',', '，', '\n')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()

fun uploadParseMode(sizeBytes: Long): UploadParseMode =
    if (sizeBytes > WEBSITE_SERVER_EPUB_THRESHOLD_BYTES) UploadParseMode.SERVER_CHUNKED else UploadParseMode.LOCAL
