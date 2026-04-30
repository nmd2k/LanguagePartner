"""Unit tests for VADProcessor.

silero-VAD model loading is mocked so tests run without downloading weights.
"""
import sys
import types
from unittest.mock import MagicMock, patch
from typing import List

import numpy as np
import pytest


# ---------------------------------------------------------------------------
# Torch / silero-VAD stubs
# ---------------------------------------------------------------------------

def _make_torch_hub_stub(speech_probs: List[float]):
    """Return a minimal torch stub whose hub.load yields a VAD model that
    returns *speech_probs* sequentially."""
    import types as _types

    torch_mod = _types.ModuleType("torch")

    # Tensor stub
    class _Tensor:
        def __init__(self, data):
            self._data = np.asarray(data, dtype=np.float32)

        def item(self):
            return float(self._data.flat[0])

        def __len__(self):
            return len(self._data)

        def __getitem__(self, idx):
            return _Tensor(self._data[idx])

    class _TorchModule:
        """Minimal stub for the silero-VAD callable model."""
        def __init__(self, probs):
            self._probs = iter(probs)
            self._exhausted_value = 0.0

        def __call__(self, tensor, sample_rate):
            try:
                val = next(self._probs)
            except StopIteration:
                val = self._exhausted_value
            return _Tensor([val])

        def reset_states(self):
            pass

    model = _TorchModule(speech_probs)

    def _hub_load(repo, model_name, **kwargs):
        # Return (model, utils) — utils tuple with 5 elements
        return model, (None, None, None, None, None)

    hub_mod = _types.ModuleType("torch.hub")
    hub_mod.load = _hub_load
    torch_mod.hub = hub_mod

    def _from_numpy(arr):
        return _Tensor(arr)

    torch_mod.from_numpy = _from_numpy

    def _zeros(n):
        return _Tensor(np.zeros(n, dtype=np.float32))

    torch_mod.zeros = _zeros

    def _cat(tensors):
        combined = np.concatenate([t._data for t in tensors])
        return _Tensor(combined)

    torch_mod.cat = _cat

    return torch_mod


# ---------------------------------------------------------------------------
# VADProcessor tests
# ---------------------------------------------------------------------------

