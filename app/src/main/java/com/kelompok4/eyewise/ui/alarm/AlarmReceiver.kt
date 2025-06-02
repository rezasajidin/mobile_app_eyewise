package com.kelompok4.eyewise.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EyeWise:AlarmLock").apply {
                acquire(60_000L) // Hold wake lock for 1 minute
            }
        }

        try {
            Log.d("AlarmDebug", "=== ALARM DITERIMA ===")
            Log.d("AlarmDebug", "Waktu: ${System.currentTimeMillis()}")
            Log.d("AlarmDebug", "Intent: ${intent?.action}")

            val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra("alarm_time", intent?.getLongExtra("alarm_time", 0))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } finally {
            wakeLock.release()
        }
    }
}