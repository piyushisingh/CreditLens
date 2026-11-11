package com.creditlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditlens.app.data.local.AssessmentEntity
import com.creditlens.app.ui.components.*
import com.creditlens.app.ui.theme.*
import com.creditlens.app.ui.viewmodel.AssessmentResultState
import com.creditlens.app.ui.viewmodel.AssessmentViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun NewAssessmentScreen(
    viewModel: AssessmentViewModel
) {
    val creditLines by viewModel.creditLines.collectAsState()
    val loanAmount by viewModel.loanAmount.collectAsState()
    val totalDebt by viewModel.totalDebt.collectAsState()
    val income by viewModel.income.collectAsState()
    val yearsEmployed by viewModel.yearsEmployed.collectAsState()
    val ficoScore by viewModel.ficoScore.collectAsState()
    val recoveryRate by viewModel.recoveryRate.collectAsState()

    val errors by viewModel.errors.collectAsState()
    val resultState by viewModel.resultState.collectAsState()

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
                text = "Loan Risk Assessment",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter borrower characteristics to predict default probability and expected credit loss.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }

        // Form Card
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
                Text(
                    text = "Borrower Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )

                // 1. Credit lines outstanding
                FormInputField(
                    label = "Active Credit Lines",
                    value = creditLines,
                    onValueChange = { viewModel.updateCreditLines(it) },
                    placeholder = "e.g. 2",
                    helperText = "Number of existing active credit facilities",
                    errorText = errors["creditLines"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // 2. Loan amount
                FormInputField(
                    label = "Loan Amount Requested ($)",
                    value = loanAmount,
                    onValueChange = { viewModel.updateLoanAmount(it) },
                    placeholder = "e.g. 5000",
                    prefix = "$",
                    helperText = "Total principal amount requested (Exposure at Default)",
                    errorText = errors["loanAmount"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // 3. Total debt
                FormInputField(
                    label = "Total Existing Debt ($)",
                    value = totalDebt,
                    onValueChange = { viewModel.updateTotalDebt(it) },
                    placeholder = "e.g. 15000",
                    prefix = "$",
                    helperText = "Cumulative outstanding liabilities across all lenders",
                    errorText = errors["totalDebt"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // 4. Annual Income
                FormInputField(
                    label = "Annual Income ($)",
                    value = income,
                    onValueChange = { viewModel.updateIncome(it) },
                    placeholder = "e.g. 60000",
                    prefix = "$",
                    helperText = "Verified annual gross income",
                    errorText = errors["income"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // 5. Years Employed
                FormInputField(
                    label = "Years in Employment",
                    value = yearsEmployed,
                    onValueChange = { viewModel.updateYearsEmployed(it) },
                    placeholder = "e.g. 4.5",
                    suffix = "yrs",
                    helperText = "Length of current continuous steady employment",
                    errorText = errors["yearsEmployed"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // 6. FICO Score
                FormInputField(
                    label = "Credit Bureau FICO Score",
                    value = ficoScore,
                    onValueChange = { viewModel.updateFicoScore(it) },
                    placeholder = "300 - 850",
                    helperText = "US credit bureau score (range 300 to 850)",
                    errorText = errors["ficoScore"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // 7. Recovery Rate parameter
                FormInputField(
                    label = "Assumed Recovery Rate (%)",
                    value = recoveryRate,
                    onValueChange = { viewModel.updateRecoveryRate(it) },
                    placeholder = "10",
                    suffix = "%",
                    helperText = "Baseline modeling parameter (defaults to 10% -> LGD = 90%)",
                    errorText = errors["recoveryRate"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Submit Button
                PrimaryButton(
                    text = "Evaluate Credit Risk",
                    onClick = { viewModel.evaluate() },
                    icon = Icons.Default.Calculate,
                    isLoading = resultState is AssessmentResultState.Loading
                )
            }
        }

        // Result Handling
        when (val res = resultState) {
            is AssessmentResultState.Error -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRedBg),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Evaluation Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = res.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText
                            )
                        }
                    }
                }
            }
            is AssessmentResultState.Success -> {
                AssessmentResultCard(
                    assessment = res.assessment,
                    onReset = { viewModel.resetResult() }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun AssessmentResultCard(
    assessment: AssessmentEntity,
    onReset: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply { minimumFractionDigits = 2 }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assessment Results",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText
                )
                RiskBadge(isDefault = assessment.defaultPrediction == 1)
            }

            HorizontalDivider(color = BorderColor)

            // Primary Highlight: Expected Credit Loss
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SecondarySurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EXPECTED CREDIT LOSS (ECL)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currencyFormat.format(assessment.expectedLoss),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (assessment.defaultPrediction == 1) StatusRed else SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ECL = PD × LGD × EAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Tri-Factor Breakdown Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FactorCard(
                    title = "Prob. of Default (PD)",
                    value = percentFormat.format(assessment.probabilityOfDefault),
                    modifier = Modifier.weight(1f),
                    color = if (assessment.defaultPrediction == 1) StatusRed else SuccessGreen
                )
                FactorCard(
                    title = "Loss Given Default",
                    value = percentFormat.format(assessment.lossGivenDefault),
                    modifier = Modifier.weight(1f),
                    color = PrimaryText
                )
                FactorCard(
                    title = "Exposure (EAD)",
                    value = currencyFormat.format(assessment.exposureAtDefault),
                    modifier = Modifier.weight(1f),
                    color = PrimaryText
                )
            }

            // Derived Financial Ratios
            Text(
                text = "Engineered Financial Ratios",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = SecondaryText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatioCard(
                    title = "Debt-to-Income",
                    value = String.format(Locale.US, "%.2f", assessment.debtToIncome),
                    modifier = Modifier.weight(1f)
                )
                RatioCard(
                    title = "Loan-to-Income",
                    value = String.format(Locale.US, "%.2f", assessment.loanToIncome),
                    modifier = Modifier.weight(1f)
                )
                RatioCard(
                    title = "Debt/Credit Line",
                    value = currencyFormat.format(assessment.debtPerCreditLine),
                    modifier = Modifier.weight(1f)
                )
            }

            // Prototype Disclaimer
            Text(
                text = "ℹ️ Assessment automatically saved to local Room history. Modeling results are for decision support and prototyping purposes.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            SecondaryButton(
                text = "Perform Another Assessment",
                onClick = onReset,
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FactorCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = PrimaryText
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecondarySurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SecondaryText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = color
            )
        }
    }
}

@Composable
fun RatioCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecondarySurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SecondaryText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PrimaryText
            )
        }
    }
}
