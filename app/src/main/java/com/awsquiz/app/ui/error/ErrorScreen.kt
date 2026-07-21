package com.awsquiz.app.ui.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awsquiz.app.domain.AppError

@Composable
fun ErrorScreen(
    error: AppError,
    onStartNewSession: () -> Unit,
    onReturnHome: () -> Unit
) {
    val errorMessage = when (error) {
        is AppError.QuestionLoadError -> "Unable to load questions: ${error.message}"
        is AppError.SessionCorruptionError -> "Your saved session could not be recovered: ${error.message}"
        is AppError.InsufficientQuestionsError ->
            "Not enough questions available (${error.available} found, minimum 2 required)"
        is AppError.ScoreHistoryError -> "Score history error: ${error.message}"
    }

    val showStartNewSession = error is AppError.SessionCorruptionError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { contentDescription = "Error: $errorMessage" }
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (showStartNewSession) {
            Button(
                onClick = onStartNewSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Start New Session" }
            ) {
                Text("Start New Session")
            }
        } else {
            Button(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Return to Home" }
            ) {
                Text("Return to Home")
            }
        }
    }
}
