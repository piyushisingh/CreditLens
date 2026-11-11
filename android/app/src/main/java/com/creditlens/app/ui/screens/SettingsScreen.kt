package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.ui.components.FormInputField
import com.creditlens.app.ui.components.PrimaryButton
import com.creditlens.app.ui.components.SecondaryButton
import com.creditlens.app.ui.theme.*
import com.creditlens.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val testResult by viewModel.connectionTestResult.collectAsState()
    val scrollState = rememberScrollState()

    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Records") },
            text = { Text("Are you sure you want to delete all cached assessment records from the local Room database?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDatabase()
                        showClearConfirm = false
                    }
                ) {
                    Text("Delete", color = StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
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
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Backend connectivity and local storage configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }

        // Backend Connection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "FastAPI Backend Connection",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                }

                Text(
                    text = "Android emulator default is 'http://10.0.2.2:8000/'. For physical devices on local Wi-Fi, use your host machine IP (e.g. 'http://192.168.1.x:8000/').",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = SecondaryText
                )

                FormInputField(
                    label = "Server Base URL",
                    value = baseUrl,
                    onValueChange = { viewModel.updateBaseUrl(it) },
                    placeholder = "http://10.0.2.2:8000/"
                )

                PrimaryButton(
                    text = "Test Connection (/api/health)",
                    onClick = { viewModel.testConnection() },
                    icon = Icons.Default.Lan
                )

                if (testResult != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SecondarySurface,
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (testResult!!.startsWith("Connected")) SuccessGreen else StatusRed,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Model Baseline Configuration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Default Risk Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
                Text(
                    text = "Fixed runtime modeling defaults applied during inference.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = SecondaryText
                )
                SpecRow(label = "Default Decision Threshold", value = "0.50 (50.0% PD)")
                SpecRow(label = "Baseline Recovery Rate", value = "10.00% (LGD = 90.0%)")
                SpecRow(label = "Standard Scaling", value = "scikit-learn StandardScaler")
                SpecRow(label = "Inference Pipeline", value = "LR with class_weight='balanced'")
            }
        }

        // Database Maintenance Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Local Storage & Cache",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                }

                Text(
                    text = "Assessment records are persisted in a local SQLite database via Room. Clearing the database resets all local history records.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = SecondaryText
                )

                SecondaryButton(
                    text = "Clear Assessment Database",
                    onClick = { showClearConfirm = true },
                    icon = Icons.Default.Delete,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
