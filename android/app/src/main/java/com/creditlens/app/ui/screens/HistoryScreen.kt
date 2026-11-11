package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.ui.components.PrimaryButton
import com.creditlens.app.ui.theme.*
import com.creditlens.app.ui.viewmodel.HistoryViewModel

enum class HistorySortOption(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    HIGHEST_ECL("Highest Expected Loss"),
    HIGHEST_PD("Highest Default Probability")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val assessments by viewModel.allAssessments.collectAsState()
    var filterStatus by remember { mutableStateOf<Int?>(null) } // null = All, 0 = Low Risk, 1 = Default Risk
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(HistorySortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filteredAndSortedList = remember(assessments, filterStatus, searchQuery, sortOption) {
        var list = assessments

        // Filter by risk status
        if (filterStatus != null) {
            list = list.filter { it.defaultPrediction == filterStatus }
        }

        // Search query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.id.toString().contains(q) ||
                it.loanAmount.toString().contains(q) ||
                it.ficoScore.toString().contains(q) ||
                it.income.toString().contains(q)
            }
        }

        // Sort
        when (sortOption) {
            HistorySortOption.NEWEST -> list.sortedByDescending { it.timestamp }
            HistorySortOption.OLDEST -> list.sortedBy { it.timestamp }
            HistorySortOption.HIGHEST_ECL -> list.sortedByDescending { it.expectedLoss }
            HistorySortOption.HIGHEST_PD -> list.sortedByDescending { it.probabilityOfDefault }
        }
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
                    Text("Clear All", color = StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            },
            containerColor = CardBackground,
            titleContentColor = PrimaryText,
            textContentColor = SecondaryText
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
                    style = MaterialTheme.typography.headlineLarge,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${assessments.size} records saved locally in Room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
            if (assessments.isNotEmpty()) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear all records",
                        tint = SecondaryText
                    )
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by amount, FICO score, or ID...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText
            ),
            singleLine = true
        )

        // Filter Tabs & Sort Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("All (${assessments.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SelectedNav,
                        selectedLabelColor = PrimaryBlue,
                        containerColor = CardBackground,
                        labelColor = SecondaryText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filterStatus == null,
                        borderColor = BorderColor,
                        selectedBorderColor = PrimaryBlue
                    )
                )
                FilterChip(
                    selected = filterStatus == 0,
                    onClick = { filterStatus = 0 },
                    label = { Text("Low Risk (${assessments.count { it.defaultPrediction == 0 }})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessGreenBg,
                        selectedLabelColor = SuccessGreen,
                        containerColor = CardBackground,
                        labelColor = SecondaryText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filterStatus == 0,
                        borderColor = BorderColor,
                        selectedBorderColor = SuccessGreen
                    )
                )
                FilterChip(
                    selected = filterStatus == 1,
                    onClick = { filterStatus = 1 },
                    label = { Text("Default (${assessments.count { it.defaultPrediction == 1 }})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusRedBg,
                        selectedLabelColor = StatusRed,
                        containerColor = CardBackground,
                        labelColor = SecondaryText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filterStatus == 1,
                        borderColor = BorderColor,
                        selectedBorderColor = StatusRed
                    )
                )
            }

            // Sort Menu Button
            Box {
                IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort records",
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = androidx.compose.ui.Modifier.background(CardBackground)
                ) {
                    HistorySortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    color = if (sortOption == option) PrimaryBlue else PrimaryText,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                sortOption = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // List Content
        if (filteredAndSortedList.isEmpty()) {
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
                        text = "No assessments matching criteria",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredAndSortedList, key = { it.id }) { assessment ->
                    AssessmentSummaryCard(
                        assessment = assessment,
                        onDelete = { viewModel.deleteAssessment(assessment) }
                    )
                }
            }
        }
    }
}
