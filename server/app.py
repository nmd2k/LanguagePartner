"""LanguagePartner server — FastAPI WebSocket handler.

Protocol (see SRS §2.5):
  1. Client connects to ws://<host>:<port>/ws
  2. Client sends a config JSON frame:
       {"type": "config", "sample_rate": 16000, "mode": "speak|read",
        "source_lang": "zh", "target_lang": "en"}
  3. Server enters audio-receive loop: binary frames → VADProcessor
  4. On utterance detection: transcribe → translate → send translation JSON
  5. Mid-session messages:
        - config:  update mode, languages
        - pause:   stop processing audio (VAD skips)
        - resume:  resume processing audio
        - text_input:  translate a text string and return result
  6. On inference error: send error JSON
  7. Structured log messages are broadcast to all connected clients as "log" events.

All model calls go through the InferenceBackend ABC — no direct model calls here.
"""
import asyncio
import json
import logging
import time
import uuid
from datetime import datetime, timezone
from typing import Callable, Optional, Set

from fastapi import FastAPI, WebSocket, WebSocketDisconnect

from backend.base import InferenceBackend
from backend.languages import (
    Language,
    SUPPORTED_LANGUAGES,
    get_language,
    whisper_to_nllb,
)
from vad import VADProcessor

logger = logging.getLogger(__name__)

app = FastAPI(title="LanguagePartner Server")

# Mount the dashboard router
from dashboard import router as dashboard_router
app.include_router(dashboard_router)

_asr_backend: Optional[InferenceBackend] = None
_translation_backend: Optional[InferenceBackend] = None

# Default language pair
_DEFAULT_SOURCE = "zh"
_DEFAULT_TARGET = "en"

# Connected WebSocket clients for log broadcasting
_connected_clients: Set[WebSocket] = set()


class WebSocketLogHandler(logging.Handler):
    """Custom logging handler that broadcasts log records to all WebSocket clients."""

    def emit(self, record: logging.LogRecord) -> None:
        try:
            payload = json.dumps({
                "type": "log",
                "timestamp": datetime.now(timezone.utc).strftime("%H:%M:%S.%f")[:-3],
                "level": record.levelname,
                "message": self.format(record),
            })
            dead: Set[WebSocket] = set()
            for ws in _connected_clients:
                try:
                    coro = _ws_send_safe(ws, payload)
                    try:
                        asyncio.create_task(coro)
                    except RuntimeError:
                        dead.add(ws)
                except Exception:
                    dead.add(ws)
            _connected_clients.difference_update(dead)
        except Exception:
            pass


async def _ws_send_safe(ws: WebSocket, payload: str) -> None:
    """Send a text frame, suppressing RuntimeError if the connection is closed."""
    try:
        await ws.send_text(payload)
    except RuntimeError:
        pass


# Install the WebSocket log handler at INFO level
_ws_log_handler = WebSocketLogHandler()
_ws_log_handler.setLevel(logging.INFO)
_ws_log_handler.setFormatter(logging.Formatter("%(message)s"))
logging.getLogger().addHandler(_ws_log_handler)


def configure_backends(
    asr: InferenceBackend,
    translation: InferenceBackend,
) -> None:
    """Inject loaded backends into the app module."""
    global _asr_backend, _translation_backend
    _asr_backend = asr
    _translation_backend = translation


def _truncate_text(text: str, max_len: int = 50) -> str:
    """Truncate text for logging purposes."""
    if len(text) <= max_len:
        return text
    return text[:max_len - 3] + "..."


