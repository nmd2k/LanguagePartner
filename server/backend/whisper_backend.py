"""WhisperBackend — faster-whisper (CTranslate2) ASR backend.

Uses the faster-whisper library which is 3–4x faster than the original
openai-whisper implementation at identical accuracy.
"""
import logging
from typing import Optional

import numpy as np

from .base import InferenceBackend

logger = logging.getLogger(__name__)


class WhisperBackend(InferenceBackend):
    """ASR backend using faster-whisper with language forced to Chinese (zh).

    Args:
        model_size: Whisper model size string, e.g. ``"medium"``, ``"large-v2"``.
        device:     Inference device, e.g. ``"cpu"``, ``"cuda"``, ``"auto"``.
    """

    def __init__(self, model_size: str = "medium", device: str = "cpu") -> None:
        from faster_whisper import WhisperModel  # type: ignore

        # faster-whisper uses "cpu" / "cuda" as device strings.
        # "auto" is handled by the CLI entrypoint before reaching here.
        compute_type = "float16" if device == "cuda" else "int8"
        logger.info(
            "Loading Whisper model '%s' on device '%s' (compute_type=%s) …",
            model_size,
            device,
            compute_type,
        )
        self._model = WhisperModel(
            model_size,
            device=device,
            compute_type=compute_type,
        )
        logger.info("Whisper model loaded.")

    def transcribe(self, audio: np.ndarray) -> str:
        """Transcribe audio to Chinese text.

        Args:
            audio: Float32 numpy array, 16 kHz mono, values in [-1.0, 1.0].

        Returns:
            Transcribed ZH-CN string, or ``""`` for silent / noise-only input.
        """
        if audio is None or len(audio) == 0:
            return ""

        # Ensure float32 in case caller passes int16 or float64
        audio = np.asarray(audio, dtype=np.float32)

        # Heuristic: skip frames that are essentially silent (RMS < threshold)
        rms = float(np.sqrt(np.mean(audio ** 2)))
        if rms < 1e-4:
            logger.debug("Audio RMS %.6f below silence threshold; skipping ASR.", rms)
            return ""

        try:
            segments, _info = self._model.transcribe(
                audio,
                language="zh",
                beam_size=5,
                vad_filter=False,  # VAD already applied upstream by silero-VAD
            )
            text = "".join(segment.text for segment in segments).strip()
            logger.debug("Whisper transcribed: %r", text)
            return text
        except Exception as exc:  # pylint: disable=broad-except
            logger.exception("Whisper inference error: %s", exc)
            return ""

    def translate(self, text: str, src_lang: str, tgt_lang: str) -> str:
        """Not implemented — translation is delegated to TranslationBackend.

        This method is part of the ABC contract but WhisperBackend is not a
        translator.  Calling it raises NotImplementedError.
        """
        raise NotImplementedError(
            "WhisperBackend does not support translation. "
            "Use TranslationBackend for NMT."
        )
