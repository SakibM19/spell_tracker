package com.example.spelltracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spelltracker.adapters.PracticeAdapter
import com.example.spelltracker.data.AppDatabase
import com.example.spelltracker.data.PracticeWord
import kotlinx.coroutines.launch

class PracticeListActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: PracticeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice_list)

        db = AppDatabase.getDatabase(this)

        val newWordEditText = findViewById<EditText>(R.id.newWordEditText)
        val addWordButton = findViewById<Button>(R.id.addWordButton)
        val recyclerView = findViewById<RecyclerView>(R.id.practiceRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val words = db.practiceDao().getAll()
            adapter = PracticeAdapter(
                items = words.toMutableList(),
                onDelete = { word -> deleteWord(word) }
            )
            recyclerView.adapter = adapter
        }

        addWordButton.setOnClickListener {
            val text = newWordEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                addWord(text)
                newWordEditText.text.clear()
            }
        }
    }

    private fun addWord(text: String) {
        lifecycleScope.launch {
            val word = PracticeWord(word = text, addedDate = System.currentTimeMillis())
            db.practiceDao().insert(word)
            adapter.addItem(word)
        }
    }

    private fun deleteWord(word: PracticeWord) {
        lifecycleScope.launch {
            db.practiceDao().delete(word)
            adapter.removeItem(word)
        }
    }
}