async def _send_translation(
    websocket: WebSocket,
    source_text: str,
    translated_text: str,
    utterance_id: str,
) -> None:
    payload = {
        "type": "translation",
        "source_text": source_text,
        "translated_text": translated_text,
        "utterance_id": utterance_id,
    }
    try:
        await websocket.send_text(json.dumps(payload, ensure_ascii=False))
        logger.info(
            '[%s] source="%s" translated="%s"',
            utterance_id,
            _truncate_text(source_text),
            _truncate_text(translated_text),
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.warning("Failed to send translation result: %s", exc)


async def _send_error(websocket: WebSocket, code: str, message: str) -> None:
    payload = {"type": "error", "code": code, "message": message}
    try:
        await websocket.send_text(json.dumps(payload))
    except Exception as exc:  # pylint: disable=broad-except
        logger.warning("Failed to send error message: %s", exc)


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket) -> None:
    """WebSocket endpoint — accepts audio stream, returns translation JSON."""
    await websocket.accept()
    client = websocket.client
    logger.info("New WebSocket connection from %s:%s", client.host, client.port)
    _connected_clients.add(websocket)

    # Step 1: Wait for config message
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
    source_lang_code: str = str(config.get("source_lang", _DEFAULT_SOURCE))
    target_lang_code: str = str(config.get("target_lang", _DEFAULT_TARGET))
    paused: bool = False

    # Resolve language codes
    source_lang: Optional[Language] = get_language(source_lang_code)
    target_lang: Optional[Language] = get_language(target_lang_code)
    if source_lang is None:
        source_lang = SUPPORTED_LANGUAGES[0]  # fallback to EN
    if target_lang is None:
        target_lang = SUPPORTED_LANGUAGES[1]  # fallback to ZH

    source_nllb = source_lang.nllb_code
    target_nllb = target_lang.nllb_code
    whisper_lang = source_lang.code

    logger.info(
        "Client configured: sample_rate=%d, mode=%s, source=%s(%s), target=%s(%s)",
        sample_rate, mode,
        source_lang.name, whisper_lang,
        target_lang.name, target_nllb,
    )

    # Step 2: Set up VAD with utterance callback
    loop = asyncio.get_event_loop()
    _closed = False

    def _is_closed() -> bool:
        return _closed

    def on_utterance(utterance_audio):
        """Called by VADProcessor (sync) when an utterance is complete."""
        if _is_closed():
            return
        asyncio.run_coroutine_threadsafe(
            _handle_utterance(
                utterance_audio, websocket, loop, mode,
                whisper_lang, source_nllb, target_nllb,
                _is_closed,
            ),
            loop,
        )

    vad = VADProcessor(
        sample_rate=sample_rate,
        silence_threshold_ms=300,
        on_utterance=on_utterance,
    )

    # Step 3: Audio receive loop with mid-session message handling
    try:
        while True:
            try:
                message = await asyncio.wait_for(
                    websocket.receive(), timeout=0.1
                )
                if "bytes" in message:
                    if not paused:
                        await loop.run_in_executor(None, vad.process_chunk, message["bytes"])
                elif "text" in message:
                    msg = json.loads(message["text"])
                    msg_type = msg.get("type", "")

                    if msg_type == "config":
                        new_mode = str(msg.get("mode", mode))
                        new_source = str(msg.get("source_lang", source_lang_code))
                        new_target = str(msg.get("target_lang", target_lang_code))

                        changed = False
                        if new_mode != mode:
                            mode = new_mode
                            logger.info("Mode updated: mode=%s", mode)
                            changed = True

                        if new_source != source_lang_code:
                            sl = get_language(new_source)
                            if sl:
                                source_lang_code = new_source
                                source_lang = sl
                                source_nllb = sl.nllb_code
                                whisper_lang = sl.code
                                logger.info("Source language updated: %s", sl.name)
                                changed = True

                        if new_target != target_lang_code:
                            tl = get_language(new_target)
                            if tl:
                                target_lang_code = new_target
                                target_lang = tl
                                target_nllb = tl.nllb_code
                                logger.info("Target language updated: %s", tl.name)
                                changed = True

                    elif msg_type == "pause":
                        if not paused:
                            paused = True
                            logger.info("Translation paused.")

                    elif msg_type == "resume":
                        if paused:
                            paused = False
                            logger.info("Translation resumed.")

                    elif msg_type == "text_input":
                        text = str(msg.get("text", "")).strip()
                        text_src = str(msg.get("source_lang", source_lang_code))
                        text_tgt = str(msg.get("target_lang", target_lang_code))

                        if text:
                            sl = get_language(text_src)
                            tl = get_language(text_tgt)
                            src_nllb = sl.nllb_code if sl else source_nllb
                            tgt_nllb = tl.nllb_code if tl else target_nllb

                            translated = await _run_translate(
                                text, src_nllb, tgt_nllb, loop
                            )
                            if translated:
                                uid = str(uuid.uuid4())
                                await _send_translation(
                                    websocket, text, translated, uid
                                )
                                logger.info(
                                    '[%s] text_input "%s" → "%s"',
                                    uid,
                                    _truncate_text(text),
                                    _truncate_text(translated),
                                )

            except asyncio.TimeoutError:
                pass
            except json.JSONDecodeError:
                pass
    except WebSocketDisconnect:
        logger.info("Client %s:%s disconnected.", client.host, client.port)
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("Unexpected error in WebSocket handler: %s", exc)
    finally:
        _closed = True
        _connected_clients.discard(websocket)
        logger.info("WebSocket session ended for %s:%s.", client.host, client.port)


