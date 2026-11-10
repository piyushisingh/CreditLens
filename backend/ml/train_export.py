"""
Reproducible Training, Model Selection & Evaluation Pipeline
============================================================
Features:
- Stratified train/test split.
- Training-set-only cross-validation for model selection (preventing test set peeking).
- Pipeline-encapsulated feature engineering and scaling to eliminate leakage.
- Comprehensive test set evaluation: ROC-AUC, Accuracy, Precision, Recall, F1,
  Confusion Matrix, and Brier Score (probability calibration loss).
- Transparent handling of XGBoost with fold-level dynamic class weighting.
- Truthful, complete metadata recording (dataset stats, CV mean/std, versions).
"""

import os
import sys
import json
import logging
from typing import Any, Dict, Optional, Tuple
import numpy as np
import pandas as pd
import sklearn
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.metrics import (
    accuracy_score,
    brier_score_loss,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.pipeline import Pipeline
import joblib

from backend.ml.config import (
    ALL_FEATURE_COLS,
    CV_FOLDS,
    DEFAULT_LGD,
    DEFAULT_RECOVERY_RATE,
    DERIVED_FEATURE_COLS,
    RANDOM_STATE,
    RAW_FEATURE_COLS,
    TARGET_COL,
    TEST_SIZE,
)
from backend.ml.pipeline import get_candidate_models

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def load_dataset(csv_path: str) -> Tuple[pd.DataFrame, pd.Series]:
    """Loads and validates the raw loan dataset."""
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"Training dataset not found at: {csv_path}")

    df = pd.read_csv(csv_path)

    # Validate required columns
    missing_features = [col for col in RAW_FEATURE_COLS if col not in df.columns]
    if missing_features:
        raise ValueError(f"Dataset is missing required features: {missing_features}")
    if TARGET_COL not in df.columns:
        raise ValueError(f"Dataset is missing target column: '{TARGET_COL}'")

    # Drop missing values
    df_clean = df.dropna(subset=RAW_FEATURE_COLS + [TARGET_COL])

    X = df_clean[RAW_FEATURE_COLS].copy()
    y = df_clean[TARGET_COL].astype(int).copy()

    return X, y


def run_model_selection(
    X_train: pd.DataFrame,
    y_train: pd.Series,
) -> Tuple[str, Pipeline, Dict[str, Dict[str, float]]]:
    """
    Performs 5-fold Stratified Cross-Validation on the training set ONLY
    to select the best performing candidate model based on mean CV ROC-AUC.
    """
    candidates = get_candidate_models()
    skf = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=RANDOM_STATE)

    cv_summary: Dict[str, Dict[str, float]] = {}
    cv_means: Dict[str, float] = {}
    logger.info("Starting training-only 5-fold cross-validation...")

    for name, pipeline in candidates.items():
        scores = cross_val_score(
            pipeline,
            X_train,
            y_train,
            cv=skf,
            scoring="roc_auc",
            n_jobs=-1,
        )
        mean_auc = float(scores.mean())
        std_auc = float(scores.std())
        cv_summary[name] = {
            "mean_roc_auc": round(mean_auc, 4),
            "std_roc_auc": round(std_auc, 4),
        }
        cv_means[name] = mean_auc
        logger.info(f"  {name:22s} | Mean CV ROC-AUC: {mean_auc:.4f} (+/- {std_auc:.4f})")

    best_model_name = max(cv_means, key=cv_means.get)
    best_pipeline = candidates[best_model_name]
    logger.info(f"Selected Best Model: {best_model_name} (Mean CV ROC-AUC: {cv_means[best_model_name]:.4f})")

    return best_model_name, best_pipeline, cv_summary


