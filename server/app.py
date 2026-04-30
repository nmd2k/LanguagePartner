"""LanguagePartner server — FastAPI WebSocket handler.

Protocol (see SRS §2.5):
  1. Client connects to ws://<host>:<port>/ws
  2. Client sends a config JSON frame:
       {"type": "config", "sample_rate": 16000, "mode": "speak|read"}
  3. Server enters audio-receive loop: binary frames → VADProcessor
  4. On utterance detection: transcribe → translate → send translation JSON
  5. On inference error: send error JSON

All model calls go through the InferenceBackend ABC — no direct model calls here.
"""
import asyncio
import json
import logging
import uuid
from typing import Optional

from fastapi import FastAPI, WebSocket, WebSocketDisconnect

from backend.base import InferenceBackend
from vad import VADProcessor

logger = logging.getLogger(__name__)

app = FastAPI(title="LanguagePartner Server")

# These are set by the CLI entrypoint (server.py) before uvicorn starts.
_asr_backend: Optional[InferenceBackend] = None
_translation_backend: Optional[InferenceBackend] = None


def configure_backends(
    asr: InferenceBackend,
    translation: InferenceBackend,
) -> None:
    """Inject loaded backends into the app module.

    Called once from server.py after models are loaded.
    """
    global _asr_backend, _translation_backend
    _asr_backend = asr
    _translation_backend = translation


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket) -> None:
    """WebSocket endpoint — accepts audio stream, returns translation JSON."""
    await websocket.accept()
    client = websocket.client
    logger.info("New WebSocket connection from %s:%s", client.host, client.port)

    # ------------------------------------------------------------------
    # Step 1: Wait for config message
    # ------------------------------------------------------------------
    try:
        raw_config = await websocket.receive_text()
        config = json.loads(raw_config)
    except (WebSocketDisconnect, json.JSONDecodeError) as exc:
        logger.warning("Failed to receive config message: %s", exc)
        await websocket.close()
        return

    if config.get("type") != "config":
        logger.warning("First message was not a config frame: %r", config)
        await websocket.close(code=1008)
        return

    sample_rate: int = int(config.get("sample_rate", 16000))
    mode: str = str(config.get("mode", "read"))
    logger.info(
        "Client configured: sample_rate=%d, mode=%s", sample_rate, mode
    )

    # ------------------------------------------------------------------
    # Step 2: Set up VAD with utterance callback
    # ------------------------------------------------------------------
    loop = asyncio.get_event_loop()

    async def send_translation(source_text: str, translated_text: str) -> None:
        payload = {
            "type": "translation",
            "source_text": source_text,
            "translated_text": translated_text,
            "utterance_id": str(uuid.uuid4()),
        }
        try:
            await websocket.send_text(json.dumps(payload, ensure_ascii=False))
            logger.info(
                "Sent translation: src=%r tgt=%r", source_text, translated_text
            )
        except Exception as exc:  # pylint: disable=broad-except
            logger.warning("Failed to send translation result: %s", exc)

    async def send_error(code: str, message: str) -> None:
        payload = {"type": "error", "code": code, "message": message}
        try:
            await websocket.send_text(json.dumps(payload))
        except Exception as exc:  # pylint: disable=broad-except
            logger.warning("Failed to send error message: %s", exc)

    def on_utterance(utterance_audio):
        """Called by VADProcessor (sync) when an utterance is complete."""
        asyncio.run_coroutine_threadsafe(
            _handle_utterance(utterance_audio, websocket, loop),
            loop,
        )

    vad = VADProcessor(
        sample_rate=sample_rate,
        silence_threshold_ms=300,
        on_utterance=on_utterance,
    )

    # ------------------------------------------------------------------
    # Step 3: Audio receive loop
    # ------------------------------------------------------------------
    try:
        while True:
            data = await websocket.receive_bytes()
            # Run VAD in a thread pool to avoid blocking the event loop
            await loop.run_in_executor(None, vad.process_chunk, data)
    except WebSocketDisconnect:
        logger.info(
            "Client %s:%s disconnected.", client.host, client.port
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("Unexpected error in WebSocket handler: %s", exc)
    finally:
        logger.info("WebSocket session ended for %s:%s.", client.host, client.port)


async def _handle_utterance(
    utterance_audio,
    websocket: WebSocket,
    loop: asyncio.AbstractEventLoop,
) -> None:
    """Run ASR + translation for a detected utterance and send result."""
    if _asr_backend is None or _translation_backend is None:
        logger.error("Backends not configured; cannot process utterance.")
        return

    # ASR
    try:
        source_text: str = await loop.run_in_executor(
            None, _asr_backend.transcribe, utterance_audio
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("ASR failed: %s", exc)
        error_payload = {"type": "error", "code": "ASR_FAILED", "message": str(exc)}
        try:
            await websocket.send_text(json.dumps(error_payload))
        except Exception:  # pylint: disable=broad-except
            pass
        return

    if not source_text:
        logger.debug("ASR returned empty string; skipping translation.")
        return

    # Translation
    try:
        translated_text: str = await loop.run_in_executor(
            None,
            _translation_backend.translate,
            source_text,
            "zho_Hans",
            "eng_Latn",
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("Translation failed: %s", exc)
        error_payload = {
            "type": "error",
            "code": "TRANSLATION_FAILED",
            "message": str(exc),
        }
        try:
            await websocket.send_text(json.dumps(error_payload))
        except Exception:  # pylint: disable=broad-except
            pass
        return

    # Send result
    payload = {
        "type": "translation",
        "source_text": source_text,
        "translated_text": translated_text,
        "utterance_id": str(uuid.uuid4()),
    }
    try:
        await websocket.send_text(json.dumps(payload, ensure_ascii=False))
        logger.info(
            "Utterance processed: src=%r tgt=%r", source_text, translated_text
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.warning("Failed to send translation result: %s", exc)
