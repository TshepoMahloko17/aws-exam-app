package com.awsquiz.app.domain

data class Session(
    val id: String,
    val questionOrder: List<Int>,
    val currentIndex: Int,
    val answers: Map<Int, AnswerRecord>,
    val isCompleted: Boolean,
    val createdAt: Long
)
