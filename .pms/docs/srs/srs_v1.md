# Software Requirements Specification — LanguagePartner

**Version:** 1.0
**Date:** 2026-04-30
**Status:** Approved
**Author:** Agent + nmd2k

---

## 1. Overall Description

### 1.1 Product Overview

LanguagePartner is a real-time voice translation system consisting of a self-hosted server and an Android client app. The Android app acts purely as an input/output device: it captures the user's speech continuously and streams it to the server, which performs speech recognition and translation, then returns results to the app. The app can display the translation as text or speak it aloud via the device's built-in TTS engine.

The key differentiator from on-device solutions (e.g., RTranslator) is that all heavy model inference runs on the user's own machine (laptop/desktop), not on the phone. This avoids battery drain, thermal throttling, and storage constraints on mobile.

### 1.2 Product Scope

**In scope (MVP):**
- Self-hosted Python server: speech-to-text (ASR) + translation, launched via CLI
- Android client: continuous mic capture, WebSocket streaming, translation display, TTS playback
- Language pair: Simplified Chinese (ZH-CN) → English (EN)
- Single-user, single-session (personal use)

**Explicitly out of scope for MVP:**
- Simultaneous / streaming translation (chunk-by-chunk with post-edit layer) — V2
- Custom C++ inference runtime — V3
- Selective language filtering — V2
- Speaker diarization / voice print tracking — V3
- Multi-language pair support beyond ZH→EN — V2
- Authentication or multi-user server — future
- iOS client — future
- mDNS / automatic server discovery — future
- Server-side TTS — future

### 1.3 Intended Audience

Developers and the product owner (nmd2k). No external stakeholders for MVP.

### 1.4 Definitions, Acronyms, Abbreviations

| Term | Definition |
|------|-----------|
| ASR | Automatic Speech Recognition — converting audio to text |
| TTS | Text-to-Speech — converting text to audio |
| VAD | Voice Activity Detection — detecting speech vs silence in audio stream |
| NMT | Neural Machine Translation |
| PCM | Pulse-Code Modulation — raw uncompressed audio format |
| WebSocket | Full-duplex TCP-based protocol for real-time bidirectional communication |
| NLLB | No Language Left Behind — Meta's multilingual translation model |
| OPUS-MT | Helsinki-NLP's open-source neural machine translation models |
| silero-VAD | Lightweight, accurate VAD model by Silero team |
| Whisper | OpenAI's multilingual ASR model |
| InferenceBackend | Abstract interface for swappable model inference implementations |

---

## 2. Product Perspective

### 2.1 System Interfaces

```
┌─────────────────────────────┐         ┌──────────────────────────────────────┐
│     Android Client (Kotlin) │         │         Python Server                │
│                             │         │                                      │
│  AudioRecord (mic)          │─────────│▶ WebSocket handler                   │
│  16kHz, mono, PCM 16-bit    │ stream  │    │                                 │
│                             │         │    ├─ silero-VAD                     │
│  Display: ZH source text    │         │    │   └─ utterance segmentation      │
│  Display: EN translated text│◀────────│    ├─ Whisper medium (ASR)           │
│  Android TTS (Speak mode)   │  JSON   │    │   └─ audio → ZH-CN text         │
│                             │         │    └─ NLLB-600M / OPUS-MT (NMT)      │
│  Settings: IP:port          │         │        └─ ZH-CN → EN text            │
└─────────────────────────────┘         └──────────────────────────────────────┘
```

### 2.2 User Interfaces

**Android app screens:**

1. **Settings screen** — input field for server IP:port (e.g., `192.168.1.10:8080`); persisted across sessions.
2. **Main screen:**
   - Connection status chip (Connected / Disconnected / Connecting)
   - Read / Speak mode toggle
   - Live microphone indicator (pulsing when active)
   - Translation cards: source text (ZH) on top, translated text (EN) below
   - History of past utterance pairs (scrollable)

No wireframes required for MVP; functional correctness prioritised over polish.

### 2.3 Hardware Interfaces

- **Android device**: microphone (required), speaker (for Speak mode)
- **Server machine**: CPU (required), GPU optional (CUDA or MPS for acceleration)

### 2.4 Software Interfaces

