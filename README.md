# LanguagePartner

Real-time voice translation system: Android client streams microphone audio to a self-hosted Python server for speech recognition (Whisper) and translation (NLLB), returning results as JSON over WebSocket.

**MVP Features:**
- Chinese (Simplified) → English translation
- Speak mode (with TTS) and Read mode (text only)
- Self-hosted server (no cloud dependencies)
- Offline-capable after initial model download

---

## Prerequisites

### Server
- Python 3.10+ (check: `python3 --version`)
- pip (Python package manager)
- 2GB+ disk space for models
- 4GB+ RAM recommended
- (Optional) NVIDIA GPU with CUDA for faster inference
- (Optional) Apple Silicon Mac (MPS acceleration supported)

### Android Client
- Android device or emulator with Android 8.0+ (API 26+)
- ADB (Android Debug Bridge) for APK installation
- Microphone permission required
- Wi-Fi or USB tethering to reach server

### Development (optional)
- Git for version control
- Android Studio (for APK building)
- Node.js + websockets package (for smoke tests)

---

## Quick Start

### 1. Install and Start the Server

```bash
# Clone or navigate to project
cd LanguagePartner/server

# Create virtual environment
python -m venv .venv

# Activate virtual environment
# macOS/Linux:
source .venv/bin/activate
# Windows:
.venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Start the server
python server.py --port 8080 --model base --device auto
```

**First run:** Models will download automatically (~500MB total). This takes 5-10 minutes.

**Expected output:**
```
2026-04-30 12:00:00 INFO    server — Starting server: port=8080 model=base device=cpu
2026-04-30 12:00:00 INFO    server — Loading WhisperBackend (model=base, device=cpu) …
2026-04-30 12:00:00 INFO    server — Loading TranslationBackend …
Models loaded. Listening on ws://0.0.0.0:8080
```

**Find your server IP:**
```bash
# macOS:
ipconfig getifaddr en0

# Linux:
hostname -I | awk '{print $1}'

# Windows:
ipconfig | findstr /i "IPv4"
```

Example IP: `192.168.1.100`

---

### 2. Build and Install the Android APK

#### Option A: Pre-built APK (if available)
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

#### Option B: Build from source

```bash
# Navigate to project root
cd LanguagePartner

# Build debug APK
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug

# Install on connected device
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

**Build requirements:**
- Android SDK (install via Android Studio or cmdline-tools)
- Accept licenses: `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses`
- Required SDK packages: `platforms;android-34`, `build-tools;34.0.0`

**Expected output:**
```
BUILD SUCCESSFUL in 30s
35 actionable tasks: 35 executed
Success
```

---

### 3. First-Run Configuration

1. **Launch the app** on your Android device

2. **Enter server address:**
   - Format: `<server-ip>:<port>`
   - Example: `192.168.1.100:8080`
   - Tap **Save**

3. **Verify connection:**
   - Status chip turns **green** = Connected
   - Status chip turns **red** = Connection failed

4. **Grant microphone permission** when prompted

5. **Test translation:**
   - Ensure mode is set to **SPEAK**
   - Say "你好" (nǐ hǎo)
   - Wait 2-5 seconds
   - Verify result: `你好` → `Hello`

---

## Server CLI Reference

```bash
python server.py [OPTIONS]

Options:
  --port PORT       WebSocket listen port (default: 8080)
  --model MODEL     Whisper model: tiny|base|small|medium|large (default: medium)
  --device DEVICE   Inference device: auto|cpu|cuda|mps (default: auto)
```

**Model size guide:**

| Model | Disk | RAM | Speed | Accuracy |
|-------|------|-----|-------|----------|
| tiny  | 75MB | 500MB | Fastest | Lowest |
| base  | 140MB | 700MB | Fast | Low |
| small | 240MB | 1GB | Medium | Medium |
| medium | 760MB | 2GB | Slow | High |
| large | 1.5GB | 4GB | Slowest | Highest |

**Device selection:**
- `auto` — Detects best available (CUDA → MPS → CPU)
- `cuda` — NVIDIA GPU (requires CUDA toolkit)
- `mps` — Apple Silicon (M1/M2/M3)
- `cpu` — Universal fallback

---

## Android App Usage

### Modes

| Mode | Behavior |
|------|----------|
| **SPEAK** | Shows translation + plays English TTS audio |
| **READ** | Shows translation only (silent) |

### Connection Status

| Color | Status | Action |
|-------|--------|--------|
| Grey | Disconnected | Enter server address in Settings |
| Yellow | Connecting | Wait for connection |
| Green | Connected | Ready to use |
| Red | Error | Check server is running, retry |

### Settings

- **Server Address:** IP:port of your Python server
- Validation: Must match pattern `\d{1,3}(\.\d{1,3}){3}:\d{2,5}`
- Saved locally using Android Jetpack DataStore

---

## Testing

### Smoke Test (Server Only)

Quick verification without Android device:

```bash
cd server
pip install websockets  # Required for smoke test
python tests/test_smoke.py
```

Or with pytest:
```bash
pytest server/tests/test_smoke.py -v
```

**What it tests:**
- Server starts without crashing
- Accepts WebSocket connections
- Receives config + audio frames
- Handles disconnects gracefully
- Runs in MOCK_MODE (no model download)

### Integration Test (Full System)

See [`docs/integration-test.md`](docs/integration-test.md) for complete manual test procedure.

### Unit Tests

```bash
# Run all server tests
pytest server/tests/ -v

