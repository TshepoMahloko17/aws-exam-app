package com.awsquiz.app.ui.quiz

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awsquiz.app.domain.AnswerEvaluator
import com.awsquiz.app.domain.AnswerRecord
import com.awsquiz.app.domain.AnswerResult
import com.awsquiz.app.domain.AppError
import com.awsquiz.app.domain.Question
import com.awsquiz.app.domain.QuestionRepository
import com.awsquiz.app.domain.ScoreHistoryRecord
import com.awsquiz.app.domain.ScoreHistoryRepository
import com.awsquiz.app.domain.ScoreResult
import com.awsquiz.app.domain.ScoreTracker
import com.awsquiz.app.domain.Session
import com.awsquiz.app.domain.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class QuizUiState {
    data object Loading : QuizUiState()

    data class Active(
        val question: Question,
        val selectedOptions: Set<Int>,
        val isSubmitted: Boolean,
        val answerResult: AnswerResult?,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
        val currentQuestionNumber: Int,
        val totalQuestions: Int
    ) : QuizUiState() {
        val canSubmit: Boolean get() = selectedOptions.isNotEmpty() && !isSubmitted
        val progress: String get() = "Question $currentQuestionNumber of $totalQuestions"
    }

    data class Completed(val scoreResult: ScoreResult) : QuizUiState()

    data class Error(val error: AppError) : QuizUiState()
}

sealed class QuizEvent {
    data class ShowToast(val message: String) : QuizEvent()
}

