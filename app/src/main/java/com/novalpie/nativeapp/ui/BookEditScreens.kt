@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.novalpie.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.LoadResult

@Composable
internal fun BookEditInfoScreen(
    state: BookEditState,
    onRetry: () -> Unit,
    onDraftChange: (BookEditDraft) -> Unit,
    onCoverSelected: (String) -> Unit,
    onAccessPolicyDraftChange: (BookAccessPolicyDraft) -> Unit,
    onSaveAccessPolicy: () -> Unit,
    onTransferIdentifierChange: (String) -> Unit,
    onTransfer: () -> Unit,
    onSave: () -> Unit
) {
    val validation = validateBookEditDraft(state.draft)
    var confirmSave by remember { mutableStateOf(false) }
    var confirmPolicy by remember { mutableStateOf(false) }
    var confirmTransfer by remember { mutableStateOf(false) }
    val busy = state.saving || state.uploadingCover || state.savingAccessPolicy || state.transferringBook

    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text("保存书籍信息") },
            text = { Text("将把当前可编辑字段同步到网站。服务器仍会逐字段校验权限。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSave = false
                    onSave()
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { confirmSave = false }) { Text("取消") } }
        )
    }
    if (confirmPolicy) {
        AlertDialog(
            onDismissRequest = { confirmPolicy = false },
            title = { Text("保存读写门槛") },
            text = { Text("将同步阅读门槛、下载门槛和下载开关到网站。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmPolicy = false
                    onSaveAccessPolicy()
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { confirmPolicy = false }) { Text("取消") } }
        )
    }
    if (confirmTransfer) {
        AlertDialog(
            onDismissRequest = { confirmTransfer = false },
            title = { Text("转让书籍") },
            text = { Text("转让会把此书的管理权交给接收方。请确认接收方标识无误：${state.transferIdentifier.trim()}") },
            confirmButton = {
                TextButton(onClick = {
                    confirmTransfer = false
                    onTransfer()
                }) { Text("确认转让") }
            },
            dismissButton = { TextButton(onClick = { confirmTransfer = false }) { Text("取消") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("编辑书籍信息", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("书籍 #${state.bookId} · 字段权限、封面上传、门槛和转让均按网站接口执行。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (val info = state.info) {
            LoadResult.Idle, LoadResult.Loading -> item { LoadingBlock("正在加载书籍信息") }
            is LoadResult.Error -> item { ErrorBlock(info.message, "重新加载", onRetry) }
            is LoadResult.Success -> Unit
        }
        when (val access = state.permissions) {
            LoadResult.Idle, LoadResult.Loading -> item { LoadingBlock("正在检查编辑权限") }
            is LoadResult.Error -> item { ErrorBlock(access.message, "重试权限", onRetry) }
            is LoadResult.Success -> {
                item {
                    BookEditCoverSection(
                        title = state.draft.title,
                        url = state.draft.photoUrl,
                        enabled = access.value.photoUrl && !busy,
                        uploading = state.uploadingCover,
                        onSelected = onCoverSelected
                    )
                }
                item {
                    BookEditTextField("中文书名 *", state.draft.title, access.value.title, busy) {
                        onDraftChange(state.draft.copy(title = it))
                    }
                }
                item {
                    BookEditTextField("原文书名", state.draft.titleTranslation, access.value.titleTranslation, busy) {
                        onDraftChange(state.draft.copy(titleTranslation = it))
                    }
                }
                item {
                    BookEditTextField("作者 *", state.draft.authorName, access.value.authorName, busy) {
                        onDraftChange(state.draft.copy(authorName = it))
                    }
                }
                item {
                    BookEditTextField("简介", state.draft.description, access.value.description, busy, minLines = 5) {
                        onDraftChange(state.draft.copy(description = it))
                    }
                }
                item {
                    BookEditTextField("来源", state.draft.source, access.value.source, busy) {
                        onDraftChange(state.draft.copy(source = it))
                    }
                }
                item {
                    BookEditTextField("来源链接", state.draft.sourceUrl, access.value.sourceUrl, busy) {
                        onDraftChange(state.draft.copy(sourceUrl = it))
                    }
                }
                item {
                    BookEditTextField("语言", state.draft.language, access.value.language, busy) {
                        onDraftChange(state.draft.copy(language = it))
                    }
                }
                item { BookEditStatusSection(state, access.value, busy, onDraftChange) }
                item { BookEditTagSection(state, access.value, busy, onDraftChange) }
                item {
                    BookAccessPolicySection(
                        state = state,
                        busy = busy,
                        onDraftChange = onAccessPolicyDraftChange,
                        onSave = { confirmPolicy = true }
                    )
                }
                item {
                    BookTransferSection(
                        state = state,
                        busy = busy,
                        onIdentifierChange = onTransferIdentifierChange,
                        onTransfer = { confirmTransfer = true }
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.actionMessage?.let {
                            Text(
                                it,
                                color = if (it.contains("失败") || it.contains("错误")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            onClick = { confirmSave = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = validation == null && !busy
                        ) { Text(if (state.saving) "保存中..." else "保存基本信息") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookEditCoverSection(
    title: String,
    url: String,
    enabled: Boolean,
    uploading: Boolean,
    onSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onSelected)
    }
    var preview by remember(url) { mutableStateOf(false) }
    if (preview && url.isNotBlank()) {
        ImagePreviewDialog(url, "$title · 封面", onDismiss = { preview = false })
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(url).crossfade(true).precision(Precision.EXACT).build(),
                contentDescription = "书籍封面",
                modifier = Modifier.width(100.dp).height(150.dp).clickable(enabled = url.isNotBlank()) { preview = true },
                contentScale = ContentScale.Crop,
                loading = { LoadingBlock("加载封面") },
                error = { Text("暂无封面") }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("封面", fontWeight = FontWeight.Bold)
                Text("上传原始图片，不在 App 内压缩。点击封面可查看大图。", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = enabled) {
                    Text(if (uploading) "上传中..." else if (enabled) "选择图片" else "无编辑权限")
                }
            }
        }
    }
}

@Composable
private fun BookEditTextField(
    label: String,
    value: String,
    permitted: Boolean,
    busy: Boolean,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { if (!permitted) Text("当前账号无此字段编辑权限") },
        minLines = minLines,
        enabled = permitted && !busy
    )
}

@Composable
private fun BookEditStatusSection(
    state: BookEditState,
    permissions: BookEditPermissions,
    busy: Boolean,
    onDraftChange: (BookEditDraft) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("连载与分级", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("连载中", "已完结")) { status ->
                    FilterChip(
                        selected = state.draft.status == status,
                        onClick = { onDraftChange(state.draft.copy(status = status)) },
                        label = { Text(status) },
                        enabled = permissions.spans && !busy
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("成人内容")
                    if (!permissions.isAdult) Text("当前账号无此字段编辑权限", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.draft.isAdult,
                    onCheckedChange = { onDraftChange(state.draft.copy(isAdult = it)) },
                    enabled = permissions.isAdult && !busy
                )
            }
        }
    }
}

@Composable
private fun BookEditTagSection(
    state: BookEditState,
    permissions: BookEditPermissions,
    busy: Boolean,
    onDraftChange: (BookEditDraft) -> Unit
) {
    val enabled = permissions.tags && !busy
    fun addTag() {
        val tag = state.draft.tagDraft.trim()
        if (tag.isBlank() || tag in state.draft.tags) return
        onDraftChange(state.draft.copy(tags = state.draft.tags + tag, tagDraft = ""))
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("标签", fontWeight = FontWeight.Bold)
            if (state.draft.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.draft.tags) { tag ->
                        AssistChip(
                            onClick = { onDraftChange(state.draft.copy(tags = state.draft.tags - tag)) },
                            label = { Text("$tag  ×") },
                            enabled = enabled
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.draft.tagDraft,
                    onValueChange = { onDraftChange(state.draft.copy(tagDraft = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("新增标签") },
                    singleLine = true,
                    enabled = enabled
                )
                OutlinedButton(onClick = { addTag() }, enabled = enabled) { Text("添加") }
            }
            if (!permissions.tags) Text("当前账号无标签编辑权限", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BookAccessPolicySection(
    state: BookEditState,
    busy: Boolean,
    onDraftChange: (BookAccessPolicyDraft) -> Unit,
    onSave: () -> Unit
) {
    val draft = state.accessPolicyDraft
    val validation = validateBookAccessPolicyDraft(draft)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("阅读与下载门槛", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("允许下载")
                    Text("关闭后下载门槛会按网页逻辑强制为无。", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = draft.allowDownload,
                    onCheckedChange = { onDraftChange(draft.copy(allowDownload = it)) },
                    enabled = !busy
                )
            }
            ThresholdEditor(
                title = "下载门槛",
                type = if (draft.allowDownload) draft.downloadThresholdType else "none",
                value = draft.downloadThresholdValue,
                enabled = draft.allowDownload && !busy,
                onTypeChange = { onDraftChange(draft.copy(downloadThresholdType = it)) },
                onValueChange = { onDraftChange(draft.copy(downloadThresholdValue = it)) }
            )
            ThresholdEditor(
                title = "阅读门槛",
                type = draft.readThresholdType,
                value = draft.readThresholdValue,
                enabled = !busy,
                onTypeChange = { onDraftChange(draft.copy(readThresholdType = it)) },
                onValueChange = { onDraftChange(draft.copy(readThresholdValue = it)) }
            )
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSave,
                enabled = validation == null && !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.savingAccessPolicy) "保存中..." else "保存读写门槛") }
        }
    }
}

@Composable
private fun ThresholdEditor(
    title: String,
    type: String,
    value: String,
    enabled: Boolean,
    onTypeChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    val options = listOf("none" to "不限", "points_min" to "最低积分", "points_pay" to "付费积分")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = type == option.first,
                    onClick = { onTypeChange(option.first) },
                    label = { Text(option.second) },
                    enabled = enabled
                )
            }
        }
        if (type != "none") {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("积分值") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BookTransferSection(
    state: BookEditState,
    busy: Boolean,
    onIdentifierChange: (String) -> Unit,
    onTransfer: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("书籍转让", fontWeight = FontWeight.Bold)
            Text("支持填写 uid:数字 或用户名。服务器会再次校验接收方。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = state.transferIdentifier,
                onValueChange = onIdentifierChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("接收方 UID 或用户名") },
                singleLine = true,
                enabled = !busy
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onTransfer,
                    enabled = state.transferIdentifier.trim().isNotEmpty() && !busy
                ) { Text(if (state.transferringBook) "提交中..." else "转让书籍") }
                Spacer(Modifier.width(8.dp))
                Text("这是管理权变更操作。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
