"""
Feature Engineering and Input Validation for CreditLens
=======================================================
Implements:
1. FinancialRatioTransformer: A leak-free scikit-learn compatible transformer.
2. validate_borrower_input: Input boundary, type, and finiteness validation.
3. calculate_expected_loss: Expected Credit Loss tri-factor identity (PD * LGD * EAD).
"""

import math
from typing import Any, Dict, List, Optional, Union
import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, TransformerMixin

from backend.ml.config import (
    ALL_FEATURE_COLS,
    DEFAULT_LGD,
    DEFAULT_RECOVERY_RATE,
    DERIVED_FEATURE_COLS,
    FICO_MAX,
    FICO_MIN,
    RAW_FEATURE_COLS,
)


def _validate_numeric(val: Any, field_name: str) -> float:
    """Validates that a value is a finite number (rejects booleans, NaN, and infinity)."""
    if isinstance(val, bool):
        raise ValueError(f"Field '{field_name}' must be numeric, got boolean: {val}")
    try:
        f = float(val)
    except (ValueError, TypeError):
        raise ValueError(f"Field '{field_name}' must be numeric, got: {val}")
    if math.isnan(f) or math.isinf(f):
        raise ValueError(f"Field '{field_name}' cannot be NaN or infinity")
    return f


def _validate_integer(val: Any, field_name: str) -> int:
    """Validates that a value is an integer (rejects fractional values, booleans, NaN, and infinity)."""
    if isinstance(val, bool):
        raise ValueError(f"Field '{field_name}' must be an integer, got boolean: {val}")
    f = _validate_numeric(val, field_name)
    if not f.is_integer():
        raise ValueError(f"Field '{field_name}' must be an integer, got fractional value: {val}")
    return int(f)


class FinancialRatioTransformer(BaseEstimator, TransformerMixin):
    """
    Scikit-learn compatible transformer that computes derived financial ratios:
      - debt_to_income = total_debt_outstanding / income (0.0 if income <= 0)
      - loan_to_income = loan_amt_outstanding / income (0.0 if income <= 0)
      - debt_per_credit_line = total_debt_outstanding / (credit_lines_outstanding + 1)

    Operates seamlessly inside an sklearn Pipeline to prevent preprocessing leakage.
    Ensures identical behavior across pandas DataFrame and numpy ndarray inputs.
    """

    def __init__(self) -> None:
        pass

    def fit(self, X: Union[pd.DataFrame, np.ndarray], y: Optional[Any] = None) -> "FinancialRatioTransformer":
        return self

    def transform(self, X: Union[pd.DataFrame, np.ndarray]) -> Union[pd.DataFrame, np.ndarray]:
        if isinstance(X, pd.DataFrame):
            return self._transform_dataframe(X)
        elif isinstance(X, np.ndarray):
            return self._transform_numpy(X)
        else:
            raise TypeError(f"Expected pandas DataFrame or numpy ndarray, got {type(X)}")

    def _transform_dataframe(self, df: pd.DataFrame) -> pd.DataFrame:
        out = df.copy()

        # Handle zero or negative income identically to numpy (yield 0.0 when income <= 0)
        income = out["income"].to_numpy(dtype=float)
        total_debt = out["total_debt_outstanding"].to_numpy(dtype=float)
        loan_amt = out["loan_amt_outstanding"].to_numpy(dtype=float)
        credit_lines = out["credit_lines_outstanding"].to_numpy(dtype=float)

        with np.errstate(divide="ignore", invalid="ignore"):
            out["debt_to_income"] = np.where(income > 0, total_debt / income, 0.0)
            out["loan_to_income"] = np.where(income > 0, loan_amt / income, 0.0)
            out["debt_per_credit_line"] = total_debt / (credit_lines + 1.0)

        return out[ALL_FEATURE_COLS]

    def _transform_numpy(self, arr: np.ndarray) -> np.ndarray:
        if arr.shape[1] != len(RAW_FEATURE_COLS):
            raise ValueError(
                f"Expected numpy array with {len(RAW_FEATURE_COLS)} columns, got {arr.shape[1]}"
            )

        credit_lines = arr[:, 0].astype(float)
        loan_amt = arr[:, 1].astype(float)
        total_debt = arr[:, 2].astype(float)
        income = arr[:, 3].astype(float)

        with np.errstate(divide="ignore", invalid="ignore"):
            debt_to_income = np.where(income > 0, total_debt / income, 0.0)
            loan_to_income = np.where(income > 0, loan_amt / income, 0.0)
            debt_per_credit_line = total_debt / (credit_lines + 1.0)

        derived = np.column_stack([debt_to_income, loan_to_income, debt_per_credit_line])
        return np.hstack([arr, derived])

    def get_feature_names_out(self, input_features: Optional[List[str]] = None) -> np.ndarray:
        return np.array(ALL_FEATURE_COLS)


