package com.creditlens.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.creditlens.app.data.local.AssessmentEntity
import com.creditlens.app.data.model.HealthResponseDto
import com.creditlens.app.data.model.LoanPredictionRequestDto
import com.creditlens.app.data.repository.CreditLensRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// DASHBOARD VIEWMODEL
// -------------------------------------------------------------
data class DashboardUiState(
    val isBackendOnline: Boolean = false,
    val modelType: String? = null,
    val isCheckingHealth: Boolean = false,
    val totalAssessments: Int = 0,
    val defaultAssessments: Int = 0,
    val lowRiskAssessments: Int = 0,
    val averageProbabilityOfDefault: Double = 0.0,
    val totalExpectedLoss: Double = 0.0,
    val recentAssessments: List<AssessmentEntity> = emptyList()
)

class DashboardViewModel(
    private val repository: CreditLensRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        checkHealth()
        observeDatabase()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            repository.allAssessments.collect { all ->
                val total = all.size
                val defaults = all.count { it.defaultPrediction == 1 }
                val lowRisk = total - defaults
                val avgPd = if (total > 0) all.map { it.probabilityOfDefault }.average() else 0.0
                val totalEcl = all.sumOf { it.expectedLoss }
                val recent = all.take(5)

                _uiState.update {
                    it.copy(
                        totalAssessments = total,
                        defaultAssessments = defaults,
                        lowRiskAssessments = lowRisk,
                        averageProbabilityOfDefault = avgPd,
                        totalExpectedLoss = totalEcl,
                        recentAssessments = recent
                    )
                }
            }
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true) }
            val result = repository.checkHealth()
            result.onSuccess { health ->
                _uiState.update {
                    it.copy(
                        isBackendOnline = health.modelLoaded,
                        modelType = health.modelType,
                        isCheckingHealth = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isBackendOnline = false,
                        modelType = null,
                        isCheckingHealth = false
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ASSESSMENT VIEWMODEL (Preserves form state)
// -------------------------------------------------------------
sealed interface AssessmentResultState {
    object Idle : AssessmentResultState
    object Loading : AssessmentResultState
    data class Success(val assessment: AssessmentEntity) : AssessmentResultState
    data class Error(val message: String) : AssessmentResultState
}

class AssessmentViewModel(
    private val repository: CreditLensRepository
) : ViewModel() {

    var creditLines = MutableStateFlow("2")
        private set
    var loanAmount = MutableStateFlow("5000")
        private set
    var totalDebt = MutableStateFlow("15000")
        private set
    var income = MutableStateFlow("60000")
        private set
    var yearsEmployed = MutableStateFlow("4.5")
        private set
    var ficoScore = MutableStateFlow("720")
        private set
    var recoveryRate = MutableStateFlow("10")
        private set

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    private val _resultState = MutableStateFlow<AssessmentResultState>(AssessmentResultState.Idle)
    val resultState: StateFlow<AssessmentResultState> = _resultState.asStateFlow()

    fun updateCreditLines(value: String) { creditLines.value = value; clearError("creditLines") }
    fun updateLoanAmount(value: String) { loanAmount.value = value; clearError("loanAmount") }
    fun updateTotalDebt(value: String) { totalDebt.value = value; clearError("totalDebt") }
    fun updateIncome(value: String) { income.value = value; clearError("income") }
    fun updateYearsEmployed(value: String) { yearsEmployed.value = value; clearError("yearsEmployed") }
    fun updateFicoScore(value: String) { ficoScore.value = value; clearError("ficoScore") }
    fun updateRecoveryRate(value: String) { recoveryRate.value = value; clearError("recoveryRate") }

    private fun clearError(key: String) {
        if (_errors.value.containsKey(key)) {
            _errors.update { it.toMutableMap().apply { remove(key) } }
        }
    }

    fun validate(): Boolean {
        val errs = mutableMapOf<String, String>()

        val cl = creditLines.value.toIntOrNull()
        if (cl == null || cl < 0) errs["creditLines"] = "Must be a non-negative whole number"

        val la = loanAmount.value.toDoubleOrNull()
        if (la == null || la <= 0.0) errs["loanAmount"] = "Must be greater than 0"

        val td = totalDebt.value.toDoubleOrNull()
        if (td == null || td < 0.0) errs["totalDebt"] = "Cannot be negative"

        val inc = income.value.toDoubleOrNull()
        if (inc == null || inc < 0.0) errs["income"] = "Cannot be negative"

        val ye = yearsEmployed.value.toDoubleOrNull()
        if (ye == null || ye < 0.0) errs["yearsEmployed"] = "Cannot be negative"

        val fico = ficoScore.value.toIntOrNull()
        if (fico == null || fico !in 300..850) errs["ficoScore"] = "FICO must be between 300 and 850"

        val rr = recoveryRate.value.toDoubleOrNull()
        if (rr == null || rr !in 0.0..100.0) errs["recoveryRate"] = "Must be between 0% and 100%"

        _errors.value = errs
        return errs.isEmpty()
    }

    fun evaluate() {
        if (!validate()) return

        viewModelScope.launch {
            _resultState.value = AssessmentResultState.Loading
            val req = LoanPredictionRequestDto(
                creditLinesOutstanding = creditLines.value.toInt(),
                loanAmtOutstanding = loanAmount.value.toDouble(),
                totalDebtOutstanding = totalDebt.value.toDouble(),
                income = income.value.toDouble(),
                yearsEmployed = yearsEmployed.value.toDouble(),
                ficoScore = ficoScore.value.toInt(),
                recoveryRate = (recoveryRate.value.toDoubleOrNull() ?: 10.0) / 100.0
            )

            val result = repository.evaluateLoan(req)
            result.onSuccess { entity ->
                _resultState.value = AssessmentResultState.Success(entity)
            }.onFailure { ex ->
                _resultState.value = AssessmentResultState.Error(
                    ex.localizedMessage ?: "Network connection failed. Verify backend at http://10.0.2.2:8000"
                )
            }
        }
    }

    fun resetResult() {
        _resultState.value = AssessmentResultState.Idle
    }
}

// -------------------------------------------------------------
// HISTORY VIEWMODEL
// -------------------------------------------------------------
class HistoryViewModel(
    private val repository: CreditLensRepository
) : ViewModel() {

    val allAssessments: StateFlow<List<AssessmentEntity>> = repository.allAssessments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            repository.deleteAssessment(assessment)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// -------------------------------------------------------------
// SETTINGS VIEWMODEL
// -------------------------------------------------------------
class SettingsViewModel(
    private val repository: CreditLensRepository
) : ViewModel() {

    var baseUrl = MutableStateFlow(repository.getBaseUrl())
        private set

    var connectionTestResult = MutableStateFlow<String?>(null)
        private set

    fun updateBaseUrl(newUrl: String) {
        baseUrl.value = newUrl
        repository.setBaseUrl(newUrl)
    }

    fun testConnection() {
        viewModelScope.launch {
            connectionTestResult.value = "Testing..."
            val result = repository.checkHealth()
            result.onSuccess { health ->
                connectionTestResult.value = "Connected! Model: ${health.modelType ?: "Active"}"
            }.onFailure { ex ->
                connectionTestResult.value = "Failed: ${ex.localizedMessage}"
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// -------------------------------------------------------------
// VIEWMODEL FACTORY
// -------------------------------------------------------------
class CreditLensViewModelFactory(
    private val repository: CreditLensRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(AssessmentViewModel::class.java) ->
                AssessmentViewModel(repository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
