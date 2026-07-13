package com.example.spelltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_words")
data class PracticeWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val addedDate: Long
)
