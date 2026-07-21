package com.awsquiz.app.domain

interface SessionRepository {
    suspend fun saveSession(session: Session): Result<Unit>
    suspend fun getSession(sessionId: String): Result<Session?>
    suspend fun loadIncompleteSession(): Result<Session?>
    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun markSessionCompleted(sessionId: String): Result<Unit>
}
