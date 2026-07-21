package com.awsquiz.app.domain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll

/**
 * Property-based test for AnswerEvaluator.
 *
 * Feature: aws-exam-quiz-app, Property 1: Answer Evaluation Correctness
 *
 * Validates: Requirements 2.1, 2.2
 */
class AnswerEvaluatorPropertyTest : FreeSpec({

    "Property 1: Answer Evaluation Correctness" - {

        /**
         * For any Question (single-answer or multiple-answer) and for any set of selected
         * option indices, the AnswerEvaluator SHALL return isCorrect = true if and only if
         * the set of selected options is exactly equal to the set of correct answers.
         *
         * Validates: Requirements 2.1, 2.2
         */
        "isCorrect is true iff selectedOptions equals question.correctAnswers as sets" {
            checkAll(PropTestConfig(minSuccess = 100), arbQuestionWithSelection()) { (question, selectedOptions) ->
                val result = AnswerEvaluator.evaluate(question, selectedOptions)

                // Core property: isCorrect iff sets are equal
                result.isCorrect shouldBe (selectedOptions == question.correctAnswers)

                // Additional structural assertions
                result.selectedOptions shouldBe selectedOptions
                result.correctOptions shouldBe question.correctAnswers
            }
        }
    }
})

/**
 * Data class pairing a Question with a selection set for property testing.
 */
private data class QuestionWithSelection(
    val question: Question,
    val selectedOptions: Set<Int>
)

/**
 * Generates a random Question with 2-6 options and 1-3 correct answers,
 * paired with a random selection set drawn from valid option indices for that question.
 */
private fun arbQuestionWithSelection(): Arb<QuestionWithSelection> = arbitrary {
    val optionCount = Arb.int(2..6).bind()
    val options = (0 until optionCount).map { "Option ${it + 1}" }

    val maxCorrect = minOf(3, optionCount)
    val correctCount = Arb.int(1..maxCorrect).bind()
    val correctAnswers = Arb.set(Arb.int(0 until optionCount), correctCount..correctCount).bind()

    val question = Question(
        id = 1,
        text = "Test question",
        options = options,
        correctAnswers = correctAnswers,
        explanation = "Test explanation"
    )

    // Generate selection as a random subset of valid option indices (0 until optionCount)
    val selectionSize = Arb.int(0..optionCount).bind()
    val selectedOptions = if (selectionSize == 0) {
        emptySet()
    } else {
        Arb.set(Arb.int(0 until optionCount), selectionSize..selectionSize).bind()
    }

    QuestionWithSelection(question, selectedOptions)
}
