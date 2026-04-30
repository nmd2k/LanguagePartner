# Product Backlog — LanguagePartner

**Product:** LanguagePartner
**Last updated:** 2026-04-30

> Single source of truth for all planned work. Items ordered by priority within each section.

---

## Backlog Items

### Sprint 1 — Server Foundation

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-001 | Monorepo project structure (server/ + android/ dirs, .gitignore, README skeleton) | Chore | High | Done | 1 | Set up repo layout before any code |
| PB-002 | Python server scaffold — FastAPI app + WebSocket endpoint `/ws` | Feature | High | Done | 1 | Accept config message, log connection |
| PB-003 | InferenceBackend abstract base class (`backend/base.py`) | Chore | High | Done | 1 | ABC with `transcribe()` and `translate()` methods |
| PB-004 | WhisperBackend — Whisper medium ASR integration (ZH-CN forced) | Feature | High | Done | 1 | Use faster-whisper for speed; fallback openai-whisper |
| PB-005 | TranslationBackend — NLLB-600M or OPUS-MT zh→en integration | Feature | High | Done | 1 | Try NLLB first; OPUS-MT as fallback if VRAM tight |
| PB-006 | silero-VAD utterance segmentation pipeline | Feature | High | Done | 1 | 300ms silence threshold; flush to ASR |
| PB-007 | CLI entrypoint (`server.py`) with argparse (--port, --model, --device) | Feature | High | Done | 1 | Auto-detect GPU/CPU for --device auto |
| PB-008 | Server unit tests — ASR output format, translation output, VAD segmentation | Chore | Medium | Done | 1 | pytest; use short audio fixture files |

### Sprint 2 — Android Client

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-009 | Android project setup (Kotlin, Compose, min SDK 26, Gradle) | Chore | High | Done | 2 | Single-module app; assembleDebug passes |
| PB-010 | Settings screen — IP:port input with DataStore persistence + validation | Feature | High | Done | 2 | Regex validation; inline error; DataStore |
| PB-011 | WebSocket client — OkHttp WebSocket, lifecycle-aware connection management | Feature | High | Done | 2 | Config JSON on open; StateFlow status |
| PB-012 | AudioRecord integration — 16kHz mono PCM 16-bit continuous capture | Feature | High | Done | 2 | 512-sample chunks; IO coroutine |
| PB-013 | Audio streaming — chunk PCM to server via binary WebSocket frames | Feature | High | Done | 2 | Binary frames; little-endian PCM |
| PB-014 | Translation display — card list (ZH source + EN translation per utterance) | Feature | High | Done | 2 | LazyColumn; auto-scroll; ElevatedCard |
| PB-015 | Android TTS integration (Speak mode) — EN text → TextToSpeech | Feature | High | Done | 2 | QUEUE_ADD; Locale.ENGLISH; graceful failure |
| PB-016 | Connection status indicator + exponential backoff auto-reconnect | Feature | Medium | Done | 2 | Colour chip; 1→2→4→8→30s backoff |

### Sprint 3 — Integration & Polish

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-017 | End-to-end integration test (real Android device → local server) | Chore | High | Done | 3 | Manual test script in docs |
| PB-018 | Read / Speak mode toggle — UI + server config message on switch | Feature | High | Done | 3 | Persist mode within session |
| PB-019 | Error handling — server unreachable, ASR fail, model load fail, empty transcript | Feature | High | Done | 3 | User-visible error messages; no silent drops |
| PB-020 | Server GPU/CPU auto-detect (CUDA → MPS → CPU fallback) | Feature | Medium | Done | 3 | Log active device on startup |
| PB-021 | Setup documentation (README: server install + APK sideload steps) | Chore | Medium | Done | 3 | Target: non-developer user can follow |
| PB-022 | Server request logging (utterance ID, ASR latency, translation latency, total) | Chore | Low | Done | 3 | stdout structured log |

### Sprint 4 — v1: Language Selection, Pause Control & UI Redesign

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-023 | Language config infrastructure — remove hardcoded ZH→EN; NLLB/Whisper code mapping for EN/ZH/VI/SI; extend WebSocket config protocol | Feature | High | Done | 4 | NLLB: eng_Latn, zho_Hans, vie_Latn, sin_Sinh |
| PB-024 | Pause/Resume translation control — server skip VAD loop; Android stop/start AudioCapture; pause button UI | Feature | High | Done | 4 | Mic button toggles between mic/pause |
| PB-025 | Text input translation — server handle `text_input` message; Android text bar + send button; display result as bubble | Feature | High | Done | 4 | Reuses TranslationBackend.translate() |
| PB-026 | Main screen redesign — language bar + swap, conversation bubbles (speaker chip + original + translated + timestamp), waveform visualizer, bottom bar (text input + mic/pause) | Feature | High | Done | 4 | Follows ScreenMain mockup |
| PB-027 | Language picker screen — searchable list with code badge/name/native name; checkmark selection; 4 languages | Feature | High | Done | 4 | EN, ZH, VI, SI |
| PB-028 | Settings & Server Setup redesign — sections (Account/Translation/Developer); URL field + connection test + recent servers; DataStore for lang prefs | Feature | Medium | Done | 4 | Follows ScreenSettings + ScreenServerSetup mockups |
| PB-029 | License (CC BY-NC 4.0) + README update | Chore | Medium | Done | 4 | |

