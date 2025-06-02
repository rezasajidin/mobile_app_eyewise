package com.kelompok4.eyewise.ui.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.kelompok4.eyewise.MainActivity
import com.kelompok4.eyewise.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmFragment : Fragment() {

    private lateinit var timeTextView: TextView
    private lateinit var scheduleSwitch: SwitchCompat
    private lateinit var alarmManager: AlarmManager
    private lateinit var chipGroupDays: ChipGroup
    private lateinit var textRepeat: TextView
    private lateinit var sharedPref: SharedPreferences

    private var alarmTime: Calendar = Calendar.getInstance()
    private val days = listOf(
        R.id.chipSun, R.id.chipMon, R.id.chipTue, R.id.chipWed,
        R.id.chipThu, R.id.chipFri, R.id.chipSat
    )

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setAlarm()
        } else {
            Toast.makeText(requireContext(), "Izin notifikasi diperlukan untuk pengingat alarm", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        timeTextView = view.findViewById(R.id.timeTextView)
        scheduleSwitch = view.findViewById(R.id.scheduleSwitch)
        chipGroupDays = view.findViewById(R.id.chipGroupDays)
        textRepeat = view.findViewById(R.id.textRepeat)
        sharedPref = requireContext().getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)

        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        loadSavedData()

        timeTextView.setOnClickListener { showTimePicker() }

        scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setAlarm()
            } else {
                cancelAllAlarms()
            }
            saveAlarmState(isChecked)
        }

        chipGroupDays.setOnCheckedStateChangeListener { group, checkedIds ->
            updateRepeatText(checkedIds)
            saveSelectedDays(checkedIds)
            if (scheduleSwitch.isChecked) {
                cancelAllAlarms()
                setAlarm()
            }
        }

        return view
    }

    private fun loadSavedData() {
        val savedHour = sharedPref.getInt("alarm_hour", 20)
        val savedMinute = sharedPref.getInt("alarm_minute", 0)
        updateTimeText(savedHour, savedMinute)

        val isAlarmOn = sharedPref.getBoolean("alarm_on", false)
        scheduleSwitch.isChecked = isAlarmOn

        val savedDays = sharedPref.getStringSet("selected_days", setOf()) ?: setOf()
        savedDays.forEach { dayId ->
            chipGroupDays.findViewById<Chip>(dayId.toInt())?.isChecked = true
        }
        updateRepeatText(chipGroupDays.checkedChipIds)
    }

    private fun saveSelectedDays(checkedIds: List<Int>) {
        val daysSet = checkedIds.map { it.toString() }.toSet()
        sharedPref.edit().putStringSet("selected_days", daysSet).apply()
    }

    private fun saveAlarmState(isOn: Boolean) {
        sharedPref.edit().putBoolean("alarm_on", isOn).apply()
    }

    private fun saveAlarmTime(hour: Int, minute: Int) {
        sharedPref.edit().apply {
            putInt("alarm_hour", hour)
            putInt("alarm_minute", minute)
            apply()
        }
    }

    private fun updateRepeatText(checkedIds: List<Int>) {
        val dayNames = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val selectedDays = checkedIds.map { id ->
            when (id) {
                R.id.chipSun -> dayNames[0]
                R.id.chipMon -> dayNames[1]
                R.id.chipTue -> dayNames[2]
                R.id.chipWed -> dayNames[3]
                R.id.chipThu -> dayNames[4]
                R.id.chipFri -> dayNames[5]
                R.id.chipSat -> dayNames[6]
                else -> ""
            }
        }.filter { it.isNotEmpty() }

        textRepeat.text = if (selectedDays.isEmpty()) {
            "Berulang: Tidak ada"
        } else {
            "Berulang: ${selectedDays.joinToString(", ")}"
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                updateTimeText(hour, minute)
                saveAlarmTime(hour, minute)
                if (scheduleSwitch.isChecked) {
                    cancelAllAlarms()
                    setAlarm()
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateTimeText(hour: Int, minute: Int) {
        alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timeTextView.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun setAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val selectedDays = chipGroupDays.checkedChipIds
        if (selectedDays.isEmpty()) {
            setSingleAlarm()
        } else {
            selectedDays.forEach { dayId ->
                val dayOfWeek = when (dayId) {
                    R.id.chipSun -> Calendar.SUNDAY
                    R.id.chipMon -> Calendar.MONDAY
                    R.id.chipTue -> Calendar.TUESDAY
                    R.id.chipWed -> Calendar.WEDNESDAY
                    R.id.chipThu -> Calendar.THURSDAY
                    R.id.chipFri -> Calendar.FRIDAY
                    R.id.chipSat -> Calendar.SATURDAY
                    else -> return@forEach
                }
                setWeeklyAlarm(dayOfWeek)
            }
        }

        createAlarmSetNotification()
        Toast.makeText(requireContext(), "Alarm di-set untuk ${timeTextView.text}", Toast.LENGTH_SHORT).show()
    }

    private fun setSingleAlarm() {
        val now = Calendar.getInstance()
        val alarmCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, alarmTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, alarmTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmIntent = getAlarmPendingIntent(0)
        setExactAlarm(alarmCalendar.timeInMillis, alarmIntent)
    }

    private fun setWeeklyAlarm(dayOfWeek: Int) {
        val now = Calendar.getInstance()
        val alarmCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, alarmTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, alarmTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, dayOfWeek)

            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val alarmIntent = getAlarmPendingIntent(dayOfWeek)
        setExactAlarm(alarmCalendar.timeInMillis, alarmIntent)

        Log.d("AlarmDebug", "Alarm di-set untuk: ${SimpleDateFormat("EEEE, HH:mm", Locale.getDefault()).format(alarmCalendar.time)}")
    }

    private fun setExactAlarm(triggerTime: Long, alarmIntent: PendingIntent) {
        // Gunakan setAlarmClock untuk visibilitas tinggi
        val showIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            Intent(requireContext(), MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmInfo, alarmIntent)
    }

    private fun getAlarmPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("alarm_time", alarmTime.timeInMillis)
            putExtra("request_code", requestCode)
            action = "ALARM_ACTION_$requestCode" // Membuat action unik
        }
        return PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAllAlarms() {
        listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
            Calendar.SATURDAY, 0
        ).forEach { day ->
            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                action = "ALARM_ACTION_$day"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                day,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun createAlarmSetNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alarm_set_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Set Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows when an alarm is set"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val selectedDays = chipGroupDays.checkedChipIds
        val repeatText = if (selectedDays.isEmpty()) {
            "Sekali saja"
        } else {
            "Berulang setiap: ${updateRepeatText(selectedDays)}"
        }

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setContentTitle("Alarm Berhasil Diset")
            .setContentText("Alarm pada ${timeTextView.text} - $repeatText")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(102, notification)
    }
}