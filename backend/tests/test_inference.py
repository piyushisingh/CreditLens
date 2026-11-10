"""
Unit & Integration Tests for Inference Engine
"""

import unittest
from backend.ml.inference import (
    load_model_pipeline,
    predict_loan_default,
)


class TestInferenceEngine(unittest.TestCase):

    def test_artifact_loads_successfully(self):
        pipeline = load_model_pipeline()
        self.assertIsNotNone(pipeline)
        self.assertTrue(hasattr(pipeline, "predict"))
        self.assertTrue(hasattr(pipeline, "predict_proba"))

    def test_low_risk_borrower_prediction(self):
        low_risk = {
            "credit_lines_outstanding": 0,
            "loan_amt_outstanding": 3000.0,
            "total_debt_outstanding": 2000.0,
            "income": 80000.0,
            "years_employed": 8.0,
            "fico_score": 750,
        }
        res = predict_loan_default(low_risk)

        self.assertEqual(res["default_prediction"], 0)
        self.assertLess(res["probability_of_default"], 0.20)
        self.assertAlmostEqual(res["loss_given_default"], 0.90)
        self.assertAlmostEqual(res["exposure_at_default"], 3000.0)
        self.assertGreaterEqual(res["expected_loss"], 0.0)
        self.assertIn("debt_to_income", res["derived_ratios"])

    def test_high_risk_borrower_prediction(self):
        high_risk = {
            "credit_lines_outstanding": 5,
            "loan_amt_outstanding": 8000.0,
            "total_debt_outstanding": 35000.0,
            "income": 30000.0,
            "years_employed": 1.0,
            "fico_score": 500,
        }
        res = predict_loan_default(high_risk)

        self.assertEqual(res["default_prediction"], 1)
        self.assertGreater(res["probability_of_default"], 0.80)
        self.assertAlmostEqual(res["exposure_at_default"], 8000.0)
        self.assertGreater(res["expected_loss"], 0.0)

    def test_invalid_input_rejected_during_inference(self):
        invalid_borrower = {
            "credit_lines_outstanding": 2.5,  # Fractional integer
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 10000.0,
            "income": 50000.0,
            "years_employed": 3.0,
            "fico_score": 700,
        }
        with self.assertRaises(ValueError):
            predict_loan_default(invalid_borrower)


if __name__ == "__main__":
    unittest.main()
