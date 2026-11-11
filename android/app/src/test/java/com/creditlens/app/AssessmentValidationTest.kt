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

    @Test
    fun testKpiMetricAggregations() {
        val list = listOf(
            AssessmentEntity(
                id = 1, creditLines = 1, loanAmount = 1000.0, totalDebt = 2000.0,
                income = 50000.0, yearsEmployed = 3.0, ficoScore = 750, recoveryRate = 0.10,
                defaultPrediction = 0, probabilityOfDefault = 0.10, lossGivenDefault = 0.90,
                exposureAtDefault = 1000.0, expectedLoss = 90.0, debtToIncome = 0.04,
                loanToIncome = 0.02, debtPerCreditLine = 1000.0
            ),
            AssessmentEntity(
                id = 2, creditLines = 4, loanAmount = 8000.0, totalDebt = 35000.0,
                income = 30000.0, yearsEmployed = 1.0, ficoScore = 580, recoveryRate = 0.10,
                defaultPrediction = 1, probabilityOfDefault = 0.80, lossGivenDefault = 0.90,
                exposureAtDefault = 8000.0, expectedLoss = 5760.0, debtToIncome = 1.16,
                loanToIncome = 0.27, debtPerCreditLine = 7000.0
            )
        )

        val total = list.size
        val defaults = list.count { it.defaultPrediction == 1 }
        val lowRisk = total - defaults
        val avgPd = list.map { it.probabilityOfDefault }.average()
        val totalEcl = list.sumOf { it.expectedLoss }

        assertEquals(2, total)
        assertEquals(1, defaults)
        assertEquals(1, lowRisk)
        assertEquals(0.45, avgPd, 0.001)
        assertEquals(5850.0, totalEcl, 0.01)
    }

    @Test
    fun testBaseUrlSanitization() {
        com.creditlens.app.data.api.ApiClient.setBaseUrl("http://192.168.1.10:8000")
        assertEquals("http://192.168.1.10:8000/", com.creditlens.app.data.api.ApiClient.getBaseUrl())

        com.creditlens.app.data.api.ApiClient.setBaseUrl("http://10.0.2.2:8000/")
        assertEquals("http://10.0.2.2:8000/", com.creditlens.app.data.api.ApiClient.getBaseUrl())
    }

    @Test
    fun testHistorySortingLogic() {
        val item1 = AssessmentEntity(
            id = 1, timestamp = 1000L, creditLines = 1, loanAmount = 1000.0, totalDebt = 2000.0,
            income = 50000.0, yearsEmployed = 3.0, ficoScore = 750, recoveryRate = 0.10,
            defaultPrediction = 0, probabilityOfDefault = 0.10, lossGivenDefault = 0.90,
            exposureAtDefault = 1000.0, expectedLoss = 90.0, debtToIncome = 0.04,
            loanToIncome = 0.02, debtPerCreditLine = 1000.0
        )
        val item2 = AssessmentEntity(
            id = 2, timestamp = 2000L, creditLines = 4, loanAmount = 8000.0, totalDebt = 35000.0,
            income = 30000.0, yearsEmployed = 1.0, ficoScore = 580, recoveryRate = 0.10,
            defaultPrediction = 1, probabilityOfDefault = 0.80, lossGivenDefault = 0.90,
            exposureAtDefault = 8000.0, expectedLoss = 5760.0, debtToIncome = 1.16,
            loanToIncome = 0.27, debtPerCreditLine = 7000.0
        )

        val list = listOf(item1, item2)

        val byNewest = list.sortedByDescending { it.timestamp }
        assertEquals(2, byNewest.first().id)

        val byOldest = list.sortedBy { it.timestamp }
        assertEquals(1, byOldest.first().id)

        val byHighestLoss = list.sortedByDescending { it.expectedLoss }
        assertEquals(5760.0, byHighestLoss.first().expectedLoss, 0.01)

        val byHighestPd = list.sortedByDescending { it.probabilityOfDefault }
        assertEquals(0.80, byHighestPd.first().probabilityOfDefault, 0.01)
    }
}
