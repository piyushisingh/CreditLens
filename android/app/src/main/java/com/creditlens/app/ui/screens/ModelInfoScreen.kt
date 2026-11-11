package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.ui.theme.*

@Composable
fun ModelInfoScreen() {
    val scrollState = rememberScrollState()

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
                text = "Model Specifications",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Technical documentation of the CreditLens risk modeling architecture.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Active Model Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Active Machine Learning Architecture",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                        color = TextPrimary
                    )
                }

                HorizontalDivider(color = CardBorder)

                SpecRow(label = "Selected Model", value = "Logistic Regression (Balanced Weights)")
                SpecRow(label = "Pipeline Encapsulation", value = "FinancialRatioTransformer + StandardScaler")
                SpecRow(label = "Cross-Validation", value = "5-Fold Stratified CV on Training Data Only")
                SpecRow(label = "Train/Test Partition", value = "80% Train (8,000) / 20% Held-Out Test (2,000)")
                SpecRow(label = "Test ROC-AUC Score", value = "1.0000")
                SpecRow(label = "Test Recall", value = "1.0000 (0 False Negatives on Test Split)")
                SpecRow(label = "Test Accuracy", value = "99.65%")
                SpecRow(label = "Brier Calibration Score", value = "0.003903")
            }
        }

        // Mathematical Formulation Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Expected Credit Loss (ECL) Identity",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
                Text(
                    text = "ECL = PD × LGD × EAD",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "• Probability of Default (PD): Estimated via logistic sigmoid on the 9-dimensional standardized feature space.\n" +
                           "• Loss Given Default (LGD): Parameterized at 90.00% based on a 10% recovery rate baseline assumption (LGD = 1 - Recovery Rate).\n" +
                           "• Exposure at Default (EAD): The total outstanding loan amount at the time of evaluation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Features & Exclusions Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Feature Matrix & Engineering",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
                Text(
                    text = "Raw Inputs (6):\n" +
                           "1. credit_lines_outstanding (0 to 5)\n" +
                           "2. loan_amt_outstanding ($)\n" +
                           "3. total_debt_outstanding ($)\n" +
                           "4. income ($)\n" +
                           "5. years_employed (yrs)\n" +
                           "6. fico_score (300 to 850)\n\n" +
                           "Engineered Ratios (3):\n" +
                           "• Debt-to-Income = total_debt / income\n" +
                           "• Loan-to-Income = loan_amt / income\n" +
                           "• Debt per Line = total_debt / (credit_lines + 1)\n\n" +
                           "Excluded: customer_id is deliberately excluded to prevent spurious correlation on non-predictive identifiers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Prototype Disclaimer
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Research Prototype Notice: CreditLens does not claim formal regulatory certification (such as Basel II/III or IFRS 9). The 10% recovery rate assumption is an exploratory baseline rather than a universal standard.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary
        )
    }
}
