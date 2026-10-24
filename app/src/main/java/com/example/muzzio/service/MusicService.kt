package com.example.muzzio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.muzzio.R
import com.example.muzzio.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var controller: PlayerController

    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession.Builder(this, controller.getPlayer()).build()
        createNotificationChannel()

        controller.getPlayer().addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                updateNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_PLAY -> controller.toggle()
            ACTION_PAUSE -> controller.toggle()
            ACTION_NEXT -> controller.next()
            ACTION_PREV -> controller.previous()
        }

        updateNotification()
        return START_STICKY
    }

    // Пользователь убрал приложение из списка задач
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = controller.getPlayer()

        // просто останавливаем музыку, но НЕ release()
        player.pause()
        player.stop()
        player.clearMediaItems()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        super.onTaskRemoved(rootIntent)
    }

    private fun updateNotification() {
        val notification = buildNotification(controller.getPlayer().isPlaying)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val title = controller.getCurrentTitle()

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePending = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPending = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPending = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(openAppPending)
            .addAction(R.drawable.ic_prev, "Prev", prevPending)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPausePending
            )
            .addAction(R.drawable.ic_next, "Next", nextPending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        // ExoPlayer НЕ трогаем, он singleton
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_PLAY = "ACTION_PLAY"
        private const val ACTION_PAUSE = "ACTION_PAUSE"
        private const val ACTION_NEXT = "ACTION_NEXT"
        private const val ACTION_PREV = "ACTION_PREV"
    }
}
