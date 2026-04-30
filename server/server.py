"""LanguagePartner server entrypoint.

Usage:
    python server.py --port 8765 --model medium --device auto
    python server.py --port 8765 --translation-backend hy-mt1.5

Flags:
    --port                 WebSocket listen port (default: 8765)
    --model                Whisper model size: tiny/base/small/medium/large (default: medium)
    --device               Inference device: auto/cpu/cuda (default: auto)
                            auto → tries CUDA first, then MPS (Apple Silicon), falls back to CPU.
    --translation-backend  Translation backend: nllb | hy-mt1.5 (default: nllb)

Environment variables:
    MOCK_MODE=1            Start with mock backends (no model loading, for testing)
    HYMT15_MODEL_PATH      Path to Hy-MT1.5 GGUF model file
"""
import argparse
import logging
import os
import sys

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("server")


def _resolve_device(device_arg: str) -> str:
    """Resolve 'auto' to 'cuda', 'mps', or 'cpu'."""
    if device_arg != "auto":
        return device_arg

    try:
        import torch  # type: ignore

        if torch.cuda.is_available():
            logger.info("CUDA available — using GPU.")
            return "cuda"
        elif torch.backends.mps.is_available():
            logger.info("MPS available (Apple Silicon) — using MPS.")
            return "mps"
        else:
            logger.info("CUDA and MPS not available — falling back to CPU.")
            return "cpu"
    except ImportError:
        logger.warning("PyTorch not installed; defaulting to CPU.")
        return "cpu"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="LanguagePartner: real-time voice translation server"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=8765,
        help="WebSocket listen port (default: 8765)",
    )
    parser.add_argument(
        "--model",
        type=str,
        default="medium",
        help="Whisper model size: tiny|base|small|medium|large (default: medium)",
    )
    parser.add_argument(
        "--device",
        type=str,
        default="auto",
        choices=["auto", "cpu", "cuda", "mps"],
        help="Inference device: auto|cpu|cuda|mps (default: auto)",
    )
    parser.add_argument(
        "--translation-backend",
        type=str,
        default="nllb",
        choices=["nllb", "hy-mt1.5"],
        help="Translation backend: nllb (NLLB-600M) | hy-mt1.5 (Hy-MT1.5 via llama.cpp) (default: nllb)",
    )
    args = parser.parse_args()

    # Check for mock mode (for testing without models)
    if os.environ.get("MOCK_MODE") == "1":
        logger.info("MOCK_MODE enabled — using mock backends (no model loading)")
        from backend.base import InferenceBackend

        class MockASR(InferenceBackend):
            def transcribe(self, audio, language=None):
                return ""  # Always return empty transcript for silence

            def translate(self, text, src_lang, tgt_lang):
                return ""

        class MockTranslation(InferenceBackend):
            def translate(self, text, src_lang, tgt_lang):
                if not text.strip():
                    return ""
                return "Hello"  # Mock translation

            def transcribe(self, audio, language=None):
                return ""

        asr = MockASR()
        translation = MockTranslation()
    else:
        device = _resolve_device(args.device)
        logger.info(
            "Starting server: port=%d model=%s device=%s",
            args.port,
            args.model,
            device,
        )

        # Load backends
        logger.info("Loading WhisperBackend (model=%s, device=%s) …", args.model, device)
        from backend.whisper_backend import WhisperBackend  # type: ignore

        asr = WhisperBackend(model_size=args.model, device=device)

        # Load translation backend
        if args.translation_backend == "hy-mt1.5":
            logger.info("Loading LlamaCppBackend (Hy-MT1.5) …")
            from backend.llama_translation_backend import LlamaCppBackend  # type: ignore

            translation = LlamaCppBackend(device=device)
            if not translation.is_loaded:
                logger.warning(
                    "Hy-MT1.5 failed to load; falling back to NLLB-600M. "
                    "Reason: %s", translation.status_message
                )
                from backend.translation_backend import TranslationBackend  # type: ignore
                translation = TranslationBackend(device=device)
        else:
            logger.info("Loading TranslationBackend (NLLB-600M) …")
            from backend.translation_backend import TranslationBackend  # type: ignore

            translation = TranslationBackend(device=device)

    # Wire backends into the FastAPI app
    from app import app, configure_backends  # type: ignore

    configure_backends(asr, translation)

    print(
        f"Models loaded. "
        f"Listening on ws://0.0.0.0:{args.port} "
        f"[ASR: whisper-{args.model}, "
        f"MT: {type(translation).__name__}]"
    )

    # Start uvicorn
    import uvicorn  # type: ignore

    uvicorn.run(app, host="0.0.0.0", port=args.port, log_level="info")


if __name__ == "__main__":
    main()
