package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminPresentationTest {
    @Test
    fun adminReviewFiltersMatchWebsiteReviewControls() {
        assertEquals(
            listOf("" to "全部", "upload" to "上传", "delete" to "删除"),
            adminReviewTypeOptions()
        )
        assertEquals(
            listOf("" to "全部", "pending" to "待审核", "approved" to "已通过", "rejected" to "已拒绝"),
            adminReviewStatusOptions()
        )
    }

    @Test
    fun adminOperationFiltersKeepAllOptionAndDynamicActionTypes() {
        assertEquals(
            listOf("" to "全部", "success" to "成功", "failed" to "失败", "pending" to "处理中"),
            adminOperationStatusOptions()
        )
        assertEquals(
            listOf("" to "全部", "upload_novel" to "upload_novel", "delete_comment" to "delete_comment"),
            adminOperationActionOptions(listOf("upload_novel", "upload_novel", "", "delete_comment"))
        )
    }
}
