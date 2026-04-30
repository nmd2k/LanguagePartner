"""Hy-MT1.5 translation backend via llama.cpp.

Uses llama-cpp-python to load a GGUF-quantized Hy-MT1.5 model
(tencent/Hy-MT1.5-1.8B-1.25bit-GGUF). Falls back gracefully if the
model file or llama-cpp-python is not available.

Implements the InferenceBackend ABC so it is a drop-in replacement
for TranslationBackend.
"""
import logging
import os
from typing import Optional

import numpy as np

from backend.base import InferenceBackend

logger = logging.getLogger(__name__)

_MODEL_FILE = os.environ.get(
    "HYMT15_MODEL_PATH",
    "models/Hy-MT1.5-1.8B-1.25bit-GGUF/Hy-MT1.5-1.8B-1.25bit.Q4_K_M.gguf",
)

_SRC_LANG_MAP: dict[str, str] = {
    "eng_Latn": "English",
    "zho_Hans": "Chinese",
    "vie_Latn": "Vietnamese",
    "sin_Sinh": "Sinhala",
}

_TGT_LANG_MAP: dict[str, str] = {
    "eng_Latn": "English",
    "zho_Hans": "Chinese",
    "vie_Latn": "Vietnamese",
    "sin_Sinh": "Sinhala",
}


class LlamaCppBackend(InferenceBackend):
    """Translation backend using llama.cpp with Hy-MT1.5 GGUF model."""

    def __init__(self, model_path: Optional[str] = None, device: str = "auto"):
        self._model_path = model_path or _MODEL_FILE
        self._device = device
        self._model = None
        self._loaded = False
        self._load_error: Optional[str] = None

        try:
            from llama_cpp import Llama
            self._Llama = Llama
        except ImportError:
            self._load_error = (
                "llama-cpp-python not installed. "
                "Install with: pip install llama-cpp-python"
            )
            logger.warning(self._load_error)
            return

        if not os.path.exists(self._model_path):
            self._load_error = (
                f"Model not found at {self._model_path}. "
                f"Download from HuggingFace: tencent/Hy-MT1.5-1.8B-1.25bit-GGUF"
            )
            logger.warning(self._load_error)
            return

        self._load_model()

    def _load_model(self) -> None:
        try:
            n_gpu_layers = -1 if self._device in ("cuda", "mps") else 0
            self._model = self._Llama(
                model_path=self._model_path,
                n_ctx=512,
                n_threads=4,
                n_gpu_layers=n_gpu_layers,
                verbose=False,
            )
            self._loaded = True
            logger.info(
                "Hy-MT1.5 model loaded from %s (device=%s, gpu_layers=%d)",
                self._model_path, self._device, n_gpu_layers,
            )
        except Exception as exc:
            self._load_error = f"Failed to load Hy-MT1.5 model: {exc}"
            logger.error(self._load_error)

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    @property
    def status_message(self) -> str:
        if self._loaded:
            return "loaded"
        if self._load_error:
            return f"error: {self._load_error}"
        return "not loaded"

    def translate(
        self, text: str, src_lang: str, tgt_lang: str
    ) -> str:
        if not self._loaded:
            logger.error("Hy-MT1.5 backend not loaded: %s", self._load_error)
            return ""

        if not text.strip():
            return ""

        src_name = _SRC_LANG_MAP.get(src_lang, src_lang)
        tgt_name = _TGT_LANG_MAP.get(tgt_lang, tgt_lang)

        prompt = (
            f"<|im_start|>user\n"
            f"Translate the following text from {src_name} to {tgt_name}:\n\n"
            f"{text}\n\n"
            f"Translation:\n"
            f"<|im_end|>\n"
            f"<|im_start|>assistant\n"
        )

        try:
            output = self._model(
                prompt,
                max_tokens=256,
                temperature=0.0,
                top_p=0.95,
                stop=["<|im_end|>", "<|im_start|>"],
                echo=False,
            )
            result = output["choices"][0]["text"].strip()
            return result
        except Exception as exc:
            logger.exception("Hy-MT1.5 translation failed: %s", exc)
            return ""

    def transcribe(
        self, audio: np.ndarray, language: Optional[str] = None
    ) -> str:
        raise NotImplementedError("LlamaCppBackend does not support ASR")
