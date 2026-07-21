package com.awsquiz.app.ui.result

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(
    correctCount: Int,
    totalCount: Int,
    percentage: Int,
    passed: Boolean,
    onReturnHome: () -> Unit
) {
    val passColor = Color(0xFF2E7D32)
    val failColor = Color(0xFFC62828)
    val indicatorColor = if (passed) passColor else failColor
    val indicatorText = if (passed) "PASSED" else "FAILED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quiz Complete",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "$correctCount/$totalCount",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = "$correctCount out of $totalCount correct"
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = "$percentage percent"
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = indicatorText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = indicatorColor,
            modifier = Modifier.semantics {
                contentDescription = indicatorText
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

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
