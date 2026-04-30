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
| PB-009 | Android project setup (Kotlin, Compose, min SDK 26, Gradle) | Chore | High | Open | 2 | Single-module app structure |
| PB-010 | Settings screen — IP:port input with DataStore persistence + validation | Feature | High | Open | 2 | Regex validation; show error on invalid format |
| PB-011 | WebSocket client — OkHttp WebSocket, lifecycle-aware connection management | Feature | High | Open | 2 | Send config message on connect; handle close/error |
| PB-012 | AudioRecord integration — 16kHz mono PCM 16-bit continuous capture | Feature | High | Open | 2 | Request RECORD_AUDIO permission on first launch |
| PB-013 | Audio streaming — chunk PCM to server via binary WebSocket frames | Feature | High | Open | 2 | 512-sample chunks; coroutine-based sender |
| PB-014 | Translation display — card list (ZH source + EN translation per utterance) | Feature | High | Open | 2 | Compose LazyColumn; auto-scroll to latest |
| PB-015 | Android TTS integration (Speak mode) — EN text → TextToSpeech | Feature | High | Open | 2 | QUEUE_ADD; Locale.ENGLISH |
| PB-016 | Connection status indicator + exponential backoff auto-reconnect | Feature | Medium | Open | 2 | Status chip; 1s/2s/4s/.../30s backoff |

### Sprint 3 — Integration & Polish

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-017 | End-to-end integration test (real Android device → local server) | Chore | High | Open | 3 | Manual test script in docs |
| PB-018 | Read / Speak mode toggle — UI + server config message on switch | Feature | High | Open | 3 | Persist mode within session |
| PB-019 | Error handling — server unreachable, ASR fail, model load fail, empty transcript | Feature | High | Open | 3 | User-visible error messages; no silent drops |
| PB-020 | Server GPU/CPU auto-detect (CUDA → MPS → CPU fallback) | Feature | Medium | Open | 3 | Log active device on startup |
| PB-021 | Setup documentation (README: server install + APK sideload steps) | Chore | Medium | Open | 3 | Target: non-developer user can follow |
| PB-022 | Server request logging (utterance ID, ASR latency, translation latency, total) | Chore | Low | Open | 3 | stdout structured log |

---

## Future Backlog (V2 / V3)

| ID | Title | Type | Priority | Status | Sprint | Notes |
|----|-------|------|----------|--------|--------|-------|
| PB-F01 | Simultaneous/streaming translation — SeamlessStreaming integration | Feature | High | Open | — | Replaces batch Whisper+NLLB pipeline; chunk-by-chunk with post-edit layer |
| PB-F02 | Custom C++ inference backend — own runtime wrapping model weights | Spike | High | Open | — | Replaces PyTorch; InferenceBackend ABC makes this a swap |
| PB-F03 | Selective language filtering — user picks N languages; others excluded | Feature | Medium | Open | — | Requires language ID step before translation |
| PB-F04 | Speaker diarization + voice print tracking | Feature | Medium | Open | — | Multi-speaker conversation support; label each utterance by speaker |
| PB-F05 | Multi-language pair support (beyond ZH→EN) | Feature | Medium | Open | — | UI to select source/target; server to load appropriate model |
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
