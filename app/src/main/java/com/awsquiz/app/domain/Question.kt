package com.awsquiz.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctAnswers: Set<Int>,
    val explanation: String
) {
    val isSingleAnswer: Boolean get() = correctAnswers.size == 1
    val isMultipleAnswer: Boolean get() = correctAnswers.size > 1
}
