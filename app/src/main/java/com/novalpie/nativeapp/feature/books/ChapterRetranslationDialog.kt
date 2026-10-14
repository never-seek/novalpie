package com.novalpie.nativeapp.feature.books

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ChapterRetranslationDialog(
    state:ChapterRetranslationState,onRange:(String)->Unit,onSubmit:()->Unit,onDismiss:()->Unit,
    onWorkspace:()->Unit,onRefresh:()->Unit,onCheckedForRetry:()->Unit,
) {
    if(!state.open)return
    val preview=state.selection
    AlertDialog(
        onDismissRequest=onDismiss,title={Text("重翻章节")},
        text={Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(state.bookTitle,style=MaterialTheme.typography.titleSmall)
            OutlinedTextField(value=state.range,onValueChange=onRange,enabled=!state.submitting&&!state.loading,
                label={Text("章节范围")},placeholder={Text("例如 1-5, 8, 11-13")},modifier=Modifier.fillMaxWidth(),
                isError=!state.loading&&preview.isFailure,singleLine=true)
            Text(if(state.loading)"正在读取章节目录…" else preview.fold(
                {"${if(state.range.isBlank())"全部章节" else "已选章节"}：共 ${it.size} 章"},
                {it.message ?: "章节范围无效"}),style=MaterialTheme.typography.bodySmall)
            Text("留空选择全书。与网页相同，重翻使用本人 API：服务端 API 需已激活、健康且审核通过；仅保存在本机的 API 请在工作区自助翻译。任务会消耗您自己的 API 配额。",style=MaterialTheme.typography.bodySmall)
            state.message?.let { Text(it,color=if(state.confirmed)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            TextButton(onClick=onWorkspace,enabled=!state.submitting) { Text("前往工作区") }
            if(state.confirmed)TextButton(onClick=onRefresh) { Text("任务完成后刷新正文 / 目录") }
            if(state.uncertain)TextButton(onClick=onCheckedForRetry) { Text("我已确认未提交，可重试") }
        }},
        confirmButton={TextButton(onClick=onSubmit,enabled=!state.loading&&!state.submitting&&!state.confirmed&&!state.uncertain&&preview.isSuccess) {
            Text(if(state.submitting)"提交中…" else "提交重翻任务")
        }},dismissButton={TextButton(onClick=onDismiss){Text("关闭")}},
    )
}
