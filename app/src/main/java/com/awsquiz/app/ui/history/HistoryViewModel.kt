package com.awsquiz.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awsquiz.app.domain.ProgressTrend
import com.awsquiz.app.domain.ProgressTrendCalculator
import com.awsquiz.app.domain.ScoreHistoryRecord
import com.awsquiz.app.domain.ScoreHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Loaded(
        val scores: List<ScoreHistoryRecord>,
        val trend: ProgressTrend,
        val isEmpty: Boolean
    ) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val scoreHistoryRepository: ScoreHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun retry() {
        _uiState.value = HistoryUiState.Loading
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val allScoresResult = scoreHistoryRepository.getAllScores()
            allScoresResult.fold(
                onSuccess = { scores ->
                    if (scores.isEmpty()) {
                        _uiState.value = HistoryUiState.Loaded(
                            scores = emptyList(),
                            trend = ProgressTrend.STABLE,
                            isEmpty = true
                        )
                    } else {
                        val recentScoresResult = scoreHistoryRepository.getRecentScores(5)
                        val trend = recentScoresResult.fold(
                            onSuccess = { recentScores ->
                                // getRecentScores returns most recent first,
                                // but ProgressTrendCalculator expects oldest first
                                val percentages = recentScores.map { it.percentage }.reversed()
                                ProgressTrendCalculator.calculateTrend(percentages)
                            },
                            onFailure = {
                                ProgressTrend.STABLE
                            }
                        )
                        _uiState.value = HistoryUiState.Loaded(
                            scores = scores,
                            trend = trend,
                            isEmpty = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = HistoryUiState.Error(
                        error.message ?: "Failed to load score history"
                    )
                }
            )
        }
    }
}