def compute_derived_ratios(data: Dict[str, Union[int, float]]) -> Dict[str, float]:
    """
    Computes derived ratios for a single dictionary record, ensuring identical
    feature engineering between training and runtime inference.
    """
    income = float(data.get("income", 0.0))
    total_debt = float(data.get("total_debt_outstanding", 0.0))
    loan_amt = float(data.get("loan_amt_outstanding", 0.0))
    credit_lines = float(data.get("credit_lines_outstanding", 0.0))

    debt_to_income = (total_debt / income) if income > 0 else 0.0
    loan_to_income = (loan_amt / income) if income > 0 else 0.0
    debt_per_credit_line = total_debt / (credit_lines + 1.0)

    return {
        "debt_to_income": float(debt_to_income),
        "loan_to_income": float(loan_to_income),
        "debt_per_credit_line": float(debt_per_credit_line),
    }


def validate_borrower_input(payload: Dict[str, Any]) -> Dict[str, Union[int, float]]:
    """
    Validates single-borrower inputs against boundary, type, and finiteness requirements.
    Rejects fractional values for integer fields, booleans, NaN, and infinity.
    Raises ValueError with descriptive feedback on invalid inputs.
    """
    for col in RAW_FEATURE_COLS:
        if col not in payload:
            raise ValueError(f"Missing required input feature: '{col}'")

    credit_lines = _validate_integer(payload["credit_lines_outstanding"], "credit_lines_outstanding")
    loan_amt = _validate_numeric(payload["loan_amt_outstanding"], "loan_amt_outstanding")
    total_debt = _validate_numeric(payload["total_debt_outstanding"], "total_debt_outstanding")
    income = _validate_numeric(payload["income"], "income")
    years_employed = _validate_numeric(payload["years_employed"], "years_employed")
    fico_score = _validate_integer(payload["fico_score"], "fico_score")

    # Boundary checks
    if credit_lines < 0:
        raise ValueError("credit_lines_outstanding cannot be negative")
    if loan_amt < 0.0:
        raise ValueError("loan_amt_outstanding cannot be negative")
    if total_debt < 0.0:
        raise ValueError("total_debt_outstanding cannot be negative")
    if income < 0.0:
        raise ValueError("income cannot be negative")
    if years_employed < 0.0:
        raise ValueError("years_employed cannot be negative")
    if not (FICO_MIN <= fico_score <= FICO_MAX):
        raise ValueError(f"fico_score must be between {FICO_MIN} and {FICO_MAX}, got {fico_score}")

    return {
        "credit_lines_outstanding": credit_lines,
        "loan_amt_outstanding": loan_amt,
        "total_debt_outstanding": total_debt,
        "income": income,
        "years_employed": years_employed,
        "fico_score": fico_score,
    }


def calculate_expected_loss(
    pd_value: float,
    loan_amt_outstanding: float,
    recovery_rate: float = DEFAULT_RECOVERY_RATE,
) -> Dict[str, float]:
    """
    Calculates Expected Credit Loss (ECL):
        ECL = PD * LGD * EAD
    where:
        PD  = Probability of Default [0.0, 1.0]
        LGD = 1.0 - recovery_rate (default 0.90)
        EAD = Outstanding loan balance

    Note: This mathematical calculation is an exploratory research formulation
    and does not constitute regulatory certification or formal compliance.
    """
    pd_val = _validate_numeric(pd_value, "probability_of_default")
    loan_amt = _validate_numeric(loan_amt_outstanding, "loan_amt_outstanding")
    rec_rate = _validate_numeric(recovery_rate, "recovery_rate")

    if not (0.0 <= pd_val <= 1.0):
        raise ValueError(f"Probability of default must be in [0.0, 1.0], got {pd_val}")
    if loan_amt < 0.0:
        raise ValueError(f"loan_amt_outstanding cannot be negative, got {loan_amt}")
    if not (0.0 <= rec_rate <= 1.0):
        raise ValueError(f"recovery_rate must be in [0.0, 1.0], got {rec_rate}")

    lgd = 1.0 - rec_rate
    ead = float(loan_amt)
    expected_loss = pd_val * lgd * ead

    return {
        "probability_of_default": round(float(pd_val), 6),
        "loss_given_default": round(float(lgd), 4),
        "exposure_at_default": round(float(ead), 2),
        "expected_loss": round(float(expected_loss), 2),
    }
