package com.novalpie.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.PoliticalExamQuestion
import com.novalpie.nativeapp.model.PoliticalExamResult
import kotlinx.coroutines.delay

@Composable
fun PoliticalExamScreen(
    state: PoliticalExamState,
    hasAuthToken: Boolean,
    onStart: () -> Unit,
    onOpenLogin: () -> Unit,
    onSelectSingle: (Int, Int) -> Unit,
    onToggleMultiple: (Int, Int) -> Unit,
    onSelectTrueFalse: (Int, Boolean) -> Unit,
    onUpdateBlank: (Int, String) -> Unit,
    onTick: () -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    var confirmStart by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }

    BackHandler(enabled = state.phase == PoliticalExamPhase.Active) { confirmExit = true }

    LaunchedEffect(state.phase, state.remainingTimeSeconds, state.submitting) {
        if (state.phase == PoliticalExamPhase.Active && !state.submitting) {
            if (state.remainingTimeSeconds > 0) delay(1_000)
            onTick()
        }
    }

    when (state.phase) {
        PoliticalExamPhase.Landing -> PoliticalExamLanding(
            state = state,
            hasAuthToken = hasAuthToken,
            onStart = { confirmStart = true },
            onOpenLogin = onOpenLogin
        )
        PoliticalExamPhase.Active -> PoliticalExamActive(
            state = state,
            onSelectSingle = onSelectSingle,
            onToggleMultiple = onToggleMultiple,
            onSelectTrueFalse = onSelectTrueFalse,
            onUpdateBlank = onUpdateBlank,
            onSubmit = { confirmSubmit = true }
        )
        PoliticalExamPhase.Result -> PoliticalExamResultView(state, onReset)
    }

    if (confirmStart) {
        PoliticalExamConfirmDialog(
            title = "开始考试",
            message = "开始会创建源站考试会话并计入每日次数。考试限时 30 分钟，确定继续吗？",
            onDismiss = { confirmStart = false },
            onConfirm = { confirmStart = false; onStart() }
        )
    }
    if (confirmSubmit) {
        val session = (state.session as? LoadResult.Success)?.value
        val answered = politicalExamAnsweredCount(state.answers)
        PoliticalExamConfirmDialog(
            title = "提交考试",
            message = "已作答 $answered / ${session?.paper?.totalQuestions ?: 0} 题。提交后不能修改，确定提交吗？",
            onDismiss = { confirmSubmit = false },
            onConfirm = { confirmSubmit = false; onSubmit() }
        )
    }
    if (confirmExit) {
        PoliticalExamConfirmDialog(
            title = "离开考试",
            message = "考试计时仍以服务器会话截止时间为准。离开后可以从工具页重新进入当前本地会话。",
            onDismiss = { confirmExit = false },
            onConfirm = { confirmExit = false; onBack() }
        )
    }
}

@Composable
private fun PoliticalExamLanding(
    state: PoliticalExamState,
    hasAuthToken: Boolean,
    onStart: () -> Unit,
    onOpenLogin: () -> Unit
) {
    val overview = politicalExamOverview(hasAuthToken)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PoliticalExamOverviewCard(overview)
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("考试规则", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    overview.rules.forEach { rule -> Text("• $rule") }
                }
            }
        }
        state.actionMessage?.let { message -> item { ExamMessage(message) } }
        when (val session = state.session) {
            LoadResult.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            is LoadResult.Error -> item { ExamMessage(session.message) }
            else -> Unit
        }
        item {
            if (hasAuthToken) {
                Button(onClick = onStart, enabled = state.session != LoadResult.Loading, modifier = Modifier.fillMaxWidth()) {
                    Text(overview.primaryAction)
                }
            } else {
                Button(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) { Text(overview.primaryAction) }
            }
        }
    }
}

