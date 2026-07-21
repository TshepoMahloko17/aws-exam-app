package com.awsquiz.app.data

import com.awsquiz.app.domain.Question
import io.kotest.core.Tag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-based tests for JSON question parsing.
 *
 * Validates: Requirements 7.3, 7.5
 */
class QuestionParserPropertyTest : FunSpec({

    val parserResilienceTag = Tag("Feature: aws-exam-quiz-app, Property 8: Parser Resilience")
    val serializationRoundTripTag = Tag("Feature: aws-exam-quiz-app, Property 9: Question Serialization Round-Trip")

    /**
     * Property 8: Parser Resilience
     *
     * For any list of JSON entries composed of a mix of valid Question entries and invalid entries
     * (missing fields, malformed values), the QuestionRepository parser SHALL return exactly the
     * valid entries in their original relative order, skipping all invalid entries without failing.
     *
     * Validates: Requirements 7.3
     */
    test("Property 8: Parser returns exactly valid entries in original relative order").config(
        tags = setOf(parserResilienceTag)
    ) {
        checkAll(PropTestConfig(minSuccess = 100), arbMixedEntryList()) { mixedEntries ->
            // Build JSON string from the mixed entries
            val jsonString = buildJsonString(mixedEntries)

            // Parse using QuestionParser
            val result = QuestionParser.parseQuestions(jsonString)

            // Parser should not fail on mixed valid/invalid entries
            result.isSuccess shouldBe true

            val parsedQuestions = result.getOrThrow()

            // Extract expected valid questions in original order
            val expectedQuestions = mixedEntries
                .filter { it.isValid }
                .map { it.toExpectedQuestion() }

            // Assert count matches
            parsedQuestions.size shouldBe expectedQuestions.size

            // Assert each parsed question matches the expected valid entry in order
            parsedQuestions.zip(expectedQuestions).forEach { (parsed, expected) ->
                parsed.id shouldBe expected.id
                parsed.text shouldBe expected.text
                parsed.options shouldBe expected.options
                parsed.correctAnswers shouldBe expected.correctAnswers
                parsed.explanation shouldBe expected.explanation
            }
        }
    }

    /**
     * Property 9: Question Serialization Round-Trip
     *
     * For any valid Question object, serializing it to JSON and parsing it back SHALL produce
     * an equivalent Question with identical text, identical options in the same order,
     * identical correctAnswers set, and identical explanation text.
     *
     * Validates: Requirements 7.5
     */
    test("Property 9: Serialize and parse back produces equivalent Question").config(
        tags = setOf(serializationRoundTripTag)
    ) {
        val jsonCodec = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        checkAll(PropTestConfig(minSuccess = 100), arbValidQuestion()) { question ->
            // Serialize Question to JSON
            val serialized = jsonCodec.encodeToString(question)

            // Wrap in the question bank format for parsing
            val wrappedJson = """{"questions": [$serialized]}"""

            // Parse back using QuestionParser
            val result = QuestionParser.parseQuestions(wrappedJson)

            result.isSuccess shouldBe true

            val parsedQuestions = result.getOrThrow()
            parsedQuestions.size shouldBe 1

            val parsed = parsedQuestions[0]
            parsed.id shouldBe question.id
            parsed.text shouldBe question.text
            parsed.options shouldBe question.options
            parsed.correctAnswers shouldBe question.correctAnswers
            parsed.explanation shouldBe question.explanation
        }
    }
})

// --- Data classes for test generation ---

/**
 * Represents a test entry that can be either valid or invalid,
 * along with metadata about its validity for assertion purposes.
 */
private data class TestEntry(
    val jsonFragment: String,
    val isValid: Boolean,
    val id: Int = 0,
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctAnswers: Set<Int> = emptySet(),
    val explanation: String = ""
) {
    fun toExpectedQuestion(): Question = Question(
        id = id,
        text = text,
        options = options,
        correctAnswers = correctAnswers,
        explanation = explanation
    )
}

// --- Generators ---

/**
 * Generates a mixed list of valid and invalid JSON question entries.
 */
private fun arbMixedEntryList(): Arb<List<TestEntry>> = arbitrary {
    val size = Arb.int(1..10).bind()
    val entries = mutableListOf<TestEntry>()
    for (index in 0 until size) {
        val makeValid = Arb.boolean().bind()
        if (makeValid) {
            // Generate a valid entry inline
            val id = index + 1
            val text = "Question text for id $id"
            val optionCount = Arb.int(2..6).bind()
            val options = (0 until optionCount).map { "Option ${it + 1}" }
            val maxCorrect = minOf(3, optionCount)
            val correctCount = Arb.int(1..maxCorrect).bind()
            val correctAnswers = Arb.set(Arb.int(0 until optionCount), correctCount..correctCount).bind()
            val explanation = "Explanation for question $id"

            val correctAnswersJson = correctAnswers.joinToString(",", "[", "]")
            val optionsJson = options.joinToString(",") { "\"$it\"" }
            val jsonFragment = """{"id":$id,"text":"$text","options":[$optionsJson],"correctAnswers":$correctAnswersJson,"explanation":"$explanation"}"""

            entries.add(
                TestEntry(
                    jsonFragment = jsonFragment,
                    isValid = true,
                    id = id,
                    text = text,
                    options = options,
                    correctAnswers = correctAnswers,
                    explanation = explanation
                )
            )
        } else {
            // Generate an invalid entry inline
            val invalidType = Arb.int(0..5).bind()
            val id = index + 1
            val jsonFragment = when (invalidType) {
                0 -> """{"id":null,"text":"Some text","options":["A","B"],"correctAnswers":[0],"explanation":"Explanation"}"""
                1 -> """{"id":$id,"text":"","options":["A","B"],"correctAnswers":[0],"explanation":"Explanation"}"""
                2 -> """{"id":$id,"text":"Question","options":["A"],"correctAnswers":[0],"explanation":"Explanation"}"""
                3 -> """{"id":$id,"text":"Question","options":["A","B"],"correctAnswers":[],"explanation":"Explanation"}"""
                4 -> """{"id":$id,"text":"Question","options":["A","B"],"correctAnswers":[5],"explanation":"Explanation"}"""
                else -> """{"id":$id,"text":"Question","options":["A","B"],"correctAnswers":[0],"explanation":""}"""
            }
            entries.add(
                TestEntry(
                    jsonFragment = jsonFragment,
                    isValid = false
                )
            )
        }
    }
    entries.toList()
}

/**
 * Generates a random valid Question object for round-trip testing.
 */
private fun arbValidQuestion(): Arb<Question> = arbitrary {
    val id = Arb.int(1..10000).bind()
    val optionCount = Arb.int(2..6).bind()
    val options = (0 until optionCount).map { "Option ${it + 1}" }

    val maxCorrect = minOf(3, optionCount)
    val correctCount = Arb.int(1..maxCorrect).bind()
    val correctAnswers = Arb.set(Arb.int(0 until optionCount), correctCount..correctCount).bind()

    val text = "What is the correct answer for question $id?"
    val explanation = "The explanation for question $id is that the correct answers are ${correctAnswers.joinToString()}"

    Question(
        id = id,
        text = text,
        options = options,
        correctAnswers = correctAnswers,
        explanation = explanation
    )
}

/**
 * Builds a complete JSON string from a list of test entries.
 */
private fun buildJsonString(entries: List<TestEntry>): String {
    val entriesJson = entries.joinToString(",") { it.jsonFragment }
    return """{"questions":[$entriesJson]}"""
}
