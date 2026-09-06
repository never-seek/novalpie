package com.novalpie.nativeapp.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.Text

@Composable
internal fun BlockedUsersPanel(accountId:Long,onOpenUser:(Long)->Unit) {
    val context=LocalContext.current
    val container=remember(context){AppContainer.from(context).also{it.refreshEnvironmentFromStores()}}
    val revision by container.environment.revisions.collectAsState()
    val model=remember(accountId,revision){BlockingViewModel(container.blockingRepository)}
    DisposableEffect(model){model.load();onDispose{model.dispose()}}
    val state=model.state
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("屏蔽列表",style=MaterialTheme.typography.titleMedium)
        state.message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        when(val result=state.users) {
            LoadResult.Idle,LoadResult.Loading->CircularProgressIndicator()
            is LoadResult.Error->{Text(result.message);TextButton(onClick={model.load()}){Text("重试")}}
            is LoadResult.Success->{
                Text("已屏蔽 ${result.value.total} 位用户",style=MaterialTheme.typography.bodySmall)
                if(result.value.users.isEmpty())Text("暂无屏蔽用户")
                result.value.users.forEach {user->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={onOpenUser(user.id)},modifier=Modifier.weight(1f)){Text(user.name)}
                    TextButton(enabled=state.busyId==null,onClick={model.unblock(user.id)}){Text("解除屏蔽")}
                }}
                Row {
                    TextButton(onClick={model.load(state.page-1)},enabled=state.page>1&&state.busyId==null){Text("上一页")}
                    Text("${state.page} / ${result.value.pages.coerceAtLeast(1)}",Modifier.padding(12.dp))
                    TextButton(onClick={model.load(state.page+1)},enabled=state.page<result.value.pages&&state.busyId==null){Text("下一页")}
                }
            }
        }
    }
}

@Composable
internal fun UserBlockButton(userId:Long,selfId:Long?,book:Boolean=false) {
    if(userId<=0||selfId==null||(!book&&userId==selfId))return
    val context=LocalContext.current
    val container=remember(context){AppContainer.from(context).also{it.refreshEnvironmentFromStores()}}
    val revision by container.environment.revisions.collectAsState()
    val model=remember(userId,selfId,book,revision){BlockTargetViewModel(container.blockingRepository,userId,book)}
    DisposableEffect(model){model.refresh();onDispose{model.dispose()}}
    val state=model.state
    var confirm by remember(userId,selfId,book,revision){mutableStateOf(false)}
    Column {
        TextButton(enabled=state.blocked!=null&&!state.busy,onClick={confirm=true}) {
            Text(if(state.blocked==true)"解除屏蔽${if(book)"书籍"else"用户"}"else"屏蔽${if(book)"书籍"else"用户"}")
        }
        state.message?.let{Text(it,style=MaterialTheme.typography.labelSmall);TextButton(onClick=model::refresh){Text("刷新状态")}}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text(if(state.blocked==true)"解除屏蔽"else if(book)"屏蔽此书籍？"else"屏蔽此用户？")},
        text={Text(if(book)"按网站屏蔽规则生效，可在这本书的菜单中解除。"else"按网站屏蔽规则生效，可在我的屏蔽列表恢复。")},
        confirmButton={TextButton(onClick={confirm=false;model.toggle()}){Text("确定")}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}})
}
