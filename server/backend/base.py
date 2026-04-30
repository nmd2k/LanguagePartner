# Abstract base — all model calls go through this interface.
# No direct model calls should appear in server/app.py.
from abc import ABC, abstractmethod

import numpy as np


class InferenceBackend(ABC):
    """Abstract interface for speech recognition and translation backends.

    Implementations must be swappable without touching the WebSocket handler.
    """

    @abstractmethod
    def transcribe(self, audio: np.ndarray) -> str:
        """Convert raw audio to text.

        Args:
            audio: Float32 numpy array of audio samples at 16 kHz, mono.
                   Values should be normalised to the range [-1.0, 1.0].

        Returns:
            Transcribed text string, or empty string if audio is silent/invalid.
        """
        ...

    @abstractmethod
    def translate(self, text: str, src_lang: str, tgt_lang: str) -> str:
        """Translate text from one language to another.

        Args:
            text:     Source text to translate.
            src_lang: Source language code (e.g. ``"zho_Hans"`` for NLLB).
            tgt_lang: Target language code (e.g. ``"eng_Latn"`` for NLLB).

        Returns:
            Translated text string, or empty string if *text* is empty.
        """
        ...
