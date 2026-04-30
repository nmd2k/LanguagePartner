# Sprint 3 Backlog — Integration & Polish

**Sprint:** 3
**Goal:** Harden the full system — wire mode toggle through to server, add user-visible error handling, add MPS device support, structured server logging, and write a setup guide that lets a non-developer get the app running end-to-end.
**Start date:** 2026-04-30
**End date:** 2026-05-14
**Branch:** `sprint/3`

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
| S3-001 | PB-018 | Read/Speak mode toggle — send updated config message to server on switch | 2026-04-30 | pytest + `./gradlew assembleDebug` |
| S3-002 | PB-019 | User-visible error handling — server unreachable, ASR fail, model load fail, empty transcript | 2026-04-30 | `./gradlew assembleDebug` |
| S3-003 | PB-020 | Server: MPS device support (Apple Silicon) — CUDA → MPS → CPU fallback chain | 2026-04-30 | pytest + code review |
| S3-004 | PB-022 | Server structured logging — utterance ID, ASR latency, translation latency, total | 2026-04-30 | pytest + code review |
| S3-005 | PB-017 | End-to-end integration test guide + server smoke test script | 2026-04-30 | docs created + smoke test written |
| S3-006 | PB-021 | Setup documentation — README complete with server install + APK sideload | 2026-04-30 | README.md complete |

---

## Not Done (deferred)

| ID | Backlog ref | Task | Reason | Moved to sprint |
|----|-------------|------|--------|-----------------|

---

## Sprint Notes

### Carryover from Sprint 2
- Mode toggle UI exists in Android but mode change does NOT yet send a new config frame to server — fix in S3-001
- Server `type=error` messages received by Android but not displayed — fix in S3-002
- `TranslationViewModel.toggleMode()` exists; needs to call `webSocketClient.sendConfigMessage(mode)`

### Key files to modify
**Server:**
- `server/app.py` — accept config message mid-session (currently only parsed on first message); add timing + logging; add MPS device detection
- `server/server.py` — CUDA→MPS→CPU fallback chain

**Android:**
- `android/app/src/main/java/com/languagepartner/app/websocket/WebSocketClient.kt` — add `sendConfigMessage(mode: String)` method
- `android/app/src/main/java/com/languagepartner/app/viewmodel/TranslationViewModel.kt` — call `sendConfigMessage()` on toggle; collect error results for Snackbar
- `android/app/src/main/java/com/languagepartner/app/ui/main/MainScreen.kt` — show Snackbar on error

**New files:**
- `server/tests/test_smoke.py`
- `docs/integration-test.md`
- Update `README.md`

---

## Acceptance sign-off
- [ ] All Done items pass their acceptance criteria
- [ ] `pytest server/tests/` still passes (no regressions from server changes)
- [ ] `./gradlew assembleDebug` still passes (no regressions from Android changes)
- [ ] `test_smoke.py` passes
- [ ] Sprint report written at `report/sprint3-report.md`
- [ ] Product backlog updated (PB-017–022 marked Done)
- [ ] All MVP backlog items (PB-001–022) are Done → MVP complete