---

### Sprint 5 — User Experiment (UX Polish & Model Hosting)

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-030 | Fix reverse/swap button — prevent source==target collision; swap when picking a language that matches the other side | Bug | High | Open | 5 | Bug #1 |
| PB-031 | Fix conversation bubble alignment — remove alternating left/right; all bubbles on one side; append new utterances to bottom | Bug | High | Open | 5 | Bugs #2, #3 |
| PB-032 | Hide typing bar and swap when listening; waveform animation only active when mic is recording | Bug | High | Open | 5 | Bug #4 |
| PB-033 | Dynamic TTS locale — set TTS voice language based on target language, not hardcoded English | Bug | High | Open | 5 | Bug #5 |
| PB-034 | Android Debug screen — log viewer panel showing server log entries (timestamps, levels, messages) | Feature | Medium | Open | 5 | Follows ScreenDebug mockup; Bug #6 |
| PB-035 | Server model hosting dashboard — Running Models table + Model Browser catalog with Load/Unload, role filter, VRAM/latency stats | Feature | High | Open | 5 | Follows DashModels mockup; Feature #7 |
| PB-036 | Swap NLLB-600M with Hy-MT1.5 via llama.cpp — integrate tencent/Hy-MT1.5-1.8B-1.25bit-GGUF for faster translation | Feature | High | Open | 5 | Feature #8 |
| PB-037 | Server-side structured logging — emit log events via event bus for Android debug panel consumption | Feature | Medium | Open | 5 | Server→Client log streaming |
| PB-038 | UX polish — instant scroll (no animation), keyboard dismiss on text send, waveform only in LiveListeningIndicator, bubble style match ScreenMain mockup | Bug | High | Done | 5 | Bugs #7-#9 |

---

## Future Backlog (V2 / V3)

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-F01 | Simultaneous/streaming translation — SeamlessStreaming integration | Feature | High | Open | — | Replaces batch Whisper+NLLB pipeline; chunk-by-chunk with post-edit layer |
| PB-F02 | Custom C++ inference backend — own runtime wrapping model weights | Spike | High | Open | — | Replaces PyTorch; InferenceBackend ABC makes this a swap |
| PB-F03 | Selective language filtering — user picks N languages; others excluded | Feature | Medium | Open | — | Requires language ID step before translation |
| PB-F04 | Speaker diarization + voice print tracking | Feature | Medium | Open | — | Multi-speaker conversation support; label each utterance by speaker |
| PB-F05 | Multi-language pair support (beyond ZH→EN) | Feature | Medium | **Done via Sprint 4** | 4 | PB-023 covers this |
| PB-F06 | mDNS server auto-discovery | Feature | Low | Open | — | Remove manual IP:port entry |
| PB-F07 | Authentication (API key or token) | Feature | Low | Open | — | Required before any multi-user or internet-exposed deployment |
| PB-F08 | iOS client | Feature | Low | Open | — | Separate native app or shared Kotlin Multiplatform layer |
| PB-F09 | Server-side TTS (Piper) — return audio instead of text | Feature | Low | Open | — | Enables consistent voice quality independent of device TTS |

---

## Types
- **Feature** — new user-visible capability
- **Bug** — defect in existing behaviour
- **Chore** — internal improvement (scaffold, refactor, tooling, tests, docs)
- **Spike** — time-boxed research or proof-of-concept

## Status values
- **Open** — ready to be picked into a sprint
- **In Sprint** — currently assigned to an active sprint
- **Done** — completed and verified
- **Deferred** — intentionally postponed
- **Cancelled** — no longer needed

---

## Revision History

| Date | Author | Change |
|------|--------|--------|
| 2026-04-30 | Agent + nmd2k | Created — MVP scope (3 sprints) + future backlog |
| 2026-04-30 | Agent (sprint/1) | PB-001–008 marked Done after Sprint 1 completion |
| 2026-04-30 | Agent (sprint/2) | PB-009–016 marked Done after Sprint 2 completion |
| 2026-04-30 | Agent (sprint/3) | PB-017–022 marked Done after Sprint 3 completion; MVP complete |
| 2026-04-30 | Agent + nmd2k | Sprint 4 v1 scope added — PB-023–029 (language selection, pause control, text input, UI redesign, license); PB-F05 closed as Done via Sprint 4 |
| 2026-04-30 | Agent | Sprint 4 closed — PB-023–029 marked Done; Sprint 5 scope added — PB-030–037 (UX polish, debug screen, model hosting, Hy-MT1.5) |
