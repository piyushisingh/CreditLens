"""
ML Pipeline Configuration & Constants for CreditLens
====================================================
Defines feature schemas, validation limits, baseline modeling assumptions,
and model hyperparameters.
"""

from typing import List

# Features required directly from user / loan application
RAW_FEATURE_COLS: List[str] = [
    "credit_lines_outstanding",
    "loan_amt_outstanding",
    "total_debt_outstanding",
    "income",
    "years_employed",
    "fico_score",
]

# Target column in training data (1 = Default, 0 = Non-default)
TARGET_COL: str = "default"

# Derived financial ratios
DERIVED_FEATURE_COLS: List[str] = [
    "debt_to_income",
    "loan_to_income",
    "debt_per_credit_line",
]

# Complete feature vector in exact order expected by the estimator
ALL_FEATURE_COLS: List[str] = RAW_FEATURE_COLS + DERIVED_FEATURE_COLS

# Baseline Modeling Assumptions (Research Prototype)
# NOTE: 10% recovery rate assumption -> LGD = 0.90 baseline.
# This is an exploratory prototype assumption, not an empirical or regulatory standard.
DEFAULT_RECOVERY_RATE: float = 0.10
DEFAULT_LGD: float = 1.0 - DEFAULT_RECOVERY_RATE

# Input Validation Boundaries
FICO_MIN: int = 300
FICO_MAX: int = 850
INCOME_MIN: float = 0.0
LOAN_AMT_MIN: float = 0.0
TOTAL_DEBT_MIN: float = 0.0
CREDIT_LINES_MIN: int = 0
YEARS_EMPLOYED_MIN: float = 0.0

# Stratified Split & Random Seed
RANDOM_STATE: int = 42
TEST_SIZE: float = 0.20
CV_FOLDS: int = 5
