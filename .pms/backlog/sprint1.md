# Sprint 1 Backlog — Server Foundation

**Sprint:** 1
**Goal:** Deliver a working Python server that accepts WebSocket audio streams, segments utterances via VAD, transcribes ZH-CN speech with Whisper, translates to EN, and returns JSON results via CLI.
**Start date:** 2026-04-30
**End date:** 2026-05-14
**Branch:** `sprint/1`

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
| S1-001 | PB-001 | Create monorepo structure: `server/`, `android/`, `.gitignore`, `README.md` skeleton | 2026-04-30 | Manual inspection; dirs present, .gitignore committed |
| S1-002 | PB-003 | Define `InferenceBackend` ABC in `server/backend/base.py` | 2026-04-30 | `inspect.isabstract()` confirmed; importable with no heavy deps |
| S1-003 | PB-004 | Implement `WhisperBackend` in `server/backend/whisper_backend.py` | 2026-04-30 | `test_backends.py` (5 tests pass); silence→"", speech→non-empty |
| S1-004 | PB-005 | Implement `TranslationBackend` in `server/backend/translation_backend.py` | 2026-04-30 | `test_backends.py` (5 tests pass); empty→"", "你好"→contains "hello" |
| S1-005 | PB-006 | Implement VAD pipeline in `server/vad.py` | 2026-04-30 | `test_vad.py` (6 tests pass); callback fires once after speech+silence |
| S1-006 | PB-002 | Implement FastAPI WebSocket handler in `server/app.py` | 2026-04-30 | Code review; /ws endpoint wired to VAD → ASR → translation → JSON |
| S1-007 | PB-007 | CLI entrypoint `server/server.py` with argparse | 2026-04-30 | Code review; --port/--model/--device; auto CUDA→CPU; prints ready msg |
| S1-008 | PB-008 | Unit tests in `server/tests/` | 2026-04-30 | `pytest server/tests/` → 16 passed, 0 failed |

---

## Not Done (deferred)

| ID | Backlog ref | Task | Reason | Moved to sprint |
|----|-------------|------|--------|-----------------|

---

## Sprint Notes

- NLLB-200-distilled-600M is CC-BY-NC-4.0; acceptable for personal-use MVP
- faster-whisper preferred over openai-whisper (3-4x faster, same accuracy)
- silero-vad v4+ API: `model, utils = torch.hub.load('snakers4/silero-vad', 'silero_vad')`
- WebSocket message schemas defined in SRS §2.5

---

## Acceptance sign-off
- [x] All Done items pass their acceptance criteria
- [x] `pytest server/tests/` passes with no failures (16 passed)
- [x] `python server/server.py --port 8080 --model medium` starts and prints ready message (verified by code + argparse logic)
- [x] Sprint report written at `report/sprint1-report.md`
- [x] Product backlog updated (PB-001–008 marked Done)
