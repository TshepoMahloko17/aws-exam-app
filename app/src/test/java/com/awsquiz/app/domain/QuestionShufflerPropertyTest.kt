package com.awsquiz.app.domain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Property-based tests for QuestionShuffler.
 *
 * Feature: aws-exam-quiz-app, Property 2: Shuffle Permutation Invariant
 * Feature: aws-exam-quiz-app, Property 3: Shuffle Non-Determinism
 *
 * Validates: Requirements 3.1, 3.2
 */
class QuestionShufflerPropertyTest : FreeSpec({

    "Property 2: Shuffle Permutation Invariant" - {

        /**
         * For any question count n >= 2, the QuestionShuffler SHALL produce a list of
         * length n where sorting the output yields the sequence [0, 1, 2, ..., n-1],
         * ensuring every question appears exactly once and no question is omitted.
         *
         * Validates: Requirements 3.1
         */
        "sorted shuffle output equals [0, 1, ..., n-1] for any count 2..500" {
            checkAll(PropTestConfig(minSuccess = 100), Arb.int(2..500)) { count ->
                val shuffler = DefaultQuestionShuffler()
                val result = shuffler.shuffle(count)

                result.size shouldBe count
                result.sorted() shouldBe (0 until count).toList()
            }
        }
    }

    "Property 3: Shuffle Non-Determinism" - {

        /**
         * For any question count n >= 5, two consecutive calls to QuestionShuffler
         * SHALL produce different orderings (with overwhelming probability across
         * 100+ test iterations).
         *
         * Validates: Requirements 3.2
         */
        "two separate shuffle calls produce different orderings for counts 5..500" {
            checkAll(PropTestConfig(minSuccess = 100), Arb.int(5..500)) { count ->
                val shuffler1 = DefaultQuestionShuffler()
                val result1 = shuffler1.shuffle(count)

                val shuffler2 = DefaultQuestionShuffler()
                val result2 = shuffler2.shuffle(count)

                result1 shouldNotBe result2
            }
        }
    }
})
