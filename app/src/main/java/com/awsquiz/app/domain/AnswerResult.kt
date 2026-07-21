package com.awsquiz.app.domain

data class AnswerResult(
    val isCorrect: Boolean,
    val selectedOptions: Set<Int>,
    val correctOptions: Set<Int>
)
