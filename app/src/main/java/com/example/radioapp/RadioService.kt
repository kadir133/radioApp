package com.example.radioapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class RadioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var isForeground = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "radio_playback_channel"
        private const val CHANNEL_NAME = "Radyo Oynatma"
        const val ACTION_SHOW_NOTIFICATION = "SHOW_NOTIFICATION"
        const val ACTION_HIDE_NOTIFICATION = "HIDE_NOTIFICATION"
    }

    override fun onCreate() {
        super.onCreate()

        // ExoPlayer oluştur
        player = ExoPlayer.Builder(this).build()

        // MediaSession oluştur
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(MediaSessionCallback())
            .build()

        // Notification channel oluştur (Android 8.0+)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_NOTIFICATION -> {
                if (player?.isPlaying == true) {
                    showNotification()
                }
            }
            ACTION_HIDE_NOTIFICATION -> {
                hideNotification()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showNotification() {
        if (!isForeground && player?.isPlaying == true) {
            try {
                startForeground(NOTIFICATION_ID, createNotification())
                isForeground = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideNotification() {
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Radyo çalarken gösterilen bildirimler"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stationName = mediaSession?.player?.currentMediaItem?.mediaMetadata?.title ?: "Radyo"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Radyo Çalıyor")
            .setContentText(stationName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Uygulama kapatıldığında müziği durdur
        player?.stop()
        hideNotification()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        hideNotification()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return super.onPlaybackResumption(mediaSession, controller)
        }
    }
}