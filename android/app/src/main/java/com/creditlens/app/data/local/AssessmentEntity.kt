package com.creditlens.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val creditLines: Int,
    val loanAmount: Double,
    val totalDebt: Double,
    val income: Double,
    val yearsEmployed: Double,
    val ficoScore: Int,
    val recoveryRate: Double,
    val defaultPrediction: Int,
    val probabilityOfDefault: Double,
    val lossGivenDefault: Double,
    val exposureAtDefault: Double,
    val expectedLoss: Double,
    val debtToIncome: Double,
    val loanToIncome: Double,
    val debtPerCreditLine: Double
)
