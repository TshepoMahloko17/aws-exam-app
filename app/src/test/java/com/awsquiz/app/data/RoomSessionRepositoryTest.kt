package com.awsquiz.app.data

import com.awsquiz.app.domain.AnswerRecord
import com.awsquiz.app.domain.Session
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoomSessionRepositoryTest {

    private lateinit var fakeDao: FakeSessionDao
    private lateinit var repository: RoomSessionRepository

    @BeforeEach
    fun setUp() {
        fakeDao = FakeSessionDao()
        repository = RoomSessionRepository(fakeDao)
    }

    @Test
    fun `saveSession converts Session to entity and stores it`() = runTest {
        val session = Session(
            id = "test-1",
            questionOrder = listOf(3, 1, 4, 1, 5),
            currentIndex = 2,
            answers = mapOf(
                0 to AnswerRecord(selectedOptions = setOf(1), isCorrect = true),
                1 to AnswerRecord(selectedOptions = setOf(0, 2), isCorrect = false)
            ),
            isCompleted = false,
            createdAt = 1700000000L
        )

        val result = repository.saveSession(session)

        assertTrue(result.isSuccess)
        val stored = fakeDao.storedSessions["test-1"]!!
        assertEquals("test-1", stored.id)
        assertEquals(2, stored.currentIndex)
        assertEquals(false, stored.isCompleted)
        assertEquals(1700000000L, stored.createdAt)
    }

    @Test
    fun `loadIncompleteSession returns null when no session exists`() = runTest {
        val result = repository.loadIncompleteSession()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `loadIncompleteSession round-trips session correctly`() = runTest {
        val session = Session(
            id = "test-2",
            questionOrder = listOf(0, 2, 4),
            currentIndex = 1,
            answers = mapOf(
                0 to AnswerRecord(selectedOptions = setOf(3), isCorrect = true)
            ),
            isCompleted = false,
            createdAt = 1700000001L
        )

        repository.saveSession(session)
        val result = repository.loadIncompleteSession()

        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()!!
        assertEquals(session.id, loaded.id)
        assertEquals(session.questionOrder, loaded.questionOrder)
        assertEquals(session.currentIndex, loaded.currentIndex)
        assertEquals(session.answers, loaded.answers)
        assertEquals(session.isCompleted, loaded.isCompleted)
        assertEquals(session.createdAt, loaded.createdAt)
    }

    @Test
    fun `loadIncompleteSession returns failure on corrupted JSON`() = runTest {
        // Manually insert corrupted entity
        fakeDao.storedSessions["corrupt"] = SessionEntity(
            id = "corrupt",
            questionOrderJson = "not valid json",
            currentIndex = 0,
            answersJson = "{}",
            isCompleted = false,
            createdAt = 0L
        )

        val result = repository.loadIncompleteSession()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteSession removes the session`() = runTest {
        val session = Session(
            id = "del-1",
            questionOrder = listOf(0),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = 0L
        )
        repository.saveSession(session)

        val result = repository.deleteSession("del-1")

        assertTrue(result.isSuccess)
        assertNull(fakeDao.storedSessions["del-1"])
    }

    @Test
    fun `markSessionCompleted sets isCompleted to true`() = runTest {
        val session = Session(
            id = "comp-1",
            questionOrder = listOf(0, 1, 2),
            currentIndex = 2,
            answers = mapOf(
                0 to AnswerRecord(setOf(1), true),
                1 to AnswerRecord(setOf(2), false),
                2 to AnswerRecord(setOf(0), true)
            ),
            isCompleted = false,
            createdAt = 1700000002L
        )
        repository.saveSession(session)

        val result = repository.markSessionCompleted("comp-1")

        assertTrue(result.isSuccess)
        val stored = fakeDao.storedSessions["comp-1"]!!
        assertTrue(stored.isCompleted)
    }

    @Test
    fun `markSessionCompleted returns failure when session not found`() = runTest {
        val result = repository.markSessionCompleted("nonexistent")

        assertTrue(result.isFailure)
    }

    @Test
    fun `saveSession with empty answers and question order`() = runTest {
        val session = Session(
            id = "empty-1",
            questionOrder = emptyList(),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = 0L
        )

        val result = repository.saveSession(session)
        assertTrue(result.isSuccess)

        val loadResult = repository.loadIncompleteSession()
        assertTrue(loadResult.isSuccess)
        val loaded = loadResult.getOrNull()!!
        assertEquals(emptyList<Int>(), loaded.questionOrder)
        assertEquals(emptyMap<Int, AnswerRecord>(), loaded.answers)
    }
}

/**
 * Fake DAO for unit testing without Room/Android dependencies.
 */
class FakeSessionDao : SessionDao {
    val storedSessions = mutableMapOf<String, SessionEntity>()

    override suspend fun upsertSession(session: SessionEntity) {
        storedSessions[session.id] = session
    }

    override suspend fun getSession(sessionId: String): SessionEntity? {
        return storedSessions[sessionId]
    }

    override suspend fun getIncompleteSession(): SessionEntity? {
        return storedSessions.values.firstOrNull { !it.isCompleted }
    }

    override suspend fun deleteSession(sessionId: String) {
        storedSessions.remove(sessionId)
    }
}
