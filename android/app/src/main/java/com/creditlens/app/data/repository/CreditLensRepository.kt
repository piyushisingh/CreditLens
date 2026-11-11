package com.creditlens.app.data.repository

import android.content.Context
import com.creditlens.app.data.api.ApiClient
import com.creditlens.app.data.local.AssessmentDao
import com.creditlens.app.data.local.AssessmentEntity
import com.creditlens.app.data.model.HealthResponseDto
import com.creditlens.app.data.model.LoanPredictionRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CreditLensRepository(
    private val assessmentDao: AssessmentDao,
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("creditlens_config", Context.MODE_PRIVATE)

    init {
        val savedUrl = prefs?.getString("base_url", ApiClient.DEFAULT_BASE_URL) ?: ApiClient.DEFAULT_BASE_URL
        ApiClient.setBaseUrl(savedUrl)
    }

    val allAssessments: Flow<List<AssessmentEntity>> = assessmentDao.getAllAssessments()
    val recentAssessments: Flow<List<AssessmentEntity>> = assessmentDao.getRecentAssessments(3)
    val totalCount: Flow<Int> = assessmentDao.getTotalCount()
    val defaultCount: Flow<Int> = assessmentDao.getDefaultCount()

    fun getBaseUrl(): String = ApiClient.getBaseUrl()

    fun setBaseUrl(url: String) {
        ApiClient.setBaseUrl(url)
        prefs?.edit()?.putString("base_url", ApiClient.getBaseUrl())?.apply()
    }

    suspend fun checkHealth(): Result<HealthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().getHealth()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun evaluateLoan(request: LoanPredictionRequestDto): Result<AssessmentEntity> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().predictLoan(request)

            val entity = AssessmentEntity(
                creditLines = request.creditLinesOutstanding,
                loanAmount = request.loanAmtOutstanding,
                totalDebt = request.totalDebtOutstanding,
                income = request.income,
                yearsEmployed = request.yearsEmployed,
                ficoScore = request.ficoScore,
                recoveryRate = request.recoveryRate,
                defaultPrediction = response.defaultPrediction,
                probabilityOfDefault = response.probabilityOfDefault,
                lossGivenDefault = response.lossGivenDefault,
                exposureAtDefault = response.exposureAtDefault,
                expectedLoss = response.expectedLoss,
                debtToIncome = response.derivedRatios.debtToIncome,
                loanToIncome = response.derivedRatios.loanToIncome,
                debtPerCreditLine = response.derivedRatios.debtPerCreditLine
            )

            val id = assessmentDao.insertAssessment(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAssessment(assessment: AssessmentEntity) = withContext(Dispatchers.IO) {
        assessmentDao.deleteAssessment(assessment)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        assessmentDao.clearAll()
    }
}
