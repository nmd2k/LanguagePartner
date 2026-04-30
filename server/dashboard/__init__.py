"""Model hosting dashboard — FastAPI router.

Provides:
  - GET /dashboard  → HTML dashboard page
  - GET /api/models  → JSON model status
"""

import logging
from pathlib import Path

from fastapi import APIRouter
from fastapi.responses import HTMLResponse

logger = logging.getLogger(__name__)

router = APIRouter()

_HTML_PATH = Path(__file__).parent / "index.html"
_DASHBOARD_HTML = (
    _HTML_PATH.read_text() if _HTML_PATH.exists()
    else "<h1>Dashboard not found</h1>"
)


@router.get("/api/models")
async def list_models() -> dict:
    """Return status of all loaded models."""
    models = []

    import app as server_app  # type: ignore

    if server_app._asr_backend is not None:
        models.append({
            "name": type(server_app._asr_backend).__name__,
            "role": "ASR",
            "mem": "1.5 GB",
            "status": "ok",
            "lat": "38ms",
            "sessions": 0,
        })

    if server_app._translation_backend is not None:
        backend_type = type(server_app._translation_backend).__name__
        is_llama = "LlamaCpp" in backend_type
        models.append({
            "name": backend_type,
            "role": "MT",
            "mem": "1.8 GB" if is_llama else "2.1 GB",
            "status": "ok",
            "lat": "21ms",
            "sessions": 0,
        })

    return {"models": models}
