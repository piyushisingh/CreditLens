package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.ui.components.FormInputField
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
                    Text("Delete", color = RiskHighRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
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
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Backend connectivity and local storage configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Backend Connection Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Android emulator default is 'http://10.0.2.2:8000/'. For physical devices on local Wi-Fi, use your host IP (e.g. 'http://192.168.1.x:8000/').",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )

                FormInputField(
                    label = "Server Base URL",
                    value = baseUrl,
                    onValueChange = { viewModel.updateBaseUrl(it) },
                    placeholder = "http://10.0.2.2:8000/"
                )

                Button(
                    onClick = { viewModel.testConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Connection (/api/health)")
                }

                if (testResult != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundDark,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (testResult!!.startsWith("Connected")) RiskLowGreen else RiskHighRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Database Maintenance Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Local Storage & Cache",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Text(
                    text = "Assessment records are persisted in a local SQLite database via Room. Clearing the database resets all local history.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )

                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    border = BorderStroke(1.dp, RiskHighRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = RiskHighRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Assessment Database", color = RiskHighRed)
                }
            }
        }
    }
}
