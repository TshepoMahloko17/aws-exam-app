package com.awsquiz.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProgressTrendCalculatorTest {

    @Test
    fun `returns STABLE for empty list`() {
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(emptyList()))
    }

    @Test
    fun `returns STABLE for single score`() {
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(listOf(50)))
    }

    @Test
    fun `returns IMPROVING for monotonically increasing scores with steep slope`() {
        // Scores: 40, 50, 60, 70, 80 — clearly increasing with slope > 2
        assertEquals(ProgressTrend.IMPROVING, ProgressTrendCalculator.calculateTrend(listOf(40, 50, 60, 70, 80)))
    }

    @Test
    fun `returns DECLINING for monotonically decreasing scores with steep slope`() {
        // Scores: 80, 70, 60, 50, 40 — clearly decreasing with slope < -2
        assertEquals(ProgressTrend.DECLINING, ProgressTrendCalculator.calculateTrend(listOf(80, 70, 60, 50, 40)))
    }

    @Test
    fun `returns STABLE for all identical scores`() {
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(listOf(70, 70, 70, 70, 70)))
    }

    @Test
    fun `returns STABLE for oscillating scores`() {
        // Scores: 0, 100, 0, 100, 0 — oscillating, slope should be near 0
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(listOf(0, 100, 0, 100, 0)))
    }

    @Test
    fun `returns STABLE for exactly 2 identical scores`() {
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(listOf(50, 50)))
    }

    @Test
    fun `returns IMPROVING for exactly 2 scores with large increase`() {
        // With 2 scores: slope = (y1 - y0) = 60 - 40 = 20, which is > 2
        assertEquals(ProgressTrend.IMPROVING, ProgressTrendCalculator.calculateTrend(listOf(40, 60)))
    }

    @Test
    fun `returns DECLINING for exactly 2 scores with large decrease`() {
        // With 2 scores: slope = (y1 - y0) = 40 - 60 = -20, which is < -2
        assertEquals(ProgressTrend.DECLINING, ProgressTrendCalculator.calculateTrend(listOf(60, 40)))
    }

    @Test
    fun `returns STABLE for slight increase within threshold`() {
        // With 2 scores: slope = (y1 - y0) = 51 - 50 = 1, which is within ±2
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(listOf(50, 51)))
    }

    @Test
    fun `returns STABLE for slight decrease within threshold`() {
        // With 2 scores: slope = (y1 - y0) = 49 - 50 = -1, which is within ±2
        assertEquals(ProgressTrend.STABLE, ProgressTrendCalculator.calculateTrend(listOf(50, 49)))
    }
}
