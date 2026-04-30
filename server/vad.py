"""VADProcessor — silero-VAD utterance segmentation pipeline.

Receives raw 16-bit PCM binary chunks from the WebSocket handler, runs
silero-VAD on each chunk, accumulates speech frames, and fires an
``on_utterance`` callback when a complete utterance is detected (i.e. speech
followed by ≥ ``silence_threshold_ms`` ms of silence).
"""
import logging
from typing import Callable, Optional

import numpy as np

logger = logging.getLogger(__name__)

# Silero-VAD operates on 512-sample windows at 16 kHz (32 ms per chunk).
_SILERO_CHUNK_SAMPLES = 512
_INT16_MAX = 32768.0


class VADProcessor:
    """Utterance segmentation using silero-VAD.

    Args:
        sample_rate:          Audio sample rate in Hz (must be 16000 for silero-VAD).
        silence_threshold_ms: Milliseconds of continuous silence required to
                              mark the end of an utterance (default: 300).
        on_utterance:         Callback invoked with a float32 numpy array
                              containing the complete utterance audio whenever
                              one is detected.
    """

    def __init__(
        self,
        sample_rate: int = 16000,
        silence_threshold_ms: int = 300,
        on_utterance: Optional[Callable[[np.ndarray], None]] = None,
    ) -> None:
        self._sample_rate = sample_rate
        self._silence_threshold_samples = int(
            (silence_threshold_ms / 1000.0) * sample_rate
        )
        self._on_utterance = on_utterance

        # Internal state
        self._speech_buffer: list[np.ndarray] = []
        self._silence_samples: int = 0
        self._in_speech: bool = False

        # Load silero-VAD model
        logger.info("Loading silero-VAD model …")
        import torch  # type: ignore

        model, utils = torch.hub.load(
            "snakers4/silero-vad",
            "silero_vad",
            force_reload=False,
            onnx=False,
        )
        self._vad_model = model
        (
            self._get_speech_timestamps,
            _,
            _,
            _,
            _,
        ) = utils
        logger.info("silero-VAD loaded.")

    def reset(self) -> None:
        """Clear internal state (call between sessions if reusing the instance)."""
        self._speech_buffer = []
        self._silence_samples = 0
        self._in_speech = False
        if hasattr(self._vad_model, "reset_states"):
            self._vad_model.reset_states()

    def process_chunk(self, chunk: bytes) -> None:
        """Process a single raw 16-bit PCM audio chunk.

        Args:
            chunk: Raw bytes of 16-bit signed little-endian PCM audio at
                   ``self._sample_rate`` Hz.  Expected to be 512 samples
                   (1024 bytes) but shorter/longer chunks are handled.
        """
        import torch  # type: ignore

        if not chunk:
            return

        # Decode int16 LE bytes to float32 [-1.0, 1.0]
        int16_array = np.frombuffer(chunk, dtype=np.int16)
        float32_array = int16_array.astype(np.float32) / _INT16_MAX

        # silero-VAD expects a 1-D float32 tensor
        audio_tensor = torch.from_numpy(float32_array)

        # Pad to minimum required chunk size if needed
        if len(audio_tensor) < _SILERO_CHUNK_SAMPLES:
            pad = torch.zeros(_SILERO_CHUNK_SAMPLES - len(audio_tensor))
            audio_tensor = torch.cat([audio_tensor, pad])
        elif len(audio_tensor) > _SILERO_CHUNK_SAMPLES:
            # Process in 512-sample windows
            for i in range(0, len(audio_tensor), _SILERO_CHUNK_SAMPLES):
                window = audio_tensor[i : i + _SILERO_CHUNK_SAMPLES]
                if len(window) < _SILERO_CHUNK_SAMPLES:
                    pad = torch.zeros(_SILERO_CHUNK_SAMPLES - len(window))
                    window = torch.cat([window, pad])
                self._process_window(window, float32_array[i : i + _SILERO_CHUNK_SAMPLES])
            return

        self._process_window(audio_tensor, float32_array)

    def _process_window(
        self, audio_tensor: "torch.Tensor", float32_samples: np.ndarray
    ) -> None:
        """Run VAD on a single 512-sample window and update state."""
        try:
            speech_prob: float = self._vad_model(
                audio_tensor, self._sample_rate
            ).item()
        except Exception as exc:  # pylint: disable=broad-except
            logger.warning("silero-VAD inference error (skipping chunk): %s", exc)
            return

        is_speech = speech_prob >= 0.5

        if is_speech:
            if not self._in_speech:
                logger.debug("Speech start detected (prob=%.3f).", speech_prob)
                self._in_speech = True
            self._speech_buffer.append(float32_samples)
            self._silence_samples = 0
        else:
            if self._in_speech:
                # Accumulate silence samples; flush utterance when threshold reached
                self._silence_samples += len(float32_samples)
                # Keep silence trailing audio for naturalness
                self._speech_buffer.append(float32_samples)

                if self._silence_samples >= self._silence_threshold_samples:
                    self._flush_utterance()
            # else: still outside speech — ignore

    def _flush_utterance(self) -> None:
        """Concatenate buffered speech and fire the on_utterance callback."""
        if not self._speech_buffer:
            return

        utterance = np.concatenate(self._speech_buffer)
        logger.debug(
            "Utterance detected: %.2f s (%d samples).",
            len(utterance) / self._sample_rate,
            len(utterance),
        )

        self._speech_buffer = []
        self._silence_samples = 0
        self._in_speech = False

        if self._on_utterance is not None:
            try:
                self._on_utterance(utterance)
            except Exception as exc:  # pylint: disable=broad-except
                logger.exception("on_utterance callback raised: %s", exc)
