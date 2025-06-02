package com.kelompok4.eyewise

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultActivity : AppCompatActivity() {
    private lateinit var resultLabel: TextView
    private lateinit var resultScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        resultLabel = findViewById(R.id.resultLabel)
        resultScore = findViewById(R.id.resultScore)

        try {
            val label = intent.getStringExtra("LABEL") ?: "Unknown"
            val score = intent.getFloatExtra("SCORE", 0f)

            resultLabel.text = "Detected: $label"
            resultScore.text = "Confidence: ${"%.2f".format(score * 100)}%"
        } catch (e: Exception) {
            resultLabel.text = "Error processing results"
            resultScore.text = ""
            Log.e("ResultActivity", "Error displaying results", e)
        }
    }
}
