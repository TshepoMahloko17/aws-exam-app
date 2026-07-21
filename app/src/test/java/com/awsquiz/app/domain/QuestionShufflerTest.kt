package com.awsquiz.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestionShufflerTest {

    private val shuffler = DefaultQuestionShuffler()

    @Test
    fun `shuffle returns list of correct size`() {
        val result = shuffler.shuffle(10)
        assertEquals(10, result.size)
    }

    @Test
    fun `shuffle returns valid permutation with each index exactly once`() {
        val count = 50
        val result = shuffler.shuffle(count)

        assertEquals(count, result.size)
        assertEquals((0 until count).toList(), result.sorted())
    }

    @Test
    fun `shuffle with count of 2 returns valid permutation`() {
        val result = shuffler.shuffle(2)
        assertEquals(2, result.size)
        assertEquals(listOf(0, 1), result.sorted())
    }

    @Test
    fun `shuffle produces different orderings on consecutive calls`() {
        val count = 20
        val results = (1..10).map { shuffler.shuffle(count) }
        // With 10 shuffles of 20 items, it's virtually impossible they are all identical
        assertTrue(results.distinct().size > 1)
    }

    @Test
    fun `shuffle with count of 0 returns empty list`() {
        val result = shuffler.shuffle(0)
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `shuffle with count of 1 returns single element`() {
        val result = shuffler.shuffle(1)
        assertEquals(listOf(0), result)
    }
}
