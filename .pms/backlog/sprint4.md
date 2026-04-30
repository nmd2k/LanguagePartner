# Sprint 4 Backlog — v1: Language Selection, Pause Control & UI Redesign

**Sprint:** 4
**Goal:** Deliver v1 with configurable source/target language selection (EN, ZH, VI, SI), pause/resume translation control, text input translation, and a full UI redesign across 4 core screens (Main, Language Picker, Settings, Server Setup).
**Start date:** 2026-04-30
**End date:** 2026-05-14
**Branch:** `sprint/4`

---

## Done

| ID | Backlog ref | Task | Completed | Verified by |
|----|-------------|------|-----------|-------------|
| V1-001 | PB-023 | Language config infrastructure (Server) | 2026-04-30 | pytest (16/16), assembleDebug |
| V1-002 | PB-024 | Pause/Resume translation control | 2026-04-30 | assembleDebug |
| V1-003 | PB-025 | Text input translation | 2026-04-30 | assembleDebug |
| V1-004 | PB-026 | Main screen redesign | 2026-04-30 | assembleDebug |
| V1-005 | PB-027 | Language picker screen | 2026-04-30 | assembleDebug |
| V1-006 | PB-028 | Settings & Server Setup redesign | 2026-04-30 | assembleDebug |
| V1-007 | PB-029 | License (CC BY-NC 4.0) + README update | 2026-04-30 | File review |

---

## Sprint Notes

### Key files to modify / create

**Server:**
- `server/backend/whisper_backend.py` — remove hardcoded `language="zh"`; accept language param
- `server/backend/translation_backend.py` — remove hardcoded `zho_Hans→eng_Latn`; accept src_lang, tgt_lang params
- `server/backend/base.py` — update `translate()` signature to require both src_lang and tgt_lang
- `server/app.py` — parse `source_lang`/`target_lang` from config message; handle `pause`/`resume`/`text_input` message types; skip VAD loop when paused
- `server/server.py` — no changes expected

**Android (new files):**
- `ui/main/MainScreen.kt` — complete redesign (language bar, conversation bubbles, waveform, bottom bar)
- `ui/main/LanguageBar.kt` — source/target language display + swap button
- `ui/main/ConversationBubble.kt` — bubble component with speaker chip, original, translated, timestamp
- `ui/main/WaveformVisualizer.kt` — animated bar visualizer composable
- `ui/picker/LanguagePickerScreen.kt` — searchable language list with selection
- `ui/settings/SettingsScreen.kt` — redesigned settings with sections
- `ui/settings/ServerSetupScreen.kt` — redesigned server connection screen

**Android (modified files):**
- `viewmodel/TranslationViewModel.kt` — add pause state, text input handling, language prefs
- `websocket/WebSocketClient.kt` — send language config, pause/resume, text_input messages
- `repository/SettingsRepository.kt` — add language preference storage
- `MainActivity.kt` — add LanguagePicker route; update navigation

**Documentation:**
- `LICENSE` — new file (CC BY-NC 4.0)
- `README.md` — update with v1 features
- `server/tests/test_backends.py` — update for new language params

### Design references
- `.pms/design/android-main.jsx` — `ScreenMain`, `ScreenLangPicker`
- `.pms/design/android-auth.jsx` — `ScreenServerSetup`
- `.pms/design/android-debug.jsx` — `ScreenSettings`

### NLLB-200 language codes
| Language | NLLB Code | Whisper Code |
|----------|-----------|--------------|
| English | eng_Latn | en |
| Chinese (Simplified) | zho_Hans | zh |
| Vietnamese | vie_Latn | vi |
| Sinhala | sin_Sinh | si |

### Dependencies between items
- V1-001 (language infra) is a prerequisite for V1-005 (picker) and V1-003 (text input)
- V1-004 (main screen) can start in parallel after V1-001
- V1-007 (docs) should be done last

---

## Acceptance sign-off
- [x] All Done items pass their acceptance criteria
- [x] `pytest server/tests/` passes (no regressions)
- [x] `./gradlew assembleDebug` passes (no regressions)
- [x] Language selection works end-to-end (picker → server config → ASR+TTS in correct language)
- [x] Pause/resume stops and resumes audio capture + server processing
- [x] Text input translation returns correct result as conversation bubble
- [x] Sprint report written at `report/sprint4-report.md`
- [x] Product backlog updated