@Composable
private fun PoliticalExamOverviewCard(overview: PoliticalExamOverview) {
    ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(overview.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(overview.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        overview.statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                overview.stats.forEach { stat ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            stat,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoliticalExamActive(
    state: PoliticalExamState,
    onSelectSingle: (Int, Int) -> Unit,
    onToggleMultiple: (Int, Int) -> Unit,
    onSelectTrueFalse: (Int, Boolean) -> Unit,
    onUpdateBlank: (Int, String) -> Unit,
    onSubmit: () -> Unit
) {
    val session = (state.session as? LoadResult.Success)?.value
    if (session == null) {
        ExamMessage("考试会话不可用")
        return
    }
    val answered = politicalExamAnsweredCount(state.answers)
    val total = session.paper.totalQuestions
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("考试进行中", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(formatPoliticalExamTime(state.remainingTimeSeconds), color = if (state.remainingTimeSeconds <= 300) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = if (total == 0) 0f else answered.toFloat() / total,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("已作答 $answered / $total 题", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.submitting) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.actionMessage?.let { message -> item { ExamMessage(message) } }

        if (session.paper.singleChoice.isNotEmpty()) {
            item { ExamSectionTitle("一、单选题", "${session.paper.singleChoice.size} 题 · 每题 1 分") }
            itemsIndexed(session.paper.singleChoice, key = { index, _ -> "single-$index" }) { index, question ->
                ExamChoiceQuestion(
                    number = index + 1,
                    question = question,
                    selected = state.answers.singleChoice.getOrNull(index)?.let(::setOf).orEmpty(),
                    multiple = false,
                    onSelect = { onSelectSingle(index, it) }
                )
            }
        }
        if (session.paper.multipleChoice.isNotEmpty()) {
            item { ExamSectionTitle("二、多选题", "${session.paper.multipleChoice.size} 题 · 每题 2 分") }
            itemsIndexed(session.paper.multipleChoice, key = { index, _ -> "multiple-$index" }) { index, question ->
                ExamChoiceQuestion(
                    number = index + 1,
                    question = question,
                    selected = state.answers.multipleChoice.getOrNull(index).orEmpty().toSet(),
                    multiple = true,
                    onSelect = { onToggleMultiple(index, it) }
                )
            }
        }
        if (session.paper.trueFalse.isNotEmpty()) {
            item { ExamSectionTitle("三、判断题", "${session.paper.trueFalse.size} 题 · 每题 1 分") }
            itemsIndexed(session.paper.trueFalse, key = { index, _ -> "boolean-$index" }) { index, question ->
                ExamTrueFalseQuestion(
                    number = index + 1,
                    question = question.question,
                    answer = state.answers.trueFalse.getOrNull(index),
                    onSelect = { onSelectTrueFalse(index, it) }
                )
            }
        }
        if (session.paper.fillBlank.isNotEmpty()) {
            item { ExamSectionTitle("四、填空题", "${session.paper.fillBlank.size} 题 · 每题 1 分") }
            itemsIndexed(session.paper.fillBlank, key = { index, _ -> "blank-$index" }) { index, question ->
                ExamFillBlankQuestion(
                    number = index + 1,
                    question = question.question,
                    answer = state.answers.fillBlank.getOrNull(index).orEmpty(),
                    onChange = { onUpdateBlank(index, it) }
                )
            }
        }
        item {
            Button(onClick = onSubmit, enabled = !state.submitting, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.submitting) "正在提交…" else "提交考试")
            }
        }
    }
}

@Composable
private fun ExamChoiceQuestion(
    number: Int,
    question: PoliticalExamQuestion,
    selected: Set<Int>,
    multiple: Boolean,
    onSelect: (Int) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number. ${question.question}", fontWeight = FontWeight.SemiBold)
            question.options.forEachIndexed { index, option ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(index) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (index in selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (multiple) Checkbox(index in selected, onCheckedChange = { onSelect(index) })
                        else RadioButton(index in selected, onClick = { onSelect(index) })
                        Text("${('A'.code + index).toChar()}. $option", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamTrueFalseQuestion(number: Int, question: String, answer: Boolean?, onSelect: (Boolean) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$number. $question", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (answer == true) Button({ onSelect(true) }, Modifier.weight(1f)) { Text("正确") }
                else OutlinedButton({ onSelect(true) }, Modifier.weight(1f)) { Text("正确") }
                if (answer == false) Button({ onSelect(false) }, Modifier.weight(1f)) { Text("错误") }
                else OutlinedButton({ onSelect(false) }, Modifier.weight(1f)) { Text("错误") }
            }
        }
    }
}

@Composable
private fun ExamFillBlankQuestion(number: Int, question: String, answer: String, onChange: (String) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number. $question", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(answer, onChange, label = { Text("答案") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PoliticalExamResultView(state: PoliticalExamState, onReset: () -> Unit) {
    val result = (state.result as? LoadResult.Success)?.value
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (result == null) {
            item { ExamMessage((state.result as? LoadResult.Error)?.message ?: "成绩不可用") }
        } else {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (result.passed) "考试通过" else "考试未通过", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (result.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        Text("${result.score} / ${result.total}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(if (result.passed) "新的阅读权限已按源站结果同步" else "可按源站限制重新参加考试")
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("分题型结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ExamResultLine("单选题", result, "single_choice")
                        ExamResultLine("多选题", result, "multiple_choice")
                        ExamResultLine("判断题", result, "true_false")
                        ExamResultLine("填空题", result, "fill_blank")
                    }
                }
            }
            item { Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("返回考试说明") } }
        }
    }
}

@Composable
private fun ExamResultLine(label: String, result: PoliticalExamResult, key: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(politicalExamCorrectSummary(result, key), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExamSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExamMessage(message: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}

@Composable
private fun PoliticalExamConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
