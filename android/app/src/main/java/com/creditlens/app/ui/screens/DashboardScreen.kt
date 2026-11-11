package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
    onNavigateToHistory: () -> Unit,
    onNavigateToModelInfo: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply { minimumFractionDigits = 2 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heading & Overview Description
        item {
            Column {
                Text(
                    text = "Credit Risk Overview",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Intelligent credit risk assessment and expected credit loss (ECL) calculation powered by machine learning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        // Actions: New Assessment, View History, Model Info
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "New Assessment",
                    onClick = onNavigateToNewAssessment,
                    icon = Icons.Default.Calculate
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "View History",
                        onClick = onNavigateToHistory,
                        icon = Icons.Default.History,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Model Info",
                        onClick = onNavigateToModelInfo,
                        icon = Icons.Default.Analytics,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Vertically Stacked KPI Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Total Assessments",
                    value = "${state.totalAssessments}",
                    subtitle = "All-time local records",
                    icon = Icons.Default.Assessment,
                    accentColor = PrimaryBlue
                )

                MetricCard(
                    title = "High-Risk Cases",
                    value = "${state.defaultAssessments}",
                    subtitle = "Flagged default probability > 50%",
                    icon = Icons.Default.Warning,
                    accentColor = StatusRed
                )

                MetricCard(
                    title = "Low-Risk Cases",
                    value = "${state.lowRiskAssessments}",
                    subtitle = "Standard borrower credit profile",
                    icon = Icons.Default.CheckCircle,
                    accentColor = SuccessGreen
                )

                MetricCard(
                    title = "Average Probability of Default",
                    value = percentFormat.format(state.averageProbabilityOfDefault),
                    subtitle = "Mean PD across portfolio records",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = PrimaryBlue
                )

                MetricCard(
                    title = "Total Expected Loss",
                    value = currencyFormat.format(state.totalExpectedLoss),
                    subtitle = "Cumulative ECL portfolio exposure",
                    icon = Icons.Default.AttachMoney,
                    accentColor = WarningAmber
                )
            }
        }

        // Recent Assessments Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Assessments",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
                if (state.totalAssessments > 3) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text(
                            text = "View all (${state.totalAssessments})",
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Recent List
        if (state.recentAssessments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No assessments recorded yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'New Assessment' above to perform borrower risk evaluation.",
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
fun AssessmentSummaryCard(
    assessment: AssessmentEntity,
    onDelete: (() -> Unit)? = null
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply { minimumFractionDigits = 2 }
    val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RiskBadge(isDefault = assessment.defaultPrediction == 1)
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete record",
                                tint = SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Loan Amount", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        currencyFormat.format(assessment.loanAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = PrimaryText
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FICO Score", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${assessment.ficoScore}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = PrimaryText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Prob. of Default", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        percentFormat.format(assessment.probabilityOfDefault),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (assessment.defaultPrediction == 1) StatusRed else SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Expected Credit Loss (ECL):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = SecondaryText
                )
                Text(
                    currencyFormat.format(assessment.expectedLoss),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                )
            }
        }
    }
}