def evaluate_model_on_test(
    pipeline: Pipeline,
    X_test: pd.DataFrame,
    y_test: pd.Series,
) -> Dict[str, Any]:
    """
    Evaluates the fitted pipeline on the held-out test set.
    Calculates ranking (ROC-AUC), classification metrics, confusion matrix,
    and probability calibration error (Brier Score).
    """
    y_pred = pipeline.predict(X_test)
    y_prob = pipeline.predict_proba(X_test)[:, 1]

    auc = float(roc_auc_score(y_test, y_prob))
    acc = float(accuracy_score(y_test, y_pred))
    prec = float(precision_score(y_test, y_pred, zero_division=0))
    rec = float(recall_score(y_test, y_pred, zero_division=0))
    f1 = float(f1_score(y_test, y_pred, zero_division=0))
    cm = confusion_matrix(y_test, y_pred).tolist()
    brier = float(brier_score_loss(y_test, y_prob))

    return {
        "roc_auc": round(auc, 4),
        "accuracy": round(acc, 4),
        "precision": round(prec, 4),
        "recall": round(rec, 4),
        "f1_score": round(f1, 4),
        "brier_score": round(brier, 6),
        "confusion_matrix": cm,
    }


def train_and_evaluate(
    data_path: str,
    export_dir: Optional[str] = None,
) -> Dict[str, Any]:
    """
    Main orchestration routine:
      1. Loads data.
      2. Stratified train/test split (80/20).
      3. Cross-validation on train split for model selection.
      4. Fits the selected pipeline on the full training set.
      5. Evaluates on the held-out test set.
      6. Serializes model artifact if export_dir is provided.
    """
    X, y = load_dataset(data_path)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=RANDOM_STATE, stratify=y
    )

    best_name, best_pipeline, cv_summary = run_model_selection(X_train, y_train)

    # Fit selected pipeline on complete training split
    logger.info(f"Fitting {best_name} on complete training set...")
    best_pipeline.fit(X_train, y_train)

    # Evaluate on held-out test set
    logger.info("Evaluating selected model on held-out test set...")
    test_metrics = evaluate_model_on_test(best_pipeline, X_test, y_test)

    # Library versions
    try:
        import xgboost
        xgb_ver = xgboost.__version__
    except Exception:
        xgb_ver = None

    dataset_stats = {
        "total_samples": int(len(y)),
        "class_distribution": {
            "non_default_0": int((y == 0).sum()),
            "default_1": int((y == 1).sum()),
        },
        "default_rate": round(float((y == 1).mean()), 4),
        "train_samples": int(len(y_train)),
        "test_samples": int(len(y_test)),
    }

    summary = {
        "selected_model": best_name,
        "feature_order": {
            "raw_features": RAW_FEATURE_COLS,
            "derived_features": DERIVED_FEATURE_COLS,
            "all_features": ALL_FEATURE_COLS,
        },
        "dataset_statistics": dataset_stats,
        "cv_roc_auc_scores": cv_summary,
        "test_metrics": test_metrics,
        "assumptions": {
            "recovery_rate_baseline": DEFAULT_RECOVERY_RATE,
            "loss_given_default_baseline": DEFAULT_LGD,
            "note": "Research prototype baseline modeling assumption, not regulatory certified or empirical standard.",
        },
        "library_versions": {
            "python": sys.version.split()[0],
            "scikit_learn": sklearn.__version__,
            "pandas": pd.__version__,
            "numpy": np.__version__,
            "xgboost": xgb_ver,
        },
    }

    if export_dir:
        os.makedirs(export_dir, exist_ok=True)
        model_path = os.path.join(export_dir, "model_pipeline.joblib")
        metadata_path = os.path.join(export_dir, "metadata.json")

        joblib.dump(best_pipeline, model_path)
        with open(metadata_path, "w", encoding="utf-8") as f:
            json.dump(summary, f, indent=2)

        logger.info(f"Saved model pipeline to: {model_path}")
        logger.info(f"Saved metadata to: {metadata_path}")

    return summary


if __name__ == "__main__":
    default_data = os.path.abspath(
        os.path.join(
            os.path.dirname(__file__),
            "..", "..", "..", "loan data set", "Task 3 and 4_Loan_Data.csv"
        )
    )
    data_file = sys.argv[1] if len(sys.argv) > 1 else default_data
    output_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(__file__), "artifacts")

    print("=" * 60)
    print("CreditLens ML Pipeline — Train & Evaluation Run")
    print(f"Data Source: {data_file}")
    print(f"Export Path: {output_dir}")
    print("=" * 60)
    results = train_and_evaluate(data_file, export_dir=output_dir)
    print("\nExecution Summary:")
    print(json.dumps(results, indent=2))
