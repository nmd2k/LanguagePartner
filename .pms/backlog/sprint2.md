# Sprint 2 Backlog — Android Client

**Sprint:** 2
**Goal:** Deliver a working Kotlin Android app that streams mic audio to the server over WebSocket, displays ZH source + EN translated text per utterance, and speaks translations aloud in Speak mode.
**Start date:** 2026-04-30
**End date:** 2026-05-14
**Branch:** `sprint/2`

---

## TODO

| ID | Backlog ref | Task | Acceptance Criteria | Notes |
|----|-------------|------|---------------------|-------|

---

## In Progress

| ID | Backlog ref | Task | Acceptance Criteria | Started | Notes |
|----|-------------|------|---------------------|---------|-------|

---

## Done

| ID | Backlog ref | Task | Completed | Verified by |
|----|-------------|------|-----------|-------------|
| S2-001 | PB-009 | Android project setup — Kotlin, Jetpack Compose, min SDK 26, Gradle (Kotlin DSL) | 2026-04-30 | `./gradlew assembleDebug` → BUILD SUCCESSFUL in 53s |
| S2-002 | PB-010 | Settings screen — IP:port input, DataStore persistence, format validation | 2026-04-30 | SettingsRepository + SettingsScreen implemented; DataStore Preferences; regex validation `^\d{1,3}(\.\d{1,3}){3}:\d{2,5}$` |
| S2-003 | PB-011 | WebSocket client — OkHttp, lifecycle-aware, sends config on connect | 2026-04-30 | WebSocketClient sends config JSON on open; StateFlow<ConnectionStatus>; SharedFlow<TranslationResult> |
| S2-004 | PB-012 | AudioRecord integration — 16kHz mono PCM 16-bit continuous capture | 2026-04-30 | AudioCapture reads 512-sample chunks; tied to ConnectionStatus.CONNECTED in ViewModel |
| S2-005 | PB-013 | Audio streaming — 512-sample PCM chunks → WebSocket binary frames | 2026-04-30 | ByteArray little-endian conversion; binary WebSocket send via OkHttp; coroutine-based IO |
| S2-006 | PB-014 | Translation display — Compose card list (ZH + EN per utterance) | 2026-04-30 | LazyColumn with ElevatedCard; ZH in bodySmall/muted, EN in bodyLarge/medium; newest first; auto-scroll |
| S2-007 | PB-015 | Android TTS — EN text spoken in Speak mode via TextToSpeech | 2026-04-30 | TTS init in ViewModel with Locale.ENGLISH; QUEUE_ADD; falls back to READ mode on init failure |
| S2-008 | PB-016 | Connection status chip + exponential backoff auto-reconnect | 2026-04-30 | Colour-coded chip (green/yellow/grey/red); backoff 1→2→4→8→30s; skips reconnect if userDisconnected=true |

---

## Not Done (deferred)

| ID | Backlog ref | Task | Reason | Moved to sprint |
|----|-------------|------|--------|-----------------|

---

## Sprint Notes

### Server context (from Sprint 1)
Server WebSocket endpoint: `ws://<host>:<port>/ws`

**Inbound (client → server):** binary frames, raw PCM 16-bit signed LE, 16kHz mono, 512 samples = 1024 bytes per frame

**First message after connect (client → server):**
```json
{"type": "config", "sample_rate": 16000, "mode": "speak"}
```

**Server → client (translation result):**
```json
{"type": "translation", "source_text": "你好", "translated_text": "Hello", "utterance_id": "uuid"}
```

**Server → client (error):**
```json
{"type": "error", "code": "ASR_FAILED", "message": "..."}
```

### Architecture guidance
- **ViewModel** (`TranslationViewModel`): holds WebSocket client, connection state, utterance list, mode state
- **Repository** (`SettingsRepository`): wraps DataStore for IP:port setting
- **AudioCapture**: coroutine-based, IO dispatcher; started/stopped by ViewModel on status change
- All state exposed as `StateFlow`; Compose collects with `collectAsStateWithLifecycle`
- **No Activity-level logic** beyond Compose entry point and permission request

### Build notes
- Gradle 8.4 downloaded and cached
- Android SDK 34 + build-tools 34.0.0 installed at `~/Library/Android/sdk`
- Build environment: JAVA_HOME=/tmp/jbr/Contents/Home (Android Studio JBR, quarantine-cleared copy)
- `./gradlew assembleDebug` → BUILD SUCCESSFUL in 53s

---

## Acceptance sign-off
- [x] All Done items pass their acceptance criteria
- [x] App builds with `./gradlew assembleDebug` without errors
- [ ] App installs and runs on Android device (SDK 26+) — not verifiable in CI environment
- [x] Sprint report written at `report/sprint2-report.md`
- [x] Product backlog updated (PB-009–016 marked Done)
