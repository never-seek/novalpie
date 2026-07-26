package com.novalpie.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The site is inconsistent about where it puts an error explanation, in the same way it is
 * inconsistent about response shapes. These cases are the ones the response normalizers already
 * have to tolerate, so the error path tolerates them too.
 *
 * Robolectric is required because `org.json` ships in the Android platform rather than as a
 * dependency; under a plain JVM runner its methods are stubbed to throw, so every JSON case here
 * fails for a reason unrelated to what it is testing.
 */
@RunWith(RobolectricTestRunner::class)
class NovalPieApiExceptionTest {

    @Test
    fun readsAFlatMessageField() {
        assertEquals(
            "章节标题重复，请修改后重试",
            NovalPieApiException.extractServerMessage("""{"message":"章节标题重复，请修改后重试"}"""),
        )
    }

    @Test
    fun acceptsTheCommonAliasesForThatField() {
        assertEquals("没有权限", NovalPieApiException.extractServerMessage("""{"error":"没有权限"}"""))
        assertEquals("参数错误", NovalPieApiException.extractServerMessage("""{"detail":"参数错误"}"""))
        assertEquals("请稍后", NovalPieApiException.extractServerMessage("""{"msg":"请稍后"}"""))
        assertEquals(
            "上传失败",
            NovalPieApiException.extractServerMessage("""{"error_message":"上传失败"}"""),
        )
    }

    @Test
    fun looksInsideDataWhenTheMessageIsNested() {
        assertEquals(
            "书籍已下架",
            NovalPieApiException.extractServerMessage("""{"data":{"message":"书籍已下架"},"code":404}"""),
        )
    }

    @Test
    fun readsTheFirstValidationMessageFromAnErrorsMap() {
        assertEquals(
            "标题不能为空",
            NovalPieApiException.extractServerMessage("""{"errors":{"title":["标题不能为空"]}}"""),
        )
        assertEquals(
            "内容太短",
            NovalPieApiException.extractServerMessage("""{"errors":{"content":"内容太短"}}"""),
        )
    }

    @Test
    fun acceptsAShortPlainTextBody() {
        assertEquals("Too Many Requests", NovalPieApiException.extractServerMessage("Too Many Requests"))
    }

    /**
     * An HTML error page or a long body would be worse than showing the status code alone, so
     * neither is offered to the user.
     */
    @Test
    fun rejectsBodiesThatWouldNotHelpTheUser() {
        assertNull(NovalPieApiException.extractServerMessage(""))
        assertNull(NovalPieApiException.extractServerMessage("   "))
        assertNull(NovalPieApiException.extractServerMessage("<html><body>502 Bad Gateway</body></html>"))
        assertNull(NovalPieApiException.extractServerMessage("x".repeat(400)))
        // Well-formed JSON with nothing message-like in it.
        assertNull(NovalPieApiException.extractServerMessage("""{"code":500,"trace_id":"abc123"}"""))
        // Not JSON at all despite the brace.
        assertNull(NovalPieApiException.extractServerMessage("""{not json"""))
    }

    @Test
    fun treatsTheStringNullAsAbsent() {
        assertNull(NovalPieApiException.extractServerMessage("""{"message":"null"}"""))
        assertNull(NovalPieApiException.extractServerMessage("""{"message":""}"""))
    }

    /**
     * The `NovalPie API <code>:` prefix is retained so existing status-code handling keeps working,
     * with the server text appended.
     */
    @Test
    fun exceptionMessageKeepsTheStatusPrefixAndAppendsServerText() {
        assertEquals(
            "NovalPie API 422: /api/novels/1/chapters - 标题重复",
            NovalPieApiException(422, "/api/novels/1/chapters", "标题重复").message,
        )
        assertEquals(
            "NovalPie API 500: /api/favorites",
            NovalPieApiException(500, "/api/favorites", null).message,
        )
    }
}
