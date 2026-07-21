package com.awsquiz.app.domain

data class ScoreHistoryRecord(
    val id: String,
    val completionDate: Long,
    val percentage: Int,
    val passed: Boolean
)
