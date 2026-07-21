package com.awsquiz.app.data

import com.awsquiz.app.domain.Question
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializable wrapper for the top-level JSON structure.
 */
@Serializable
data class QuestionBankJson(
    val questions: List<QuestionEntryJson> = emptyList()
)

/**
 * Raw JSON entry with nullable/optional fields for lenient parsing.
 * Invalid entries are filtered out during validation.
 */
@Serializable
data class QuestionEntryJson(
    val id: Int? = null,
    val text: String? = null,
    val options: List<String>? = null,
    val correctAnswers: List<Int>? = null,
    val explanation: String? = null
)

/**
 * Pure parsing utility that converts a JSON string into a list of valid [Question] objects.
 *
 * - Parses using kotlinx.serialization with lenient settings
 * - Skips invalid entries (missing fields, empty text, insufficient options, out-of-range indices)
 * - Returns error result if the JSON is entirely unparseable
 * - Preserves the relative order of valid entries
 */
object QuestionParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses a JSON string containing a question bank and returns the valid questions.
     *
     * @param jsonString The raw JSON string to parse
     * @return Result containing the list of valid questions, or a failure if parsing fails entirely
     */
    fun parseQuestions(jsonString: String): Result<List<Question>> {
        val questionBank: QuestionBankJson
        try {
            questionBank = json.decodeFromString<QuestionBankJson>(jsonString)
        } catch (e: SerializationException) {
            return Result.failure(e)
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }

        val validQuestions = questionBank.questions.mapNotNull { entry ->
            validateEntry(entry)
        }

        return Result.success(validQuestions)
    }

    /**
     * Validates a raw JSON entry and converts it to a [Question] domain object.
     * Returns null if the entry is invalid.
     */
    internal fun validateEntry(entry: QuestionEntryJson): Question? {
        val id = entry.id ?: return null

        val text = entry.text
        if (text.isNullOrBlank()) return null

        val options = entry.options
        if (options == null || options.size < 2) return null

        val correctAnswers = entry.correctAnswers
        if (correctAnswers.isNullOrEmpty()) return null

        // Validate all correctAnswer indices are in valid range [0, options.size - 1]
        for (index in correctAnswers) {
            if (index < 0 || index >= options.size) return null
        }

        val explanation = entry.explanation
        if (explanation.isNullOrBlank()) return null

        return Question(
            id = id,
            text = text,
            options = options,
            correctAnswers = correctAnswers.toSet(),
            explanation = explanation
        )
    }
}