# Run specific test file
pytest server/tests/test_backends.py -v
pytest server/tests/test_vad.py -v
```

---

## Architecture

```
┌─────────────────────┐         ┌─────────────────────────────────┐
│  Android Client     │         │  Python Server                  │
│  (Kotlin, Jetpack)  │         │  (FastAPI + WebSocket)          │
├─────────────────────┤         ├─────────────────────────────────┤
│  AudioRecord        │ ──────► │  WebSocket /ws                  │
│  16kHz PCM mono     │         │                                 │
│                     │         │  silero-VAD                     │
│                     │         │  (utterance detection)          │
│                     │         │         │                       │
│                     │         │  Whisper ASR                    │
│                     │         │  (Chinese → text)               │
│                     │         │         │                       │
│                     │         │  NLLB-200                       │
│                     │         │  (translate to English)         │
│                     │         └────────────────┬────────────────┘
│  Display result     │ ◄────────────────────────┘
│  Android TTS        │    JSON: {type, source_text,
│  (Speak mode only)  │          translated_text, utterance_id}
└─────────────────────┘
```

---

## Known Issues

### Model Download Time
**Symptom:** Server hangs on first start for 5-10 minutes.  
**Cause:** Whisper and NLLB models download from HuggingFace.  
**Fix:** Wait for download to complete. Models cached in `~/.cache/huggingface`.

### NLLB License (Non-Commercial)
**Warning:** NLLB-200 is licensed under CC-BY-NC-4.0 (non-commercial only).  
**Impact:** This project is for personal/educational use only.

### MPS (Apple Silicon) Limitations
**Symptom:** Slower inference on M1/M2 vs CUDA.  
**Cause:** MPS backend less optimized than CUDA.  
**Workaround:** Use `--device cpu` for more stable performance.

### Android TTS Language
**Symptom:** TTS sounds unnatural or wrong accent.  
**Cause:** Device TTS engine may not have English voice installed.  
**Fix:** Settings → Accessibility → Text-to-speech → Install English voice.

### Connection Timeouts
**Symptom:** App shows "Error" status intermittently.  
**Cause:** Server on different subnet, firewall blocking port 8080.  
**Fix:** Ensure same Wi-Fi network, allow port 8080 in firewall.

### ADB Not Recognizing Device
**Symptom:** `adb devices` shows "unauthorized" or empty.  
**Fix:**
1. Enable USB Debugging on Android
2. Accept RSA key popup on device
3. Try `adb kill-server && adb start-server`

---

## Troubleshooting

### Server won't start
```bash
# Check Python version
python --version  # Must be 3.10+

# Check dependencies
pip install -r requirements.txt

# Check port is free
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows
```

### APK build fails
```bash
# Clean build
./gradlew clean assembleDebug

# Check SDK installation
echo $ANDROID_HOME
ls $ANDROID_HOME/platforms
```

### No translation results
1. Check server logs for errors
2. Verify microphone permission granted
3. Speak louder/closer to device
4. Check VAD threshold (may miss quiet speech)

### TTS not playing
1. Check device volume
2. Ensure mode is SPEAK (not READ)
3. Check Android TTS engine settings

---

## Development

### Project Structure
```
LanguagePartner/
├── server/
│   ├── app.py              # FastAPI WebSocket handler
│   ├── server.py           # CLI entrypoint
│   ├── vad.py              # VAD utterance detection
│   ├── backend/
│   │   ├── base.py         # InferenceBackend ABC
│   │   ├── whisper_backend.py
│   │   └── translation_backend.py
│   ├── tests/
│   │   ├── test_backends.py
│   │   ├── test_vad.py
│   │   └── test_smoke.py
│   └── requirements.txt
├── android/
│   ├── app/
│   │   ├── src/main/java/com/languagepartner/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── audio/AudioCapture.kt
│   │   │   ├── websocket/WebSocketClient.kt
│   │   │   ├── viewmodel/TranslationViewModel.kt
│   │   │   ├── repository/SettingsRepository.kt
│   │   │   └── ui/...
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── docs/
│   └── integration-test.md
└── README.md
```

### Running from IDE

**Server (PyCharm/VSCode):**
- Working directory: `server/`
- Run script: `server.py`
- Environment: `MOCK_MODE=1` for testing

**Android (Android Studio):**
- Open `android/` as project
- Sync Gradle files
- Run on device/emulator

---

## License

**Personal use only.**

- Whisper: MIT license
- silero-VAD: Apache 2.0
- NLLB-200: CC-BY-NC-4.0 (non-commercial)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.1.0 | 2026-04-30 | MVP: Chinese→English, Speak/Read modes, self-hosted server |

---

## Support

For issues or questions:
1. Check [Known Issues](#known-issues) above
2. Review server logs for error messages
3. Run smoke test: `python tests/test_smoke.py`
4. See integration test guide: `docs/integration-test.md`
