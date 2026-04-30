"""LanguagePartner server entrypoint.

Usage:
    python server.py --port 8080 --model medium --device auto

Flags:
    --port    WebSocket listen port (default: 8080)
    --model   Whisper model size: tiny/base/small/medium/large (default: medium)
    --device  Inference device: auto/cpu/cuda (default: auto)
              auto → tries CUDA first, falls back to CPU.
"""
import argparse
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("server")


def _resolve_device(device_arg: str) -> str:
    """Resolve 'auto' to 'cuda' or 'cpu'."""
    if device_arg != "auto":
        return device_arg

    try:
        import torch  # type: ignore

        if torch.cuda.is_available():
            logger.info("CUDA available — using GPU.")
            return "cuda"
        else:
            logger.info("CUDA not available — falling back to CPU.")
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
        default=8080,
        help="WebSocket listen port (default: 8080)",
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
        choices=["auto", "cpu", "cuda"],
        help="Inference device: auto|cpu|cuda (default: auto)",
    )
    args = parser.parse_args()

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

    logger.info("Loading TranslationBackend …")
    from backend.translation_backend import TranslationBackend  # type: ignore

    translation = TranslationBackend(device=device)

    # Wire backends into the FastAPI app
    from app import app, configure_backends  # type: ignore

    configure_backends(asr, translation)

    print(f"Models loaded. Listening on ws://0.0.0.0:{args.port}")

    # Start uvicorn
    import uvicorn  # type: ignore

    uvicorn.run(app, host="0.0.0.0", port=args.port, log_level="info")


if __name__ == "__main__":
    main()
