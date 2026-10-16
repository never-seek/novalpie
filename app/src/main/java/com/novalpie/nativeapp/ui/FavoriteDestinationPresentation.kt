package com.novalpie.nativeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.feature.library.FavoriteDestinationState
import com.novalpie.nativeapp.model.LoadResult

@Composable
internal fun FavoriteDestinationDialog(
    state: FavoriteDestinationState,
    onDismiss: () -> Unit,
    onChoose: (Long) -> Unit,
    onRetry: () -> Unit,
    onStartCreate: () -> Unit,
    onCancelCreate: () -> Unit,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
) {
    if (!state.visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.creating) "新建收藏夹" else "收藏到") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.creating) {
                    OutlinedTextField(value = state.name, onValueChange = onNameChange, singleLine = true,
                        enabled = !state.busy, label = { Text("收藏夹名称") }, modifier = Modifier.fillMaxWidth())
                } else {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        DestinationRow("默认收藏夹", !state.busy) { onChoose(0) }
                        when (val groups = state.groups) {
                            is LoadResult.Success -> groups.value.filter { (it.id ?: 0) > 0 }.forEach { group ->
                                DestinationRow(group.name, !state.busy) { onChoose(group.id!!) }
                            }
                            is LoadResult.Error -> TextButton(onClick = onRetry, enabled = !state.busy) { Text("重新加载收藏夹") }
                            else -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = onStartCreate, enabled = !state.busy) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("新建收藏夹")
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            if (state.creating) Button(onClick = onCreate, enabled = state.name.isNotBlank() && !state.busy) { Text("创建并收藏") }
        },
        dismissButton = {
            TextButton(onClick = if (state.creating) onCancelCreate else onDismiss, enabled = !state.busy) {
                Text(if (state.creating) "返回" else "取消")
            }
        },
    )
}

@Composable
private fun DestinationRow(name: String, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun FavoriteGroupCreateDialog(busy: Boolean, message: String?, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(busy, message) {
        if (submitted && !busy && message == "已创建分组") onDismiss()
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("新建收藏夹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; submitted = false }, label = { Text("收藏夹名称") },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                if (submitted && !busy && !message.isNullOrBlank() && message != "已创建分组") {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = name.isNotBlank() && !busy, onClick = { submitted = true; onCreate(name.trim()) }) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}
