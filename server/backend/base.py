# Abstract base — all model calls go through this interface.
# No direct model calls should appear in server/app.py.
from abc import ABC, abstractmethod
from typing import Optional

import numpy as np


class InferenceBackend(ABC):
    """Abstract interface for speech recognition and translation backends.

    Implementations must be swappable without touching the WebSocket handler.
    """

    @abstractmethod
    def transcribe(
        self,
        audio: np.ndarray,
        language: Optional[str] = None,
    ) -> str:
        """Convert raw audio to text.

        Args:
            audio:    Float32 numpy array of audio samples at 16 kHz, mono.
                      Values should be normalised to the range [-1.0, 1.0].
            language: Whisper language code (e.g. ``"zh"``, ``"en"``).
                      If ``None``, the backend may auto-detect.

        Returns:
            Transcribed text string, or empty string if audio is silent/invalid.
        """
        ...

    @abstractmethod
    def translate(self, text: str, src_lang: str, tgt_lang: str) -> str:
        """Translate text from one language to another.

        Args:
            text:     Source text to translate.
            src_lang: NLLB source language code (e.g. ``"zho_Hans"``).
            tgt_lang: NLLB target language code (e.g. ``"eng_Latn"``).

        Returns:
            Translated text string, or empty string if *text* is empty.
        """
        ...
