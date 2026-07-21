package com.awsquiz.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awsquiz.app.domain.AppError
import com.awsquiz.app.domain.QuestionRepository
import com.awsquiz.app.domain.QuestionShuffler
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

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Ready(
        val hasIncompleteSession: Boolean,
        val canStartNew: Boolean
    ) : HomeUiState()
    data class Error(val error: AppError) : HomeUiState()
    data class ShowConfirmDialog(
        val hasIncompleteSession: Boolean,
        val canStartNew: Boolean
    ) : HomeUiState()
}

sealed class HomeNavigationEvent {
    data class NavigateToQuiz(val sessionId: String) : HomeNavigationEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val questionRepository: QuestionRepository,
    private val questionShuffler: QuestionShuffler
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<HomeNavigationEvent>()
    val navigationEvents: SharedFlow<HomeNavigationEvent> = _navigationEvents.asSharedFlow()

    private var incompleteSession: Session? = null
    private var questionCount: Int = 0

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val questionsResult = questionRepository.loadQuestions()
            questionsResult.fold(
                onSuccess = { questions ->
                    if (questions.size < 2) {
                        _uiState.value = HomeUiState.Error(
                            AppError.InsufficientQuestionsError(available = questions.size)
                        )
                        return@launch
                    }
                    questionCount = questions.size

                    val sessionResult = sessionRepository.loadIncompleteSession()
                    sessionResult.fold(
                        onSuccess = { session ->
                            incompleteSession = session
                            _uiState.value = HomeUiState.Ready(
                                hasIncompleteSession = session != null,
                                canStartNew = true
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = HomeUiState.Error(
                                AppError.SessionCorruptionError(
                                    error.message ?: "Failed to load session"
                                )
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(
                        AppError.QuestionLoadError(
                            error.message ?: "Failed to load questions"
                        )
                    )
                }
            )
        }
    }

    fun startNewSession() {
        if (incompleteSession != null) {
            val currentState = _uiState.value
            if (currentState is HomeUiState.Ready) {
                _uiState.value = HomeUiState.ShowConfirmDialog(
                    hasIncompleteSession = currentState.hasIncompleteSession,
                    canStartNew = currentState.canStartNew
                )
            }
        } else {
            createNewSession()
        }
    }

    fun confirmStartNew() {
        viewModelScope.launch {
            val oldSession = incompleteSession
            if (oldSession != null) {
                sessionRepository.deleteSession(oldSession.id)
            }
            incompleteSession = null
            createNewSession()
        }
    }

    fun dismissDialog() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.ShowConfirmDialog) {
            _uiState.value = HomeUiState.Ready(
                hasIncompleteSession = currentState.hasIncompleteSession,
                canStartNew = currentState.canStartNew
            )
        }
    }

    fun resumeSession() {
        val session = incompleteSession ?: return
        viewModelScope.launch {
            _navigationEvents.emit(HomeNavigationEvent.NavigateToQuiz(session.id))
        }
    }

    private fun createNewSession() {
        viewModelScope.launch {
            val shuffledOrder = questionShuffler.shuffle(questionCount)
            val newSession = Session(
                id = UUID.randomUUID().toString(),
                questionOrder = shuffledOrder,
                currentIndex = 0,
                answers = emptyMap(),
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
            val saveResult = saveSessionWithRetry(newSession)
            saveResult.fold(
                onSuccess = {
                    incompleteSession = newSession
                    _navigationEvents.emit(HomeNavigationEvent.NavigateToQuiz(newSession.id))
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(
                        AppError.SessionCorruptionError(
                            error.message ?: "Failed to save session"
                        )
                    )
                }
            )
        }
    }

    private suspend fun saveSessionWithRetry(session: Session): Result<Unit> {
        val firstAttempt = sessionRepository.saveSession(session)
        if (firstAttempt.isSuccess) return firstAttempt
        return sessionRepository.saveSession(session)
    }
}