| Component | Library / Tool | Version |
|---|---|---|
| ASR | openai-whisper or faster-whisper | latest stable |
| Translation | facebook/nllb-200-distilled-600M (HuggingFace) or Helsinki-NLP/opus-mt-zh-en | latest stable |
| VAD | silero-vad | latest stable |
| Server framework | FastAPI + uvicorn | ≥0.100 |
| WebSocket (server) | websockets (via FastAPI/Starlette) | built-in |
| WebSocket (Android) | OkHttp WebSocket | ≥4.12 |
| Audio capture (Android) | android.media.AudioRecord | SDK built-in |
| TTS (Android) | android.speech.tts.TextToSpeech | SDK built-in |
| Settings persistence | Jetpack DataStore (Preferences) | ≥1.0 |
| Android UI | Jetpack Compose | ≥1.5 |

### 2.5 Communication Interfaces

**Protocol:** WebSocket (ws://)

**Audio stream (client → server):**
- Format: raw PCM, 16-bit signed little-endian, 16kHz, mono
- Chunk size: 512 samples (32ms) per send
- Encoding: binary WebSocket frames

**Translation result (server → client):**
```json
{
  "type": "translation",
  "source_text": "你好，今天天气怎么样？",
  "translated_text": "Hello, how is the weather today?",
  "utterance_id": "uuid-v4"
}
```

**Error message (server → client):**
```json
{
  "type": "error",
  "code": "ASR_FAILED",
  "message": "Whisper inference error: ..."
}
```

**Control message (client → server):**
```json
{
  "type": "config",
  "sample_rate": 16000,
  "mode": "speak"
}
```
Sent once on connection establishment.

### 2.6 Memory Constraints

| Component | Estimated RAM / VRAM |
|---|---|
| Whisper medium (CPU) | ~5GB RAM |
| Whisper medium (CUDA) | ~5GB VRAM |
| NLLB-600M | ~2.5GB RAM |
| silero-VAD | ~50MB |
| **Total (CPU)** | ~8GB RAM |
| **Total (GPU)** | ~8GB VRAM |

Server machine should have ≥16GB RAM for comfortable CPU inference.

---

## 3. Design Constraints

1. **Swappable inference backend**: all model calls must go through the `InferenceBackend` abstract interface defined in `server/backend/base.py`. No direct model calls in the WebSocket handler.
2. **Monorepo**: `server/` (Python) and `android/` (Kotlin) in the same repository.
3. **No cloud dependencies**: all inference runs locally; no calls to external APIs.
4. **No authentication for MVP**: server accepts any WebSocket connection on the configured port.
5. **Android min SDK 26** (Android 8.0) for AudioRecord API stability and modern Kotlin/Compose support.
6. **Audio format fixed at 16kHz mono PCM 16-bit**: matches Whisper's expected input; no resampling on server for MVP.

---

## 4. Product Functions

| # | Function | Priority |
|---|----------|----------|
| F-01 | Continuous mic capture and WebSocket audio streaming | High |
| F-02 | Server-side VAD utterance segmentation | High |
| F-03 | ZH-CN speech-to-text via Whisper | High |
| F-04 | ZH→EN text translation via NLLB/OPUS-MT | High |
| F-05 | Display source + translated text in Android app | High |
| F-06 | Speak mode: read translated text via Android TTS | High |
| F-07 | Server CLI entrypoint (port, model, device flags) | High |
| F-08 | Pluggable InferenceBackend interface | High |
| F-09 | Settings screen with IP:port configuration | High |
| F-10 | Connection status + auto-reconnect | Medium |
| F-11 | GPU/CPU auto-detection on server | Medium |
| F-12 | Basic server logging (latency per utterance) | Low |

---

## 5. User Characteristics

Single user (developer / product owner). Technical proficiency: comfortable running Python CLI commands, sideloading APKs, and configuring LAN IP addresses. No accessibility requirements for MVP.

---

## 6. Specific Requirements

### 6.1 Functional Requirements

#### FR-S01: WebSocket Server Endpoint
- **Description:** Server exposes a WebSocket endpoint at `ws://<host>:<port>/ws`
- **Inputs:** TCP connection from Android client
- **Processing:** Accept connection; expect first message to be a `config` JSON frame
- **Outputs:** Acknowledge config; begin accepting binary audio frames
- **Acceptance Criteria:** Client can connect; server logs new connection with remote address

#### FR-S02: Continuous Audio Reception
- **Description:** Server receives binary WebSocket frames containing raw PCM audio chunks
- **Inputs:** Binary frames, 16-bit signed LE, 16kHz mono, 512 samples each
- **Processing:** Append chunks to an in-memory ring buffer
- **Outputs:** Growing audio buffer available to VAD
- **Acceptance Criteria:** Server accumulates audio without frame loss at 32ms chunk cadence

#### FR-S03: VAD Utterance Segmentation
- **Description:** silero-VAD processes buffered audio to detect speech/silence boundaries
- **Inputs:** Audio buffer (ring buffer)
- **Processing:** Run silero-VAD on each incoming chunk; when speech→silence transition detected with silence ≥300ms, flush buffered speech as one utterance
- **Outputs:** Complete utterance audio array (numpy float32, 16kHz)
- **Acceptance Criteria:** Utterance detected and passed to ASR within 350ms of speech end

#### FR-S04: Whisper ASR
- **Description:** Whisper medium model transcribes utterance audio to ZH-CN text
- **Inputs:** Utterance audio (numpy float32, 16kHz)
- **Processing:** Run via `InferenceBackend.transcribe(audio)` → WhisperBackend calls model; language forced to `zh`
- **Outputs:** ZH-CN transcription string
- **Acceptance Criteria:** Returns non-empty string for clear Chinese speech input; handles short (<1s) noise gracefully (returns empty string, no crash)

#### FR-S05: Translation
- **Description:** NMT model translates ZH-CN text to EN
- **Inputs:** ZH-CN transcription string
- **Processing:** Run via `InferenceBackend.translate(text, src_lang, tgt_lang)` → TranslationBackend; skip if transcription empty
- **Outputs:** EN translation string
- **Acceptance Criteria:** Produces fluent English translation for common ZH-CN phrases; empty input returns empty output

#### FR-S06: Result Delivery
- **Description:** Server sends translation result to client over WebSocket
- **Inputs:** ZH source text, EN translated text, utterance UUID
- **Processing:** Serialize to JSON; send as text WebSocket frame
- **Outputs:** JSON message per FR-S06 schema (§2.5)
- **Acceptance Criteria:** Client receives result within 5s of utterance end on CPU inference

#### FR-S07: CLI Entrypoint
- **Description:** Server started via `python server.py` with CLI flags
- **Inputs:** `--port` (default 8080), `--model` (default `medium`), `--device` (default `auto`)
- **Processing:** Parse args; load models; start uvicorn
- **Outputs:** Server running, models loaded, ready message printed
- **Acceptance Criteria:** `python server.py --port 8080 --model medium` starts successfully; prints "Models loaded. Listening on ws://0.0.0.0:8080"

#### FR-S08: InferenceBackend Interface
- **Description:** Abstract base class isolates model implementation from server logic
- **Inputs:** N/A (design constraint)
- **Processing:** `base.py` defines ABC with `transcribe(audio: np.ndarray) -> str` and `translate(text: str, src: str, tgt: str) -> str`
- **Outputs:** N/A
- **Acceptance Criteria:** WhisperBackend and TranslationBackend implement the ABC; server handler imports only `InferenceBackend`

---

#### FR-A01: Settings Screen
- **Description:** User enters server address (IP:port) in a settings screen
- **Inputs:** User text input
- **Processing:** Validate format (regex `\d{1,3}(\.\d{1,3}){3}:\d{2,5}`); persist via DataStore
- **Outputs:** Saved address available to WebSocket client
- **Acceptance Criteria:** Address persists across app restarts; invalid format shows inline error

#### FR-A02: Mode Toggle
- **Description:** Toggle between Read mode (text display only) and Speak mode (TTS playback)
- **Inputs:** User tap on toggle
- **Processing:** Update mode state; send updated `config` message to server if connected
- **Outputs:** UI reflects active mode; TTS enabled/disabled accordingly
- **Acceptance Criteria:** Toggle persists within session; mode change takes effect on next utterance result

#### FR-A03: Continuous Mic Capture
- **Description:** App captures microphone audio continuously while connected to server
- **Inputs:** Microphone hardware
- **Processing:** AudioRecord at 16kHz, mono, PCM_16BIT; read in 512-sample chunks; dispatch to WebSocket sender coroutine
- **Outputs:** Continuous stream of audio chunks
- **Acceptance Criteria:** Audio captured without gaps; mic indicator visible; RECORD_AUDIO permission requested on first launch

#### FR-A04: Audio Streaming
- **Description:** App streams audio chunks to server via WebSocket binary frames
- **Inputs:** 512-sample PCM chunks from AudioRecord
- **Processing:** Convert short array to ByteArray (little-endian); send as OkHttp WebSocket binary message
- **Outputs:** Binary frames delivered to server
- **Acceptance Criteria:** Server receives all frames in order; no out-of-memory on sustained streaming

#### FR-A05: Translation Display
- **Description:** App displays each utterance result as a card with source + translated text
- **Inputs:** JSON message from server (type=translation)
- **Processing:** Parse JSON; prepend card to scrollable list
- **Outputs:** Visible card showing ZH source (smaller, muted) and EN translation (larger, prominent)
- **Acceptance Criteria:** New cards appear within 200ms of receiving WebSocket message; list scrolls automatically to latest

#### FR-A06: TTS Playback (Speak Mode)
- **Description:** In Speak mode, translated EN text is spoken via Android TextToSpeech
- **Inputs:** EN translated text string
- **Processing:** TextToSpeech.speak() with QUEUE_ADD; language set to Locale.ENGLISH
- **Outputs:** Audio output from device speaker
- **Acceptance Criteria:** TTS speaks text within 500ms of card appearing; queues correctly for rapid successive utterances

#### FR-A07: Connection Status
- **Description:** App shows real-time connection status
- **Inputs:** WebSocket lifecycle events (open, close, failure)
- **Processing:** Map events to status enum (CONNECTING, CONNECTED, DISCONNECTED, ERROR)
- **Outputs:** Status chip on main screen with colour coding (green/grey/red)
- **Acceptance Criteria:** Status updates within 1s of actual state change

#### FR-A08: Auto-Reconnect
- **Description:** App automatically retries connection on disconnect
- **Inputs:** WebSocket close/failure event
- **Processing:** Exponential backoff retry (1s, 2s, 4s, max 30s); stop retrying if user navigates to settings
- **Outputs:** Reconnection attempt; success restores streaming
- **Acceptance Criteria:** App reconnects automatically within 35s of server restart without user action

---

### 6.2 Performance Requirements

| Metric | Target (MVP) |
|---|---|
| End-to-end latency (CPU inference) | < 5s per utterance |
| End-to-end latency (GPU inference) | < 2s per utterance |
| Audio chunk delivery interval | 32ms (512 samples @ 16kHz) |
| VAD detection delay after speech end | < 350ms |
| Server startup time (models loaded) | < 60s on CPU |
| Concurrent sessions | 1 (personal use) |

### 6.3 External Interface Requirements

See §2.5 for full WebSocket message schemas.

Server must respond to each utterance with a result or error message — no silent drops. If ASR or translation fails, send `type=error` message.

### 6.4 System Attributes

| Attribute | Requirement |
|-----------|-------------|
| Reliability | Server recovers from per-utterance inference errors without crashing; logs stack trace |
| Availability | Single-user local deployment; no uptime SLA |
| Security | No auth for MVP; server should only be run on trusted LAN |
| Maintainability | InferenceBackend ABC enables backend swap without touching server/handler code |
| Portability | Server: Python ≥3.10, runs on macOS/Linux/Windows. Android: min SDK 26 |

---

## 7. Other Requirements

- **Licensing**: all dependencies must be permissively licensed (MIT, Apache 2.0). NLLB-200 is CC-BY-NC-4.0 (non-commercial) — acceptable for personal use MVP; must be replaced with a commercially licensed model before any public release.
- **Privacy**: no audio or text is transmitted to any third-party server. All data stays on the user's LAN.
- **No telemetry**: app and server collect no usage data.

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-30 | Agent + nmd2k | Initial draft — MVP scope |
