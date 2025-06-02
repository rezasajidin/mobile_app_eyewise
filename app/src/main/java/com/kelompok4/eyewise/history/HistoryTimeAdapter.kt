package com.kelompok4.eyewise.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kelompok4.eyewise.R
import com.kelompok4.eyewise.data.ScreenTimeHistory

class HistoryTimeAdapter(private val historyList: List<ScreenTimeHistory>) :
    RecyclerView.Adapter<HistoryTimeAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.historyDate)
        val durationText: TextView = itemView.findViewById(R.id.historyDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_screen_time, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]

        holder.dateText.text = history.date
        holder.durationText.text = "${history.hours} hours ${history.minutes} minutes"

        // Anda bisa mengubah background berdasarkan durasi
        if (history.hours >= 7) {
            holder.itemView.setBackgroundResource(R.drawable.card_red)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.card_blue)
        }
    }

    override fun getItemCount() = historyList.size
}