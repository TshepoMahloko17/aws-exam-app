package com.awsquiz.app.data

import com.awsquiz.app.domain.AnswerRecord
import com.awsquiz.app.domain.Session
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.util.UUID

/**
 * Property-based tests for Session persistence round-trip and completion detection.
 *
 * Feature: aws-exam-quiz-app, Property 4: Session Persistence Round-Trip
 * Feature: aws-exam-quiz-app, Property 5: Session Completion Detection
 *
 * Validates: Requirements 3.3, 4.1, 4.3, 4.4
 */
class SessionPersistencePropertyTest : FreeSpec({

    "Property 4: Session Persistence Round-Trip" - {

        /**
         * For any valid Session object (with arbitrary questionOrder, currentIndex, answers map,
         * and completion status), saving the session to the SessionRepository and loading it back
         * SHALL produce a Session with identical questionOrder, identical answers map, identical
         * currentIndex, and identical completion status.
         *
         * We set isCompleted = false so the session can be retrieved via loadIncompleteSession.
         *
         * Validates: Requirements 3.3, 4.1, 4.3
         */
        "save and load produces identical session for random sessions" {
            checkAll(PropTestConfig(minSuccess = 100), arbIncompleteSession()) { session ->
                val fakeDao = FakeSessionDao()
                val repository = RoomSessionRepository(fakeDao)

                repository.saveSession(session)
                val result = repository.loadIncompleteSession()

                result.isSuccess shouldBe true
                val loaded = result.getOrNull()!!

                loaded.id shouldBe session.id
                loaded.questionOrder shouldBe session.questionOrder
                loaded.currentIndex shouldBe session.currentIndex
                loaded.answers shouldBe session.answers
                loaded.isCompleted shouldBe session.isCompleted
                loaded.createdAt shouldBe session.createdAt
            }
        }
    }

    "Property 5: Session Completion Detection" - {

        /**
         * For any Session where the number of entries in the answers map equals the length of
         * the questionOrder list, marking the session as completed SHALL succeed and the session
         * SHALL be marked as completed. For any Session where the answers count is less than the
         * questionOrder length, the session SHALL NOT be marked as completed (remains incomplete).
         *
         * Validates: Requirements 4.4
         */
        "completed sessions are correctly detected via markSessionCompleted" {
            checkAll(PropTestConfig(minSuccess = 100), arbCompletedSession()) { session ->
                val fakeDao = FakeSessionDao()
                val repository = RoomSessionRepository(fakeDao)

                // Save the session as incomplete first
                val incompleteSession = session.copy(isCompleted = false)
                repository.saveSession(incompleteSession)

                // Mark it completed
                val markResult = repository.markSessionCompleted(session.id)
                markResult.isSuccess shouldBe true

                // Verify the session is now completed in storage
                val storedEntity = fakeDao.storedSessions[session.id]!!
                storedEntity.isCompleted shouldBe true
            }
        }

        "incomplete sessions remain loadable as incomplete" {
            checkAll(PropTestConfig(minSuccess = 100), arbIncompleteSessionPartialAnswers()) { session ->
                val fakeDao = FakeSessionDao()
                val repository = RoomSessionRepository(fakeDao)

                repository.saveSession(session)

                // Session should be loadable as incomplete
                val result = repository.loadIncompleteSession()
                result.isSuccess shouldBe true
                val loaded = result.getOrNull()!!
                loaded.isCompleted shouldBe false

                // Answers size should be less than questionOrder size
                loaded.answers.size shouldBe session.answers.size
                (loaded.answers.size < loaded.questionOrder.size) shouldBe true
            }
        }
    }
})

// --- Custom Arb generators ---

/**
 * Generates a random incomplete Session (isCompleted = false) with:
 * - Random String id (alphanumeric, 5-20 chars)
 * - Random questionOrder (1..50 elements, values 0..99)
 * - Random currentIndex (0..questionOrder.size-1)
 * - Random answers map (random subset of question indices -> random AnswerRecord)
 * - isCompleted = false (so it can be loaded via loadIncompleteSession)
 * - Random positive createdAt
 */
private fun arbIncompleteSession(): Arb<Session> = arbitrary {
    val id = UUID.randomUUID().toString()
    val orderSize = Arb.int(1..50).bind()
    val questionOrder = (0 until orderSize).map {
        Arb.int(0..99).bind()
    }
    val currentIndex = if (orderSize > 0) Arb.int(0 until orderSize).bind() else 0
    val answersCount = Arb.int(0..orderSize).bind()
    val answers = (0 until answersCount).associate { idx ->
        val selectedCount = Arb.int(1..4).bind()
        val selectedOptions = (0 until selectedCount).map { Arb.int(0..5).bind() }.toSet()
        val isCorrect = Arb.boolean().bind()
        idx to AnswerRecord(selectedOptions = selectedOptions, isCorrect = isCorrect)
    }
    val createdAt = Arb.long(1L..9_999_999_999L).bind()

    Session(
        id = id,
        questionOrder = questionOrder,
        currentIndex = currentIndex,
        answers = answers,
        isCompleted = false,
        createdAt = createdAt
    )
}

/**
 * Generates a session where answers.size == questionOrder.size (fully answered).
 * Used to test that markSessionCompleted correctly marks a session as completed.
 */
private fun arbCompletedSession(): Arb<Session> = arbitrary {
    val id = UUID.randomUUID().toString()
    val orderSize = Arb.int(1..50).bind()
    val questionOrder = (0 until orderSize).map {
        Arb.int(0..99).bind()
    }
    val answers = (0 until orderSize).associate { idx ->
        val selectedCount = Arb.int(1..4).bind()
        val selectedOptions = (0 until selectedCount).map { Arb.int(0..5).bind() }.toSet()
        val isCorrect = Arb.boolean().bind()
        idx to AnswerRecord(selectedOptions = selectedOptions, isCorrect = isCorrect)
    }
    val createdAt = Arb.long(1L..9_999_999_999L).bind()

    Session(
        id = id,
        questionOrder = questionOrder,
        currentIndex = orderSize - 1,
        answers = answers,
        isCompleted = false,
        createdAt = createdAt
    )
}

/**
 * Generates a session where answers.size < questionOrder.size (partially answered).
 * Used to test that incomplete sessions remain loadable and are not marked as completed.
 */
private fun arbIncompleteSessionPartialAnswers(): Arb<Session> = arbitrary {
    val id = UUID.randomUUID().toString()
    val orderSize = Arb.int(2..50).bind() // At least 2 so we can have fewer answers
    val questionOrder = (0 until orderSize).map {
        Arb.int(0..99).bind()
    }
    val answersCount = Arb.int(0 until orderSize).bind() // Strictly less than orderSize
    val answers = (0 until answersCount).associate { idx ->
        val selectedCount = Arb.int(1..4).bind()
        val selectedOptions = (0 until selectedCount).map { Arb.int(0..5).bind() }.toSet()
        val isCorrect = Arb.boolean().bind()
        idx to AnswerRecord(selectedOptions = selectedOptions, isCorrect = isCorrect)
    }
    val currentIndex = Arb.int(0 until orderSize).bind()
    val createdAt = Arb.long(1L..9_999_999_999L).bind()

    Session(
        id = id,
        questionOrder = questionOrder,
        currentIndex = currentIndex,
        answers = answers,
        isCompleted = false,
        createdAt = createdAt
    )
}
