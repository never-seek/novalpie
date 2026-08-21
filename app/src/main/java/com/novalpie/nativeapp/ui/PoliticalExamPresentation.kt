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
    title = "考试",
    subtitle = "通过考试后即可使用无替换模式阅读。",
    statusLabel = if (hasAuthToken) "已登录" else "需要登录",
    stats = listOf("100 分", "30 分钟", "80 分通过", "每日 3 次"),
    rules = listOf(
        "考试包含 40 道单选题（每题 1 分）、10 道多选题（每题 2 分）、25 道判断题（每题 1 分）、25 道填空题（每题 1 分），共 100 分",
        "考试时间限制为 30 分钟",
        "通过标准：80 分及以上",
        "题目和选项顺序随机，请认真作答",
        "多选题必须全部选对才得分，少选、多选、错选均不得分",
        "考试过程中请勿切换页面或刷新，否则可能丢失进度",
        "每天最多可参加 3 次考试"
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
