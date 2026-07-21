package com.awsquiz.app.domain

interface QuestionRepository {
    suspend fun loadQuestions(): Result<List<Question>>
}
