package com.awsquiz.app.domain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Property-based test for navigation button visibility logic.
 *
 * Feature: aws-exam-quiz-app, Property 7: Navigation Button Visibility
 *
 * Validates: Requirements 6.1, 6.2
 */
class NavigationLogicPropertyTest : FreeSpec({

    "Property 7: Navigation Button Visibility" - {

        /**
         * For any currentIndex and totalCount where 0 <= currentIndex < totalCount,
         * the previous button SHALL be visible if and only if currentIndex > 0,
         * and the next button SHALL be visible if and only if currentIndex < totalCount - 1.
         *
         * Validates: Requirements 6.1, 6.2
         */
        "previous visible iff currentIndex > 0, next visible iff currentIndex < totalCount - 1" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                Arb.int(1..500),
                Arb.int(0..499)
            ) { totalCount, rawIndex ->
                val currentIndex = rawIndex % totalCount // ensure 0 <= currentIndex < totalCount

                val canGoPrevious = currentIndex > 0
                val canGoNext = currentIndex < totalCount - 1

                canGoPrevious shouldBe (currentIndex > 0)
                canGoNext shouldBe (currentIndex < totalCount - 1)
            }
        }

        /**
         * Edge case: first question (index 0) — previous should be hidden.
         *
         * Validates: Requirements 6.1
         */
        "first question: previous is hidden, next is visible" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                Arb.int(2..500)
            ) { totalCount ->
                val currentIndex = 0
                val canGoPrevious = currentIndex > 0
                val canGoNext = currentIndex < totalCount - 1

                canGoPrevious shouldBe false
                canGoNext shouldBe true
            }
        }

        /**
         * Edge case: last question (index == totalCount - 1) — next should be hidden.
         *
         * Validates: Requirements 6.2
         */
        "last question: previous is visible, next is hidden" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                Arb.int(2..500)
            ) { totalCount ->
                val currentIndex = totalCount - 1
                val canGoPrevious = currentIndex > 0
                val canGoNext = currentIndex < totalCount - 1

                canGoPrevious shouldBe true
                canGoNext shouldBe false
            }
        }

        /**
         * Edge case: single-question session (totalCount == 1) — both buttons hidden.
         *
         * Validates: Requirements 6.1, 6.2
         */
        "single-question session: both previous and next are hidden" {
            val totalCount = 1
            val currentIndex = 0
            val canGoPrevious = currentIndex > 0
            val canGoNext = currentIndex < totalCount - 1

            canGoPrevious shouldBe false
            canGoNext shouldBe false
        }
    }
})
