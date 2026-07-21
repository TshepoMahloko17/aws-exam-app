package com.awsquiz.app.data

import com.awsquiz.app.domain.ScoreHistoryRecord
import com.awsquiz.app.domain.ScoreHistoryRepository
import javax.inject.Inject

class RoomScoreHistoryRepository @Inject constructor(
    private val scoreHistoryDao: ScoreHistoryDao
) : ScoreHistoryRepository {

    override suspend fun saveScore(scoreRecord: ScoreHistoryRecord): Result<Unit> {
        return try {
            val entity = ScoreHistoryEntity(
                id = scoreRecord.id,
                completionDate = scoreRecord.completionDate,
                percentage = scoreRecord.percentage,
                passed = scoreRecord.passed
            )
            scoreHistoryDao.insertScore(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllScores(): Result<List<ScoreHistoryRecord>> {
        return try {
            val entities = scoreHistoryDao.getAllScores()
            val records = entities.map { entity ->
                ScoreHistoryRecord(
                    id = entity.id,
                    completionDate = entity.completionDate,
                    percentage = entity.percentage,
                    passed = entity.passed
                )
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentScores(count: Int): Result<List<ScoreHistoryRecord>> {
        return try {
            val entities = scoreHistoryDao.getRecentScores(count)
            val records = entities.map { entity ->
                ScoreHistoryRecord(
                    id = entity.id,
                    completionDate = entity.completionDate,
                    percentage = entity.percentage,
                    passed = entity.passed
                )
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
