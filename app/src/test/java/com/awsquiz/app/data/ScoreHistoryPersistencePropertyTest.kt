package com.awsquiz.app.data

import com.awsquiz.app.domain.ScoreHistoryRecord
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

/**
 * Property-based tests for ScoreHistory persistence via RoomScoreHistoryRepository.
 *
 * Uses a FakeScoreHistoryDao to test the repository mapping logic without
 * requiring an Android/Room environment.
 *
 * Feature: aws-exam-quiz-app, Property 11: Score History Persistence Round-Trip
 * Feature: aws-exam-quiz-app, Property 12: Score History Ordering
 *
 * Validates: Requirements 9.1, 9.2
 */
@OptIn(io.kotest.common.ExperimentalKotest::class)
class ScoreHistoryPersistencePropertyTest : FreeSpec({

    "Property 11: Score History Persistence Round-Trip" - {

        /**
         * For any valid ScoreHistoryRecord, saving it to the repository and loading
         * it back SHALL produce a record with identical id, completionDate, percentage,
         * and passed flag.
         *
         * Validates: Requirements 9.1, 9.2
         */
        "save and load produces identical record for random ScoreHistoryRecords" {
            checkAll(PropTestConfig(minSuccess = 100), arbScoreHistoryRecord()) { record ->
                val fakeDao = FakeScoreHistoryDao()
                val repository = RoomScoreHistoryRepository(fakeDao)

                val saveResult = repository.saveScore(record)
                saveResult.isSuccess shouldBe true

                val loadResult = repository.getAllScores()
                loadResult.isSuccess shouldBe true

                val loadedRecords = loadResult.getOrThrow()
                loadedRecords.size shouldBe 1

                val loadedRecord = loadedRecords[0]
                loadedRecord.id shouldBe record.id
                loadedRecord.completionDate shouldBe record.completionDate
                loadedRecord.percentage shouldBe record.percentage
                loadedRecord.passed shouldBe record.passed
            }
        }
    }

    "Property 12: Score History Ordering" - {

        /**
         * For any set of ScoreHistoryRecords with distinct completion dates, the repository
         * SHALL return them ordered by completionDate descending (most recent first), and
         * the returned list SHALL contain every record that was saved.
         *
         * Validates: Requirements 9.1, 9.2
         */
        "records are returned in descending completionDate order and all records are present" {
            checkAll(PropTestConfig(minSuccess = 100), arbDistinctScoreHistoryRecords()) { records ->
                val fakeDao = FakeScoreHistoryDao()
                val repository = RoomScoreHistoryRepository(fakeDao)

                // Save all records
                records.forEach { record ->
                    val saveResult = repository.saveScore(record)
                    saveResult.isSuccess shouldBe true
                }

                // Load all records
                val loadResult = repository.getAllScores()
                loadResult.isSuccess shouldBe true

                val loadedRecords = loadResult.getOrThrow()

                // Assert all records are present
                loadedRecords.size shouldBe records.size
                loadedRecords.shouldContainAll(records)

                // Assert descending completionDate order
                for (i in 0 until loadedRecords.size - 1) {
                    (loadedRecords[i].completionDate > loadedRecords[i + 1].completionDate) shouldBe true
                }
            }
        }
    }
})

/**
 * Fake implementation of ScoreHistoryDao that stores entities in memory
 * and returns them sorted by completionDate DESC (matching Room's query behavior).
 */
class FakeScoreHistoryDao : ScoreHistoryDao {
    private val scores = mutableListOf<ScoreHistoryEntity>()

    override suspend fun insertScore(score: ScoreHistoryEntity) {
        scores.add(score)
    }

    override suspend fun getAllScores(): List<ScoreHistoryEntity> {
        return scores.sortedByDescending { it.completionDate }
    }

    override suspend fun getRecentScores(count: Int): List<ScoreHistoryEntity> {
        return scores.sortedByDescending { it.completionDate }.take(count)
    }
}

/**
 * Generates a random ScoreHistoryRecord with:
 * - id: unique string based on index and random suffix
 * - completionDate: positive Long (1..4_000_000_000_000)
 * - percentage: 0..100
 * - passed: random boolean
 */
private fun arbScoreHistoryRecord(): Arb<ScoreHistoryRecord> = arbitrary {
    val idSuffix = Arb.long(1L..999_999_999L).bind()
    val completionDate = Arb.long(1L..4_000_000_000_000L).bind()
    val percentage = Arb.int(0..100).bind()
    val passed = Arb.boolean().bind()
    ScoreHistoryRecord(
        id = "score-$idSuffix",
        completionDate = completionDate,
        percentage = percentage,
        passed = passed
    )
}

/**
 * Generates a list of 2-10 random ScoreHistoryRecords with distinct completionDates.
 * Uses spread-out date ranges to guarantee uniqueness without do-while loops.
 */
private fun arbDistinctScoreHistoryRecords(): Arb<List<ScoreHistoryRecord>> = arbitrary {
    val size = Arb.int(2..10).bind()

    // Generate distinct dates by using non-overlapping ranges
    val baseDate = Arb.long(1_000_000L..1_000_000_000L).bind()
    val records = (0 until size).map { index ->
        val offset = Arb.long(1L..999_999L).bind()
        val completionDate = baseDate + (index.toLong() * 1_000_000L) + offset
        val percentage = Arb.int(0..100).bind()
        val passed = Arb.boolean().bind()
        ScoreHistoryRecord(
            id = "score-$index-$offset",
            completionDate = completionDate,
            percentage = percentage,
            passed = passed
        )
    }
    records
}
