package com.awsquiz.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScoreTrackerTest {

    @Test
    fun `calculateScore returns zero result when totalQuestions is 0`() {
        val result = ScoreTracker.calculateScore(emptyList(), 0)
        assertEquals(ScoreResult(correctCount = 0, totalCount = 0, percentage = 0, passed = false), result)
    }

    @Test
    fun `calculateScore counts correct answers`() {
        val answers = listOf(
            AnswerResult(isCorrect = true, selectedOptions = setOf(0), correctOptions = setOf(0)),
            AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(2)),
            AnswerResult(isCorrect = true, selectedOptions = setOf(1), correctOptions = setOf(1))
        )
        val result = ScoreTracker.calculateScore(answers, 3)
        assertEquals(2, result.correctCount)
        assertEquals(3, result.totalCount)
    }

    @Test
    fun `calculateScore computes percentage rounded to nearest whole number`() {
        // 1 correct out of 3 = 33.333...% -> rounds to 33
        val answers = listOf(
            AnswerResult(isCorrect = true, selectedOptions = setOf(0), correctOptions = setOf(0)),
            AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(2)),
            AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(3))
        )
        val result = ScoreTracker.calculateScore(answers, 3)
        assertEquals(33, result.percentage)
    }

    @Test
    fun `calculateScore rounds percentage up at 0_5`() {
        // 5 correct out of 6 = 83.333...% -> rounds to 83
        val answers = (1..5).map {
            AnswerResult(isCorrect = true, selectedOptions = setOf(0), correctOptions = setOf(0))
        } + AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(0))
        val result = ScoreTracker.calculateScore(answers, 6)
        assertEquals(83, result.percentage)
    }

    @Test
    fun `calculateScore returns passed true when percentage is exactly 72`() {
        // 72 correct out of 100 = 72%
        val answers = (1..72).map {
            AnswerResult(isCorrect = true, selectedOptions = setOf(0), correctOptions = setOf(0))
        } + (1..28).map {
            AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(0))
        }
        val result = ScoreTracker.calculateScore(answers, 100)
        assertEquals(72, result.percentage)
        assertTrue(result.passed)
    }

    @Test
    fun `calculateScore returns passed false when percentage is 71`() {
        // 71 correct out of 100 = 71%
        val answers = (1..71).map {
            AnswerResult(isCorrect = true, selectedOptions = setOf(0), correctOptions = setOf(0))
        } + (1..29).map {
            AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(0))
        }
        val result = ScoreTracker.calculateScore(answers, 100)
        assertEquals(71, result.percentage)
        assertFalse(result.passed)
    }

    @Test
    fun `calculateScore returns 100 percent for all correct`() {
        val answers = (1..10).map {
            AnswerResult(isCorrect = true, selectedOptions = setOf(0), correctOptions = setOf(0))
        }
        val result = ScoreTracker.calculateScore(answers, 10)
        assertEquals(100, result.percentage)
        assertTrue(result.passed)
    }

    @Test
    fun `calculateScore returns 0 percent for all incorrect`() {
        val answers = (1..10).map {
            AnswerResult(isCorrect = false, selectedOptions = setOf(1), correctOptions = setOf(0))
        }
        val result = ScoreTracker.calculateScore(answers, 10)
        assertEquals(0, result.percentage)
        assertFalse(result.passed)
    }

    @Test
    fun `calculateScore handles empty answer list with nonzero total`() {
        val result = ScoreTracker.calculateScore(emptyList(), 10)
        assertEquals(0, result.correctCount)
        assertEquals(10, result.totalCount)
        assertEquals(0, result.percentage)
        assertFalse(result.passed)
    }
}
