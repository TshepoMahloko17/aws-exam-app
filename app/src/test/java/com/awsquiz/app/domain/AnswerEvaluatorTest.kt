package com.awsquiz.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnswerEvaluatorTest {

    private fun createQuestion(correctAnswers: Set<Int>, optionCount: Int = 4) = Question(
        id = 1,
        text = "Test question",
        options = (0 until optionCount).map { "Option ${it + 1}" },
        correctAnswers = correctAnswers,
        explanation = "Test explanation"
    )

    @Test
    fun `single answer - correct selection returns isCorrect true`() {
        val question = createQuestion(correctAnswers = setOf(2))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(2))

        assertTrue(result.isCorrect)
        assertEquals(setOf(2), result.selectedOptions)
        assertEquals(setOf(2), result.correctOptions)
    }

    @Test
    fun `single answer - incorrect selection returns isCorrect false`() {
        val question = createQuestion(correctAnswers = setOf(2))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(0))

        assertFalse(result.isCorrect)
        assertEquals(setOf(0), result.selectedOptions)
        assertEquals(setOf(2), result.correctOptions)
    }

    @Test
    fun `multi answer - exact match returns isCorrect true`() {
        val question = createQuestion(correctAnswers = setOf(1, 3))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(1, 3))

        assertTrue(result.isCorrect)
        assertEquals(setOf(1, 3), result.selectedOptions)
        assertEquals(setOf(1, 3), result.correctOptions)
    }

    @Test
    fun `multi answer - missing one correct option returns isCorrect false`() {
        val question = createQuestion(correctAnswers = setOf(1, 3))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(1))

        assertFalse(result.isCorrect)
    }

    @Test
    fun `multi answer - extra incorrect option returns isCorrect false`() {
        val question = createQuestion(correctAnswers = setOf(1, 3))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(1, 2, 3))

        assertFalse(result.isCorrect)
    }

    @Test
    fun `multi answer - completely wrong selection returns isCorrect false`() {
        val question = createQuestion(correctAnswers = setOf(1, 3))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(0, 2))

        assertFalse(result.isCorrect)
    }

    @Test
    fun `empty selection is never correct`() {
        val question = createQuestion(correctAnswers = setOf(2))
        val result = AnswerEvaluator.evaluate(question, selectedOptions = emptySet())

        assertFalse(result.isCorrect)
        assertEquals(emptySet<Int>(), result.selectedOptions)
        assertEquals(setOf(2), result.correctOptions)
    }

    @Test
    fun `result always contains correct options from question`() {
        val correctAnswers = setOf(0, 2, 3)
        val question = createQuestion(correctAnswers = correctAnswers, optionCount = 5)
        val result = AnswerEvaluator.evaluate(question, selectedOptions = setOf(1))

        assertEquals(correctAnswers, result.correctOptions)
    }
}
