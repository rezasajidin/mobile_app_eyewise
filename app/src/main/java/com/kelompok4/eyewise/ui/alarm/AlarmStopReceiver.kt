package com.kelompok4.eyewise.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(serviceIntent)
    }
}