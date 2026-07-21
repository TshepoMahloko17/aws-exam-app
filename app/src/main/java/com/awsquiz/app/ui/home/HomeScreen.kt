package com.awsquiz.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awsquiz.app.domain.AppError

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is HomeNavigationEvent.NavigateToQuiz -> {
                    onNavigateToQuiz(event.sessionId)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.semantics {
                        contentDescription = "Loading"
                    }
                )
            }

            is HomeUiState.Ready -> {
                HomeReadyContent(
                    hasIncompleteSession = state.hasIncompleteSession,
                    onStartNew = { viewModel.startNewSession() },
                    onResume = { viewModel.resumeSession() },
                    onViewHistory = onNavigateToHistory
                )
            }

            is HomeUiState.ShowConfirmDialog -> {
                HomeReadyContent(
                    hasIncompleteSession = state.hasIncompleteSession,
                    onStartNew = { viewModel.startNewSession() },
                    onResume = { viewModel.resumeSession() },
                    onViewHistory = onNavigateToHistory
                )
                ConfirmNewSessionDialog(
                    onConfirm = { viewModel.confirmStartNew() },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }

            is HomeUiState.Error -> {
                HomeErrorContent(
                    error = state.error,
                    onStartNew = { viewModel.startNewSession() }
                )
            }
        }
    }
}

@Composable
private fun HomeReadyContent(
    hasIncompleteSession: Boolean,
    onStartNew: () -> Unit,
    onResume: () -> Unit,
    onViewHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onStartNew,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Start New Session" }
        ) {
            Text("Start New Session")
        }

        if (hasIncompleteSession) {
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Continue Session" }
            ) {
                Text("Continue Session")
            }
        }

        Button(
            onClick = onViewHistory,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "View History" }
        ) {
            Text("View History")
        }
    }
}

@Composable
private fun ConfirmNewSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Session?") },
        text = {
            Text("You have an incomplete session. Starting a new one will delete your progress.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Start New")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HomeErrorContent(
    error: AppError,
    onStartNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val errorMessage = when (error) {
            is AppError.QuestionLoadError -> "Unable to load questions: ${error.message}"
            is AppError.SessionCorruptionError -> "Session error: ${error.message}"
            is AppError.InsufficientQuestionsError -> "Not enough questions available (${error.available} found, minimum 2 required)"
            is AppError.ScoreHistoryError -> error.message
        }

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { contentDescription = "Error: $errorMessage" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onStartNew,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Start New Session" }
        ) {
            Text("Start New Session")
        }
    }
}
