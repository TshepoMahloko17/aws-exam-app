package com.awsquiz.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreHistoryDao {
    @Insert
    suspend fun insertScore(score: ScoreHistoryEntity)

    @Query("SELECT * FROM score_history ORDER BY completionDate DESC")
    suspend fun getAllScores(): List<ScoreHistoryEntity>

    @Query("SELECT * FROM score_history ORDER BY completionDate DESC LIMIT :count")
    suspend fun getRecentScores(count: Int): List<ScoreHistoryEntity>
}
