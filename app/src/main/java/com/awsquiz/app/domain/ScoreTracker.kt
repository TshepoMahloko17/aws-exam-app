package com.awsquiz.app.domain

import kotlin.math.roundToInt

object ScoreTracker {
    fun calculateScore(answers: List<AnswerResult>, totalQuestions: Int): ScoreResult {
        if (totalQuestions == 0) {
            return ScoreResult(correctCount = 0, totalCount = 0, percentage = 0, passed = false)
        }

        val correctCount = answers.count { it.isCorrect }
        val percentage = (correctCount.toDouble() / totalQuestions * 100).roundToInt()
        val passed = percentage >= 72

        return ScoreResult(
            correctCount = correctCount,
            totalCount = totalQuestions,
            percentage = percentage,
            passed = passed
        )
    }
}
