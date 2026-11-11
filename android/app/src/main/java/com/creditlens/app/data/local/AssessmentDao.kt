package com.creditlens.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {

    @Query("SELECT * FROM assessments ORDER BY timestamp DESC")
    fun getAllAssessments(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAssessments(limit: Int = 3): Flow<List<AssessmentEntity>>

    @Query("SELECT COUNT(*) FROM assessments")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assessments WHERE defaultPrediction = 1")
    fun getDefaultCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Delete
    suspend fun deleteAssessment(assessment: AssessmentEntity)

    @Query("DELETE FROM assessments")
    suspend fun clearAll()
}
