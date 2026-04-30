# Integration Test Guide

Manual end-to-end test procedure for LanguagePartner. This guide verifies the complete system: Python server + Android client.

---

## Prerequisites

- Python 3.10+ with `pip`
- Android device (API 26+) or emulator
- ADB installed (`adb` in PATH)
- Network: server and Android device on same Wi-Fi, or use USB tethering

---

## Step 1: Start the Server

### 1.1 Install server dependencies

```bash
cd server
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

### 1.2 Start the server

```bash
python server.py --port 8080 --model base --device cpu
```

**Expected output:**
```
2026-04-30 12:00:00 INFO    server — Starting server: port=8080 model=base device=cpu
2026-04-30 12:00:00 INFO    server — Loading WhisperBackend (model=base, device=cpu) …
2026-04-30 12:00:00 INFO    server — Loading TranslationBackend …
Models loaded. Listening on ws://0.0.0.0:8080
```

**Note:** First run will download models (~200MB for Whisper base + ~300MB for NLLB-600M). This may take 5-10 minutes.

### 1.3 Find your server IP

On macOS/Linux:
```bash
ipconfig getifaddr en0  # Wi-Fi on macOS
# or
hostname -I | awk '{print $1}'  # Linux
```

On Windows:
```cmd
ipconfig | findstr /i "IPv4"
```

Record this IP (e.g., `192.168.1.100`).

---

## Step 2: Build and Install the Android APK

### 2.1 Navigate to project root

```bash
cd /path/to/LanguagePartner  # project root
```

### 2.2 Build the debug APK

```bash
./gradlew assembleDebug
```

**Expected output:**
```
BUILD SUCCESSFUL in 30s
35 actionable tasks: 35 executed
```

APK location: `android/app/build/outputs/apk/debug/app-debug.apk`

### 2.3 Connect Android device

Enable USB debugging on your Android device:
1. Settings → About Phone → tap "Build Number" 7 times
2. Settings → Developer Options → enable "USB Debugging"
3. Connect via USB

Verify connection:
```bash
adb devices
```

**Expected output:**
```
List of devices attached
ABC123XYZ    device
```

### 2.4 Install the APK

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

**Expected output:**
```
Success
```

---

## Step 3: Connect to Server from App

### 3.1 Launch the app

Tap the LanguagePartner icon on your Android device.

### 3.2 Configure server address

1. On first launch, you'll see the Settings screen
2. Enter your server IP and port: `192.168.1.100:8080`
3. Tap **Save**

**Validation:**
- If IP format is invalid, you'll see "Invalid IP:port format" in red
- On success, you'll navigate to the main screen

### 3.3 Verify connection

On the main screen, check the connection status chip:
- **Grey** → Disconnected
- **Yellow** → Connecting
- **Green** → Connected
- **Red** → Error

**Expected:** Status turns green within 2 seconds.

**Server log verification:**
```
INFO    app — New WebSocket connection from 192.168.1.50:54321
INFO    app — Client configured: sample_rate=16000, mode=speak
```

---

## Step 4: Speak Chinese and Verify English Result

### 4.1 Set mode to SPEAK

Ensure the mode toggle shows **SPEAK** (not READ).

### 4.2 Grant microphone permission

If prompted, tap "Allow" for microphone access.

### 4.3 Speak a Chinese phrase

Say clearly: **"你好"** (nǐ hǎo — hello)

**What happens:**
1. Blue mic indicator pulses while you speak
2. After you stop, wait ~2-5 seconds for processing
3. Result appears in the utterance list

### 4.4 Verify the result

**Expected UI display:**
- Source text (grey, small): `你好`
- Translated text (black, larger): `Hello`

**Server log verification:**
```
INFO    app — Sent translation: src='你好' tgt='Hello'
INFO    app — Utterance processed: src='你好' tgt='Hello'
```

### 4.5 Test additional phrases

| Chinese | Expected English |
|---------|------------------|
| 谢谢 | Thank you |
| 再见 | Goodbye |
| 我饿了 | I'm hungry |
| 这是什么 | What is this |

---

## Step 5: Test Mode Toggle

### 5.1 Toggle to READ mode

Tap the **SPEAK** chip to switch to **READ**.

**Server log verification:**
```
INFO    app — Client configured: sample_rate=16000, mode=read
```

### 5.2 Speak another phrase

Say: **"谢谢"**

**Expected behavior:**
- Translation appears: `谢谢` → `Thank you`
- **No audio playback** (TTS only active in SPEAK mode)

### 5.3 Toggle back to SPEAK mode

Tap **READ** to switch back to **SPEAK**.

**Expected behavior:**
- Next translation triggers English TTS playback
- You should hear "Thank you" spoken

---

## Step 6: Test Error Handling

### 6.1 Server unreachable

1. Stop the server: `Ctrl+C` in terminal
2. In the app, connection status turns **Red** within 5 seconds
3. UI shows error message or retry prompt

**Recovery:**
1. Restart server
2. App should auto-reconnect within 10 seconds (exponential backoff: 1s, 2s, 4s, 8s, 30s)

### 6.2 Invalid server address

1. Go to Settings
2. Enter invalid IP: `999.999.999.999:8080`
3. Tap Save

**Expected:** Inline error "Invalid IP:port format"

### 6.3 Network disconnection

1. While connected, turn off Wi-Fi on Android device
2. Status changes to **Red** (ERROR)
3. Re-enable Wi-Fi
4. App auto-reconnects

### 6.4 Empty utterance (silence)

1. Stay silent for 5 seconds without speaking
2. **Expected:** No translation card appears (server silently skips empty transcripts)

**Server log:**
```
DEBUG   app — ASR returned empty string; skipping translation.
```

---

## Pass/Fail Criteria

| Test | Pass Condition |
|------|----------------|
| Server starts | No crash, logs "Listening on ws://..." |
| APK builds | `BUILD SUCCESSFUL` |
| APK installs | `adb install` returns "Success" |
| Connection | Status chip turns green |
| Chinese → English | "你好" → "Hello" appears |
| Mode toggle (SPEAK) | TTS audio plays |
| Mode toggle (READ) | No TTS audio |
| Server disconnect | Status turns red |
| Auto-reconnect | Status returns green after server restart |
| Empty utterance | No spurious translation cards |

---

## Troubleshooting

### "Connection refused" on Android
- Server not running? Check terminal
- Wrong IP? Verify with `ipconfig` / `hostname`
- Firewall blocking? Allow port 8080

### Models not downloading
- Check internet connection
- Try `--model tiny` for smaller download
- HuggingFace may be rate-limiting; wait 10 minutes

### No audio from TTS
- Check device volume
- Ensure mode is SPEAK (not READ)
- First TTS init may take 1-2 seconds

### App crashes on launch
- Check `adb logcat | grep languagepartner`
- Minimum Android version: 8.0 (API 26)

---

## Smoke Test (Automated)

For quick server verification without Android device:

```bash
cd server
python tests/test_smoke.py
```

See `server/tests/test_smoke.py` for implementation details.

---

**Document version:** 1.0  
**Last updated:** 2026-04-30
