package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.ui.theme.*

@Composable
fun AboutScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header
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
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SelectedNav,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "CreditLens",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 1.0.0 (Native Android · Jetpack Compose)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "A decision-support mobile prototype integrating a calibrated machine learning risk backend with an authoritative Lovable-inspired native Android experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Architecture Summary
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
                    text = "Technology Stack",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
                HorizontalDivider(color = BorderColor)
                SpecRow(label = "Mobile Framework", value = "Kotlin 2.0.21 · Jetpack Compose")
                SpecRow(label = "Design System", value = "Material 3 (Authoritative Dark Theme)")
                SpecRow(label = "Architecture", value = "MVVM · Repository Pattern")
                SpecRow(label = "Local Storage", value = "SQLite via Room 2.6.1")
                SpecRow(label = "Networking", value = "Retrofit 2.11 · OkHttp 4.12")
                SpecRow(label = "Backend Service", value = "FastAPI 0.162 · Python 3.14")
                SpecRow(label = "ML Engine", value = "scikit-learn 1.8 · XGBoost 3.2")
                SpecRow(label = "Repository", value = "github.com/piyushisingh/CreditLens")
            }
        }

        // Disclaimers & Licensing
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SecondarySurface),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Disclaimers & Responsible Use",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CreditLens is an educational and research prototype developed to study machine learning credit risk modeling and mobile integration. It is not licensed for consumer banking, production lending, or credit underwriting decisions without comprehensive regulatory validation and audit.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
        }
    }
}
