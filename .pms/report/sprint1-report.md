# Sprint 1 Report

**Sprint:** 1
**Period:** 2026-04-30 → 2026-04-30
**Branch:** `sprint/1`
**Author:** Agent (claude-sonnet-4-6)
**Status:** Final

---

## 1. Execution Plan (Phase 1 — Planning)

**Sprint goal:** Deliver a working Python server that accepts WebSocket audio streams, segments utterances via silero-VAD, transcribes ZH-CN speech with Whisper, translates to EN via NLLB, and returns JSON results. All accessible via a CLI entrypoint.

**Task ordering rationale:**
1. S1-001 first — directory structure unblocks all other tasks.
2. S1-002 second — InferenceBackend ABC must exist before backends are implemented.
3. S1-003 + S1-004 in parallel — WhisperBackend and TranslationBackend are independent.
4. S1-005 — VADProcessor depends on no backend code, implements its own silero-VAD integration.
5. S1-006 — FastAPI handler wires all components; depends on S1-002 through S1-005.
6. S1-007 — CLI entrypoint depends on S1-006 (app.py) and the two backends.
7. S1-008 — tests written last with full knowledge of the interface contracts.

**Risks identified:**
- silero-VAD `torch.hub.load` downloads on first use — mitigated by mocking in tests.
- faster-whisper's `WhisperModel` API differs slightly from openai-whisper — confirmed by reading library docs.
- Test isolation: `sys.modules` pollution between test cases when reloading modules — managed via `importlib.reload()` per test.

---

## 2. Analysis Summary (Phase 2 — Analysis)

**Repository state at sprint start:** Only root-level `CLAUDE.md`, `README.md` (template placeholder), and `.pms/` docs existed. No server or Android code present.

**SRS key points for Sprint 1 (§6.1):**
- FR-S01–FR-S08 define the server-side requirements in full.
- InferenceBackend ABC mandated by FR-S08 and Design Constraint 1.
- WebSocket protocol schema in SRS §2.5 defines exact JSON shapes.
- Audio format: 16-bit signed LE PCM, 16kHz, mono, 512 samples per chunk.

**Design decisions:**
- Used `faster-whisper` (CTranslate2) instead of `openai-whisper` per sprint instructions — 3-4x faster at equal accuracy.
- NLLB language codes: `"zho_Hans"` (Simplified Chinese) and `"eng_Latn"` (English Latin script) — not ISO 639 codes.
- VADProcessor uses a streaming state machine (in_speech flag + silence sample counter) rather than batch `get_speech_timestamps()` to stay compatible with the chunked streaming model.
- WhisperBackend and TranslationBackend each implement the InferenceBackend ABC but only fulfill one of the two methods — the other raises `NotImplementedError`. This keeps the ABC contract while maintaining separation of concerns; `app.py` holds two backend references (`_asr_backend`, `_translation_backend`) typed to `InferenceBackend`.
- WebSocket handler uses `asyncio.run_coroutine_threadsafe` to dispatch VAD callbacks (which fire synchronously in an executor thread) back to the async event loop.

**Design documents created/updated:**
- No SDD was required; design decisions are non-trivial but fully specified in the SRS. Notes captured in this report.

---

## 3. Implementation Summary (Phase 3 — Implementation)

### Completed items

| ID | Task | Key file(s) | Notes |
|----|------|-------------|-------|
| S1-001 | Monorepo structure | `server/`, `android/`, `.gitignore`, `README.md` | Python + Android + model binary ignore rules; README with setup placeholders |
| S1-002 | InferenceBackend ABC | `server/backend/base.py` | Abstract `transcribe(np.ndarray) → str` and `translate(str, str, str) → str` |
| S1-003 | WhisperBackend | `server/backend/whisper_backend.py` | faster-whisper; language="zh" forced; RMS silence heuristic; graceful exception handling |
| S1-004 | TranslationBackend | `server/backend/translation_backend.py` | NLLB-200-distilled-600M; HuggingFace transformers; empty input guard |
| S1-005 | VADProcessor | `server/vad.py` | silero-VAD streaming state machine; 300ms silence threshold; on_utterance callback |
| S1-006 | FastAPI WebSocket handler | `server/app.py` | `/ws` endpoint; config frame; binary PCM loop; VAD→ASR→translate→JSON; error JSON |
| S1-007 | CLI entrypoint | `server/server.py` | argparse --port/--model/--device; CUDA→CPU auto-detect; "Models loaded." print |
| S1-008 | Unit tests | `server/tests/test_backends.py`, `server/tests/test_vad.py` | 16 tests total; all heavy models mocked |

### Not completed (deferred)

None. All 8 sprint tasks are Done.

### Key decisions made

- **RMS silence heuristic in WhisperBackend:** Added a pre-inference check (`rms < 1e-4`) to return `""` immediately for near-silent audio without loading the full Whisper model for inference. This satisfies the acceptance criterion ("handles short noise gracefully") without relying on Whisper's own VAD filter (which is disabled since silero-VAD already handles upstream segmentation).
- **Two separate backend references in app.py:** Rather than a single combined `InferenceBackend`, `app.py` holds `_asr_backend` and `_translation_backend` separately. This cleanly separates ASR from NMT and allows independent swapping of either component.
- **`asyncio.run_coroutine_threadsafe` in VAD callback:** VAD runs in a `run_in_executor` thread; the utterance callback must post results back to the async event loop. `run_coroutine_threadsafe` is the correct cross-thread async dispatch mechanism.
- **Module import strategy for tests:** Tests inject stubs into `sys.modules` before `importlib.reload()` to ensure the module under test picks up the mock dependencies without needing an installed package.

