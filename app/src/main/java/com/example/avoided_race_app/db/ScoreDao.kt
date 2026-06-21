package com.example.avoided_race_app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    fun insert(entry: ScoreEntry)

    @Query("SELECT * FROM scores ORDER BY score DESC LIMIT 10")
    fun getTop10(): List<ScoreEntry>
}
