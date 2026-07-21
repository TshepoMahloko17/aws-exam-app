package com.awsquiz.app.ui

import androidx.lifecycle.SavedStateHandle
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

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeQuestionRepository: FakeQuestionRepository
    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var fakeScoreHistoryRepository: FakeScoreHistoryRepository

    private val testQuestions = listOf(
        Question(
            id = 1,
            text = "Which AWS service provides a fully managed NoSQL database?",
            options = listOf("Amazon RDS", "Amazon DynamoDB", "Amazon Redshift", "Amazon Aurora"),
            correctAnswers = setOf(1),
            explanation = "DynamoDB is AWS's fully managed NoSQL database service."
        ),
        Question(
            id = 2,
            text = "Which services can be used for messaging?",
            options = listOf("Amazon SQS", "Amazon SNS", "Amazon S3", "Amazon RDS"),
            correctAnswers = setOf(0, 1),
            explanation = "SQS and SNS are messaging services."
        ),
        Question(
            id = 3,
            text = "What is the default compute service?",
            options = listOf("Lambda", "EC2", "ECS", "Fargate"),
            correctAnswers = setOf(1),
            explanation = "EC2 is the default compute service."
        )
    )

    private val testSessionId = "test-session-123"

    private val testSession = Session(
        id = testSessionId,
        questionOrder = listOf(1, 2, 3),
        currentIndex = 0,
        answers = emptyMap(),
        isCompleted = false,
        createdAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQuestionRepository = FakeQuestionRepository()
        fakeSessionRepository = FakeSessionRepository()
        fakeScoreHistoryRepository = FakeScoreHistoryRepository()

        fakeQuestionRepository.questionsResult = Result.success(testQuestions)
        fakeSessionRepository.incompleteSession = testSession
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sessionId: String = testSessionId): QuizViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId))
        return QuizViewModel(
            savedStateHandle = savedStateHandle,
            sessionRepository = fakeSessionRepository,
            questionRepository = fakeQuestionRepository,
            scoreHistoryRepository = fakeScoreHistoryRepository
        )
    }

    // --- Initial State Tests ---

    @Test
    fun `initial state has no options selected and submit button disabled`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Active)
        val activeState = state as QuizUiState.Active

        assertEquals(emptySet<Int>(), activeState.selectedOptions)
        assertFalse(activeState.canSubmit)
        assertFalse(activeState.isSubmitted)
        assertNull(activeState.answerResult)
    }

    @Test
    fun `initial state shows first question with correct navigation`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertEquals(testQuestions[0], state.question)
        assertFalse(state.canGoPrevious)
        assertTrue(state.canGoNext)
        assertEquals(1, state.currentQuestionNumber)
        assertEquals(3, state.totalQuestions)
    }

    @Test
    fun `initial state displays correct progress text`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertEquals("Question 1 of 3", state.progress)
    }

    // --- Answer Submission Tests ---

    @Test
    fun `submitting correct answer shows success feedback`() = runTest {
        val viewModel = createViewModel()

        // Select the correct option for question 1 (DynamoDB at index 1)
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertTrue(state.isSubmitted)
        assertNotNull(state.answerResult)
        assertTrue(state.answerResult!!.isCorrect)
        assertEquals(setOf(1), state.answerResult!!.selectedOptions)
        assertEquals(setOf(1), state.answerResult!!.correctOptions)
    }

    @Test
    fun `submitting incorrect answer shows failure with correct options`() = runTest {
        val viewModel = createViewModel()

        // Select incorrect option for question 1 (RDS at index 0)
        viewModel.selectOption(0)
        viewModel.submitAnswer()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertTrue(state.isSubmitted)
        assertNotNull(state.answerResult)
        assertFalse(state.answerResult!!.isCorrect)
        assertEquals(setOf(0), state.answerResult!!.selectedOptions)
        assertEquals(setOf(1), state.answerResult!!.correctOptions)
    }

    @Test
    fun `submit button disabled after answer is submitted`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(1)
        viewModel.submitAnswer()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertFalse(state.canSubmit)
    }

    @Test
    fun `selecting option enables submit button`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(0)

        val state = viewModel.uiState.value as QuizUiState.Active
        assertTrue(state.canSubmit)
        assertEquals(setOf(0), state.selectedOptions)
    }

    @Test
    fun `cannot modify selection after submission`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Attempt to change selection after submit
        viewModel.selectOption(0)

        val state = viewModel.uiState.value as QuizUiState.Active
        // Selection should still reflect submitted answer
        assertEquals(setOf(1), state.selectedOptions)
    }

    // --- Navigation Tests ---

    @Test
    fun `previous button hidden on first question`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertFalse(state.canGoPrevious)
    }

    @Test
    fun `next button hidden on last question`() = runTest {
        // Start session at the last question
        val sessionAtLast = testSession.copy(currentIndex = 2)
        fakeSessionRepository.incompleteSession = sessionAtLast

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertFalse(state.canGoNext)
        assertTrue(state.canGoPrevious)
    }

    @Test
    fun `navigating to next question updates state correctly`() = runTest {
        val viewModel = createViewModel()

        // Answer first question to enable meaningful navigation
        viewModel.selectOption(1)
        viewModel.submitAnswer()
        viewModel.goToNext()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertEquals(testQuestions[1], state.question)
        assertEquals(2, state.currentQuestionNumber)
        assertTrue(state.canGoPrevious)
        assertTrue(state.canGoNext)
        assertEquals("Question 2 of 3", state.progress)
    }

    @Test
    fun `navigating to previous question works correctly`() = runTest {
        // Start at second question
        val sessionAtSecond = testSession.copy(currentIndex = 1)
        fakeSessionRepository.incompleteSession = sessionAtSecond

        val viewModel = createViewModel()

        viewModel.goToPrevious()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertEquals(testQuestions[0], state.question)
        assertEquals(1, state.currentQuestionNumber)
        assertFalse(state.canGoPrevious)
    }

    @Test
    fun `middle question shows both navigation buttons`() = runTest {
        val sessionAtMiddle = testSession.copy(currentIndex = 1)
        fakeSessionRepository.incompleteSession = sessionAtMiddle

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertTrue(state.canGoPrevious)
        assertTrue(state.canGoNext)
    }

    // --- Answered Question Revisit Tests ---

    @Test
    fun `navigating back to answered question shows read-only state with feedback`() = runTest {
        val viewModel = createViewModel()

        // Answer first question correctly
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Navigate to next question
        viewModel.goToNext()

        // Navigate back to first question
        viewModel.goToPrevious()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertTrue(state.isSubmitted)
        assertFalse(state.canSubmit)
        assertNotNull(state.answerResult)
        assertTrue(state.answerResult!!.isCorrect)
        assertEquals(setOf(1), state.selectedOptions)
    }

    @Test
    fun `revisiting answered question shows previously selected options`() = runTest {
        val viewModel = createViewModel()

        // Answer first question incorrectly
        viewModel.selectOption(0)
        viewModel.submitAnswer()

        // Navigate away and back
        viewModel.goToNext()
        viewModel.goToPrevious()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertTrue(state.isSubmitted)
        assertEquals(setOf(0), state.answerResult!!.selectedOptions)
        assertEquals(setOf(1), state.answerResult!!.correctOptions)
        assertFalse(state.answerResult!!.isCorrect)
    }

    @Test
    fun `unanswered question shows default unselected state`() = runTest {
        val viewModel = createViewModel()

        // Answer first question
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Navigate to unanswered second question
        viewModel.goToNext()

        val state = viewModel.uiState.value as QuizUiState.Active

        assertFalse(state.isSubmitted)
        assertNull(state.answerResult)
        assertEquals(emptySet<Int>(), state.selectedOptions)
        assertTrue(state.canSubmit.not()) // no selection yet
    }

    // --- Session Completion Tests ---

    @Test
    fun `completing all questions transitions to Completed state with score`() = runTest {
        // Session with only 2 questions for simpler test
        val twoQuestionSession = Session(
            id = testSessionId,
            questionOrder = listOf(1, 3),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        fakeSessionRepository.incompleteSession = twoQuestionSession

        val viewModel = createViewModel()

        // Answer first question correctly (question id=1, correct=1)
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Navigate to second question
        viewModel.goToNext()

        // Answer second question correctly (question id=3, correct=1)
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Completed)
        val completedState = state as QuizUiState.Completed

        assertEquals(2, completedState.scoreResult.correctCount)
        assertEquals(2, completedState.scoreResult.totalCount)
        assertEquals(100, completedState.scoreResult.percentage)
        assertTrue(completedState.scoreResult.passed)
    }

    @Test
    fun `partial correct answers show correct score on completion`() = runTest {
        val twoQuestionSession = Session(
            id = testSessionId,
            questionOrder = listOf(1, 3),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        fakeSessionRepository.incompleteSession = twoQuestionSession

        val viewModel = createViewModel()

        // Answer first question correctly
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Navigate to second and answer incorrectly
        viewModel.goToNext()
        viewModel.selectOption(0) // wrong answer
        viewModel.submitAnswer()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Completed)
        val completedState = state as QuizUiState.Completed

        assertEquals(1, completedState.scoreResult.correctCount)
        assertEquals(2, completedState.scoreResult.totalCount)
        assertEquals(50, completedState.scoreResult.percentage)
        assertFalse(completedState.scoreResult.passed)
    }

    @Test
    fun `session completion saves score to history`() = runTest {
        val twoQuestionSession = Session(
            id = testSessionId,
            questionOrder = listOf(1, 3),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        fakeSessionRepository.incompleteSession = twoQuestionSession

        val viewModel = createViewModel()

        // Answer both questions
        viewModel.selectOption(1)
        viewModel.submitAnswer()
        viewModel.goToNext()
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Verify score was saved
        assertTrue(fakeScoreHistoryRepository.savedScores.isNotEmpty())
        val savedScore = fakeScoreHistoryRepository.savedScores.first()
        assertEquals(100, savedScore.percentage)
        assertTrue(savedScore.passed)
    }

    @Test
    fun `session completion marks session as completed`() = runTest {
        val twoQuestionSession = Session(
            id = testSessionId,
            questionOrder = listOf(1, 3),
            currentIndex = 0,
            answers = emptyMap(),
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        fakeSessionRepository.incompleteSession = twoQuestionSession

        val viewModel = createViewModel()

        viewModel.selectOption(1)
        viewModel.submitAnswer()
        viewModel.goToNext()
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        assertTrue(fakeSessionRepository.completedSessionIds.contains(testSessionId))
    }

    // --- Progress Display Tests ---

    @Test
    fun `progress shows Question X of Y format`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as QuizUiState.Active
        assertEquals("Question 1 of 3", state.progress)

        viewModel.selectOption(1)
        viewModel.submitAnswer()
        viewModel.goToNext()

        val nextState = viewModel.uiState.value as QuizUiState.Active
        assertEquals("Question 2 of 3", nextState.progress)
    }

    // --- Error State Tests ---

    @Test
    fun `error state when session not found`() = runTest {
        fakeSessionRepository.incompleteSession = null

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Error)
    }

    @Test
    fun `error state when questions fail to load`() = runTest {
        fakeQuestionRepository.questionsResult = Result.failure(RuntimeException("File not found"))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Error)
        val errorState = state as QuizUiState.Error
        assertTrue(errorState.error is AppError.QuestionLoadError)
    }

    // --- Fake Implementations ---

    private class FakeQuestionRepository : QuestionRepository {
        var questionsResult: Result<List<Question>> = Result.success(emptyList())

        override suspend fun loadQuestions(): Result<List<Question>> = questionsResult
    }

    private class FakeSessionRepository : SessionRepository {
        var incompleteSession: Session? = null
        val savedSessions = mutableListOf<Session>()
        val completedSessionIds = mutableListOf<String>()

        override suspend fun saveSession(session: Session): Result<Unit> {
            savedSessions.add(session)
            return Result.success(Unit)
        }

        override suspend fun loadIncompleteSession(): Result<Session?> {
            return Result.success(incompleteSession)
        }

        override suspend fun deleteSession(sessionId: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun markSessionCompleted(sessionId: String): Result<Unit> {
            completedSessionIds.add(sessionId)
            return Result.success(Unit)
        }
    }

    private class FakeScoreHistoryRepository : ScoreHistoryRepository {
        val savedScores = mutableListOf<ScoreHistoryRecord>()

        override suspend fun saveScore(scoreRecord: ScoreHistoryRecord): Result<Unit> {
            savedScores.add(scoreRecord)
            return Result.success(Unit)
        }

        override suspend fun getAllScores(): Result<List<ScoreHistoryRecord>> {
            return Result.success(savedScores.toList())
        }

        override suspend fun getRecentScores(count: Int): Result<List<ScoreHistoryRecord>> {
            return Result.success(savedScores.take(count))
        }
    }
}
