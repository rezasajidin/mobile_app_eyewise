package com.kelompok4.eyewise.history

import com.kelompok4.eyewise.R


class HistoryRepository {

    fun getHistoryItems(): List<HistoryItem> {
        return listOf(
            HistoryItem(
                date = "Tue, 13/05",
                message = "You look well-rested",
                iconResId = R.drawable.ic_happy_face,
                backgroundColor = "#E3F2FD"
            ),
            HistoryItem(
                date = "Mon, 12/05",
                message = "Great job staying focused!",
                iconResId = R.drawable.ic_thumbs_up,
                backgroundColor = "#E8F5E8"
            ),
            HistoryItem(
                date = "Sun, 11/05",
                message = "You exceeded your screen time limit",
                iconResId = R.drawable.ic_warning,
                backgroundColor = "#FFE6E6"
            )
        )
    }

    // Method to add new history item
    fun addHistoryItem(item: HistoryItem) {
        // Implementation to save to database or shared preferences
    }

    // Method to clear history
    fun clearHistory() {

    }
}
