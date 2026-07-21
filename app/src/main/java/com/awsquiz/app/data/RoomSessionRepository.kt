package com.awsquiz.app.data

import com.awsquiz.app.domain.AnswerRecord
import com.awsquiz.app.domain.Session
import com.awsquiz.app.domain.SessionRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RoomSessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: Session): Result<Unit> {
        return try {
            val entity = toEntity(session)
            sessionDao.upsertSession(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSession(sessionId: String): Result<Session?> {
        return try {
            val entity = sessionDao.getSession(sessionId)
                ?: return Result.success(null)
            val session = toSession(entity)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadIncompleteSession(): Result<Session?> {
        return try {
            val entity = sessionDao.getIncompleteSession()
                ?: return Result.success(null)
            val session = toSession(entity)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            sessionDao.deleteSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markSessionCompleted(sessionId: String): Result<Unit> {
        return try {
            val entity = sessionDao.getSession(sessionId)
                ?: return Result.failure(
                    IllegalStateException("Session not found: $sessionId")
                )
            val updatedEntity = entity.copy(isCompleted = true)
            sessionDao.upsertSession(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toEntity(session: Session): SessionEntity {
        val questionOrderJson = json.encodeToString(session.questionOrder)
        val answersJson = json.encodeToString(serializeAnswers(session.answers))
        return SessionEntity(
            id = session.id,
            questionOrderJson = questionOrderJson,
            currentIndex = session.currentIndex,
            answersJson = answersJson,
            isCompleted = session.isCompleted,
            createdAt = session.createdAt
        )
    }

    private fun toSession(entity: SessionEntity): Session {
        val questionOrder: List<Int> = json.decodeFromString(entity.questionOrderJson)
        val answers: Map<Int, AnswerRecord> = deserializeAnswers(
            json.decodeFromString<Map<String, AnswerRecord>>(entity.answersJson)
        )
        return Session(
            id = entity.id,
            questionOrder = questionOrder,
            currentIndex = entity.currentIndex,
            answers = answers,
            isCompleted = entity.isCompleted,
            createdAt = entity.createdAt
        )
    }

    private fun serializeAnswers(answers: Map<Int, AnswerRecord>): Map<String, AnswerRecord> {
        return answers.mapKeys { (key, _) -> key.toString() }
    }

    private fun deserializeAnswers(serialized: Map<String, AnswerRecord>): Map<Int, AnswerRecord> {
        return serialized.mapKeys { (key, _) -> key.toInt() }
    }
}
