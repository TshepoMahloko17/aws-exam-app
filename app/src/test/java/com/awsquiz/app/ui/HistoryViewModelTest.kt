package com.awsquiz.app.ui

import com.awsquiz.app.domain.ProgressTrend
import com.awsquiz.app.domain.ScoreHistoryRecord
import com.awsquiz.app.domain.ScoreHistoryRepository
import com.awsquiz.app.ui.history.HistoryUiState
import com.awsquiz.app.ui.history.HistoryViewModel
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
class HistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeScoreHistoryRepository: FakeScoreHistoryRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeScoreHistoryRepository = FakeScoreHistoryRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            scoreHistoryRepository = fakeScoreHistoryRepository
        )
    }

    // --- Empty History Tests ---

    @Test
    fun `empty history shows loaded state with isEmpty true`() = runTest {
        fakeScoreHistoryRepository.allScoresResult = Result.success(emptyList())
        fakeScoreHistoryRepository.recentScoresResult = Result.success(emptyList())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Loaded)
        val loaded = state as HistoryUiState.Loaded

        assertTrue(loaded.isEmpty)
        assertTrue(loaded.scores.isEmpty())
        assertEquals(ProgressTrend.STABLE, loaded.trend)
    }

    // --- Loaded History Tests ---

    @Test
    fun `loaded history shows scores from repository in descending date order`() = runTest {
        val scores = listOf(
            ScoreHistoryRecord(id = "1", completionDate = 3000L, percentage = 85, passed = true),
            ScoreHistoryRecord(id = "2", completionDate = 2000L, percentage = 72, passed = true),
            ScoreHistoryRecord(id = "3", completionDate = 1000L, percentage = 60, passed = false)
        )
        fakeScoreHistoryRepository.allScoresResult = Result.success(scores)
        fakeScoreHistoryRepository.recentScoresResult = Result.success(scores)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Loaded)
        val loaded = state as HistoryUiState.Loaded

        assertFalse(loaded.isEmpty)
        assertEquals(3, loaded.scores.size)
        // Verify descending date order (most recent first)
        assertEquals(3000L, loaded.scores[0].completionDate)
        assertEquals(2000L, loaded.scores[1].completionDate)
        assertEquals(1000L, loaded.scores[2].completionDate)
    }

    // --- Trend Calculation Tests ---

    @Test
    fun `trend is IMPROVING when scores have positive slope greater than 2`() = runTest {
        // Recent scores returned newest-first: 90, 80, 70, 60, 50
        // ViewModel reverses to oldest-first: 50, 60, 70, 80, 90 → slope > 2 → IMPROVING
        val recentScores = listOf(
            ScoreHistoryRecord(id = "5", completionDate = 5000L, percentage = 90, passed = true),
            ScoreHistoryRecord(id = "4", completionDate = 4000L, percentage = 80, passed = true),
            ScoreHistoryRecord(id = "3", completionDate = 3000L, percentage = 70, passed = false),
            ScoreHistoryRecord(id = "2", completionDate = 2000L, percentage = 60, passed = false),
            ScoreHistoryRecord(id = "1", completionDate = 1000L, percentage = 50, passed = false)
        )
        fakeScoreHistoryRepository.allScoresResult = Result.success(recentScores)
        fakeScoreHistoryRepository.recentScoresResult = Result.success(recentScores)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Loaded)
        val loaded = state as HistoryUiState.Loaded

        assertEquals(ProgressTrend.IMPROVING, loaded.trend)
    }

    @Test
    fun `trend is DECLINING when scores have negative slope less than minus 2`() = runTest {
        // Recent scores returned newest-first: 50, 60, 70, 80, 90
        // ViewModel reverses to oldest-first: 90, 80, 70, 60, 50 → slope < -2 → DECLINING
        val recentScores = listOf(
            ScoreHistoryRecord(id = "5", completionDate = 5000L, percentage = 50, passed = false),
            ScoreHistoryRecord(id = "4", completionDate = 4000L, percentage = 60, passed = false),
            ScoreHistoryRecord(id = "3", completionDate = 3000L, percentage = 70, passed = false),
            ScoreHistoryRecord(id = "2", completionDate = 2000L, percentage = 80, passed = true),
            ScoreHistoryRecord(id = "1", completionDate = 1000L, percentage = 90, passed = true)
        )
        fakeScoreHistoryRepository.allScoresResult = Result.success(recentScores)
        fakeScoreHistoryRepository.recentScoresResult = Result.success(recentScores)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Loaded)
        val loaded = state as HistoryUiState.Loaded

        assertEquals(ProgressTrend.DECLINING, loaded.trend)
    }

    @Test
    fun `trend is STABLE when scores have slope between minus 2 and 2`() = runTest {
        // Recent scores returned newest-first: 70, 70, 71, 70, 70
        // ViewModel reverses to oldest-first: 70, 70, 71, 70, 70 → slope ≈ 0 → STABLE
        val recentScores = listOf(
            ScoreHistoryRecord(id = "5", completionDate = 5000L, percentage = 70, passed = false),
            ScoreHistoryRecord(id = "4", completionDate = 4000L, percentage = 70, passed = false),
            ScoreHistoryRecord(id = "3", completionDate = 3000L, percentage = 71, passed = false),
            ScoreHistoryRecord(id = "2", completionDate = 2000L, percentage = 70, passed = false),
            ScoreHistoryRecord(id = "1", completionDate = 1000L, percentage = 70, passed = false)
        )
        fakeScoreHistoryRepository.allScoresResult = Result.success(recentScores)
        fakeScoreHistoryRepository.recentScoresResult = Result.success(recentScores)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Loaded)
        val loaded = state as HistoryUiState.Loaded

        assertEquals(ProgressTrend.STABLE, loaded.trend)
    }

    // --- Error State Tests ---

    @Test
    fun `error state when getAllScores returns failure`() = runTest {
        fakeScoreHistoryRepository.allScoresResult =
            Result.failure(RuntimeException("Database error"))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Error)
        val errorState = state as HistoryUiState.Error
        assertEquals("Database error", errorState.message)
    }

    // --- Retry Tests ---

    @Test
    fun `retry after error reloads and transitions to loaded state`() = runTest {
        // Start with error
        fakeScoreHistoryRepository.allScoresResult =
            Result.failure(RuntimeException("Network error"))

        val viewModel = createViewModel()

        // Verify error state
        assertTrue(viewModel.uiState.value is HistoryUiState.Error)

        // Fix the repository and retry
        val scores = listOf(
            ScoreHistoryRecord(id = "1", completionDate = 1000L, percentage = 75, passed = true)
        )
        fakeScoreHistoryRepository.allScoresResult = Result.success(scores)
        fakeScoreHistoryRepository.recentScoresResult = Result.success(scores)

        viewModel.retry()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Loaded)
        val loaded = state as HistoryUiState.Loaded
        assertFalse(loaded.isEmpty)
        assertEquals(1, loaded.scores.size)
        assertEquals(75, loaded.scores[0].percentage)
    }

    // --- Fake Implementation ---

    private class FakeScoreHistoryRepository : ScoreHistoryRepository {
        var allScoresResult: Result<List<ScoreHistoryRecord>> = Result.success(emptyList())
        var recentScoresResult: Result<List<ScoreHistoryRecord>> = Result.success(emptyList())

        override suspend fun saveScore(scoreRecord: ScoreHistoryRecord): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun getAllScores(): Result<List<ScoreHistoryRecord>> {
            return allScoresResult
        }

        override suspend fun getRecentScores(count: Int): Result<List<ScoreHistoryRecord>> {
            return recentScoresResult
        }
    }
}
