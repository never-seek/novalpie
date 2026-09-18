package com.novalpie.nativeapp.feature.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.*

/** Visible system task while streaming or packaging. No reference to any screen/ViewModel. */
class NativeDownloadService:Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private lateinit var container:AppContainer
    private var hasStarted=false
    override fun onCreate() {
        super.onCreate()
        container=AppContainer.from(this)
        if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"原生小说下载",NotificationManager.IMPORTANCE_LOW).apply{setSound(null,null)})
        val initial=notification(DownloadUiState(task=container.pendingDownload,busy=true))
        if(Build.VERSION.SDK_INT>=29)startForeground(ID,initial,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(ID,initial)
        scope.launch {
            container.downloads.state.collect {state->
                if(!hasStarted)return@collect
                getSystemService(NotificationManager::class.java).notify(ID,notification(state))
                if(!state.busy) {
                    if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_DETACH) else @Suppress("DEPRECATION")stopForeground(false)
                    stopSelf()
                }
            }
        }
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            START->{
                val task=container.pendingDownload;container.pendingDownload=null
                if(task==null){stopSelf();return START_NOT_STICKY}
                hasStarted=true
                container.downloads.start(task)
            }
            PAUSE->container.downloads.pause()
            RESUME->container.downloads.resume()
            CANCEL->container.downloads.cancel()
            else->stopSelf()
        }
        return START_NOT_STICKY
    }
    @Suppress("DEPRECATION")
    private fun notification(state:DownloadUiState):Notification {
        val phase=state.task?.phase
        val paused=phase==DownloadPhase.Paused
        val builder=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,CHANNEL)else Notification.Builder(this)
        builder.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(state.task?.title ?: "小说下载")
            .setContentText(downloadStatusText(state)).setOnlyAlertOnce(true).setOngoing(state.busy)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(PendingIntent.getActivity(this,ID,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        if(state.busy)builder.addAction(if(paused)android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
            if(paused)"继续"else"暂停",command(if(paused)RESUME else PAUSE))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"取消",command(CANCEL))
        return builder.build()
    }
    private fun command(action:String)=PendingIntent.getService(this,action.hashCode(),Intent(this,NativeDownloadService::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy(){scope.cancel();super.onDestroy()}
    override fun onTimeout(startId:Int){container.downloads.interrupt();stopSelf()}
    override fun onTimeout(startId:Int,fgsType:Int){container.downloads.interrupt();stopSelf()}
    companion object {
        private const val CHANNEL="novalpie-native-downloads"
        private const val ID=7102
        private const val START="novalpie.download.START"
        private const val PAUSE="novalpie.download.PAUSE"
        private const val RESUME="novalpie.download.RESUME"
        private const val CANCEL="novalpie.download.CANCEL"
        internal fun start(context:Context,task:DownloadTask) {
            val container=AppContainer.from(context)
            container.refreshEnvironmentFromStores()
            require(container.environment.token?.let{com.novalpie.nativeapp.data.decodeAuthTokenProfile(it)}?.id==task.accountId){"请使用创建任务的账号继续下载"}
            if(container.downloads.state.value.busy || container.pendingDownload!=null)return
            container.pendingDownload=task
            try{ContextCompat.startForegroundService(context,Intent(context,NativeDownloadService::class.java).setAction(START))}
            catch(failure:Exception){container.pendingDownload=null;throw failure}
        }
    }
}

internal fun downloadStatusText(state:DownloadUiState):String {
    state.message?.let{return it}
    val task=state.task ?: return "等待下载"
    return when(task.phase) {
        DownloadPhase.Queued->"正在准备下载…"
        DownloadPhase.Authorizing->"正在申请下载授权…"
        DownloadPhase.AuthorizationUncertain->"上次授权结果未确认，请核对后再继续"
        DownloadPhase.Transferring->"正在下载正文…"
        DownloadPhase.Packaging->"已整理${task.completedChapters}章 · ${task.completedAssets}张插图"
        DownloadPhase.Saving->"正在保存到下载目录…"
        DownloadPhase.Paused->"下载已暂停"
        DownloadPhase.Completed->(if(task.destinationUri?.contains(".downloads/local_downloads/")==true)
            "系统下载目录不可用，已保存到 App 本机下载区，可打开或分享；卸载会移除本机文件"
            else "已保存到下载目录") + (if (task.sourceOnlyImages > 0) "；保留${task.sourceOnlyImages}处仅导出文件提供的插图" else "") +
            (if (task.failedAssets > 0) "；${task.failedAssets}张插图获取失败已占位" else "")
        DownloadPhase.NeedsRetry->"下载被中断，可从检查点重试"
        DownloadPhase.Failed->task.failure ?: "下载失败，可重试"
        DownloadPhase.Cancelled->"已取消，已完成资源可供重试"
    }
}
