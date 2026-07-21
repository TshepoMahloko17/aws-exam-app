package com.awsquiz.app.domain

sealed class AppError {
    data class QuestionLoadError(val message: String) : AppError()
    data class SessionCorruptionError(val message: String) : AppError()
    data class InsufficientQuestionsError(val available: Int) : AppError()
    data class ScoreHistoryError(val message: String) : AppError()
}
