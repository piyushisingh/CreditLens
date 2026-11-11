package com.creditlens.app

import com.creditlens.app.data.local.AssessmentEntity
import com.creditlens.app.data.model.DerivedRatiosDto
import com.creditlens.app.data.model.LoanPredictionRequestDto
import com.creditlens.app.data.model.LoanPredictionResponseDto
import org.junit.Assert.*
import org.junit.Test

class AssessmentValidationTest {

    @Test
    fun testLoanPredictionDtoCreation() {
        val request = LoanPredictionRequestDto(
            creditLinesOutstanding = 2,
            loanAmtOutstanding = 5000.0,
            totalDebtOutstanding = 15000.0,
            income = 60000.0,
            yearsEmployed = 4.5,
            ficoScore = 720,
            recoveryRate = 0.10
        )

        assertEquals(2, request.creditLinesOutstanding)
        assertEquals(5000.0, request.loanAmtOutstanding, 0.001)
        assertEquals(720, request.ficoScore)
        assertEquals(0.10, request.recoveryRate, 0.001)
    }

    @Test
    fun testExpectedLossFormulaIdentity() {
        val pd = 0.20
        val lgd = 0.90
        val ead = 10000.0
        val expectedLoss = pd * lgd * ead

        assertEquals(1800.0, expectedLoss, 0.01)
    }

    @Test
    fun testDerivedRatiosCalculation() {
        val totalDebt = 20000.0
        val income = 80000.0
        val loanAmt = 8000.0
        val creditLines = 3

        val dti = totalDebt / income
        val lti = loanAmt / income
        val dpcl = totalDebt / (creditLines + 1)

        assertEquals(0.25, dti, 0.001)
        assertEquals(0.10, lti, 0.001)
        assertEquals(5000.0, dpcl, 0.001)
    }

    @Test
    fun testEntityMapping() {
        val response = LoanPredictionResponseDto(
            defaultPrediction = 0,
            probabilityOfDefault = 0.005,
            lossGivenDefault = 0.90,
            exposureAtDefault = 5000.0,
            expectedLoss = 22.50,
            derivedRatios = DerivedRatiosDto(0.25, 0.10, 5000.0),
            disclaimer = "Prototype"
        )

        val entity = AssessmentEntity(
            creditLines = 2,
            loanAmount = 5000.0,
            totalDebt = 15000.0,
            income = 60000.0,
            yearsEmployed = 4.5,
            ficoScore = 720,
            recoveryRate = 0.10,
            defaultPrediction = response.defaultPrediction,
            probabilityOfDefault = response.probabilityOfDefault,
            lossGivenDefault = response.lossGivenDefault,
            exposureAtDefault = response.exposureAtDefault,
            expectedLoss = response.expectedLoss,
            debtToIncome = response.derivedRatios.debtToIncome,
            loanToIncome = response.derivedRatios.loanToIncome,
            debtPerCreditLine = response.derivedRatios.debtPerCreditLine
        )

        assertEquals(0, entity.defaultPrediction)
        assertEquals(22.50, entity.expectedLoss, 0.01)
        assertEquals(720, entity.ficoScore)
        assertTrue(entity.timestamp > 0)
    }

    @Test
    fun testFicoScoreValidationLimits() {
        val validFico = 720
        assertTrue(validFico in 300..850)

        val invalidLowFico = 250
        assertFalse(invalidLowFico in 300..850)

        val invalidHighFico = 900
        assertFalse(invalidHighFico in 300..850)
    }
}
