package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.ui.theme.*
import com.creditlens.app.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val assessments by viewModel.allAssessments.collectAsState()
    var filterStatus by remember { mutableStateOf<Int?>(null) } // null = All, 0 = Non-Default, 1 = Default
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filteredList = remember(assessments, filterStatus) {
        if (filterStatus == null) assessments
        else assessments.filter { it.defaultPrediction == filterStatus }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Clear Assessment History") },
            text = { Text("Are you sure you want to permanently delete all local assessment records? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Clear All", color = RiskHighRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CardDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Assessment History",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${assessments.size} records saved locally in Room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (assessments.isNotEmpty()) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear all",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterStatus == null,
                onClick = { filterStatus = null },
                label = { Text("All (${assessments.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = TextPrimary
                )
            )
            FilterChip(
                selected = filterStatus == 0,
                onClick = { filterStatus = 0 },
                label = { Text("Low Risk (${assessments.count { it.defaultPrediction == 0 }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RiskLowGreen,
                    selectedLabelColor = TextPrimary
                )
            )
            FilterChip(
                selected = filterStatus == 1,
                onClick = { filterStatus = 1 },
                label = { Text("Default Risk (${assessments.count { it.defaultPrediction == 1 }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RiskHighRed,
                    selectedLabelColor = TextPrimary
                )
            )
        }

        // List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No assessments matching this filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Run an evaluation in 'New Assessment' to add records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList, key = { it.id }) { assessment ->
                    AssessmentSummaryCard(assessment)
                }
            }
        }
    }
}
