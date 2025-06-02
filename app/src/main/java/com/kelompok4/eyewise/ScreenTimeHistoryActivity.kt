package com.kelompok4.eyewise

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kelompok4.eyewise.R
import com.kelompok4.eyewise.history.HistoryTimeAdapter
import com.kelompok4.eyewise.data.ScreenTimeHistory
import com.kelompok4.eyewise.ScreenTimeTracker

class ScreenTimeHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_time_history)

        val warningText = findViewById<TextView>(R.id.warningText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        val recyclerView = findViewById<RecyclerView>(R.id.historyRecyclerView)

        // Get data from ScreenTimeTracker
        val weeklyStats = ScreenTimeTracker.getWeeklyScreenTime(this)

        // Convert to our model
        val historyList = weeklyStats.map {
            ScreenTimeHistory(it.first, it.second.first, it.second.second)
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HistoryTimeAdapter(historyList)

        // Update warning text
        val hasExcessiveUsage = historyList.any { it.hours >= 7 }
        if (hasExcessiveUsage) {
            warningText.text = getString(R.string.excessive_usage_warning)
            descriptionText.text = getString(R.string.excessive_usage_description)
        } else {
            warningText.text = getString(R.string.normal_usage_message)
            descriptionText.text = getString(R.string.normal_usage_description)
        }
    }
}