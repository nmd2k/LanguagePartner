# LanguagePartner

Real-time voice translation system: Android client streams microphone audio to a self-hosted Python server for speech recognition (Whisper) and translation (NLLB), returning results as JSON over WebSocket.

---

## Setup — Server

### Prerequisites

- Python 3.10+
- (Optional) CUDA-capable GPU for faster inference

### Install

```bash
cd server
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

### Run

```bash
python server.py --port 8080 --model medium --device auto
```

Arguments:
- `--port` — WebSocket port (default: `8080`)
- `--model` — Whisper model size: `tiny`, `base`, `small`, `medium`, `large` (default: `medium`)
- `--device` — Inference device: `auto`, `cpu`, `cuda` (default: `auto`; tries CUDA then falls back to CPU)

---

## Setup — Android

> Sprint 2 — documentation to be added after Android client implementation.

### Prerequisites

- Android device with Android 8.0+ (API 26+)
- ADB or direct APK sideloading

### Install

*(Coming in Sprint 2)*

---

## Architecture

```
Android Client (Kotlin)          Python Server
  AudioRecord (16kHz PCM)  ──►  WebSocket /ws
                                   │
                                   ├── silero-VAD (utterance segmentation)
                                   ├── Whisper medium (ZH-CN ASR)
                                   └── NLLB-600M (ZH→EN translation)
  Display source + translated  ◄──  JSON result
  Android TTS (Speak mode)
```

---

## License

Personal use only. NLLB-200 model is CC-BY-NC-4.0 (non-commercial).
