"""
Credit Risk Inference Engine for CreditLens
===========================================
Loads the fitted scikit-learn pipeline artifact and evaluates loan default
risk and Expected Credit Loss (ECL) for borrower inputs.
"""

import os
from typing import Any, Dict, Optional
import pandas as pd
import joblib
from sklearn.pipeline import Pipeline

from backend.ml.config import (
    DEFAULT_RECOVERY_RATE,
    RAW_FEATURE_COLS,
)
from backend.ml.features import (
    calculate_expected_loss,
    compute_derived_ratios,
    validate_borrower_input,
)

_MODEL_CACHE: Optional[Pipeline] = None
DEFAULT_ARTIFACT_PATH = os.path.join(
    os.path.dirname(__file__), "artifacts", "model_pipeline.joblib"
)


def load_model_pipeline(artifact_path: Optional[str] = None) -> Pipeline:
    """
    Loads and caches the serialized model pipeline artifact.
    """
    global _MODEL_CACHE
    path = artifact_path or DEFAULT_ARTIFACT_PATH

    if not os.path.exists(path):
        raise FileNotFoundError(
            f"Model artifact not found at: {path}. "
            f"Please run backend/ml/train_export.py first."
        )

    if _MODEL_CACHE is None or artifact_path is not None:
        pipeline = joblib.load(path)
        if artifact_path is None:
            _MODEL_CACHE = pipeline
        return pipeline

    return _MODEL_CACHE


def predict_loan_default(
    borrower_data: Dict[str, Any],
    recovery_rate: float = DEFAULT_RECOVERY_RATE,
    artifact_path: Optional[str] = None,
) -> Dict[str, Any]:
    """
    Performs end-to-end loan default risk assessment:
      1. Validates and sanitizes input data.
      2. Loads the model pipeline.
      3. Predicts default classification and calibrated probability.
      4. Calculates Expected Credit Loss (ECL = PD * LGD * EAD).
      5. Returns detailed decision-support payload.
    """
    validated = validate_borrower_input(borrower_data)
    pipeline = load_model_pipeline(artifact_path)

    # Format into DataFrame with correct raw feature schema
    input_df = pd.DataFrame([validated])[RAW_FEATURE_COLS]

    # Model inference
    default_pred = int(pipeline.predict(input_df)[0])
    pd_value = float(pipeline.predict_proba(input_df)[0][1])

    # ECL calculation
    loan_amt = float(validated["loan_amt_outstanding"])
    ecl_summary = calculate_expected_loss(
        pd_value=pd_value,
        loan_amt_outstanding=loan_amt,
        recovery_rate=recovery_rate,
    )

    derived = compute_derived_ratios(validated)

    return {
        "default_prediction": default_pred,  # 0 = non-default, 1 = default
        "probability_of_default": round(pd_value, 6),
        "loss_given_default": ecl_summary["loss_given_default"],
        "exposure_at_default": ecl_summary["exposure_at_default"],
        "expected_loss": ecl_summary["expected_loss"],
        "derived_ratios": derived,
        "input_features": validated,
    }
