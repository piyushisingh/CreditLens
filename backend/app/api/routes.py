"""
API Endpoints for CreditLens
============================
Defines /api/health and /api/predict routes.
"""

import logging
from fastapi import APIRouter, HTTPException, Request, status
from backend.app.schemas.loan import (
    DerivedRatiosResponse,
    HealthResponse,
    LoanPredictionRequest,
    LoanPredictionResponse,
)
from backend.ml.inference import (
    load_model_pipeline,
    predict_loan_default,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api", tags=["Credit Risk Assessment"])


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health Check",
    description="Returns service status and ML model loaded state.",
)
async def health_check(request: Request) -> HealthResponse:
    model_loaded = getattr(request.app.state, "model_loaded", False)
    model_type = getattr(request.app.state, "model_type", None)

    # Verify or initialize model loading
    if not model_loaded:
        try:
            pipeline = load_model_pipeline()
            model_loaded = True
            model_type = type(pipeline.named_steps["classifier"]).__name__
            request.app.state.model_loaded = True
            request.app.state.model_type = model_type
        except Exception as e:
            logger.warning(f"Health check model load failed: {e}")
            model_loaded = False

    return HealthResponse(
        status="healthy" if model_loaded else "degraded",
        model_loaded=model_loaded,
        model_type=model_type,
        version="0.1.0",
    )


@router.post(
    "/predict",
    response_model=LoanPredictionResponse,
    status_code=status.HTTP_200_OK,
    summary="Assess Loan Default Risk & Expected Loss",
    description="Computes default classification, Probability of Default (PD), and Expected Credit Loss (ECL).",
)
async def predict_loan(payload: LoanPredictionRequest, request: Request) -> LoanPredictionResponse:
    if not getattr(request.app.state, "model_loaded", False):
        try:
            pipeline = load_model_pipeline()
            request.app.state.model_loaded = True
            request.app.state.model_type = type(pipeline.named_steps["classifier"]).__name__
        except FileNotFoundError:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Model pipeline artifact is missing or unavailable.",
            )
        except Exception as e:
            logger.error(f"Model loading error: {e}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=f"Failed to load model pipeline: {str(e)}",
            )

    try:
        raw_dict = payload.model_dump()
        recovery_rate = raw_dict.pop("recovery_rate", 0.10)
        result = predict_loan_default(raw_dict, recovery_rate=recovery_rate)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        logger.error(f"Inference error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred during risk model inference.",
        )

    return LoanPredictionResponse(
        default_prediction=result["default_prediction"],
        probability_of_default=result["probability_of_default"],
        loss_given_default=result["loss_given_default"],
        exposure_at_default=result["exposure_at_default"],
        expected_loss=result["expected_loss"],
        derived_ratios=DerivedRatiosResponse(**result["derived_ratios"]),
    )
