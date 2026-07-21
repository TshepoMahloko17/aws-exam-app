package com.awsquiz.app.domain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

/**
 * Property-based test for ProgressTrendCalculator.
 *
 * Feature: aws-exam-quiz-app, Property 13: Progress Trend Calculation
 *
 * Validates: Requirements 9.3
 */
class ProgressTrendCalculatorPropertyTest : FreeSpec({

    "Property 13: Progress Trend Calculation" - {

        /**
         * For any list of 0-5 integer percentage scores (each between 0 and 100),
         * the ProgressTrendCalculator SHALL return IMPROVING if the linear regression slope
         * exceeds +2, DECLINING if the slope is below -2, and STABLE otherwise.
         * For any list with fewer than 2 scores, it SHALL return STABLE.
         *
         * Validates: Requirements 9.3
         */
        "trend matches expected result based on linear regression slope with ±2 threshold" {
            checkAll(PropTestConfig(minSuccess = 100), Arb.list(Arb.int(0..100), 0..5)) { scores ->
                val result = ProgressTrendCalculator.calculateTrend(scores)
                val expected = computeExpectedTrend(scores)

                result shouldBe expected
            }
        }

        /**
         * For any list with fewer than 2 scores, STABLE is returned regardless of content.
         *
         * Validates: Requirements 9.3
         */
        "STABLE returned for lists with fewer than 2 scores" {
            checkAll(PropTestConfig(minSuccess = 100), Arb.list(Arb.int(0..100), 0..1)) { scores ->
                val result = ProgressTrendCalculator.calculateTrend(scores)

                result shouldBe ProgressTrend.STABLE
            }
        }
    }
})

/**
 * Independently computes the expected progress trend using linear regression.
 *
 * slope = (n * sum(i * y_i) - sum(i) * sum(y_i)) / (n * sum(i^2) - (sum(i))^2)
 * where i = 0, 1, ..., n-1
 *
 * If n < 2 → STABLE
 * If slope > 2 → IMPROVING
 * If slope < -2 → DECLINING
 * Otherwise → STABLE
 */
private fun computeExpectedTrend(scores: List<Int>): ProgressTrend {
    val n = scores.size
    if (n < 2) return ProgressTrend.STABLE

    var sumI = 0.0
    var sumY = 0.0
    var sumIY = 0.0
    var sumI2 = 0.0

    for (i in 0 until n) {
        val y = scores[i].toDouble()
        sumI += i
        sumY += y
        sumIY += i * y
        sumI2 += i.toDouble() * i.toDouble()
    }

    val denominator = n * sumI2 - sumI * sumI
    val slope = if (denominator == 0.0) {
        0.0
    } else {
        (n * sumIY - sumI * sumY) / denominator
    }

    return when {
        slope > 2.0 -> ProgressTrend.IMPROVING
        slope < -2.0 -> ProgressTrend.DECLINING
        else -> ProgressTrend.STABLE
    }
}
