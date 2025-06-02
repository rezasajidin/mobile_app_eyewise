package com.kelompok4.eyewise.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kelompok4.eyewise.R

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvHistoryDate)
        val messageText: TextView = view.findViewById(R.id.tvHistoryMessage)
        val iconImage: ImageView = view.findViewById(R.id.ivHistoryIcon)
        val containerLayout: View = view.findViewById(R.id.historyContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        holder.dateText.text = item.date
        holder.messageText.text = item.message
        holder.iconImage.setImageResource(item.iconResId)

        // Set custom background color with fallback
        val backgroundColor = try {
            Color.parseColor(item.backgroundColor)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#E3F2FD") // default fallback
        }

        holder.containerLayout.setBackgroundColor(backgroundColor)
    }

    override fun getItemCount(): Int = historyList.size
}
