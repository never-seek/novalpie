package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.PoliticalExamAnswers
import com.novalpie.nativeapp.model.PoliticalExamResult

enum class PoliticalExamPhase {
    Landing,
    Active,
    Result
}

internal data class PoliticalExamOverview(
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val stats: List<String>,
    val rules: List<String>,
    val primaryAction: String
)

internal fun politicalExamOverview(hasAuthToken: Boolean): PoliticalExamOverview = PoliticalExamOverview(
    title = "政治考试",
    subtitle = "通过后按源站规则解锁阅读权限，题目和顺序由服务器实时生成。",
    statusLabel = if (hasAuthToken) "已登录" else "需要登录",
    stats = listOf("100 题", "30 分钟", "80 分通过", "每日次数受限"),
    rules = listOf(
        "40 道单选题，每题 1 分",
        "10 道多选题，每题 2 分，必须全部选对",
        "25 道判断题，每题 1 分",
        "25 道填空题，每题 1 分",
        "开始与提交都会同步源站账号状态"
    ),
    primaryAction = if (hasAuthToken) "开始考试" else "登录后参加考试"
)

internal fun politicalExamAnsweredCount(answers: PoliticalExamAnswers): Int =
    answers.singleChoice.count { it != null } +
        answers.multipleChoice.count { it.isNotEmpty() } +
        answers.trueFalse.count { it != null } +
        answers.fillBlank.count { it.isNotBlank() }

internal fun formatPoliticalExamTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

internal fun politicalExamCorrectSummary(result: PoliticalExamResult, key: String): String {
    val details = result.details[key].orEmpty()
    return "${details.count { it.correct }} / ${details.size}"
}
