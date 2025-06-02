package com.kelompok4.eyewise.ui.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.kelompok4.eyewise.MainActivity
import com.kelompok4.eyewise.R
import java.text.SimpleDateFormat
import java.util.*

class AlarmSoundService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private val channelId = "alarm_channel"
    private val notificationId = 101

    companion object {
        const val ACTION_STOP = "com.kelompok4.eyewise.STOP_ALARM"
        const val NOTIFICATION_CHANNEL_NAME = "Alarm Notifications"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EyeWise:ServiceLock").apply {
                acquire(10_000L)
            }
        }

        return try {
            when (intent?.action) {
                ACTION_STOP -> stopAlarm()
                else -> startAlarm(intent?.getLongExtra("alarm_time", System.currentTimeMillis()) ?: System.currentTimeMillis())
            }
            START_STICKY
        } finally {
            wakeLock.release()
        }
    }

    private fun startAlarm(alarmTime: Long) {
        val stopIntent = Intent(this, AlarmSoundService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alarmTime))

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Waktunya Tidur!")
            .setContentText("Alarm berbunyi pada $timeText")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_stop,
                "MATIKAN ALARM",
                stopPendingIntent
            )
            .build()

        startForeground(notificationId, notification)
        mediaPlayer.start()
    }

    private fun stopAlarm() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm notifications"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}