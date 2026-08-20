package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookManagementPresentationTest {
    @Test
    fun accessPolicyDraftUsesWebsiteThresholdLimits() {
        assertNull(
            validateBookAccessPolicyDraft(
                BookAccessPolicyDraft(
                    allowDownload = true,
                    downloadThresholdType = "points_min",
                    downloadThresholdValue = "100",
                    readThresholdType = "points_pay",
                    readThresholdValue = "50"
                )
            )
        )

        assertEquals(
            "阅读门槛 不能超过 50",
            validateBookAccessPolicyDraft(
                BookAccessPolicyDraft(readThresholdType = "points_pay", readThresholdValue = "51")
            )
        )
        assertEquals(
            "下载门槛 不能超过 100",
            validateBookAccessPolicyDraft(
                BookAccessPolicyDraft(downloadThresholdType = "points_min", downloadThresholdValue = "101")
            )
        )
    }

    @Test
    fun accessPolicyDraftDisablesDownloadThresholdWhenDownloadsAreOff() {
        val policy = bookAccessPolicyFromDraft(
            BookAccessPolicyDraft(
                allowDownload = false,
                downloadThresholdType = "points_pay",
                downloadThresholdValue = "50",
                readThresholdType = "points_min",
                readThresholdValue = "20"
            )
        )

        assertEquals(false, policy.allowDownload)
        assertEquals("none", policy.downloadThresholdType)
        assertEquals(0, policy.downloadThresholdValue)
        assertEquals("points_min", policy.readThresholdType)
        assertEquals(20, policy.readThresholdValue)
    }

    @Test
    fun chapterIllustrationPlaceholderMatchesWebsiteFormat() {
        assertEquals("[[img:1]]", chapterIllustrationPlaceholder(0))
        assertEquals("[[img:3]]", chapterIllustrationPlaceholder(3))
    }
}
