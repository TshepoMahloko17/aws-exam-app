package com.awsquiz.app.data

import android.content.res.AssetManager
import android.util.Log
import com.awsquiz.app.domain.Question
import com.awsquiz.app.domain.QuestionRepository
import java.io.IOException

/**
 * Implementation of [QuestionRepository] that loads questions from a bundled JSON asset file.
 *
 * - Delegates parsing to [QuestionParser] for testability
 * - Skips invalid entries and logs warnings
 * - Returns error result if the file is missing or entirely unparseable
 * - Caches parsed questions in memory after successful load
 */
class JsonQuestionRepository(
    private val assetManager: AssetManager
) : QuestionRepository {

    companion object {
        private const val TAG = "JsonQuestionRepository"
        private const val QUESTIONS_FILE = "questions.json"
    }

    private var cachedQuestions: List<Question>? = null

    override suspend fun loadQuestions(): Result<List<Question>> {
        // Return cached result if available
        cachedQuestions?.let { return Result.success(it) }

        val jsonString: String
        try {
            jsonString = assetManager.open(QUESTIONS_FILE).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to open questions file: ${e.message}")
            return Result.failure(e)
        }

        val parseResult = QuestionParser.parseQuestions(jsonString)
        if (parseResult.isFailure) {
            Log.w(TAG, "Failed to parse questions JSON: ${parseResult.exceptionOrNull()?.message}")
            return Result.failure(parseResult.exceptionOrNull()!!)
        }

        val validQuestions = parseResult.getOrThrow()

        if (validQuestions.isEmpty()) {
            val error = IllegalStateException("No valid questions found in $QUESTIONS_FILE")
            Log.w(TAG, error.message!!)
            return Result.failure(error)
        }

        cachedQuestions = validQuestions
        return Result.success(validQuestions)
    }
}
