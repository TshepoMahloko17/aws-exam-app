package com.awsquiz.app.domain

import kotlin.random.Random

/**
 * Produces a randomized permutation of question indices for each new session.
 * Stateless — receives a count and returns a shuffled index list.
 */
interface QuestionShuffler {

    /**
     * Returns a shuffled list of indices [0, 1, ..., questionCount - 1].
     *
     * @param questionCount The total number of questions to shuffle
     * @return A list containing each index exactly once in randomized order
     */
    fun shuffle(questionCount: Int): List<Int>
}

/**
 * Default implementation of [QuestionShuffler] using [kotlin.random.Random]
 * with a time-based seed for non-deterministic shuffling.
 */
class DefaultQuestionShuffler : QuestionShuffler {

    override fun shuffle(questionCount: Int): List<Int> {
        val random = Random(System.nanoTime())
        return (0 until questionCount).shuffled(random)
    }
}
