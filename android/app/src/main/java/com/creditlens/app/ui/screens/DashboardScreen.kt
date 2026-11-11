package com.creditlens.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.data.local.AssessmentEntity
import com.creditlens.app.ui.components.*
import com.creditlens.app.ui.theme.*
import com.creditlens.app.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNewAssessment: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply { minimumFractionDigits = 2 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Column {
                Text(
                    text = "CreditLens",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Intelligent Credit Risk & Expected Loss Engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Backend Status Banner
        item {
            BackendStatusBanner(
                isOnline = state.isBackendOnline,
                modelType = state.modelType,
                onRefresh = { viewModel.checkHealth() }
            )
        }

        // Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Evaluated",
                    value = "${state.totalAssessments}",
                    subtitle = "All-time local records",
                    icon = Icons.Default.Analytics,
                    accentColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                val defaultRate = if (state.totalAssessments > 0) {
                    state.defaultAssessments.toDouble() / state.totalAssessments.toDouble()
                } else 0.0

                MetricCard(
                    title = "Default Ratio",
                    value = percentFormat.format(defaultRate),
                    subtitle = "${state.defaultAssessments} flagged high risk",
                    icon = Icons.Default.Warning,
                    accentColor = if (state.defaultAssessments > 0) RiskHighRed else RiskLowGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action CTA Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlueVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNewAssessment() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Credit Risk Assessment",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Evaluate borrower probability of default & expected loss",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextPrimary.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Evaluate",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Recent Activity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Assessments",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (state.totalAssessments > 3) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text("View all (${state.totalAssessments})", color = PrimaryBlue)
                    }
                }
            }
        }

        // Recent List
        if (state.recentAssessments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No assessments recorded yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Text(
                            text = "Click 'New Credit Risk Assessment' above to begin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(state.recentAssessments) { assessment ->
                AssessmentSummaryCard(assessment)
            }
        }
    }
}

@Composable
fun AssessmentSummaryCard(assessment: AssessmentEntity) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply { minimumFractionDigits = 2 }
    val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(assessment.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                RiskBadge(isDefault = assessment.defaultPrediction == 1)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Loan Amount", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        currencyFormat.format(assessment.loanAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FICO Score", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        "${assessment.ficoScore}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Prob. of Default", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        percentFormat.format(assessment.probabilityOfDefault),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (assessment.defaultPrediction == 1) RiskHighRed else RiskLowGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Expected Credit Loss (ECL):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
                Text(
                    currencyFormat.format(assessment.expectedLoss),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}
