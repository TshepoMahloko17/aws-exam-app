package com.awsquiz.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class AnswerRecord(
    val selectedOptions: Set<Int>,
    val isCorrect: Boolean
)
