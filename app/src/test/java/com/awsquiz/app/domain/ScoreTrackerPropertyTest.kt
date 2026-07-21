package com.awsquiz.app.domain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import kotlin.math.roundToInt

/**
 * Property-based test for ScoreTracker.
 *
 * Feature: aws-exam-quiz-app, Property 6: Score Calculation Correctness
 *
 * Validates: Requirements 5.1, 5.2, 5.3
 */
class ScoreTrackerPropertyTest : FreeSpec({

    "Property 6: Score Calculation Correctness" - {

        /**
         * For any list of AnswerResult objects and a total question count, the ScoreTracker
         * SHALL compute:
         * (a) correctCount equal to the number of results where isCorrect is true
         * (b) percentage equal to round(correctCount / totalCount × 100) to the nearest whole number
         * (c) passed equal to true if and only if percentage >= 72
         *
         * Validates: Requirements 5.1, 5.2, 5.3
         */
        "correctCount, percentage, and passed are correctly computed for random answer lists" {
            checkAll(PropTestConfig(minSuccess = 100), arbAnswerResults()) { answers ->
                val totalQuestions = answers.size
                val result = ScoreTracker.calculateScore(answers, totalQuestions)

                val expectedCorrectCount = answers.count { it.isCorrect }
                val expectedPercentage = (expectedCorrectCount.toDouble() / totalQuestions * 100).roundToInt()
                val expectedPassed = expectedPercentage >= 72

                result.correctCount shouldBe expectedCorrectCount
                result.totalCount shouldBe totalQuestions
                result.percentage shouldBe expectedPercentage
                result.passed shouldBe expectedPassed
            }
        }

        /**
         * Edge case: when totalQuestions = 0, result should be ScoreResult(0, 0, 0, false).
         *
         * Validates: Requirements 5.1, 5.2, 5.3
         */
        "returns zero score result when totalQuestions is 0" {
            val result = ScoreTracker.calculateScore(emptyList(), 0)

            result.correctCount shouldBe 0
            result.totalCount shouldBe 0
            result.percentage shouldBe 0
            result.passed shouldBe false
        }
    }
})

/**
 * Generates a random list of AnswerResult objects with 1..200 items.
 * Each AnswerResult has a random isCorrect flag; selectedOptions and correctOptions
 * use dummy values since ScoreTracker only inspects isCorrect.
 */
private fun arbAnswerResults(): Arb<List<AnswerResult>> = arbitrary {
    val size = Arb.int(1..200).bind()
    (0 until size).map {
        val isCorrect = Arb.boolean().bind()
        AnswerResult(
            isCorrect = isCorrect,
            selectedOptions = setOf(0),
            correctOptions = setOf(0)
        )
    }
}
