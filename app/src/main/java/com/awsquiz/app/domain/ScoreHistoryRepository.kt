package com.awsquiz.app.domain

interface ScoreHistoryRepository {
    suspend fun saveScore(scoreRecord: ScoreHistoryRecord): Result<Unit>
    suspend fun getAllScores(): Result<List<ScoreHistoryRecord>>
    suspend fun getRecentScores(count: Int): Result<List<ScoreHistoryRecord>>
}
