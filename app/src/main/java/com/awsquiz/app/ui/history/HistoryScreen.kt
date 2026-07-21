package com.awsquiz.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awsquiz.app.domain.ProgressTrend
import com.awsquiz.app.domain.ScoreHistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is HistoryUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.semantics {
                        contentDescription = "Loading"
                    }
                )
            }

            is HistoryUiState.Loaded -> {
                if (state.isEmpty) {
                    EmptyHistoryContent(onNavigateHome = onNavigateHome)
                } else {
                    HistoryListContent(
                        scores = state.scores,
                        trend = state.trend,
                        onNavigateHome = onNavigateHome
                    )
                }
            }

            is HistoryUiState.Error -> {
                HistoryErrorContent(
                    message = state.message,
                    onRetry = { viewModel.retry() },
                    onNavigateHome = onNavigateHome
                )
            }
        }
    }
}


@Composable
private fun EmptyHistoryContent(onNavigateHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No past attempts",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics {
                contentDescription = "No past attempts"
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Return to Home" }
        ) {
            Text("Return to Home")
        }
    }
}

@Composable
private fun HistoryListContent(
    scores: List<ScoreHistoryRecord>,
    trend: ProgressTrend,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Score History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (scores.size >= 2) {
            TrendIndicator(trend = trend)
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scores) { record ->
                ScoreHistoryItem(record = record)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Return to Home" }
        ) {
            Text("Return to Home")
        }
    }
}

@Composable
private fun TrendIndicator(trend: ProgressTrend) {
    val (trendText, trendSymbol) = when (trend) {
        ProgressTrend.IMPROVING -> "Improving" to "↑"
        ProgressTrend.DECLINING -> "Declining" to "↓"
        ProgressTrend.STABLE -> "Stable" to "→"
    }

    val trendColor = when (trend) {
        ProgressTrend.IMPROVING -> Color(0xFF2E7D32)
        ProgressTrend.DECLINING -> Color(0xFFC62828)
        ProgressTrend.STABLE -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = "Progress trend: $trendText"
        }
    ) {
        Text(
            text = "Trend: ",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "$trendSymbol $trendText",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = trendColor
        )
    }
}

@Composable
private fun ScoreHistoryItem(record: ScoreHistoryRecord) {
    val passColor = Color(0xFF2E7D32)
    val failColor = Color(0xFFC62828)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateText = dateFormat.format(Date(record.completionDate))
    val badgeText = if (record.passed) "PASSED" else "FAILED"
    val badgeColor = if (record.passed) passColor else failColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$dateText, ${record.percentage} percent, $badgeText"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${record.percentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun HistoryErrorContent(
    message: String,
    onRetry: () -> Unit,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics {
                contentDescription = "Error: $message"
            }
        )

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Retry" }
        ) {
            Text("Retry")
        }

        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Return to Home" }
        ) {
            Text("Return to Home")
        }
    }
}
