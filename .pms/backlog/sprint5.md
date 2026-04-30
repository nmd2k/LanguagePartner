# Sprint 5 Backlog — User Experiment (UX Polish & Model Hosting)

**Sprint:** 5
**Goal:** Fix 6 UX bugs from sprint 4, add Android Debug screen, implement server model hosting dashboard, and integrate Hy-MT1.5 translation model via llama.cpp.
**Start date:** 2026-04-30
**End date:** 2026-05-14
**Branch:** `sprint/5`

---

## In Progress

| ID | Backlog ref | Task | Started | Status |
|----|-------------|------|---------|--------|
| — | — | — | — | — |

---

## Done

| ID | Backlog ref | Task | Completed | Verified by |
|----|-------------|------|-----------|-------------|
| — | — | — | — | — |

---

## Sprint Items

### Bug Fixes

| ID | Backlog ref | Task | Priority |
|----|-------------|------|----------|
| S5-001 | PB-030 | Fix reverse/swap button — when selecting a language that matches the other side, swap instead of creating duplicate (source==target collision) | High |
| S5-002 | PB-031 | Fix conversation bubble alignment — remove alternating left/right pattern; all bubbles aligned to one side; append new utterances to bottom (latest conv at bottom) | High |
| S5-003 | PB-032 | Hide typing bar and language swap when listening; waveform animation visible only when mic is actively recording (isListening) | High |
| S5-004 | PB-033 | Dynamic TTS locale — set TTS voice language based on target language code (en→Locale.ENGLISH, zh→Locale.CHINESE, vi→"vi", si→"si") | High |
| S5-005 | PB-034 | Android Debug screen — log viewer panel with timestamps, log levels (INFO/DEBUG/WARN/ERROR), coloured entries, stat row; follow ScreenDebug mockup | Medium |
| S5-006 | PB-035 | Server model hosting dashboard — Running Models table + Model Browser catalog with Load/Unload buttons, role filter (ASR/MT/TTS), VRAM/latency stats | High |
| S5-007 | PB-036 | Swap NLLB-600M with Hy-MT1.5 via llama.cpp — download tencent/Hy-MT1.5-1.8B-1.25bit-GGUF; create LlamaCppBackend using llama-cpp-python; keep NLLB as fallback | High |
| S5-008 | PB-037 | Server-side structured logging — emit log events via a shared event bus; stream logs to Android debug panel via WebSocket | Medium |

---

## Sprint Notes

### Key files to modify / create

**Android (modify):**
- `viewmodel/TranslationViewModel.kt` — fix swapLanguages collision; fix prepend→append ordering; dynamic TTS locale; add log streaming
- `ui/main/MainScreen.kt` — remove alternating bubble alignment; hide swap+typing bar when listening; waveform only on isListening
- `ui/settings/SettingsScreen.kt` — add Debug console navigation row (currently no-op)
- `MainActivity.kt` — add Debug screen route

**Android (new):**
- `ui/debug/DebugScreen.kt` — log viewer panel following ScreenDebug mockup

**Server (modify):**
- `backend/translation_backend.py` — keep NLLB as default; support optional Hy-MT1.5 backend
- `app.py` — add structured log event bus; stream logs over WebSocket as "log" messages
- `server.py` — support `--translation-backend` CLI flag (nllb | hy-mt1.5)

**Server (new):**
- `backend/llama_translation_backend.py` — Hy-MT1.5 via llama-cpp-python implementing InferenceBackend ABC
- `dashboard/` — optional web dashboard for model hosting (FastAPI static + templates)

### Design references
- `.pms/design/android-debug.jsx` — `ScreenDebug` (log viewer)
- `.pms/design/dashboard-models.jsx` — `DashModels` (model hosting)
- `.pms/design/android-main.jsx` — `ScreenMain` (for bubble alignment and listening state reference)
- `.pms/design/dashboard-overview.jsx` — `DashShell`, `DashCard` (dashboard chrome)

### Dependencies between items
- S5-001 to S5-004 (bug fixes) are independent and can run in parallel
- S5-005 (debug screen) depends on S5-008 (server logs) for real data but can build UI first with mock data
- S5-007 (Hy-MT1.5) requires model download first; keep NLLB as fallback to avoid blocking other items
- S5-006 (model hosting dashboard) can run independently; S5-007 model will appear in the dashboard

### Language-locale mapping for TTS
| Language | Whisper Code | Android Locale |
|----------|-------------|----------------|
| English | en | `Locale.ENGLISH` |
| Chinese Simplified | zh | `Locale.CHINESE` |
| Vietnamese | vi | `Locale("vi")` |
| Sinhala | si | `Locale("si")` |

---

## Acceptance sign-off
- [ ] All Done items pass their acceptance criteria
- [ ] `pytest server/tests/` passes (no regressions)
- [ ] `./gradlew assembleDebug` passes (no regressions)
- [ ] Swap button handles same-language collision (swaps instead of duplicating)
- [ ] Conversation bubbles appear at bottom, aligned to one side, newest at bottom
- [ ] Typing bar and language swap hidden when actively listening
- [ ] Waveform animation only shows when mic is recording
- [ ] TTS speaks in the correct target language voice
- [ ] Debug screen shows log entries with coloured levels
- [ ] Model hosting dashboard shows running models with load/unload
- [ ] Hy-MT1.5 backend produces translations via llama.cpp
- [ ] Sprint report written at `report/sprint5-report.md`
- [ ] Product backlog updated
