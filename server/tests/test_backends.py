"""Unit tests for WhisperBackend and TranslationBackend.

All heavy model loading is mocked so tests run without downloading models.
"""
import sys
import types
from unittest.mock import MagicMock, patch

import numpy as np
import pytest


# ---------------------------------------------------------------------------
# Helpers to build minimal stubs for heavy optional dependencies
# ---------------------------------------------------------------------------

def _make_faster_whisper_stub():
    """Return a minimal faster_whisper module stub."""
    fw = types.ModuleType("faster_whisper")

    class _Segment:
        def __init__(self, text):
            self.text = text

    class _WhisperModel:
        def __init__(self, *args, **kwargs):
            pass

        def transcribe(self, audio, **kwargs):
            # Default: return a single segment with non-empty text
            rms = float(np.sqrt(np.mean(audio ** 2)))
            if rms < 1e-4:
                return [], None
            return [_Segment("你好")], None

    fw.WhisperModel = _WhisperModel
    return fw


def _make_transformers_stub(translation_result: str = "Hello"):
    """Return minimal transformers module stub."""
    transformers = types.ModuleType("transformers")

    class _FakeTensor:
        """Minimal tensor stub that doesn't require torch."""
        def __init__(self, data=None):
            self._data = data

        def to(self, device):
            return self

    class _Tokenizer:
        src_lang = "zho_Hans"

        def __call__(self, text, **kwargs):
            return {"input_ids": _FakeTensor(), "attention_mask": _FakeTensor()}

        def convert_tokens_to_ids(self, token):
            return 256047  # arbitrary token id

        def batch_decode(self, tokens, **kwargs):
            return [translation_result]

    class _Model:
        def __init__(self):
            self.training = False

        def to(self, device):
            return self

        def eval(self):
            return self

        def generate(self, **kwargs):
            return _FakeTensor()

    transformers.AutoTokenizer = MagicMock(
        return_value=_Tokenizer(),
        **{"from_pretrained": MagicMock(return_value=_Tokenizer())},
    )
    transformers.AutoTokenizer.from_pretrained = MagicMock(return_value=_Tokenizer())

    transformers.AutoModelForSeq2SeqLM = MagicMock()
    transformers.AutoModelForSeq2SeqLM.from_pretrained = MagicMock(
        return_value=_Model()
    )
    return transformers


# ---------------------------------------------------------------------------
# WhisperBackend tests
# ---------------------------------------------------------------------------

class TestWhisperBackend:
    """Tests for server/backend/whisper_backend.py"""

    def _make_backend(self):
        """Instantiate WhisperBackend with mocked faster-whisper."""
        stub = _make_faster_whisper_stub()
        sys.modules["faster_whisper"] = stub
        # Import fresh to pick up stub
        import importlib
        import backend.whisper_backend as wb_mod
        importlib.reload(wb_mod)
        return wb_mod.WhisperBackend(model_size="tiny", device="cpu")

    def test_transcribe_empty_audio_returns_empty_string(self):
        """1 second of silence (all zeros) should return ''."""
        backend = self._make_backend()
        silent_audio = np.zeros(16000, dtype=np.float32)
        result = backend.transcribe(silent_audio)
        assert result == "", f"Expected '' for silent audio, got {result!r}"

    def test_transcribe_none_audio_returns_empty_string(self):
        """None audio should return '' gracefully."""
        backend = self._make_backend()
        # Pass zero-length array to simulate None-like input
        result = backend.transcribe(np.array([], dtype=np.float32))
        assert result == ""

    def test_transcribe_speech_audio_returns_nonempty_string(self):
        """Non-silent audio should return a non-empty transcription."""
        backend = self._make_backend()
        # Simulate speech: sine wave at 440 Hz
        t = np.linspace(0, 1.0, 16000, dtype=np.float32)
        audio = np.sin(2 * np.pi * 440 * t) * 0.5
        result = backend.transcribe(audio)
        assert isinstance(result, str)
        assert len(result) > 0, "Expected non-empty transcription for speech audio"

    def test_transcribe_returns_string_type(self):
        """transcribe() must always return a str."""
        backend = self._make_backend()
        audio = np.random.randn(16000).astype(np.float32) * 0.1
        result = backend.transcribe(audio)
        assert isinstance(result, str)

    def test_transcribe_exception_returns_empty_string(self):
        """If the model raises an exception, transcribe() returns ''."""
        stub = _make_faster_whisper_stub()

        class _BrokenModel:
            def __init__(self, *args, **kwargs):
                pass
            def transcribe(self, audio, **kwargs):
                raise RuntimeError("model exploded")

        stub.WhisperModel = _BrokenModel
        sys.modules["faster_whisper"] = stub

        import importlib
        import backend.whisper_backend as wb_mod
        importlib.reload(wb_mod)
        backend = wb_mod.WhisperBackend(model_size="tiny", device="cpu")

        audio = np.ones(16000, dtype=np.float32) * 0.5
        result = backend.transcribe(audio)
        assert result == "", f"Expected '' on exception, got {result!r}"


# ---------------------------------------------------------------------------
# TranslationBackend tests
# ---------------------------------------------------------------------------

class TestTranslationBackend:
    """Tests for server/backend/translation_backend.py"""

    def _make_backend(self, translation_result="Hello"):
        """Instantiate TranslationBackend with mocked transformers."""
        stub = _make_transformers_stub(translation_result)
        sys.modules["transformers"] = stub
        import importlib
        import backend.translation_backend as tb_mod
        importlib.reload(tb_mod)
        return tb_mod.TranslationBackend(model_name="facebook/nllb-200-distilled-600M", device="cpu")

    def test_translate_empty_string_returns_empty(self):
        """translate('', ...) must return ''."""
        backend = self._make_backend()
        result = backend.translate("", "zho_Hans", "eng_Latn")
        assert result == "", f"Expected '' for empty input, got {result!r}"

    def test_translate_whitespace_only_returns_empty(self):
        """translate('   ', ...) must return ''."""
        backend = self._make_backend()
        result = backend.translate("   ", "zho_Hans", "eng_Latn")
        assert result == ""

    def test_translate_known_pair_hello(self):
        """translate('你好', 'zho_Hans', 'eng_Latn') should contain 'hello'."""
        backend = self._make_backend(translation_result="Hello")
        result = backend.translate("你好", "zho_Hans", "eng_Latn")
        assert "hello" in result.lower(), (
            f"Expected 'hello' in translation of '你好', got {result!r}"
        )

    def test_translate_returns_string_type(self):
        """translate() must always return a str."""
        backend = self._make_backend()
        result = backend.translate("测试", "zho_Hans", "eng_Latn")
        assert isinstance(result, str)

    def test_translate_exception_returns_empty_string(self):
        """If the model raises, translate() returns ''."""
        stub = _make_transformers_stub()

        class _BrokenModel:
            def to(self, device): return self
            def eval(self): return self
            def generate(self, **kwargs): raise RuntimeError("broken")

        stub.AutoModelForSeq2SeqLM.from_pretrained = MagicMock(
            return_value=_BrokenModel()
        )
        sys.modules["transformers"] = stub

        import importlib
        import backend.translation_backend as tb_mod
        importlib.reload(tb_mod)
        backend = tb_mod.TranslationBackend(device="cpu")

        result = backend.translate("你好", "zho_Hans", "eng_Latn")
        assert result == ""
