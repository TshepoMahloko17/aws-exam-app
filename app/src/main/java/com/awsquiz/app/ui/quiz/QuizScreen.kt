package com.awsquiz.app.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awsquiz.app.domain.AnswerResult
import com.awsquiz.app.domain.AppError
import com.awsquiz.app.domain.Question
import android.widget.Toast

@Composable
fun QuizScreen(
    viewModel: QuizViewModel = hiltViewModel(),
    onSessionCompleted: (correctCount: Int, totalCount: Int, percentage: Int, passed: Boolean) -> Unit,
    onNavigateHome: () -> Unit,
    onError: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuizEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is QuizUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.semantics {
                        contentDescription = "Loading quiz"
                    }
                )
            }

            is QuizUiState.Active -> {
                QuizActiveContent(
                    state = state,
                    onOptionSelected = viewModel::selectOption,
                    onSubmit = viewModel::submitAnswer,
                    onPrevious = viewModel::goToPrevious,
                    onNext = viewModel::goToNext,
                    onNavigateHome = onNavigateHome
                )
            }

            is QuizUiState.Completed -> {
                LaunchedEffect(state) {
                    onSessionCompleted(
                        state.scoreResult.correctCount,
                        state.scoreResult.totalCount,
                        state.scoreResult.percentage,
                        state.scoreResult.passed
                    )
                }
            }

            is QuizUiState.Error -> {
                QuizErrorContent(error = state.error, onGoHome = onError)
            }
        }
    }
}

@Composable
private fun QuizActiveContent(
    state: QuizUiState.Active,
    onOptionSelected: (Int) -> Unit,
    onSubmit: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Return to Home" }
        ) {
            Text("Return to Home")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator
        Text(
            text = state.progress,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { contentDescription = state.progress }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Question text
        Text(
            text = state.question.text,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-answer hint
        if (state.question.isMultipleAnswer) {
            Text(
                text = "Select ${state.question.correctAnswers.size} answers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Select ${state.question.correctAnswers.size} answers"
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Options
        state.question.options.forEachIndexed { index, option ->
            val label = ('A' + index).toString()
            OptionItem(
                label = label,
                text = option,
                index = index,
                isSelected = state.selectedOptions.contains(index),
                isSingleAnswer = state.question.isSingleAnswer,
                isSubmitted = state.isSubmitted,
                answerResult = state.answerResult,
                onSelect = { onOptionSelected(index) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback after submission
        if (state.isSubmitted && state.answerResult != null) {
            FeedbackSection(answerResult = state.answerResult, explanation = state.question.explanation)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Submit button
        if (!state.isSubmitted) {
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Submit Answer" }
            ) {
                Text("Submit")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (state.canGoPrevious) {
                TextButton(
                    onClick = onPrevious,
                    modifier = Modifier.semantics { contentDescription = "Previous Question" }
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (state.canGoNext) {
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.semantics { contentDescription = "Next Question" }
                ) {
                    Text("Next")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OptionItem(
    label: String,
    text: String,
    index: Int,
    isSelected: Boolean,
    isSingleAnswer: Boolean,
    isSubmitted: Boolean,
    answerResult: AnswerResult?,
    onSelect: () -> Unit
) {
    val backgroundColor = when {
        isSubmitted && answerResult != null -> {
            val isCorrectOption = answerResult.correctOptions.contains(index)
            val wasSelected = answerResult.selectedOptions.contains(index)
            when {
                isCorrectOption -> Color(0xFFE8F5E9) // light green
                wasSelected && !isCorrectOption -> Color(0xFFFFEBEE) // light red
                else -> MaterialTheme.colorScheme.surface
            }
        }
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        if (isSingleAnswer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isSubmitted) {
                            Modifier.selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = onSelect
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = if (!isSubmitted) onSelect else null
                )
                Text(
                    text = "$label. $text",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isSubmitted) {
                            Modifier.toggleable(
                                value = isSelected,
                                role = Role.Checkbox,
                                onValueChange = { onSelect() }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = if (!isSubmitted) { _ -> onSelect() } else null
                )
                Text(
                    text = "$label. $text",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    answerResult: AnswerResult,
    explanation: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (answerResult.isCorrect) {
                Color(0xFFE8F5E9)
            } else {
                Color(0xFFFFEBEE)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (answerResult.isCorrect) "Correct!" else "Incorrect",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (answerResult.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.semantics {
                    contentDescription = if (answerResult.isCorrect) "Correct" else "Incorrect"
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuizErrorContent(
    error: AppError,
    onGoHome: () -> Unit
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
            color = MaterialTheme.colorScheme.error
        )

        Button(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Return to Home" }
        ) {
            Text("Return to Home")
        }
    }
}