---

## 4. Testing & Evaluation (Phase 4)

### Test run summary

| Suite | Total | Passed | Failed | Skipped |
|-------|-------|--------|--------|---------|
| Unit (backends) | 10 | 10 | 0 | 0 |
| Unit (VAD) | 6 | 6 | 0 | 0 |
| **Total** | **16** | **16** | **0** | **0** |

Command: `python3 -m pytest server/tests/ -v`

### New tests added

| Test file | Coverage area |
|-----------|--------------|
| `server/tests/test_backends.py` | WhisperBackend (5 tests): empty audio → "", speech → non-empty, exception → "", type safety; TranslationBackend (5 tests): empty input → "", whitespace → "", 你好→hello, type safety, exception → "" |
| `server/tests/test_vad.py` | VADProcessor (6 tests): utterance detected after speech+silence, no fire before threshold, no fire without speech, float32 array type check, empty chunk safety, reset clears state |

### Failures and resolution

| Failure | Classification | Resolution |
|---------|---------------|------------|
| `ModuleNotFoundError: No module named 'server.backend'` | Minor | Tests imported `server.backend.X` instead of `backend.X`; fixed import paths in test files after diagnosing pytest path resolution |
| `TranslationBackend.test_translate_known_pair_hello` FAILED: stub tokenizer imported `torch` at call time | Minor | Removed `import torch` from stub `_Tokenizer.__call__`; replaced with `_FakeTensor` stub that avoids real torch dependency |

Both were fixed in-sprint. No items deferred.

---

## 5. Documentation Updates (Phase 5)

- [x] `backlog/sprint1.md` — all 8 items moved to Done; acceptance sign-off checked
- [x] `docs/product_backlog.md` — PB-001–008 marked Done; revision history updated
- [x] API docs — no interface changes beyond what SRS §2.5 already specifies; no update needed
- [x] This report complete

---

## 6. Product Backlog Changes

### Items closed this sprint

| ID | Title |
|----|-------|
| PB-001 | Monorepo project structure |
| PB-002 | Python server scaffold — FastAPI app + WebSocket endpoint `/ws` |
| PB-003 | InferenceBackend abstract base class |
| PB-004 | WhisperBackend — Whisper medium ASR integration |
| PB-005 | TranslationBackend — NLLB-600M zh→en integration |
| PB-006 | silero-VAD utterance segmentation pipeline |
| PB-007 | CLI entrypoint with argparse |
| PB-008 | Server unit tests |

### New items added to backlog

None. No scope creep discovered during implementation; all work stayed within the defined sprint boundary.

---

## 7. Lessons Learned & Recommendations for Next Sprint

**What went well:**
- Mocking strategy using `sys.modules` + `importlib.reload()` works cleanly without needing a separate test environment with models installed.
- The InferenceBackend ABC constraint cleanly separated app.py from model concerns — no direct model calls leaked into the handler.
- VAD state machine approach (streaming, chunk-by-chunk) is the right design for a WebSocket streaming server vs. batch processing.

**Harder than expected:**
- pytest module resolution when running from the project root with `server/` on the Python path: the `tests/__init__.py` caused pytest to treat `tests` as a package under the `server` package, breaking `import server.backend.X`. Fixed by using `import backend.X` directly (relying on `pythonpath = server` in `pytest.ini`).
- The NLLB tokenizer stub needed to be completely torch-free; the initial stub used `torch.zeros()` inside the stub's `__call__`, which failed when `torch` was not installed.

**Recommendations for Sprint 2 (Android):**
- Consider adding an integration smoke test (separate from unit tests) that starts the FastAPI app in-process via `TestClient` or `starlette.testclient` and sends mock WebSocket frames — would give higher confidence in the full pipeline without real models.
- Android project should mirror the server structure: separate audio capture, WebSocket client, and display layers — matching the server's InferenceBackend separation principle.

---

## 8. Next Sprint Preview

| ID | Title | Rationale |
|----|-------|-----------|
| PB-009 | Android project setup (Kotlin, Compose, min SDK 26, Gradle) | Foundation for all Android work; must come first |
| PB-010 | Settings screen — IP:port input with DataStore persistence | Required before WebSocket client can connect anywhere |
| PB-011 | WebSocket client — OkHttp WebSocket, lifecycle-aware connection management | Core communication layer |
| PB-012 | AudioRecord integration — 16kHz mono PCM 16-bit continuous capture | Supplies audio to the streaming pipeline |
| PB-013 | Audio streaming — chunk PCM to server via binary WebSocket frames | Completes the client→server pipeline |
| PB-014 | Translation display — card list (ZH source + EN translation per utterance) | Visible output; core user-facing feature |
| PB-015 | Android TTS integration (Speak mode) | Completes Speak mode feature |
| PB-016 | Connection status indicator + exponential backoff auto-reconnect | Resilience and UX polish |
