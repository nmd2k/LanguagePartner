"""LanguagePartner server — FastAPI WebSocket handler.

Protocol (see SRS §2.5):
  1. Client connects to ws://<host>:<port>/ws
  2. Client sends a config JSON frame:
       {"type": "config", "sample_rate": 16000, "mode": "speak|read"}
  3. Server enters audio-receive loop: binary frames → VADProcessor
  4. On utterance detection: transcribe → translate → send translation JSON
  5. On inference error: send error JSON
  6. Client can send mid-session config messages to update mode

All model calls go through the InferenceBackend ABC — no direct model calls here.
"""
import asyncio
import json
import logging
import time
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


def _truncate_text(text: str, max_len: int = 50) -> str:
    """Truncate text for logging purposes."""
    if len(text) <= max_len:
        return text
    return text[:max_len - 3] + "..."


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
            _handle_utterance(utterance_audio, websocket, loop, mode),
            loop,
        )

    vad = VADProcessor(
        sample_rate=sample_rate,
        silence_threshold_ms=300,
        on_utterance=on_utterance,
    )

    # ------------------------------------------------------------------
    # Step 3: Audio receive loop with mid-session config handling
    # ------------------------------------------------------------------
    try:
        while True:
            try:
                data = await asyncio.wait_for(
                    websocket.receive_bytes(), timeout=0.1
                )
                # Run VAD in a thread pool to avoid blocking the event loop
                await loop.run_in_executor(None, vad.process_chunk, data)
            except asyncio.TimeoutError:
                # Check for text messages (config updates) periodically
                try:
                    message = await asyncio.wait_for(
                        websocket.receive(), timeout=0.01
                    )
                    if "text" in message:
                        msg = json.loads(message["text"])
                        if msg.get("type") == "config":
                            new_mode = str(msg.get("mode", mode))
                            if new_mode != mode:
                                mode = new_mode
                                logger.info("Mode updated mid-session: mode=%s", mode)
                except (asyncio.TimeoutError, json.JSONDecodeError):
                    pass
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
    mode: str,
) -> None:
    """Run ASR + translation for a detected utterance and send result."""
    if _asr_backend is None or _translation_backend is None:
        logger.error("Backends not configured; cannot process utterance.")
        return

    utterance_id = str(uuid.uuid4())
    total_start = time.perf_counter()

    # ASR
    asr_start = time.perf_counter()
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

    asr_latency = time.perf_counter() - asr_start

    if not source_text:
        logger.debug("ASR returned empty string; skipping translation.")
        return

    # Translation
    translate_start = time.perf_counter()
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

    translate_latency = time.perf_counter() - translate_start
    total_latency = time.perf_counter() - total_start

    # Send result
    payload = {
        "type": "translation",
        "source_text": source_text,
        "translated_text": translated_text,
        "utterance_id": utterance_id,
    }
    try:
        await websocket.send_text(json.dumps(payload, ensure_ascii=False))
        src_truncated = _truncate_text(source_text)
        tgt_truncated = _truncate_text(translated_text)
        logger.info(
            '[%s] asr=%.2fs translate=%.2fs total=%.2fs source="%s" translated="%s"',
            utterance_id,
            asr_latency,
            translate_latency,
            total_latency,
            src_truncated,
            tgt_truncated,
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.warning("Failed to send translation result: %s", exc)
