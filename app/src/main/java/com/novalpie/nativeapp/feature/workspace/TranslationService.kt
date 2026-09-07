package com.novalpie.nativeapp.feature.workspace

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Visible foreground lifetime for user-requested translation; no Activity or draft is retained. */
open class TranslationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var container: AppContainer
    internal open fun coordinator(): TranslationCoordinator = container.translations
    internal open fun refreshEnvironment() = container.refreshEnvironmentFromStores()
    internal open fun takePendingTask(): TranslationTask? = container.pendingTranslation.also { container.pendingTranslation = null }
    private var startError: String? = null
    private var started = false
    private val commands = Mutex()
    private var pendingCommands = 0
    override fun onCreate() {
        super.onCreate(); container = AppContainer.from(this)
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "自助翻译", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        val notification = notification(TranslationQueueState())
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(ID, notification)
        scope.launch { coordinator().state.collect { state ->
            if (!started) return@collect
            getSystemService(NotificationManager::class.java).notify(ID, notification(state, startError))
            if (pendingCommands == 0 && state.activeId == null && state.tasks.none { it.phase == TranslationPhase.Queued }) {
                if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH) else @Suppress("DEPRECATION") stopForeground(false)
                stopSelf()
            }
        } }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pendingCommands++
        scope.launch {
            try { commands.withLock {
                startError = null
                refreshEnvironment()
                coordinator().restore()
                when (intent?.action) {
                    START -> {
                        val task = takePendingTask()
                        if (task != null) coordinator().enqueue(task).join()
                    }
                    RESUME -> intent.getStringExtra("task")?.let { coordinator().resume(it).join() }
                    PAUSE -> intent.getStringExtra("task")?.let { coordinator().pause(it).join() }
                    STOP -> intent.getStringExtra("task")?.let { coordinator().cancel(it).join() }
                    RETRY_CONFIRMED -> intent.getStringExtra("task")?.let { coordinator().confirmRetryUncertain(it).join() }
                }
            } } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                startError = "任务未能启动，请回到工作区检查账号、配置及可用空间"
            } finally {
                pendingCommands--
                started = true
                val state = coordinator().state.value
                getSystemService(NotificationManager::class.java).notify(ID, notification(state, startError))
                if (pendingCommands == 0 && state.activeId == null && state.tasks.none { it.phase == TranslationPhase.Queued }) {
                    if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH) else @Suppress("DEPRECATION") stopForeground(false)
                    stopSelfResult(startId)
                }
            }
        }
        return START_NOT_STICKY
    }
    @Suppress("DEPRECATION") private fun notification(state: TranslationQueueState, error: String? = null): Notification {
        val task = state.tasks.firstOrNull { it.id == state.activeId } ?: state.tasks.firstOrNull()
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        val open = Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("novalpie://app/workspace"))
        builder.setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle(task?.title ?: "自助翻译")
            .setContentText(error ?: state.error ?: task?.let { translationStatus(it, state.paused) } ?: "暂无运行中的翻译任务")
            .setOnlyAlertOnce(true).setOngoing(state.activeId != null).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(PendingIntent.getActivity(this, ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        if (task != null && state.activeId != null) {
            builder.addAction(android.R.drawable.ic_media_pause, if (state.paused) "继续" else "暂停", command(if (state.paused) RESUME else PAUSE, task.id))
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", command(STOP, task.id))
        }
        return builder.build()
    }
    private fun command(action: String, id: String) = PendingIntent.getService(this, action.hashCode(), Intent(this, javaClass).setAction(action).putExtra("task", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        // No orphan model requests when Android ends the foreground service.
        coordinator().state.value.activeId?.let { coordinator().cancel(it) }
        scope.cancel(); super.onDestroy()
    }
    override fun onTimeout(startId: Int, fgsType: Int) { coordinator().state.value.activeId?.let(coordinator()::cancel); stopSelf() }
    companion object {
        private const val CHANNEL = "novalpie-translations"
        private const val ID = 7103
        private const val START = "novalpie.translation.START"
        private const val RESUME = "novalpie.translation.RESUME"
        private const val PAUSE = "novalpie.translation.PAUSE"
        private const val STOP = "novalpie.translation.STOP"
        private const val RETRY_CONFIRMED = "novalpie.translation.CONFIRM_RETRY"
        internal fun start(context: Context, task: TranslationTask) {
            val container = AppContainer.from(context); container.refreshEnvironmentFromStores()
            require(container.environment.token?.let { com.novalpie.nativeapp.data.decodeAuthTokenProfile(it)?.id } == task.accountId) { "请使用任务所属账号登录" }
            check(container.pendingTranslation == null) { "上一任务正在启动，请稍后再试" }
            container.pendingTranslation = task
            try { ContextCompat.startForegroundService(context, Intent(context, TranslationService::class.java).setAction(START)) }
            catch (failure: Exception) { container.pendingTranslation = null; throw failure }
        }
        internal fun resume(context: Context, taskId: String, confirmed: Boolean = false) {
            ContextCompat.startForegroundService(context, Intent(context, TranslationService::class.java).setAction(if (confirmed) RETRY_CONFIRMED else RESUME).putExtra("task", taskId))
        }
    }
}

internal fun translationStatus(task: TranslationTask, paused: Boolean = false): String = if (paused) "已暂停派发，在途请求结束后保存检查点" else when (task.phase) {
    TranslationPhase.Queued -> "排队中"
    TranslationPhase.Preparing -> "准备章节"
    TranslationPhase.Translating -> "${task.completed.size}/${task.total}章 · 本章${task.finishedChunks}/${task.totalChunks}块"
    TranslationPhase.Submitting -> "正在提交完整章节"
    TranslationPhase.SubmissionUncertain -> "提交结果待确认，没有重复发送"
    TranslationPhase.Paused -> "已暂停，可继续"
    TranslationPhase.Failed -> task.message ?: "任务失败，可重试"
    TranslationPhase.Completed -> "全部章节提交完成"
    TranslationPhase.Cancelled -> "已取消"
}
