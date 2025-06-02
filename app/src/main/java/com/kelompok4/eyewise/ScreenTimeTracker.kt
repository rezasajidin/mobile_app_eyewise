package com.kelompok4.eyewise

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.*

object ScreenTimeTracker {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayScreenTime(context: Context): Pair<Int, Int> {
        if (!hasUsageStatsPermission(context)) {
            return Pair(0, 0)
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        var totalTime = 0L
        stats?.forEach { usageStats ->
            if (usageStats.packageName != context.packageName) {
                totalTime += usageStats.totalTimeInForeground
            }
        }

        val hours = (totalTime / (1000 * 60 * 60)).toInt()
        val minutes = ((totalTime / (1000 * 60)) % 60).toInt()

        return Pair(hours, minutes)
    }

    fun getWeeklyScreenTime(context: Context): List<Pair<String, Pair<Int, Int>>> {
        if (!hasUsageStatsPermission(context)) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val result = mutableListOf<Pair<String, Pair<Int, Int>>>()
        val dateFormat = android.text.format.DateFormat.getDateFormat(context)

        stats?.groupBy { it.packageName }?.forEach { (_, usageStatsList) ->
            usageStatsList.forEach { usageStats ->
                val date = Date(usageStats.lastTimeUsed)
                val dateStr = dateFormat.format(date)
                val totalTime = usageStats.totalTimeInForeground
                val hours = (totalTime / (1000 * 60 * 60)).toInt()
                val minutes = ((totalTime / (1000 * 60)) % 60).toInt()
                result.add(Pair(dateStr, Pair(hours, minutes)))
            }
        }

        return result.sortedByDescending { it.first }
    }

    fun requestUsageStatsPermission(context: Context): Boolean {
        if (!hasUsageStatsPermission(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
            return false
        }
        return true
    }
}