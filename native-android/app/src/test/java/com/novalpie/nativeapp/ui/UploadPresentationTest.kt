package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadPresentationTest {
    @Test
    fun validatesWebsiteUploadRequiredFieldsAndSubmitType() {
        assertEquals("请输入书名", validateUploadBookDraft(UploadBookDraft()))
        assertEquals("请输入作者", validateUploadBookDraft(UploadBookDraft(title = "Book")))
        assertEquals(
            "请先选择并解析 EPUB 文件",
            validateUploadBookDraft(UploadBookDraft(title = "Book", author = "Writer"))
        )
        assertNull(
            validateUploadBookDraft(
                UploadBookDraft(title = "Book", author = "Writer", chapterCount = 2, submitType = "shared")
            )
        )
        assertEquals(
            "提交方式无效",
            validateUploadBookDraft(
                UploadBookDraft(title = "Book", author = "Writer", chapterCount = 2, submitType = "invalid")
            )
        )
    }

    @Test
    fun normalizesTagsLikeWebsiteCommaSeparatedInput() {
        assertEquals(
            listOf("奇幻", "恋爱", "冒险"),
            normalizeUploadTags(" 奇幻,恋爱，奇幻\n冒险 ")
        )
    }

    @Test
    fun uploadSizePolicyMatchesCurrentWebsiteThresholds() {
        assertEquals(5L * 1024L * 1024L, WEBSITE_UPLOAD_CHUNK_BYTES)
        assertEquals(50L * 1024L * 1024L, WEBSITE_SERVER_EPUB_THRESHOLD_BYTES)
        assertEquals(UploadParseMode.LOCAL, uploadParseMode(50L * 1024L * 1024L))
        assertEquals(UploadParseMode.SERVER_CHUNKED, uploadParseMode(50L * 1024L * 1024L + 1L))
    }
}
