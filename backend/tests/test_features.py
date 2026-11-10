"""
Unit & Regression Tests for Feature Engineering & Pipeline Transformation
"""

import unittest
from unittest.mock import patch
import numpy as np
import pandas as pd
from sklearn.base import clone, is_classifier
from sklearn.exceptions import NotFittedError
from sklearn.model_selection import cross_val_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression

from backend.ml.features import (
    FinancialRatioTransformer,
    compute_derived_ratios,
)
from backend.ml.config import ALL_FEATURE_COLS, RAW_FEATURE_COLS
from backend.ml.pipeline import HAS_XGBOOST, LeakageSafeXGBClassifier, get_candidate_models


class TestFinancialRatioTransformer(unittest.TestCase):

    def setUp(self):
        self.sample_df = pd.DataFrame([{
            "credit_lines_outstanding": 2,
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 15000.0,
            "income": 50000.0,
            "years_employed": 5.0,
            "fico_score": 680,
        }])

    def test_ratio_calculations_dataframe(self):
        transformer = FinancialRatioTransformer()
        transformed = transformer.transform(self.sample_df)

        self.assertListEqual(list(transformed.columns), ALL_FEATURE_COLS)
        self.assertAlmostEqual(transformed["debt_to_income"].iloc[0], 15000.0 / 50000.0)
        self.assertAlmostEqual(transformed["loan_to_income"].iloc[0], 5000.0 / 50000.0)
        self.assertAlmostEqual(transformed["debt_per_credit_line"].iloc[0], 15000.0 / (2 + 1))

    def test_zero_income_handling(self):
        df_zero = self.sample_df.copy()
        df_zero["income"] = 0.0

        transformer = FinancialRatioTransformer()
        transformed = transformer.transform(df_zero)

        self.assertEqual(transformed["debt_to_income"].iloc[0], 0.0)
        self.assertEqual(transformed["loan_to_income"].iloc[0], 0.0)
        self.assertFalse(transformed.isna().any().any())

    def test_zero_credit_lines_handling(self):
        df_zero_lines = self.sample_df.copy()
        df_zero_lines["credit_lines_outstanding"] = 0

        transformer = FinancialRatioTransformer()
        transformed = transformer.transform(df_zero_lines)

        self.assertAlmostEqual(transformed["debt_per_credit_line"].iloc[0], 15000.0 / 1.0)

    def test_numpy_array_transformation(self):
        arr = self.sample_df[RAW_FEATURE_COLS].values
        transformer = FinancialRatioTransformer()
        transformed = transformer.transform(arr)

        self.assertEqual(transformed.shape, (1, 9))
        self.assertAlmostEqual(transformed[0, 6], 15000.0 / 50000.0)
        self.assertAlmostEqual(transformed[0, 7], 5000.0 / 50000.0)
        self.assertAlmostEqual(transformed[0, 8], 15000.0 / 3.0)

    def test_dataframe_and_numpy_identity_zero_and_negative_income(self):
        records = [
            {"credit_lines_outstanding": 3, "loan_amt_outstanding": 6000.0, "total_debt_outstanding": 18000.0, "income": 60000.0, "years_employed": 4.0, "fico_score": 700},
            {"credit_lines_outstanding": 1, "loan_amt_outstanding": 4000.0, "total_debt_outstanding": 10000.0, "income": 0.0, "years_employed": 1.0, "fico_score": 600},
            {"credit_lines_outstanding": 0, "loan_amt_outstanding": 2000.0, "total_debt_outstanding": 5000.0, "income": -1000.0, "years_employed": 0.0, "fico_score": 550},
        ]
        df = pd.DataFrame(records)
        arr = df[RAW_FEATURE_COLS].values

        transformer = FinancialRatioTransformer()
        df_out = transformer.transform(df)
        np_out = transformer.transform(arr)

        np.testing.assert_allclose(df_out.values, np_out, rtol=1e-7, atol=1e-7)
        self.assertEqual(df_out["debt_to_income"].iloc[1], 0.0)
        self.assertEqual(df_out["loan_to_income"].iloc[1], 0.0)
        self.assertEqual(df_out["debt_to_income"].iloc[2], 0.0)
        self.assertEqual(df_out["loan_to_income"].iloc[2], 0.0)

    def test_training_and_inference_feature_consistency(self):
        """Verifies feature names, order, and values match across DataFrame, NumPy, and dict."""
        payload = {
            "credit_lines_outstanding": 3,
            "loan_amt_outstanding": 7500.0,
            "total_debt_outstanding": 22500.0,
            "income": 75000.0,
            "years_employed": 6.0,
            "fico_score": 710,
        }
        df_in = pd.DataFrame([payload])[RAW_FEATURE_COLS]
        arr_in = df_in.values

        transformer = FinancialRatioTransformer()
        df_out = transformer.transform(df_in)
        np_out = transformer.transform(arr_in)
        dict_out = compute_derived_ratios(payload)

        # Check column names and ordering
        self.assertListEqual(list(df_out.columns), ALL_FEATURE_COLS)

        # Check values consistency
        self.assertAlmostEqual(df_out["debt_to_income"].iloc[0], dict_out["debt_to_income"])
        self.assertAlmostEqual(df_out["loan_to_income"].iloc[0], dict_out["loan_to_income"])
        self.assertAlmostEqual(df_out["debt_per_credit_line"].iloc[0], dict_out["debt_per_credit_line"])
        self.assertAlmostEqual(np_out[0, 6], dict_out["debt_to_income"])
        self.assertAlmostEqual(np_out[0, 7], dict_out["loan_to_income"])
        self.assertAlmostEqual(np_out[0, 8], dict_out["debt_per_credit_line"])


