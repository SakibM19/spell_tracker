package com.example.spelltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistake_words")
data class MistakeWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val suggestion: String?, // the spell checker's closest correct word, if any
    val timestamp: Long
)
