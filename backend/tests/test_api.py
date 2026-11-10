"""
Integration & Regression Tests for CreditLens FastAPI Endpoints
==============================================================
Tests /api/health and /api/predict against valid, boundary, and error cases.
"""

import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient

from backend.app.main import app


class TestCreditLensAPI(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Create TestClient with lifespan context
        cls.client = TestClient(app)

    def test_health_endpoint(self):
        response = self.client.get("/api/health")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "healthy")
        self.assertTrue(data["model_loaded"])
        self.assertEqual(data["model_type"], "LogisticRegression")
        self.assertEqual(data["version"], "0.1.0")

    def test_predict_low_risk_borrower(self):
        payload = {
            "credit_lines_outstanding": 0,
            "loan_amt_outstanding": 3000.0,
            "total_debt_outstanding": 2000.0,
            "income": 80000.0,
            "years_employed": 8.0,
            "fico_score": 750,
            "recovery_rate": 0.10,
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()

        self.assertEqual(data["default_prediction"], 0)
        self.assertLess(data["probability_of_default"], 0.10)
        self.assertAlmostEqual(data["loss_given_default"], 0.90)
        self.assertAlmostEqual(data["exposure_at_default"], 3000.0)
        self.assertGreaterEqual(data["expected_loss"], 0.0)
        self.assertIn("debt_to_income", data["derived_ratios"])
        self.assertIn("disclaimer", data)

    def test_predict_high_risk_borrower(self):
        payload = {
            "credit_lines_outstanding": 5,
            "loan_amt_outstanding": 8000.0,
            "total_debt_outstanding": 35000.0,
            "income": 30000.0,
            "years_employed": 1.0,
            "fico_score": 500,
            "recovery_rate": 0.10,
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()

        self.assertEqual(data["default_prediction"], 1)
        self.assertGreater(data["probability_of_default"], 0.90)
        self.assertAlmostEqual(data["exposure_at_default"], 8000.0)
        self.assertGreater(data["expected_loss"], 5000.0)

    def test_predict_missing_required_field_returns_422(self):
        # Missing 'income'
        payload = {
            "credit_lines_outstanding": 2,
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 15000.0,
            "years_employed": 3.0,
            "fico_score": 700,
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_negative_income_returns_422(self):
        payload = {
            "credit_lines_outstanding": 2,
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 15000.0,
            "income": -1000.0,
            "years_employed": 3.0,
            "fico_score": 700,
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_out_of_range_fico_returns_422(self):
        # FICO score below 300
        payload_low = {
            "credit_lines_outstanding": 2,
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 15000.0,
            "income": 50000.0,
            "years_employed": 3.0,
            "fico_score": 250,
        }
        response = self.client.post("/api/predict", json=payload_low)
        self.assertEqual(response.status_code, 422)

        # FICO score above 850
        payload_high = payload_low.copy()
        payload_high["fico_score"] = 920
        response2 = self.client.post("/api/predict", json=payload_high)
        self.assertEqual(response2.status_code, 422)

    def test_predict_fractional_credit_lines_returns_422(self):
        payload = {
            "credit_lines_outstanding": 2.5,
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 15000.0,
            "income": 50000.0,
            "years_employed": 3.0,
            "fico_score": 700,
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_extra_field_rejected_returns_422(self):
        payload = {
            "credit_lines_outstanding": 2,
            "loan_amt_outstanding": 5000.0,
            "total_debt_outstanding": 15000.0,
            "income": 50000.0,
            "years_employed": 3.0,
            "fico_score": 700,
            "customer_id": 8153374,  # Extra unexpected field
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_model_unavailable_returns_503(self):
        with patch("backend.app.api.routes.load_model_pipeline", side_effect=FileNotFoundError("Artifact missing")):
            with patch.object(app.state, "model_loaded", False):
                payload = {
                    "credit_lines_outstanding": 2,
                    "loan_amt_outstanding": 5000.0,
                    "total_debt_outstanding": 15000.0,
                    "income": 50000.0,
                    "years_employed": 3.0,
                    "fico_score": 700,
                }
                response = self.client.post("/api/predict", json=payload)
                self.assertEqual(response.status_code, 503)
                self.assertIn("Model pipeline artifact is missing or unavailable", response.json()["detail"])

    def test_expected_loss_mathematical_identity(self):
        payload = {
            "credit_lines_outstanding": 1,
            "loan_amt_outstanding": 10000.0,
            "total_debt_outstanding": 5000.0,
            "income": 60000.0,
            "years_employed": 5.0,
            "fico_score": 720,
            "recovery_rate": 0.20,  # LGD = 0.80
        }
        response = self.client.post("/api/predict", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()

        expected = round(data["probability_of_default"] * 0.80 * 10000.0, 2)
        self.assertAlmostEqual(data["expected_loss"], expected, places=1)


if __name__ == "__main__":
    unittest.main()
