package com.novalpie.nativeapp.feature.reader.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.ReaderTtsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** System-managed lifetime for native speech; the reading page only supplies commands/state. */
class ReaderPlaybackService : Service() {
    private val serviceScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private lateinit var container:AppContainer
    private lateinit var mediaSession:MediaSession
    private lateinit var audio:AudioManager
    private var focus:AudioFocusRequest?=null
    private var hasFocus=false
    private val focusListener=AudioManager.OnAudioFocusChangeListener { change ->
        if(change<0) {
            hasFocus=false
            container.playback.interrupt("其他音频占用，听书已暂停")
        }
    }
    private val noisyReceiver=object:BroadcastReceiver() {
        override fun onReceive(context:Context?,intent:Intent?) {
            if(intent?.action==AudioManager.ACTION_AUDIO_BECOMING_NOISY)container.playback.interrupt("耳机已断开，听书已暂停")
        }
    }

    override fun onCreate() {
        super.onCreate()
        container=AppContainer.from(this)
        audio=getSystemService(AudioManager::class.java)
        if(Build.VERSION.SDK_INT>=26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL,"小说听书",NotificationManager.IMPORTANCE_LOW).apply {setSound(null,null)},
            )
        }
        mediaSession=MediaSession(this,"NovalPiePlayback").apply {
            setCallback(object:MediaSession.Callback() {
                override fun onPlay() {resumePlayback()}
                override fun onPause() {container.playback.pause()}
                override fun onStop() {container.playback.stop();stopSelf()}
            })
            isActive=true
        }
        ContextCompat.registerReceiver(this,noisyReceiver,IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),ContextCompat.RECEIVER_NOT_EXPORTED)
        val initial=notification(container.playback.state.value)
        if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,initial,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(NOTIFICATION_ID,initial)
        serviceScope.launch {
            container.playback.state.collectLatest { state ->
                updateMediaSession(state)
                if(state.status !in setOf(SpeechStatus.Speaking,SpeechStatus.Loading))abandonFocus()
                if(state.status==SpeechStatus.Stopped && container.pendingSpeech==null) {
                    if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
                    stopSelf()
                } else getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,notification(state))
            }
        }
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            ACTION_START -> {
                val pending=container.pendingSpeech
                container.pendingSpeech=null
                if(pending==null)stopSelf()
                else if(requestFocus())container.playback.start(pending.chapter,pending.settings,pending.startIndex)
                else container.playback.preparePaused(pending.chapter,pending.settings,pending.startIndex,"无法取得音频焦点，请稍后继续")
            }
            ACTION_PAUSE -> container.playback.pause()
            ACTION_RESUME -> resumePlayback()
            ACTION_STOP -> {container.playback.stop();stopSelf()}
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun resumePlayback() {
        if(!requestFocus())return
        if(container.playback.state.value.status==SpeechStatus.Error)container.playback.retry()
        else container.playback.resume()
    }

    @Suppress("DEPRECATION")
    private fun requestFocus():Boolean {
        if(hasFocus)return true
        val result=if(Build.VERSION.SDK_INT>=26) {
            val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(focusListener).build()
            focus=request
            audio.requestAudioFocus(request)
        } else audio.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)
        hasFocus=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if(Build.VERSION.SDK_INT>=26)focus?.let(audio::abandonAudioFocusRequest) else audio.abandonAudioFocus(focusListener)
        focus=null;hasFocus=false
    }

    private fun updateMediaSession(state:SpeechPlaybackState) {
        val status=when(state.status) {
            SpeechStatus.Speaking->PlaybackState.STATE_PLAYING
            SpeechStatus.Paused->PlaybackState.STATE_PAUSED
            SpeechStatus.Loading->PlaybackState.STATE_BUFFERING
            SpeechStatus.Error->PlaybackState.STATE_ERROR
            SpeechStatus.Stopped->PlaybackState.STATE_STOPPED
        }
        mediaSession.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
            .setState(status,PlaybackState.PLAYBACK_POSITION_UNKNOWN,1f).build())
        mediaSession.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,state.chapter?.chapterTitle ?: "小说听书")
            .putString(MediaMetadata.METADATA_KEY_ALBUM,state.chapter?.bookTitle.orEmpty()).build())
    }

    @Suppress("DEPRECATION")
    private fun notification(state:SpeechPlaybackState):Notification {
        val chapter=state.chapter
        val open=Intent(this,MainActivity::class.java).apply {
            action=Intent.ACTION_VIEW
            chapter?.let {data=Uri.parse("novalpie://app/book/${it.bookId}/${it.chapterId}")}
            flags=Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending=PendingIntent.getActivity(this,20,open,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playing=state.status in setOf(SpeechStatus.Speaking,SpeechStatus.Loading)
        val builder=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,CHANNEL) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(chapter?.bookTitle ?: "小说听书")
            .setContentText(state.message ?: chapter?.chapterTitle ?: "正在准备听书")
            .setContentIntent(pending).setOnlyAlertOnce(true).setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(if(playing)android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,if(playing)"暂停" else "继续",command(if(playing)ACTION_PAUSE else ACTION_RESUME))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"停止",command(ACTION_STOP))
            .setStyle(Notification.MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0,1)).build()
    }

    private fun command(action:String)=PendingIntent.getService(this,action.hashCode(),Intent(this,ReaderPlaybackService::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy() {
        serviceScope.cancel()
        unregisterReceiver(noisyReceiver)
        abandonFocus()
        mediaSession.release()
        container.playback.stop()
        container.speechEngine.close()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL="novalpie-reader-playback"
        private const val NOTIFICATION_ID=7101
        private const val ACTION_START="novalpie.speech.START"
        private const val ACTION_PAUSE="novalpie.speech.PAUSE"
        private const val ACTION_RESUME="novalpie.speech.RESUME"
        private const val ACTION_STOP="novalpie.speech.STOP"

        internal fun start(context:Context,chapter:SpeechChapter,settings:ReaderTtsSettings,startIndex:Int=0) {
            val container=AppContainer.from(context)
            container.refreshEnvironmentFromStores()
            container.pendingSpeech=AppContainer.PendingSpeech(chapter.copy(segments=chapter.segments.toList()),settings,startIndex)
            try {ContextCompat.startForegroundService(context,Intent(context,ReaderPlaybackService::class.java).setAction(ACTION_START))}
            catch(failure:Exception){container.pendingSpeech=null;throw failure}
        }
        internal fun resume(context:Context) {
            ContextCompat.startForegroundService(context,Intent(context,ReaderPlaybackService::class.java).setAction(ACTION_RESUME))
        }
    }
}
