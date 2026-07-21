package com.awsquiz.app.integration

import androidx.lifecycle.SavedStateHandle
import com.awsquiz.app.data.QuestionParser
import com.awsquiz.app.domain.*
import com.awsquiz.app.ui.quiz.QuizUiState
import com.awsquiz.app.ui.quiz.QuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for the full session lifecycle.
 * Tests end-to-end flows: session creation, answering, completion, persistence, and history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionLifecycleIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var questionRepository: InMemoryQuestionRepository
    private lateinit var sessionRepository: InMemorySessionRepository
    private lateinit var scoreHistoryRepository: InMemoryScoreHistoryRepository

    private val testQuestions = listOf(
        Question(id = 1, text = "Q1", options = listOf("A", "B", "C", "D"), correctAnswers = setOf(0), explanation = "A is correct"),
        Question(id = 2, text = "Q2", options = listOf("A", "B", "C", "D"), correctAnswers = setOf(1), explanation = "B is correct"),
        Question(id = 3, text = "Q3", options = listOf("A", "B", "C"), correctAnswers = setOf(0, 2), explanation = "A and C are correct")
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        questionRepository = InMemoryQuestionRepository(testQuestions)
        sessionRepository = InMemorySessionRepository()
        scoreHistoryRepository = InMemoryScoreHistoryRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSession(questionOrder: List<Int> = listOf(1, 2, 3)): Session {
        val session = Session(
            id = "session-1",
            questionOrder = questionOrder,
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        return session
    }

    private fun createViewModel(sessionId: String = "session-1"): QuizViewModel {
        return QuizViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId)),
            sessionRepository = sessionRepository,
            questionRepository = questionRepository,
            scoreHistoryRepository = scoreHistoryRepository
        )
    }

    // --- Test: Full session lifecycle ---

    @Test
    fun `create session, answer all questions, complete, verify result and score history`() = runTest {
        val session = createSession(listOf(1, 2, 3))
        sessionRepository.saveSession(session)

        val viewModel = createViewModel()

        // Answer Q1 correctly (correct = 0)
        viewModel.selectOption(0)
        viewModel.submitAnswer()
        viewModel.goToNext()

        // Answer Q2 correctly (correct = 1)
        viewModel.selectOption(1)
        viewModel.submitAnswer()
        viewModel.goToNext()

        // Answer Q3 correctly (correct = 0, 2)
        viewModel.selectOption(0)
        viewModel.selectOption(2)
        viewModel.submitAnswer()

        // Verify completed state
        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Completed)
        val completed = state as QuizUiState.Completed
        assertEquals(3, completed.scoreResult.correctCount)
        assertEquals(3, completed.scoreResult.totalCount)
        assertEquals(100, completed.scoreResult.percentage)
        assertTrue(completed.scoreResult.passed)

        // Verify score history was saved
        val scores = scoreHistoryRepository.getAllScores().getOrThrow()
        assertEquals(1, scores.size)
        assertEquals(100, scores[0].percentage)
        assertTrue(scores[0].passed)

        // Verify session was marked completed
        val loadedSession = sessionRepository.loadIncompleteSession().getOrThrow()
        assertNull(loadedSession) // No incomplete session remaining
    }

    // --- Test: Partial answer, close, resume ---

    @Test
    fun `create session, answer partial, close, resume, verify state preserved`() = runTest {
        val session = createSession(listOf(1, 2, 3))
        sessionRepository.saveSession(session)

        // First view model - answer Q1 only
        val viewModel1 = createViewModel()
        viewModel1.selectOption(0) // correct for Q1
        viewModel1.submitAnswer()
        viewModel1.goToNext() // move to Q2

        // "Close" by dropping the view model. Session should be persisted.
        // Verify the session state was saved to repository
        val savedSession = sessionRepository.loadIncompleteSession().getOrThrow()
        assertNotNull(savedSession)
        assertEquals("session-1", savedSession!!.id)
        assertEquals(1, savedSession.currentIndex) // on Q2
        assertEquals(1, savedSession.answers.size) // Q1 answered
        assertTrue(savedSession.answers[0]!!.isCorrect)

        // Resume - create new view model with same session
        val viewModel2 = createViewModel()

        val state = viewModel2.uiState.value as QuizUiState.Active
        // Should be on Q2 (index 1)
        assertEquals(2, state.currentQuestionNumber)
        assertEquals(testQuestions[1], state.question) // Q2
        assertFalse(state.isSubmitted) // Q2 not yet answered

        // Navigate back to Q1 - should be read-only with previous answer
        viewModel2.goToPrevious()
        val q1State = viewModel2.uiState.value as QuizUiState.Active
        assertTrue(q1State.isSubmitted)
        assertTrue(q1State.answerResult!!.isCorrect)
    }

    // --- Test: Multiple sessions accumulate in history ---

    @Test
    fun `complete multiple sessions, verify history accumulates and ordering`() = runTest {
        // Complete first session - all correct
        val session1 = Session(
            id = "session-1",
            questionOrder = listOf(1, 2),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = 1000L
        )
        sessionRepository.saveSession(session1)

        val vm1 = createViewModel("session-1")
        vm1.selectOption(0) // Q1 correct
        vm1.submitAnswer()
        vm1.goToNext()
        vm1.selectOption(1) // Q2 correct
        vm1.submitAnswer()

        assertTrue(vm1.uiState.value is QuizUiState.Completed)

        // Complete second session - all wrong
        val session2 = Session(
            id = "session-2",
            questionOrder = listOf(1, 2),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = 2000L
        )
        sessionRepository.saveSession(session2)

        val vm2 = createViewModel("session-2")
        vm2.selectOption(3) // Q1 wrong
        vm2.submitAnswer()
        vm2.goToNext()
        vm2.selectOption(3) // Q2 wrong
        vm2.submitAnswer()

        assertTrue(vm2.uiState.value is QuizUiState.Completed)

        // Verify history has both records, ordered by date descending
        val scores = scoreHistoryRepository.getAllScores().getOrThrow()
        assertEquals(2, scores.size)
        // Most recent (session 2) first
        assertTrue(scores[0].completionDate >= scores[1].completionDate)
        assertEquals(0, scores[0].percentage) // all wrong
        assertEquals(100, scores[1].percentage) // all correct
    }

    // --- Test: JSON parsing performance ---

    @Test
    fun `JSON parsing completes within 3 seconds for 80 plus questions`() = runTest {
        // Generate a JSON string with 100 questions
        val questionsJson = buildString {
            append("""{"questions":[""")
            for (i in 1..100) {
                if (i > 1) append(",")
                append("""{"id":$i,"text":"Question $i about AWS service","options":["Option A","Option B","Option C","Option D"],"correctAnswers":[${i % 4}],"explanation":"Explanation for question $i"}""")
            }
            append("]}")
        }

        val startTime = System.currentTimeMillis()
        val result = QuestionParser.parseQuestions(questionsJson)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrThrow().size)
        assertTrue(elapsed < 3000, "Parsing took ${elapsed}ms, should be under 3000ms")
    }

    // --- In-memory test doubles ---

    private class InMemoryQuestionRepository(
        private val questions: List<Question>
    ) : QuestionRepository {
        override suspend fun loadQuestions(): Result<List<Question>> = Result.success(questions)
    }

    private class InMemorySessionRepository : SessionRepository {
        private val sessions = mutableMapOf<String, Session>()

        override suspend fun saveSession(session: Session): Result<Unit> {
            sessions[session.id] = session
            return Result.success(Unit)
        }

        override suspend fun loadIncompleteSession(): Result<Session?> {
            return Result.success(sessions.values.firstOrNull { !it.isCompleted })
        }

        override suspend fun deleteSession(sessionId: String): Result<Unit> {
            sessions.remove(sessionId)
            return Result.success(Unit)
        }

        override suspend fun markSessionCompleted(sessionId: String): Result<Unit> {
            val session = sessions[sessionId] ?: return Result.failure(IllegalStateException("Not found"))
            sessions[sessionId] = session.copy(isCompleted = true)
            return Result.success(Unit)
        }
    }

    private class InMemoryScoreHistoryRepository : ScoreHistoryRepository {
        private val scores = mutableListOf<ScoreHistoryRecord>()

        override suspend fun saveScore(scoreRecord: ScoreHistoryRecord): Result<Unit> {
            scores.add(scoreRecord)
            return Result.success(Unit)
        }

        override suspend fun getAllScores(): Result<List<ScoreHistoryRecord>> {
            return Result.success(scores.sortedByDescending { it.completionDate })
        }

        override suspend fun getRecentScores(count: Int): Result<List<ScoreHistoryRecord>> {
            return Result.success(scores.sortedByDescending { it.completionDate }.take(count))
        }
    }
}
