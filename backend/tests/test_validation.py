"""
Unit & Regression Tests for Input Validation and Expected Loss Calculations
"""

import unittest
import math
from backend.ml.features import (
    calculate_expected_loss,
    validate_borrower_input,
)


class TestValidationAndExpectedLoss(unittest.TestCase):

    def setUp(self):
        self.valid_payload = {
            "credit_lines_outstanding": 2,
            "loan_amt_outstanding": 10000.0,
            "total_debt_outstanding": 25000.0,
            "income": 60000.0,
            "years_employed": 4.5,
            "fico_score": 720,
        }

    def test_valid_input_passes(self):
        validated = validate_borrower_input(self.valid_payload)
        self.assertEqual(validated["credit_lines_outstanding"], 2)
        self.assertEqual(validated["fico_score"], 720)
        self.assertIsInstance(validated["income"], float)

    def test_missing_field_raises(self):
        invalid = self.valid_payload.copy()
        del invalid["income"]
        with self.assertRaises(ValueError) as ctx:
            validate_borrower_input(invalid)
        self.assertIn("Missing required input feature: 'income'", str(ctx.exception))

    def test_negative_income_raises(self):
        invalid = self.valid_payload.copy()
        invalid["income"] = -1000.0
        with self.assertRaises(ValueError) as ctx:
            validate_borrower_input(invalid)
        self.assertIn("income cannot be negative", str(ctx.exception))

    def test_negative_loan_amt_raises(self):
        invalid = self.valid_payload.copy()
        invalid["loan_amt_outstanding"] = -500.0
        with self.assertRaises(ValueError) as ctx:
            validate_borrower_input(invalid)
        self.assertIn("loan_amt_outstanding cannot be negative", str(ctx.exception))

    def test_fico_boundary_validation(self):
        # Below minimum 300
        invalid_low = self.valid_payload.copy()
        invalid_low["fico_score"] = 280
        with self.assertRaises(ValueError):
            validate_borrower_input(invalid_low)

        # Above maximum 850
        invalid_high = self.valid_payload.copy()
        invalid_high["fico_score"] = 900
        with self.assertRaises(ValueError):
            validate_borrower_input(invalid_high)

    # -------------------------------------------------------------
    # REGRESSION TESTS: Fractional values for integer fields
    # -------------------------------------------------------------
    def test_reject_fractional_credit_lines(self):
        # Float fraction
        invalid = self.valid_payload.copy()
        invalid["credit_lines_outstanding"] = 2.5
        with self.assertRaises(ValueError) as ctx:
            validate_borrower_input(invalid)
        self.assertIn("must be an integer", str(ctx.exception))

        # String fraction
        invalid_str = self.valid_payload.copy()
        invalid_str["credit_lines_outstanding"] = "2.5"
        with self.assertRaises(ValueError):
            validate_borrower_input(invalid_str)

    def test_reject_fractional_fico_score(self):
        # Float fraction
        invalid = self.valid_payload.copy()
        invalid["fico_score"] = 720.5
        with self.assertRaises(ValueError) as ctx:
            validate_borrower_input(invalid)
        self.assertIn("must be an integer", str(ctx.exception))

        # String fraction
        invalid_str = self.valid_payload.copy()
        invalid_str["fico_score"] = "720.5"
        with self.assertRaises(ValueError):
            validate_borrower_input(invalid_str)

    # -------------------------------------------------------------
    # REGRESSION TESTS: NaN, Infinity & Boolean rejection
    # -------------------------------------------------------------
    def test_reject_nan_values(self):
        for field in ["income", "loan_amt_outstanding", "total_debt_outstanding", "years_employed", "credit_lines_outstanding", "fico_score"]:
            invalid = self.valid_payload.copy()
            invalid[field] = float("nan")
            with self.assertRaises(ValueError, msg=f"Failed to reject NaN in {field}") as ctx:
                validate_borrower_input(invalid)
            self.assertIn("cannot be NaN or infinity", str(ctx.exception))

    def test_reject_infinity_values(self):
        for field in ["income", "loan_amt_outstanding", "total_debt_outstanding", "years_employed"]:
            invalid_pos = self.valid_payload.copy()
            invalid_pos[field] = float("inf")
            with self.assertRaises(ValueError, msg=f"Failed to reject +inf in {field}"):
                validate_borrower_input(invalid_pos)

            invalid_neg = self.valid_payload.copy()
            invalid_neg[field] = float("-inf")
            with self.assertRaises(ValueError, msg=f"Failed to reject -inf in {field}"):
                validate_borrower_input(invalid_neg)

    def test_reject_boolean_types(self):
        for field in ["credit_lines_outstanding", "fico_score", "income", "loan_amt_outstanding"]:
            invalid = self.valid_payload.copy()
            invalid[field] = True
            with self.assertRaises(ValueError, msg=f"Failed to reject boolean in {field}"):
                validate_borrower_input(invalid)

    # -------------------------------------------------------------
    # Expected Loss Calculations & Validation
    # -------------------------------------------------------------
    def test_expected_loss_formula(self):
        result = calculate_expected_loss(
            pd_value=0.20,
            loan_amt_outstanding=100000.0,
            recovery_rate=0.10,
        )
        self.assertEqual(result["probability_of_default"], 0.20)
        self.assertEqual(result["loss_given_default"], 0.90)
        self.assertEqual(result["exposure_at_default"], 100000.0)
        self.assertEqual(result["expected_loss"], 18000.0)

    def test_expected_loss_invalid_pd_raises(self):
        with self.assertRaises(ValueError):
            calculate_expected_loss(pd_value=1.5, loan_amt_outstanding=10000)

        with self.assertRaises(ValueError):
            calculate_expected_loss(pd_value=-0.1, loan_amt_outstanding=10000)

    def test_expected_loss_reject_nan_inf(self):
        # NaN in pd
        with self.assertRaises(ValueError):
            calculate_expected_loss(pd_value=float("nan"), loan_amt_outstanding=10000)

        # Inf in loan_amt
        with self.assertRaises(ValueError):
            calculate_expected_loss(pd_value=0.10, loan_amt_outstanding=float("inf"))

        # NaN in recovery rate
        with self.assertRaises(ValueError):
            calculate_expected_loss(pd_value=0.10, loan_amt_outstanding=10000, recovery_rate=float("nan"))

        # Boolean in pd
        with self.assertRaises(ValueError):
            calculate_expected_loss(pd_value=True, loan_amt_outstanding=10000)


if __name__ == "__main__":
    unittest.main()