class TestLeakageSafeXGBoostClassifier(unittest.TestCase):

    def setUp(self):
        if not HAS_XGBOOST or LeakageSafeXGBClassifier is None:
            self.skipTest("XGBoost not installed")
        self.X = np.array([
            [1, 1000, 2000, 50000, 3, 700],
            [2, 2000, 4000, 40000, 2, 650],
            [3, 3000, 6000, 30000, 1, 600],
            [4, 4000, 8000, 20000, 0, 550],
        ], dtype=float)
        self.y = np.array([0, 0, 0, 1])

    def test_sklearn_is_classifier_and_estimator_type(self):
        clf = LeakageSafeXGBClassifier()
        self.assertTrue(is_classifier(clf))
        self.assertEqual(clf._estimator_type, "classifier")

    def test_sklearn_clone_get_set_params(self):
        clf = LeakageSafeXGBClassifier(n_estimators=35, max_depth=4, learning_rate=0.05)
        cloned = clone(clf)
        self.assertEqual(cloned.n_estimators, 35)
        self.assertEqual(cloned.max_depth, 4)
        self.assertEqual(cloned.learning_rate, 0.05)

        cloned.set_params(n_estimators=70, max_depth=6)
        self.assertEqual(cloned.n_estimators, 70)
        self.assertEqual(cloned.max_depth, 6)

    def test_not_fitted_error_on_unfitted_estimator(self):
        clf = LeakageSafeXGBClassifier()
        with self.assertRaises(NotFittedError):
            clf.predict(self.X)
        with self.assertRaises(NotFittedError):
            clf.predict_proba(self.X)

    def test_predict_and_predict_proba_delegation(self):
        clf = LeakageSafeXGBClassifier(n_estimators=10, max_depth=2, random_state=42)
        clf.fit(self.X, self.y)

        preds = clf.predict(self.X)
        probs = clf.predict_proba(self.X)

        self.assertEqual(preds.shape, (4,))
        self.assertEqual(probs.shape, (4, 2))
        np.testing.assert_allclose(probs.sum(axis=1), 1.0, rtol=1e-5)
        self.assertAlmostEqual(clf.scale_pos_weight_, 3.0)

    def test_cross_val_score_roc_auc_integration(self):
        pipe = Pipeline([
            ("ratios", FinancialRatioTransformer()),
            ("classifier", LeakageSafeXGBClassifier(n_estimators=10, max_depth=2, random_state=42)),
        ])
        # Synthetic dataset with sufficient samples for 2-fold cross-validation
        rng = np.random.RandomState(42)
        X_df = pd.DataFrame(rng.randn(40, 6) * 1000 + 40000, columns=RAW_FEATURE_COLS)
        X_df["credit_lines_outstanding"] = rng.randint(0, 8, 40)
        X_df["fico_score"] = rng.randint(300, 850, 40)
        y_ser = pd.Series(rng.randint(0, 2, 40))

        scores = cross_val_score(pipe, X_df, y_ser, cv=2, scoring="roc_auc")
        self.assertEqual(len(scores), 2)
        self.assertTrue(all(not np.isnan(s) for s in scores))
        self.assertTrue(all(0.0 <= s <= 1.0 for s in scores))

    def test_optional_xgboost_fallback_when_unavailable(self):
        with patch("backend.ml.pipeline.HAS_XGBOOST", False):
            candidates = get_candidate_models()
            self.assertNotIn("XGBoost", candidates)
            self.assertIn("Logistic Regression", candidates)
            self.assertIn("Decision Tree", candidates)
            self.assertIn("Random Forest", candidates)


if __name__ == "__main__":
    unittest.main()
