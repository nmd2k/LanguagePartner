"""WhisperBackend — faster-whisper (CTranslate2) ASR backend.

Uses the faster-whisper library which is 3–4x faster than the original
openai-whisper implementation at identical accuracy.
"""
import logging
from typing import Optional

import numpy as np

from .base import InferenceBackend
from .languages import whisper_to_nllb

logger = logging.getLogger(__name__)


class WhisperBackend(InferenceBackend):
    """ASR backend using faster-whisper with configurable language.

    Args:
        model_size: Whisper model size string, e.g. ``"medium"``, ``"large-v2"``.
        device:     Inference device, e.g. ``"cpu"``, ``"cuda"``, ``"auto"``.
    """

    def __init__(self, model_size: str = "medium", device: str = "cpu") -> None:
        from faster_whisper import WhisperModel  # type: ignore

        effective_device = device
        if device == "mps":
            logger.info("MPS not supported by faster-whisper; using CPU instead.")
            effective_device = "cpu"
        compute_type = "float16" if effective_device == "cuda" else "int8"
        logger.info(
            "Loading Whisper model '%s' on device '%s' (compute_type=%s) …",
            model_size,
            effective_device,
            compute_type,
        )
        self._model = WhisperModel(
            model_size,
            device=effective_device,
            compute_type=compute_type,
        )
        logger.info("Whisper model loaded.")

    def transcribe(
        self,
        audio: np.ndarray,
        language: Optional[str] = None,
    ) -> str:
        """Transcribe audio to text.

        Args:
            audio:    Float32 numpy array, 16 kHz mono, values in [-1.0, 1.0].
            language: Whisper language code (e.g. ``"zh"``, ``"en"``).
                      If ``None``, Whisper auto-detects the language.

        Returns:
            Transcribed string, or ``""`` for silent / noise-only input.
        """
        if audio is None or len(audio) == 0:
            return ""

        audio = np.asarray(audio, dtype=np.float32)

        rms = float(np.sqrt(np.mean(audio ** 2)))
        if rms < 1e-4:
            logger.debug("Audio RMS %.6f below silence threshold; skipping ASR.", rms)
            return ""

        try:
            transcribe_kwargs = {
                "beam_size": 5,
                "vad_filter": False,
            }
            if language:
                transcribe_kwargs["language"] = language

            segments, _info = self._model.transcribe(
                audio,
                **transcribe_kwargs,
            )
            text = "".join(segment.text for segment in segments).strip()
            logger.debug("Whisper transcribed: %r", text)
            return text
        except Exception as exc:  # pylint: disable=broad-except
            logger.exception("Whisper inference error: %s", exc)
            return ""

    def translate(self, text: str, src_lang: str, tgt_lang: str) -> str:
        """Not implemented — translation is delegated to TranslationBackend."""
        raise NotImplementedError(
            "WhisperBackend does not support translation. "
            "Use TranslationBackend for NMT."
        )
