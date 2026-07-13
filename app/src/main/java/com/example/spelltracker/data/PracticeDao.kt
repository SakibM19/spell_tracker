package com.example.spelltracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface PracticeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: PracticeWord)

    @Query("SELECT * FROM practice_words ORDER BY addedDate DESC")
    suspend fun getAll(): List<PracticeWord>

    @Delete
    suspend fun delete(word: PracticeWord)
}
