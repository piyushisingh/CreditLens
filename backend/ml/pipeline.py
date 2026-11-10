"""
Scikit-Learn Pipeline Assembly for CreditLens
=============================================
Builds complete, leak-free pipelines containing:
  1. FinancialRatioTransformer (feature engineering)
  2. StandardScaler (for linear models only)
  3. Classifier (Logistic Regression, Decision Tree, Random Forest, or XGBoost)

Handles class weighting dynamically in fit() to prevent cross-validation leakage.
"""

import logging
from typing import Any, Dict
import numpy as np
from sklearn.base import BaseEstimator, ClassifierMixin
from sklearn.utils.validation import check_is_fitted
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier

from backend.ml.features import FinancialRatioTransformer
from backend.ml.config import RANDOM_STATE

logger = logging.getLogger(__name__)

# Transparent XGBoost handling
try:
    from xgboost import XGBClassifier
    HAS_XGBOOST = True
except (ImportError, Exception):
    HAS_XGBOOST = False
    XGBClassifier = None


if HAS_XGBOOST:
    class LeakageSafeXGBClassifier(ClassifierMixin, BaseEstimator):
        """
        Wraps XGBClassifier to compute scale_pos_weight dynamically from y in fit().
        Inherits (ClassifierMixin, BaseEstimator) in strict MRO order to support
        scikit-learn clone(), is_classifier(), get_tags(), and scoring='roc_auc'.
        """
        _estimator_type = "classifier"

        def __init__(
            self,
            n_estimators: int = 200,
            max_depth: int = 5,
            learning_rate: float = 0.1,
            random_state: int = RANDOM_STATE,
            n_jobs: int = -1,
        ) -> None:
            self.n_estimators = n_estimators
            self.max_depth = max_depth
            self.learning_rate = learning_rate
            self.random_state = random_state
            self.n_jobs = n_jobs

        def fit(self, X: Any, y: Any) -> "LeakageSafeXGBClassifier":
            y_arr = np.asarray(y)
            neg = int((y_arr == 0).sum())
            pos = int((y_arr == 1).sum())
            spw = (neg / pos) if pos > 0 else 1.0

            self.estimator_ = XGBClassifier(
                n_estimators=self.n_estimators,
                max_depth=self.max_depth,
                learning_rate=self.learning_rate,
                scale_pos_weight=spw,
                eval_metric="logloss",
                random_state=self.random_state,
                n_jobs=self.n_jobs,
            )
            self.estimator_.fit(X, y_arr)
            self.classes_ = self.estimator_.classes_
            self.scale_pos_weight_ = spw
            return self

        def predict(self, X: Any) -> np.ndarray:
            check_is_fitted(self, "estimator_")
            return self.estimator_.predict(X)

        def predict_proba(self, X: Any) -> np.ndarray:
            check_is_fitted(self, "estimator_")
            return self.estimator_.predict_proba(X)
else:
    LeakageSafeXGBClassifier = None


def get_candidate_models() -> Dict[str, Pipeline]:
    """
    Returns dictionary of candidate model pipelines for cross-validation comparison.
    All pipelines self-encapsulate feature engineering and dynamic class-weighting
    to eliminate cross-validation leakage.
    """
    candidates: Dict[str, Pipeline] = {}

    # 1. Logistic Regression (requires scaling; balanced weights dynamic in fit)
    candidates["Logistic Regression"] = Pipeline([
        ("ratios", FinancialRatioTransformer()),
        ("scaler", StandardScaler()),
        ("classifier", LogisticRegression(
            max_iter=1000,
            class_weight="balanced",
            random_state=RANDOM_STATE,
        )),
    ])

    # 2. Decision Tree (scale-invariant; balanced weights dynamic in fit)
    candidates["Decision Tree"] = Pipeline([
        ("ratios", FinancialRatioTransformer()),
        ("classifier", DecisionTreeClassifier(
            max_depth=6,
            min_samples_leaf=20,
            class_weight="balanced",
            random_state=RANDOM_STATE,
        )),
    ])

    # 3. Random Forest (scale-invariant ensemble; balanced weights dynamic in fit)
    candidates["Random Forest"] = Pipeline([
        ("ratios", FinancialRatioTransformer()),
        ("classifier", RandomForestClassifier(
            n_estimators=200,
            max_depth=8,
            min_samples_leaf=10,
            class_weight="balanced",
            random_state=RANDOM_STATE,
            n_jobs=-1,
        )),
    ])

    # 4. Optional XGBoost (gradient boosted trees; dynamic fold-level class weighting)
    if HAS_XGBOOST and LeakageSafeXGBClassifier is not None:
        candidates["XGBoost"] = Pipeline([
            ("ratios", FinancialRatioTransformer()),
            ("classifier", LeakageSafeXGBClassifier(
                n_estimators=200,
                max_depth=5,
                learning_rate=0.1,
                random_state=RANDOM_STATE,
                n_jobs=-1,
            )),
        ])
    else:
        logger.info("XGBoost is not available in environment; skipping XGBoost candidate.")

    return candidates
