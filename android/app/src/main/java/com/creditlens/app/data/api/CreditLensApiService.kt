package com.creditlens.app.data.api

import com.creditlens.app.data.model.HealthResponseDto
import com.creditlens.app.data.model.LoanPredictionRequestDto
import com.creditlens.app.data.model.LoanPredictionResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CreditLensApiService {

    @GET("api/health")
    suspend fun getHealth(): HealthResponseDto

    @POST("api/predict")
    suspend fun predictLoan(
        @Body request: LoanPredictionRequestDto
    ): LoanPredictionResponseDto
}
