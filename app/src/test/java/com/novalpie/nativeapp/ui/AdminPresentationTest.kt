package com.novalpie.nativeapp.ui

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPresentationTest {
    @Test
    fun adminOnlyToolsAreHiddenForOrdinaryAccounts() {
        assertTrue(toolsEntries(isAdmin = true).any { it.adminOnly })
        assertTrue(toolsEntries(isAdmin = false).none { it.adminOnly })
        assertTrue(toolsEntries(isAdmin = false).none { it.path.startsWith("/admin") })
    }

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
            listOf("" to "全部", "upload_novel" to "上传小说", "delete_comment" to "delete_comment"),
            adminOperationActionOptions(listOf("upload_novel", "upload_novel", "", "delete_comment"))
        )
    }

    @Test
    fun adminSourceLabelsKeepTheWebsiteInformationArchitecture() {
        assertEquals(listOf(5, 15, 30), adminOverviewDayOptions())
        assertEquals("上传请求", adminReviewTypeLabel("upload"))
        assertEquals("已拒绝", adminReviewStatusLabel("rejected"))
        assertEquals("待审核", adminKeyStatusLabel("pending"))
        assertEquals("获取新章", adminOperationActionLabel("fetch_new_chapters"))
        assertEquals("进行中", adminOperationStatusLabel("processing"))
        assertEquals("允许", adminBaseUrlActionLabel("allow"))
        assertEquals("头像框", adminShopTypeLabel("frame"))
    }

    @Test
    fun adminBadgePreviewUsesSafeSourcePaletteAndReadableLabel() {
        assertEquals(
            "透明龙",
            adminShopBadgePreviewText("<span class=\"badge__text\">透明龙</span>", "备用名称")
        )
        assertEquals(
            listOf(0xFF22D3EE.toInt(), 0xFFA855F7.toInt()),
            adminShopBadgePreviewColors(
                "color: rgba(255,255,255,.96); background: linear-gradient(rgba(34,211,238,.22), rgba(168,85,247,.24));"
            ).take(2).map { it.toArgb() }
        )
    }

    @Test
    fun badgePreviewUsesPerBadgeHexGradientAndOptionalBackgroundImage() {
        assertEquals(
            listOf(0xFFEC4899.toInt(), 0xFFA855F7.toInt()),
            adminShopBadgePreviewColors("color: #fff; background: linear-gradient(135deg, #ec4899, #a855f7); border: 1px solid #ffffff;")
                .take(2)
                .map { it.toArgb() }
        )
        assertEquals(
            "https://cdn.example.test/badge.png",
            adminShopBadgePreviewBackgroundImageUrl("background-image: url('https://cdn.example.test/badge.png');")
        )
        assertNull(adminShopBadgePreviewBackgroundImageUrl("background-image: url(data:image/png;base64,abc);"))
    }

    @Test
    fun badgePreviewResolvesSourceCssVariablesAndLeadingDot() {
        val css = """
            .badge {
              --bg: linear-gradient(135deg, #16a34a, #2563eb);
              --art: url('/uploads/shop_assets/badges/hero.webp');
              background: var(--bg);
            }
            .badge__dot { background: #fef08a; }
        """.trimIndent()

        assertTrue(adminShopBadgePreviewResolvedCss(css).contains("#16a34a"))
        assertEquals(
            listOf(0xFF16A34A.toInt(), 0xFF2563EB.toInt()),
            adminShopBadgePreviewColors(css).take(2).map { it.toArgb() },
        )
        assertEquals(true, adminShopBadgePreviewHasDot("<span class=\"badge__dot\"></span>"))
        assertEquals(0xFFFEF08A.toInt(), adminShopBadgePreviewDotColor(css).toArgb())
    }

    @Test
    fun badgePreviewResolvesRelativeAndWebpArtworkWithoutAcceptingExecutableUris() {
        assertEquals(
            "https://novalpie.cc/uploads/shop_assets/badges/asset.webp",
            adminShopBadgePreviewBackgroundImageUrl(
                "background-image: url('/uploads/shop_assets/badges/asset.webp');"
            )
        )
        assertEquals(
            "https://images.novelpia.com/badges/asset.webp",
            adminShopBadgePreviewBackgroundImageUrl(
                "background: url(//images.novelpia.com/badges/asset.webp) center / cover;"
            )
        )
        assertEquals(
            "https://novalpie.cc/assets/badges/asset.webp",
            adminShopBadgePreviewAssetUrl("assets/badges/asset.webp")
        )
        assertNull(adminShopBadgePreviewAssetUrl("javascript:alert(1)"))
        assertNull(adminShopBadgePreviewAssetUrl("data:image/webp;base64,abc"))
    }

    @Test
    fun badgePreviewKeepsSourceArtworkAndShapeMetadataWithoutExecutingCss() {
        val css = """
            .badge { background: url('https://cdn.example.test/scene.webp') center / contain no-repeat;
            border-radius: 8px; border: 1px solid #22c55e; color: #102030; }
        """.trimIndent()

        assertEquals("https://cdn.example.test/scene.webp", adminShopBadgePreviewBackgroundImageUrl(css))
        assertEquals(8, adminShopBadgePreviewCornerRadius(css))
        assertEquals(21, adminShopBadgePreviewCornerRadius("border-radius: 9999px;"))
        assertEquals(21, adminShopBadgePreviewCornerRadius("border-radius: 50%;"))
        assertEquals(21, adminShopBadgePreviewCornerRadius(null))
        assertEquals(ContentScale.Fit, adminShopBadgePreviewContentScale(css))
        assertEquals(0xFF22C55E.toInt(), adminShopBadgePreviewBorderColor(css).toArgb())
        assertEquals("Source Name", adminShopBadgePreviewForeground("<span>{{ name }}</span>", "Source Name"))
    }

    @Test
    fun badgeFallbackVariantsMatchTheWebsiteNamedPalettes() {
        assertEquals(
            listOf(0xFF34D399.toInt(), 0xFF60A5FA.toInt()),
            adminShopBadgeFallbackColors("新手读者", null).map { it.toArgb() }
        )
        assertEquals(
            listOf(0xFFF59E0B.toInt(), 0xFFF97316.toInt()),
            adminShopBadgeFallbackColors("富豪", null).map { it.toArgb() }
        )
        assertEquals(
            listOf(0xFF22D3EE.toInt(), 0xFFA855F7.toInt()),
            adminShopBadgePreviewColors(null, "赛博会员", null).map { it.toArgb() }
        )
        assertEquals(
            listOf(0xFF22C55E.toInt(), 0xFF10B981.toInt()),
            adminShopBadgePreviewColors(null, "森林旅者", null).map { it.toArgb() }
        )
    }

    @Test
    fun adminShopLocalImageUrisStayOutOfSavePayloads() {
        assertNull(adminShopRemoteImageUrl("content://com.android.providers.media.documents/image/1"))
        assertNull(adminShopRemoteImageUrl("file:///sdcard/Download/frame.png"))
        assertNull(adminShopRemoteImageUrl("data:image/png;base64,abc"))
        assertEquals("https://images.novelpia.com/frame.png", adminShopRemoteImageUrl(" https://images.novelpia.com/frame.png "))
        assertTrue(adminShopLocalPreviewNeedsRemoteUrl("frame", null, hasLocalPreview = true))
        assertFalse(adminShopLocalPreviewNeedsRemoteUrl("badge", null, hasLocalPreview = true))
        assertFalse(adminShopLocalPreviewNeedsRemoteUrl("frame", "https://images.novelpia.com/frame.png", hasLocalPreview = true))
    }
}
