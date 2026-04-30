"""TranslationBackend — HuggingFace NLLB-200-distilled-600M NMT backend.

Uses ``facebook/nllb-200-distilled-600M`` for multilingual translation.
NLLB language codes use FLORES-200 format (e.g. ``zho_Hans``, ``eng_Latn``).
"""
import logging

import numpy as np

from .base import InferenceBackend

logger = logging.getLogger(__name__)

_DEFAULT_MODEL = "facebook/nllb-200-distilled-600M"
_MAX_NEW_TOKENS = 256


class TranslationBackend(InferenceBackend):
    """NMT backend using NLLB-200-distilled-600M via HuggingFace transformers.

    Args:
        model_name: HuggingFace model identifier (default: NLLB-600M).
        device:     ``"cpu"``, ``"cuda"``, or ``"mps"`` (default: ``"cpu"``).
    """

    def __init__(
        self,
        model_name: str = _DEFAULT_MODEL,
        device: str = "cpu",
    ) -> None:
        from transformers import AutoModelForSeq2SeqLM, AutoTokenizer  # type: ignore

        logger.info("Loading translation tokenizer from '%s' …", model_name)
        self._tokenizer = AutoTokenizer.from_pretrained(model_name)

        logger.info("Loading translation model from '%s' …", model_name)
        self._model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

        self._device = device
        if device == "cuda":
            self._model = self._model.to("cuda")
        elif device == "mps":
            self._model = self._model.to("mps")

        self._model.eval()
        logger.info("Translation model loaded (device=%s).", device)

    def translate(
        self,
        text: str,
        src_lang: str,
        tgt_lang: str,
    ) -> str:
        """Translate *text* from *src_lang* to *tgt_lang*.

        Args:
            text:     Source text.
            src_lang: NLLB source language code (e.g. ``"zho_Hans"``).
            tgt_lang: NLLB target language code (e.g. ``"eng_Latn"``).

        Returns:
            Translated string, or ``""`` if *text* is empty.
        """
        if not text or not text.strip():
            return ""

        try:
            self._tokenizer.src_lang = src_lang
            inputs = self._tokenizer(
                text,
                return_tensors="pt",
                padding=True,
                truncation=True,
                max_length=512,
            )
            if self._device == "cuda":
                inputs = {k: v.to("cuda") for k, v in inputs.items()}
            elif self._device == "mps":
                inputs = {k: v.to("mps") for k, v in inputs.items()}

            forced_bos_token_id = self._tokenizer.convert_tokens_to_ids(tgt_lang)
            generated_tokens = self._model.generate(
                **inputs,
                forced_bos_token_id=forced_bos_token_id,
                max_new_tokens=_MAX_NEW_TOKENS,
            )
            translated = self._tokenizer.batch_decode(
                generated_tokens,
                skip_special_tokens=True,
            )
            result = translated[0].strip() if translated else ""
            logger.debug("Translated %r → %r", text, result)
            return result
        except Exception as exc:  # pylint: disable=broad-except
            logger.exception("Translation inference error: %s", exc)
            return ""

    def transcribe(
        self,
        audio: np.ndarray,
        language: str = None,
    ) -> str:
        """Not implemented — transcription is delegated to WhisperBackend."""
        raise NotImplementedError(
            "TranslationBackend does not support ASR. "
            "Use WhisperBackend for transcription."
        )
