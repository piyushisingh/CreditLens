"""
CreditLens FastAPI Application Entry Point
==========================================
Configures lifespan startup/shutdown, CORS for Android emulator & web,
OpenAPI documentation, and API routers.
"""

from contextlib import asynccontextmanager
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse

from backend.app.api.routes import router as api_router
from backend.ml.inference import load_model_pipeline

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan context manager.
    Loads the trained scikit-learn model pipeline once at startup.
    """
    logger.info("Initializing CreditLens API startup...")
    try:
        pipeline = load_model_pipeline()
        app.state.model_loaded = True
        app.state.model_type = type(pipeline.named_steps["classifier"]).__name__
        logger.info(f"Model pipeline loaded successfully: {app.state.model_type}")
    except Exception as e:
        app.state.model_loaded = False
        app.state.model_type = None
        logger.error(f"Failed to load model pipeline at startup: {e}")

    yield

    logger.info("Shutting down CreditLens API...")


app = FastAPI(
    title="CreditLens API",
    description=(
        "Credit Risk & Expected Loss Prediction Service (Research Prototype). "
        "Provides default probability estimation and Expected Credit Loss (ECL) calculation "
        "for the native Android CreditLens mobile client."
    ),
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS configuration for Android emulator (10.0.2.2) and local frontend development
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "*",  # Permissive for development/emulator access
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount API routes
app.include_router(api_router)


@app.get("/", include_in_schema=False)
async def root_redirect():
    """Redirect root path to interactive Swagger documentation."""
    return RedirectResponse(url="/docs")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.app.main:app", host="0.0.0.0", port=8000, reload=True)
