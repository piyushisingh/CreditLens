package com.creditlens.app.data.model

import com.google.gson.annotations.SerializedName

data class HealthResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("model_loaded") val modelLoaded: Boolean,
    @SerializedName("model_type") val modelType: String?,
    @SerializedName("version") val version: String
)

data class LoanPredictionRequestDto(
    @SerializedName("credit_lines_outstanding") val creditLinesOutstanding: Int,
    @SerializedName("loan_amt_outstanding") val loanAmtOutstanding: Double,
    @SerializedName("total_debt_outstanding") val totalDebtOutstanding: Double,
    @SerializedName("income") val income: Double,
    @SerializedName("years_employed") val yearsEmployed: Double,
    @SerializedName("fico_score") val ficoScore: Int,
    @SerializedName("recovery_rate") val recoveryRate: Double = 0.10
)

data class DerivedRatiosDto(
    @SerializedName("debt_to_income") val debtToIncome: Double,
    @SerializedName("loan_to_income") val loanToIncome: Double,
    @SerializedName("debt_per_credit_line") val debtPerCreditLine: Double
)

data class LoanPredictionResponseDto(
    @SerializedName("default_prediction") val defaultPrediction: Int,
    @SerializedName("probability_of_default") val probabilityOfDefault: Double,
    @SerializedName("loss_given_default") val lossGivenDefault: Double,
    @SerializedName("exposure_at_default") val exposureAtDefault: Double,
    @SerializedName("expected_loss") val expectedLoss: Double,
    @SerializedName("derived_ratios") val derivedRatios: DerivedRatiosDto,
    @SerializedName("disclaimer") val disclaimer: String
)