private const val TAG = "QuizViewModel"

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val questionRepository: QuestionRepository,
    private val scoreHistoryRepository: ScoreHistoryRepository
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuizEvent>()
    val events: SharedFlow<QuizEvent> = _events.asSharedFlow()

    private var session: Session? = null
    private var questions: List<Question> = emptyList()
    private var currentSelections: MutableMap<Int, Set<Int>> = mutableMapOf()

    init {
        loadSessionAndQuestions()
    }

    private fun loadSessionAndQuestions() {
        viewModelScope.launch {
            val questionsResult = questionRepository.loadQuestions()
            questionsResult.fold(
                onSuccess = { loadedQuestions ->
                    questions = loadedQuestions
                    loadSession()
                },
                onFailure = { error ->
                    _uiState.value = QuizUiState.Error(
                        AppError.QuestionLoadError(
                            error.message ?: "Failed to load questions"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadSession() {
        val sessionResult = sessionRepository.getSession(sessionId)
        sessionResult.fold(
            onSuccess = { loadedSession ->
                if (loadedSession == null) {
                    _uiState.value = QuizUiState.Error(
                        AppError.SessionCorruptionError("Session not found")
                    )
                    return
                }
                session = loadedSession
                updateActiveState()
            },
            onFailure = { error ->
                _uiState.value = QuizUiState.Error(
                    AppError.SessionCorruptionError(
                        error.message ?: "Failed to load session"
                    )
                )
            }
        )
    }

    fun selectOption(optionIndex: Int) {
        val currentSession = session ?: return
        val currentState = _uiState.value as? QuizUiState.Active ?: return
        if (currentState.isSubmitted) return

        val questionIndex = currentSession.currentIndex
        val question = currentState.question

        val currentSelected = currentSelections[questionIndex] ?: emptySet()
        val newSelected = if (question.isSingleAnswer) {
            setOf(optionIndex)
        } else {
            if (currentSelected.contains(optionIndex)) {
                currentSelected - optionIndex
            } else {
                currentSelected + optionIndex
            }
        }

        currentSelections[questionIndex] = newSelected
        updateActiveState()
    }

    fun submitAnswer() {
        val currentSession = session ?: return
        val currentState = _uiState.value as? QuizUiState.Active ?: return
        if (!currentState.canSubmit) return

        val questionIndex = currentSession.currentIndex
        val questionId = currentSession.questionOrder[questionIndex]
        val question = questions.find { it.id == questionId } ?: return
        val selectedOptions = currentSelections[questionIndex] ?: return

        val result = AnswerEvaluator.evaluate(question, selectedOptions)
        val answerRecord = AnswerRecord(
            selectedOptions = result.selectedOptions,
            isCorrect = result.isCorrect
        )

        val updatedAnswers = currentSession.answers + (questionIndex to answerRecord)
        val updatedSession = currentSession.copy(answers = updatedAnswers)
        session = updatedSession

        viewModelScope.launch {
            val saveResult = saveSessionWithRetry(updatedSession)
            if (saveResult.isFailure) {
                _uiState.value = QuizUiState.Error(
                    AppError.SessionCorruptionError(
                        saveResult.exceptionOrNull()?.message ?: "Failed to save session"
                    )
                )
                return@launch
            }

            if (updatedAnswers.size == currentSession.questionOrder.size) {
                completeSession(updatedSession)
            } else {
                updateActiveState()
            }
        }
    }

    fun goToPrevious() {
        val currentSession = session ?: return
        if (currentSession.currentIndex <= 0) return

        val updatedSession = currentSession.copy(currentIndex = currentSession.currentIndex - 1)
        session = updatedSession
        updateActiveState()

        viewModelScope.launch {
            val saveResult = saveSessionWithRetry(updatedSession)
            if (saveResult.isFailure) {
                Log.w(TAG, "Failed to save navigation state: ${saveResult.exceptionOrNull()?.message}")
            }
        }
    }

    fun goToNext() {
        val currentSession = session ?: return
        if (currentSession.currentIndex >= currentSession.questionOrder.size - 1) return

        val updatedSession = currentSession.copy(currentIndex = currentSession.currentIndex + 1)
        session = updatedSession
        updateActiveState()

        viewModelScope.launch {
            val saveResult = saveSessionWithRetry(updatedSession)
            if (saveResult.isFailure) {
                Log.w(TAG, "Failed to save navigation state: ${saveResult.exceptionOrNull()?.message}")
            }
        }
    }

    private fun updateActiveState() {
        val currentSession = session ?: return
        val questionIndex = currentSession.currentIndex
        val questionId = currentSession.questionOrder[questionIndex]
        val question = questions.find { it.id == questionId }

        if (question == null) {
            _uiState.value = QuizUiState.Error(
                AppError.SessionCorruptionError("Question not found for id: $questionId")
            )
            return
        }

        val existingAnswer = currentSession.answers[questionIndex]
        val isSubmitted = existingAnswer != null
        val selectedOptions = if (isSubmitted) {
            existingAnswer!!.selectedOptions
        } else {
            currentSelections[questionIndex] ?: emptySet()
        }

        val answerResult = if (isSubmitted) {
            AnswerResult(
                isCorrect = existingAnswer!!.isCorrect,
                selectedOptions = existingAnswer.selectedOptions,
                correctOptions = question.correctAnswers
            )
        } else {
            null
        }

        _uiState.value = QuizUiState.Active(
            question = question,
            selectedOptions = selectedOptions,
            isSubmitted = isSubmitted,
            answerResult = answerResult,
            canGoPrevious = questionIndex > 0,
            canGoNext = questionIndex < currentSession.questionOrder.size - 1,
            currentQuestionNumber = questionIndex + 1,
            totalQuestions = currentSession.questionOrder.size
        )
    }

    private suspend fun completeSession(completedSession: Session) {
        sessionRepository.markSessionCompleted(completedSession.id)

        val answerResults = completedSession.questionOrder.mapIndexed { index, questionId ->
            val answer = completedSession.answers[index]!!
            val question = questions.find { it.id == questionId }!!
            AnswerResult(
                isCorrect = answer.isCorrect,
                selectedOptions = answer.selectedOptions,
                correctOptions = question.correctAnswers
            )
        }

        val scoreResult = ScoreTracker.calculateScore(
            answerResults,
            completedSession.questionOrder.size
        )

        val scoreRecord = ScoreHistoryRecord(
            id = UUID.randomUUID().toString(),
            completionDate = System.currentTimeMillis(),
            percentage = scoreResult.percentage,
            passed = scoreResult.passed
        )

        val saveScoreResult = scoreHistoryRepository.saveScore(scoreRecord)
        if (saveScoreResult.isFailure) {
            Log.e(TAG, "Failed to save score history: ${saveScoreResult.exceptionOrNull()?.message}")
            _events.emit(QuizEvent.ShowToast("Score history could not be saved"))
        }

        _uiState.value = QuizUiState.Completed(scoreResult)
    }

    /**
     * Attempts to save session, retrying once on failure.
     */
    private suspend fun saveSessionWithRetry(session: Session): Result<Unit> {
        val firstAttempt = sessionRepository.saveSession(session)
        if (firstAttempt.isSuccess) return firstAttempt

        Log.w(TAG, "First save attempt failed, retrying: ${firstAttempt.exceptionOrNull()?.message}")
        return sessionRepository.saveSession(session)
    }
}
