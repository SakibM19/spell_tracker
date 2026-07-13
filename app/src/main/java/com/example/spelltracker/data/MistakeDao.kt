package com.example.spelltracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query

@Dao
interface MistakeDao {

    @Insert
    suspend fun insert(mistake: MistakeWord)

    @Query("SELECT * FROM mistake_words WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC")
    suspend fun getSince(sinceMillis: Long): List<MistakeWord>

    @Query("SELECT COUNT(*) FROM mistake_words WHERE timestamp >= :sinceMillis")
    suspend fun countSince(sinceMillis: Long): Int

    @Delete
    suspend fun delete(mistake: MistakeWord)
}
