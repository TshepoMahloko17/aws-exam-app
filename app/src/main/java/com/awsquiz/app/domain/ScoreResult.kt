package com.awsquiz.app.domain

data class ScoreResult(
    val correctCount: Int,
    val totalCount: Int,
    val percentage: Int,
    val passed: Boolean
)