class TestVADProcessor:
    """Tests for server/vad.py — VADProcessor."""

    def _make_vad(self, speech_probs, on_utterance=None, silence_threshold_ms=300):
        """Instantiate VADProcessor with a stubbed silero-VAD model."""
        torch_stub = _make_torch_hub_stub(speech_probs)
        sys.modules["torch"] = torch_stub

        import importlib
        import vad as vad_mod
        importlib.reload(vad_mod)

        return vad_mod.VADProcessor(
            sample_rate=16000,
            silence_threshold_ms=silence_threshold_ms,
            on_utterance=on_utterance,
        )

    def _make_pcm_chunk(self, amplitude=0.5, n_samples=512):
        """Return raw 16-bit PCM bytes for a chunk with given amplitude."""
        samples = (np.ones(n_samples, dtype=np.float32) * amplitude * 32767).astype(
            np.int16
        )
        return samples.tobytes()

    def _make_silence_chunk(self, n_samples=512):
        """Return raw 16-bit PCM bytes for a silent chunk (all zeros)."""
        return np.zeros(n_samples, dtype=np.int16).tobytes()

    # ------------------------------------------------------------------
    # Core behaviour
    # ------------------------------------------------------------------

    def test_utterance_detected_after_speech_then_silence(self):
        """Callback fires once after speech followed by enough silence."""
        SAMPLE_RATE = 16000
        SILENCE_MS = 300
        SILENCE_THRESHOLD_SAMPLES = int((SILENCE_MS / 1000.0) * SAMPLE_RATE)  # 4800
        CHUNK = 512

        # Number of silence chunks needed to exceed threshold
        silence_chunks_needed = (SILENCE_THRESHOLD_SAMPLES // CHUNK) + 1  # 10

        # 5 speech chunks → then silence_chunks_needed silence chunks
        speech_chunks = 5
        probs = (
            [0.9] * speech_chunks  # speech
            + [0.1] * silence_chunks_needed  # silence
        )

        utterances: list = []
        vad = self._make_vad(
            probs,
            on_utterance=lambda audio: utterances.append(audio),
            silence_threshold_ms=SILENCE_MS,
        )

        for _ in range(speech_chunks):
            vad.process_chunk(self._make_pcm_chunk(amplitude=0.5))

        for _ in range(silence_chunks_needed):
            vad.process_chunk(self._make_silence_chunk())

        assert len(utterances) == 1, (
            f"Expected exactly 1 utterance callback, got {len(utterances)}"
        )
        assert len(utterances[0]) > 0, "Utterance audio array must be non-empty"

    def test_no_utterance_without_silence_threshold(self):
        """Callback does NOT fire if silence threshold is never reached."""
        probs = [0.9] * 5 + [0.1] * 3  # only 3 silence chunks < threshold (10)

        utterances: list = []
        vad = self._make_vad(
            probs,
            on_utterance=lambda audio: utterances.append(audio),
            silence_threshold_ms=300,
        )

        for _ in range(5):
            vad.process_chunk(self._make_pcm_chunk())

        for _ in range(3):
            vad.process_chunk(self._make_silence_chunk())

        assert len(utterances) == 0, (
            "Callback should not fire before silence threshold is reached"
        )

    def test_no_utterance_without_speech(self):
        """Callback never fires if there is no speech at all."""
        probs = [0.0] * 20

        utterances: list = []
        vad = self._make_vad(
            probs,
            on_utterance=lambda audio: utterances.append(audio),
        )

        for _ in range(20):
            vad.process_chunk(self._make_silence_chunk())

        assert len(utterances) == 0

    def test_utterance_audio_is_float32_array(self):
        """The utterance callback receives a float32 numpy array."""
        SAMPLE_RATE = 16000
        SILENCE_MS = 300
        CHUNK = 512
        silence_chunks_needed = (int((SILENCE_MS / 1000.0) * SAMPLE_RATE) // CHUNK) + 1

        probs = [0.9] * 5 + [0.1] * silence_chunks_needed
        utterances: list = []

        vad = self._make_vad(
            probs,
            on_utterance=lambda audio: utterances.append(audio),
            silence_threshold_ms=SILENCE_MS,
        )

        for _ in range(5):
            vad.process_chunk(self._make_pcm_chunk())
        for _ in range(silence_chunks_needed):
            vad.process_chunk(self._make_silence_chunk())

        assert len(utterances) == 1
        arr = utterances[0]
        assert isinstance(arr, np.ndarray), f"Expected ndarray, got {type(arr)}"
        assert arr.dtype == np.float32, f"Expected float32, got {arr.dtype}"

    def test_process_empty_chunk_does_not_crash(self):
        """Passing an empty bytes object should not raise."""
        probs = [0.0] * 5
        vad = self._make_vad(probs)
        vad.process_chunk(b"")  # must not raise

    def test_reset_clears_state(self):
        """After reset(), a partial utterance should not trigger callback."""
        SAMPLE_RATE = 16000
        SILENCE_MS = 300
        CHUNK = 512
        silence_chunks_needed = (int((SILENCE_MS / 1000.0) * SAMPLE_RATE) // CHUNK) + 1

        # Speech but we reset before the silence threshold
        probs = [0.9] * 5 + [0.1] * silence_chunks_needed
        utterances: list = []

        vad = self._make_vad(
            probs,
            on_utterance=lambda audio: utterances.append(audio),
        )

        for _ in range(5):
            vad.process_chunk(self._make_pcm_chunk())

        # Reset mid-utterance
        vad.reset()

        # Now feed silence — should NOT trigger (buffer was cleared)
        for _ in range(silence_chunks_needed):
            vad.process_chunk(self._make_silence_chunk())

        assert len(utterances) == 0, (
            "After reset(), no utterance should be emitted for pre-reset speech"
        )
