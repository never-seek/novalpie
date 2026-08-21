package com.novalpie.nativeapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.AdminBaseUrlRule
import com.novalpie.nativeapp.model.AdminCookieConfig
import com.novalpie.nativeapp.model.AdminShopItem

/**
 * Editors are shared by the source-aligned administrator surfaces. They remain
 * intentionally modal because every save is followed by a separate confirmation
 * in [AdminScreen] before it can mutate a live administrator endpoint.
 */
@Composable
internal fun AdminCookieEditorDialog(
    initial: AdminCookieConfig,
    onDismiss: () -> Unit,
    onSave: (AdminCookieConfig, String?) -> Unit
) {
    var key by remember(initial.id) { mutableStateOf(initial.configKey) }
    var description by remember(initial.id) { mutableStateOf(initial.description.orEmpty()) }
    var cookieRaw by remember(initial.id) { mutableStateOf("") }
    var proxy by remember(initial.id) { mutableStateOf(initial.proxyIp.orEmpty()) }
    var active by remember(initial.id) { mutableStateOf(initial.isActive) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id > 0) "编辑 Cookie 配置" else "新增 Cookie 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("配置键名") },
                    enabled = initial.id <= 0,
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("说明") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = cookieRaw,
                    onValueChange = { cookieRaw = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (initial.id > 0) "新 Cookie（留空不修改）" else "Cookie") },
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    value = proxy,
                    onValueChange = { proxy = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("代理 IP/URL") },
                    singleLine = true
                )
                AdminEditorSwitchRow("启用", active) { active = !active }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            configKey = key.trim(),
                            description = description.trim().ifBlank { null },
                            proxyIp = proxy.trim().ifBlank { null },
                            isActive = active
                        ),
                        cookieRaw.trim().ifBlank { null }
                    )
                },
                enabled = key.isNotBlank() && (initial.id > 0 || cookieRaw.contains("="))
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun AdminRuleEditorDialog(
    initial: AdminBaseUrlRule,
    onDismiss: () -> Unit,
    onSave: (AdminBaseUrlRule) -> Unit
) {
    var pattern by remember(initial.id) { mutableStateOf(initial.pattern) }
    var action by remember(initial.id) { mutableStateOf(initial.action) }
    var description by remember(initial.id) { mutableStateOf(initial.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id > 0) "编辑 BaseURL 规则" else "新增 BaseURL 规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("匹配规则") },
                    enabled = initial.id <= 0,
                    singleLine = true
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("allow", "block", "manual").forEach { value ->
                        OutlinedButton(onClick = { action = value }, enabled = action != value) {
                            Text(adminBaseUrlActionLabel(value))
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("说明") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initial.copy(pattern = pattern.trim(), action = action, description = description.trim().ifBlank { null }))
                },
                enabled = pattern.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun AdminShopEditorDialog(
    initial: AdminShopItem,
    onDismiss: () -> Unit,
    onSave: (AdminShopItem) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var description by remember(initial.id) { mutableStateOf(initial.description.orEmpty()) }
    var price by remember(initial.id) { mutableStateOf(initial.price.toString()) }
    var type by remember(initial.id) { mutableStateOf(initial.type) }
    var imageUrl by remember(initial.id) { mutableStateOf(initial.imageUrl.orEmpty()) }
    var badgeHtml by remember(initial.id) { mutableStateOf(initial.badgeHtml.orEmpty()) }
    var badgeCss by remember(initial.id) { mutableStateOf(initial.badgeCss.orEmpty()) }
    var active by remember(initial.id) { mutableStateOf(initial.isActive) }
    var localFramePreview by remember(initial.id) { mutableStateOf<Uri?>(null) }
    val frameImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        localFramePreview = uri
    }
    val remoteImageUrl = adminShopRemoteImageUrl(imageUrl)
    val localPreviewNeedsUrl = adminShopLocalPreviewNeedsRemoteUrl(
        type = type,
        remoteImageUrl = remoteImageUrl,
        hasLocalPreview = localFramePreview != null
    )
    val imageUrlUsesLocalScheme = imageUrl.isNotBlank() && remoteImageUrl == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id > 0) "编辑商品" else "新增商品") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("说明") }, minLines = 2)
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("积分价格") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { type = "frame" }, enabled = type != "frame") { Text("头像框") }
                    OutlinedButton(onClick = { type = "badge" }, enabled = type != "badge") { Text("徽章") }
                }
                if (type == "frame") {
                    OutlinedTextField(imageUrl, { imageUrl = it }, Modifier.fillMaxWidth(), label = { Text("图片 URL") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { frameImagePicker.launch(arrayOf("image/*")) }) { Text("选择本地图片") }
                        if (localFramePreview != null) {
                            TextButton(onClick = { localFramePreview = null }) { Text("移除本地预览") }
                        }
                    }
                    if (localPreviewNeedsUrl) {
                        Text(
                            "本地图片仅用于草稿预览；请填写已上传的图片 URL 后再保存。",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else if (imageUrlUsesLocalScheme) {
                        Text(
                            "图片 URL 不能使用本地 URI；请选择本地图片仅作预览，或填写远程 URL。",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Text(
                            "本地选择只更新当前草稿预览，不会上传或写入图片 URL。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    OutlinedTextField(badgeHtml, { badgeHtml = it }, Modifier.fillMaxWidth(), label = { Text("徽章 HTML") }, minLines = 2)
                    OutlinedTextField(badgeCss, { badgeCss = it }, Modifier.fillMaxWidth(), label = { Text("徽章 CSS") }, minLines = 2)
                }
                Text("素材预览", style = MaterialTheme.typography.labelMedium)
                AdminShopPreview(
                    item = initial.copy(
                        name = name.trim().ifBlank { initial.name },
                        type = type,
                        imageUrl = localFramePreview?.toString() ?: remoteImageUrl,
                        badgeHtml = badgeHtml.trim().ifBlank { null },
                        badgeCss = badgeCss.trim().ifBlank { null }
                    ),
                    compact = false
                )
                AdminEditorSwitchRow("上架", active) { active = !active }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            price = price.toLongOrNull() ?: 0,
                            type = type,
                            imageUrl = remoteImageUrl,
                            badgeHtml = badgeHtml.trim().ifBlank { null },
                            badgeCss = badgeCss.trim().ifBlank { null },
                            isActive = active
                        )
                    )
                },
                enabled = name.isNotBlank() &&
                    (price.toLongOrNull() ?: -1) >= 0 &&
                    !localPreviewNeedsUrl &&
                    !imageUrlUsesLocalScheme
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AdminEditorSwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

internal fun adminReviewTypeOptions(): List<Pair<String, String>> =
    listOf("" to "全部", "upload" to "上传", "delete" to "删除")

internal fun adminReviewStatusOptions(): List<Pair<String, String>> =
    listOf("" to "全部", "pending" to "待审核", "approved" to "已通过", "rejected" to "已拒绝")

internal fun adminOperationStatusOptions(): List<Pair<String, String>> =
    listOf("" to "全部", "success" to "成功", "failed" to "失败", "pending" to "处理中")

internal fun adminOperationActionOptions(actionTypes: List<String>): List<Pair<String, String>> {
    val normalized = actionTypes
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { it to adminOperationActionLabel(it) }
    return listOf("" to "全部") + normalized
}
