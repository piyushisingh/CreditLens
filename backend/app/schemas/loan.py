"""
Pydantic Request & Response Schemas for CreditLens API
======================================================
Implements strict boundary validation and type checking.
"""

from typing import Optional
from pydantic import BaseModel, ConfigDict, Field

from backend.ml.config import (
    DEFAULT_RECOVERY_RATE,
    FICO_MAX,
    FICO_MIN,
    INCOME_MIN,
    LOAN_AMT_MIN,
    TOTAL_DEBT_MIN,
    YEARS_EMPLOYED_MIN,
)


class LoanPredictionRequest(BaseModel):
    """
    Borrower credit assessment input features.
    Strictly forbids unexpected fields and enforces domain boundaries.
    """
    model_config = ConfigDict(extra="forbid")

    credit_lines_outstanding: int = Field(
        ...,
        ge=0,
        description="Number of existing active credit lines",
        json_schema_extra={"example": 2},
    )
    loan_amt_outstanding: float = Field(
        ...,
        ge=LOAN_AMT_MIN,
        description="Total balance owed on the current loan request ($)",
        json_schema_extra={"example": 5000.0},
    )
    total_debt_outstanding: float = Field(
        ...,
        ge=TOTAL_DEBT_MIN,
        description="Cumulative outstanding borrower debt ($)",
        json_schema_extra={"example": 15000.0},
    )
    income: float = Field(
        ...,
        ge=INCOME_MIN,
        description="Annual borrower income ($)",
        json_schema_extra={"example": 60000.0},
    )
    years_employed: float = Field(
        ...,
        ge=YEARS_EMPLOYED_MIN,
        description="Number of years in steady employment",
        json_schema_extra={"example": 4.5},
    )
    fico_score: int = Field(
        ...,
        ge=FICO_MIN,
        le=FICO_MAX,
        description="Credit bureau FICO score (300 to 850)",
        json_schema_extra={"example": 720},
    )
    recovery_rate: Optional[float] = Field(
        default=DEFAULT_RECOVERY_RATE,
        ge=0.0,
        le=1.0,
        description="Assumed recovery rate in the event of default (default: 0.10)",
        json_schema_extra={"example": 0.10},
    )


class DerivedRatiosResponse(BaseModel):
    debt_to_income: float = Field(..., description="total_debt_outstanding / income")
    loan_to_income: float = Field(..., description="loan_amt_outstanding / income")
    debt_per_credit_line: float = Field(..., description="total_debt_outstanding / (credit_lines + 1)")


class LoanPredictionResponse(BaseModel):
    default_prediction: int = Field(..., description="0 = Non-default, 1 = Default")
    probability_of_default: float = Field(..., description="Model default probability in [0.0, 1.0]")
    loss_given_default: float = Field(..., description="1.0 - recovery_rate (default: 0.90)")
    exposure_at_default: float = Field(..., description="Exposure at default ($)")
    expected_loss: float = Field(..., description="Expected Credit Loss (PD * LGD * EAD) ($)")
    derived_ratios: DerivedRatiosResponse = Field(..., description="Calculated financial ratios")
    disclaimer: str = Field(
        default="Experimental research prototype output. Not certified for regulatory compliance.",
        description="Prototype notice",
    )


class HealthResponse(BaseModel):
    status: str = Field(..., description="Service health status ('healthy' or 'degraded')")
    model_loaded: bool = Field(..., description="Whether ML pipeline artifact is loaded in memory")
    model_type: Optional[str] = Field(None, description="Classifier architecture")
    version: str = Field("0.1.0", description="API version")
