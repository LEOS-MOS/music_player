package com.example.muzzio.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.muzzio.R
import com.example.muzzio.player.CoverBitmapLoader
import com.example.muzzio.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Уведомление с кнопками, обложкой и управлением на экране блокировки
 * теперь создаёт сама библиотека Media3 по данным плеера.
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var controller: PlayerController

    private var mediaSession: MediaSession? = null

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // нажатие на уведомление открывает приложение сразу на экране плеера
        val openPlayerIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPlayerPending = PendingIntent.getActivity(
            this,
            0,
            openPlayerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, controller.getPlayer())
            .setSessionActivity(openPlayerPending)
            .setBitmapLoader(CoverBitmapLoader(applicationContext))
            .build()

        // Без этого Media3 не знает о сессии и не показывает уведомление:
        // автоматически сессия добавляется только когда к сервису подключается MediaController,
        // а у нас плеер используется напрямую.
        addSession(mediaSession!!)

        val notificationProvider = MuzzioNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ic_music_note)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Пользователь убрал приложение из списка задач
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = controller.getPlayer()

        // просто останавливаем музыку, но НЕ release()
        player.pause()
        player.stop()
        player.clearMediaItems()

        stopSelf()

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // ExoPlayer НЕ трогаем, он singleton
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
