package com.novalpie.nativeapp.feature.reader.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.ui.ReaderUiOptions
import com.novalpie.nativeapp.ui.Text
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderPreferencesPanel(options: ReaderUiOptions, onApply: (ReaderUiOptions) -> Unit) {
    val dependencies = AppContainer.from(LocalContext.current)
    val model = dependencies.readerPreferences
    val revision by dependencies.environment.revisions.collectAsState()
    val authenticated = !dependencies.environment.token.isNullOrBlank()
    val state = model.state
    var confirmation by remember { mutableStateOf<Pair<String, ReaderPreferenceProfile>?>(null) }
    LaunchedEffect(revision) { if (authenticated) model.refresh() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("网页阅读配置", style = MaterialTheme.typography.titleMedium)
        Text("主动保存或加载，与网站共用配置。不会自动覆盖本机；音量键、无动画、规则屏蔽及本机字体/背景文件保留。", style = MaterialTheme.typography.bodySmall)
        if (!authenticated) Text("登录后可使用云端配置")
        OutlinedTextField(state.name, model::editName, label = { Text("新配置名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = authenticated && !state.busy)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = model::refresh, enabled = authenticated && !state.busy) { Text("刷新配置") }
            Button(onClick = { model.create(ReaderPreferenceCodec.encode(options).toString()) }, enabled = authenticated && !state.busy && state.name.isNotBlank()) { Text("保存当前配置") }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        state.profiles.forEach { profile ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(profile.name + if (profile.isDefault) " · 默认" else "", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { model.load(profile) }, enabled = !state.busy) { Text("加载") }
                        TextButton(onClick = { confirmation = "update" to profile }, enabled = !state.busy) { Text("用当前设置更新") }
                        TextButton(onClick = { confirmation = "default" to profile }, enabled = !state.busy && !profile.isDefault) { Text("设为默认") }
                        TextButton(onClick = { confirmation = "delete" to profile }, enabled = !state.busy) { Text("删除") }
                    }
                }
            }
        }
    }
    state.loaded?.let { loaded ->
        AlertDialog(onDismissRequest = model::consumeLoaded, title = { Text("应用网页阅读配置？") },
            text = { Text("将更新本机兼容的排版、字体、显示和翻页设置。网页独有资源不会导入，也不会开启轮盘或改变公共规则屏蔽。") },
            confirmButton = { TextButton(onClick = { onApply(ReaderPreferenceCodec.merge(JSONObject(loaded), options)); model.consumeLoaded() }) { Text("应用到本机") } },
            dismissButton = { TextButton(onClick = model::consumeLoaded) { Text("取消") } })
    }
    confirmation?.let { (action, profile) ->
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(if (action == "delete") "删除网页配置？" else "更新网页配置？") },
            text = { Text("此操作会修改账号中的“${profile.name}”。" + if (action == "delete") "不会清除本机阅读设置。" else "在网页也会生效。") },
            confirmButton = { TextButton(onClick = {
                confirmation = null
                when (action) { "delete" -> model.delete(profile); "default" -> model.makeDefault(profile)
                    else -> model.update(profile, ReaderPreferenceCodec.encode(options).toString()) }
            }) { Text("确认") } }, dismissButton = { TextButton(onClick = { confirmation = null }) { Text("取消") } })
    }
}
