package com.awsquiz.app.domain

/**
 * Evaluates whether a user's selected options match the correct answers for a question.
 * Uses set equality for both single-answer and multi-answer questions.
 */
object AnswerEvaluator {

    /**
     * Evaluates the user's answer against the question's correct answers.
     *
     * @param question The question being answered
     * @param selectedOptions The set of option indices selected by the user
     * @return An AnswerResult indicating correctness, the user's selections, and the correct options
     */
    fun evaluate(question: Question, selectedOptions: Set<Int>): AnswerResult {
        val isCorrect = selectedOptions == question.correctAnswers
        return AnswerResult(
            isCorrect = isCorrect,
            selectedOptions = selectedOptions,
            correctOptions = question.correctAnswers
        )
    }
}
