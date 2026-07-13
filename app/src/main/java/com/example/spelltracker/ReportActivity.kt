package com.example.spelltracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spelltracker.adapters.MistakeAdapter
import com.example.spelltracker.data.AppDatabase
import com.example.spelltracker.data.MistakeWord
import com.example.spelltracker.data.PracticeWord
import com.example.spelltracker.data.Prefs
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReportActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var prefs: Prefs
    private lateinit var adapter: MistakeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        db = AppDatabase.getDatabase(this)
        prefs = Prefs(this)

        val summaryText = findViewById<TextView>(R.id.reportSummaryText)
        val recyclerView = findViewById<RecyclerView>(R.id.mistakeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val hours = prefs.reportIntervalHours
        val sinceMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())

        lifecycleScope.launch {
            val mistakes = db.mistakeDao().getSince(sinceMillis)

            summaryText.text = if (mistakes.isEmpty()) {
                "No spelling mistakes in the last $hours hours."
            } else {
                "In the last $hours hours, you made ${mistakes.size} spelling mistake(s):"
            }

            adapter = MistakeAdapter(
                items = mistakes.toMutableList(),
                onAddToPractice = { mistake -> addToPractice(mistake) },
                onDelete = { mistake -> deleteMistake(mistake) }
            )
            recyclerView.adapter = adapter
        }
    }

    private fun addToPractice(mistake: MistakeWord) {
        lifecycleScope.launch {
            db.practiceDao().insert(PracticeWord(word = mistake.word, addedDate = System.currentTimeMillis()))
        }
    }

    private fun deleteMistake(mistake: MistakeWord) {
        lifecycleScope.launch {
            db.mistakeDao().delete(mistake)
            adapter.removeItem(mistake)
        }
    }
}
