package com.awsquiz.app.domain

/**
 * Calculates the progress trend from a list of recent quiz scores using linear regression.
 *
 * The trend is determined by computing the slope of a linear regression line over the
 * score values (indexed 0..n-1). If the slope exceeds +2, the trend is IMPROVING.
 * If the slope is below -2, the trend is DECLINING. Otherwise, STABLE.
 */
object ProgressTrendCalculator {

    private const val SLOPE_THRESHOLD = 2.0

    /**
     * Calculates the progress trend from recent scores.
     *
     * @param recentScores List of percentage scores ordered chronologically (oldest first).
     *                     Each value should be between 0 and 100.
     * @return The calculated ProgressTrend based on linear regression slope.
     *         Returns STABLE if fewer than 2 scores are provided.
     */
    fun calculateTrend(recentScores: List<Int>): ProgressTrend {
        if (recentScores.size < 2) {
            return ProgressTrend.STABLE
        }

        val slope = computeLinearRegressionSlope(recentScores)

        return when {
            slope > SLOPE_THRESHOLD -> ProgressTrend.IMPROVING
            slope < -SLOPE_THRESHOLD -> ProgressTrend.DECLINING
            else -> ProgressTrend.STABLE
        }
    }

    /**
     * Computes the slope of the linear regression line for the given scores.
     *
     * Uses the formula:
     *   slope = (n * sum(i * y_i) - sum(i) * sum(y_i)) / (n * sum(i^2) - (sum(i))^2)
     *
     * where i = 0, 1, ..., n-1 and y_i are the score values.
     */
    private fun computeLinearRegressionSlope(scores: List<Int>): Double {
        val n = scores.size
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
        if (denominator == 0.0) {
            return 0.0
        }

        return (n * sumIY - sumI * sumY) / denominator
    }
}
