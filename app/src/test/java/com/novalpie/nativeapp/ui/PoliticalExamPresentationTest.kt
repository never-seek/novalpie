package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.PoliticalExamAnswers
import com.novalpie.nativeapp.model.PoliticalExamDetail
import com.novalpie.nativeapp.model.PoliticalExamResult
import org.junit.Assert.assertEquals
import org.junit.Test

class PoliticalExamPresentationTest {
    @Test
    fun politicalExamOverviewMirrorsWebsiteRulesAndLoginState() {
        val signedIn = politicalExamOverview(hasAuthToken = true)
        val signedOut = politicalExamOverview(hasAuthToken = false)

        assertEquals("政治考试", signedIn.title)
        assertEquals("已登录", signedIn.statusLabel)
        assertEquals("开始考试", signedIn.primaryAction)
        assertEquals("需要登录", signedOut.statusLabel)
        assertEquals("登录后参加考试", signedOut.primaryAction)
        assertEquals(listOf("100 题", "30 分钟", "80 分通过", "每日次数受限"), signedIn.stats)
        assertEquals(
            listOf(
                "40 道单选题，每题 1 分",
                "10 道多选题，每题 2 分，必须全部选对",
                "25 道判断题，每题 1 分",
                "25 道填空题，每题 1 分",
                "开始与提交都会同步源站账号状态"
            ),
            signedIn.rules
        )
    }

    @Test
    fun countsOnlyCompletedAnswersAcrossAllQuestionTypes() {
        val answers = PoliticalExamAnswers(
            singleChoice = listOf(0, null),
            multipleChoice = listOf(listOf(0, 2), emptyList()),
            trueFalse = listOf(true, null),
            fillBlank = listOf("answer", "  ")
        )

        assertEquals(4, politicalExamAnsweredCount(answers))
        assertEquals("02:05", formatPoliticalExamTime(125))
    }

    @Test
    fun summarizesCorrectAnswersByWebsiteDetailKey() {
        val result = PoliticalExamResult(
            score = 2,
            total = 3,
            passed = false,
            details = mapOf(
                "single_choice" to listOf(
                    PoliticalExamDetail(correct = true),
                    PoliticalExamDetail(correct = false),
                    PoliticalExamDetail(correct = true)
                )
            )
        )

        assertEquals("2 / 3", politicalExamCorrectSummary(result, "single_choice"))
        assertEquals("0 / 0", politicalExamCorrectSummary(result, "fill_blank"))
    }
}
