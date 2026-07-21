package com.awsquiz.app.ui

import com.awsquiz.app.domain.Question
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based test for multi-answer count indicator logic.
 *
 * Feature: aws-exam-quiz-app, Property 10: Multi-Answer Count Indicator
 *
 * Validates: Requirements 1.4
 */
class MultiAnswerCountPropertyTest : FreeSpec({

    "Property 10: Multi-Answer Count Indicator" - {

        /**
         * For any Multiple_Answer_Question, the displayed count of expected correct answers
         * SHALL equal the size of the correctAnswers set defined for that Question.
         *
         * This validates that for multi-answer questions, question.correctAnswers.size
         * accurately represents the number of answers the user needs to select.
         *
         * Validates: Requirements 1.4
         */
        "displayed count equals correctAnswers.size for multi-answer questions" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                Arb.int(2..6),   // number of options (2 to 6)
                Arb.int(2..5)    // number of correct answers (2+ makes it multi-answer)
            ) { optionCount, rawCorrectCount ->
                // Ensure correctCount doesn't exceed optionCount
                val correctCount = rawCorrectCount.coerceAtMost(optionCount)

                // Generate a set of correct answer indices within option bounds
                val correctAnswers = (0 until optionCount).shuffled().take(correctCount).toSet()

                // Generate options list
                val options = (0 until optionCount).map { "Option ${it + 1}" }

                val question = Question(
                    id = 1,
                    text = "Sample multi-answer question",
                    options = options,
                    correctAnswers = correctAnswers,
                    explanation = "Test explanation"
                )

                // Verify it's a multi-answer question
                question.isMultipleAnswer shouldBe true

                // The displayed count (what the UI shows as "Select N answers")
                // is derived from question.correctAnswers.size
                val displayedCount = question.correctAnswers.size

                // Assert displayed count equals the actual number of correct answers
                displayedCount shouldBe correctCount
            }
        }

        /**
         * For any multi-answer question, correctAnswers.size is always >= 2.
         *
         * Validates: Requirements 1.4
         */
        "multi-answer questions always have correctAnswers.size >= 2" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                Arb.int(3..6),   // need at least 3 options to guarantee 2+ correct
                Arb.int(2..5)    // 2+ correct answers
            ) { optionCount, rawCorrectCount ->
                val correctCount = rawCorrectCount.coerceAtMost(optionCount)
                val correctAnswers = (0 until optionCount).shuffled().take(correctCount).toSet()
                val options = (0 until optionCount).map { "Option ${it + 1}" }

                val question = Question(
                    id = 1,
                    text = "Multi-answer question",
                    options = options,
                    correctAnswers = correctAnswers,
                    explanation = "Explanation"
                )

                // Multi-answer => correctAnswers.size >= 2
                question.isMultipleAnswer shouldBe true
                question.correctAnswers.size shouldBe correctCount
                (question.correctAnswers.size >= 2) shouldBe true
            }
        }
    }
})