async def _run_translate(
    text: str,
    src_nllb: str,
    tgt_nllb: str,
    loop: asyncio.AbstractEventLoop,
) -> str:
    """Run translation in thread pool and handle errors."""
    try:
        return await loop.run_in_executor(
            None,
            _translation_backend.translate,
            text,
            src_nllb,
            tgt_nllb,
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("Translation failed: %s", exc)
        return ""


async def _handle_utterance(
    utterance_audio,
    websocket: WebSocket,
    loop: asyncio.AbstractEventLoop,
    mode: str,
    whisper_lang: str,
    source_nllb: str,
    target_nllb: str,
    is_closed: Optional[Callable[[], bool]] = None,
) -> None:
    """Run ASR + translation for a detected utterance and send result."""
    if is_closed is not None and is_closed():
        return

    if _asr_backend is None or _translation_backend is None:
        logger.error("Backends not configured; cannot process utterance.")
        return

    utterance_id = str(uuid.uuid4())
    total_start = time.perf_counter()

    # ASR
    asr_start = time.perf_counter()
    try:
        source_text: str = await loop.run_in_executor(
            None, _asr_backend.transcribe, utterance_audio, whisper_lang
        )
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("ASR failed: %s", exc)
        if is_closed is not None and is_closed():
            return
        await _send_error(websocket, "ASR_FAILED", str(exc))
        return

    asr_latency = time.perf_counter() - asr_start

    if not source_text:
        logger.debug("ASR returned empty string; skipping translation.")
        return

    # Translation
    translate_start = time.perf_counter()
    translated_text = await _run_translate(
        source_text, source_nllb, target_nllb, loop
    )

    if not translated_text:
        return

    translate_latency = time.perf_counter() - translate_start
    total_latency = time.perf_counter() - total_start

    if is_closed is not None and is_closed():
        return

    try:
        await websocket.send_text(
            json.dumps(
                {
                    "type": "translation",
                    "source_text": source_text,
                    "translated_text": translated_text,
                    "utterance_id": utterance_id,
                },
                ensure_ascii=False,
            )
        )
        logger.info(
            '[%s] asr=%.2fs translate=%.2fs total=%.2fs source="%s" translated="%s"',
            utterance_id,
            asr_latency,
            translate_latency,
            total_latency,
            _truncate_text(source_text),
            _truncate_text(translated_text),
        )
    except RuntimeError:
        logger.debug("Cannot send translation (connection closed).")
    except Exception as exc:  # pylint: disable=broad-except
        logger.warning("Failed to send translation result: %s", exc)
