package com.languagepartner.app.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.languagepartner.app.audio.AudioCapture
import com.languagepartner.app.repository.SettingsRepository
import com.languagepartner.app.websocket.ConnectionStatus
import com.languagepartner.app.websocket.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "TranslationViewModel"

enum class TranslationMode { SPEAK, READ }

data class Utterance(
    val id: String,
    val sourceText: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // no timeout for streaming
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val webSocketClient = WebSocketClient(okHttpClient)
    private val settingsRepository = SettingsRepository(application)

    // Connection status delegated from WebSocketClient
    val connectionStatus: StateFlow<ConnectionStatus> = webSocketClient.connectionStatus

    // Utterance list — newest first
    private val _utterances = MutableStateFlow<List<Utterance>>(emptyList())
    val utterances: StateFlow<List<Utterance>> = _utterances.asStateFlow()

    // Mode state
    private val _mode = MutableStateFlow(TranslationMode.SPEAK)
    val mode: StateFlow<TranslationMode> = _mode.asStateFlow()

    // Server address from DataStore
    val serverAddress: StateFlow<String> = settingsRepository.serverAddress
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Audio capture
    private var audioCapture: AudioCapture? = null

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Reconnect state
    private var retryJob: Job? = null
    private val backoffMs = listOf(1000L, 2000L, 4000L, 8000L, 30000L)
    private var userDisconnected = false

    init {
        initTts()
        collectTranslationResults()
        watchConnectionStatus()
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(TAG, "TTS: English language not supported — disabling Speak mode")
                    ttsReady = false
                    if (_mode.value == TranslationMode.SPEAK) {
                        _mode.value = TranslationMode.READ
                    }
                } else {
                    ttsReady = true
                    Log.d(TAG, "TTS initialized with English locale")
                }
            } else {
                Log.e(TAG, "TTS init failed with status: $status — disabling Speak mode")
                ttsReady = false
                if (_mode.value == TranslationMode.SPEAK) {
                    _mode.value = TranslationMode.READ
                }
            }
        }
    }

    private fun collectTranslationResults() {
        viewModelScope.launch {
            webSocketClient.translationResults.collect { result ->
                val utterance = Utterance(
                    id = result.utteranceId,
                    sourceText = result.sourceText,
                    translatedText = result.translatedText
                )
                // Prepend newest first
                _utterances.value = listOf(utterance) + _utterances.value
                // Speak in SPEAK mode
                if (_mode.value == TranslationMode.SPEAK && ttsReady) {
                    tts?.speak(result.translatedText, TextToSpeech.QUEUE_ADD, null, result.utteranceId)
                }
            }
        }
    }

    private fun watchConnectionStatus() {
        viewModelScope.launch {
            connectionStatus.collect { status ->
                when (status) {
                    ConnectionStatus.CONNECTED -> {
                        Log.d(TAG, "Connected — starting audio capture")
                        retryJob?.cancel()
                        retryJob = null
                        startAudioCapture()
                    }
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                        Log.d(TAG, "Disconnected/Error — stopping audio capture")
                        stopAudioCapture()
                        if (!userDisconnected) {
                            scheduleReconnect()
                        }
                    }
                    ConnectionStatus.CONNECTING -> {
                        // No action needed
                    }
                }
            }
        }
    }

    private fun scheduleReconnect() {
        val address = serverAddress.value
        if (address.isEmpty()) {
            Log.d(TAG, "No server address configured; skipping reconnect")
            return
        }
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            var attempt = 0
            while (true) {
                val delayMs = backoffMs.getOrElse(attempt) { backoffMs.last() }
                Log.d(TAG, "Reconnect attempt ${attempt + 1} in ${delayMs}ms")
                delay(delayMs)
                if (userDisconnected) break
                Log.d(TAG, "Attempting reconnect to $address")
                webSocketClient.connect(address, _mode.value.toModeString())
                // Wait for status change
                delay(5000L)
                if (connectionStatus.value == ConnectionStatus.CONNECTED) break
                attempt++
            }
        }
    }

    private fun startAudioCapture() {
        if (audioCapture != null) return
        audioCapture = AudioCapture { pcmBytes ->
            webSocketClient.sendAudioChunk(pcmBytes)
        }
        audioCapture?.start()
    }

    private fun stopAudioCapture() {
        audioCapture?.stop()
        audioCapture = null
    }

    fun connect(address: String) {
        userDisconnected = false
        retryJob?.cancel()
        retryJob = null
        webSocketClient.connect(address, _mode.value.toModeString())
    }

    fun disconnect() {
        userDisconnected = true
        retryJob?.cancel()
        retryJob = null
        stopAudioCapture()
        webSocketClient.disconnect()
    }

    fun toggleMode() {
        _mode.value = when (_mode.value) {
            TranslationMode.SPEAK -> TranslationMode.READ
            TranslationMode.READ -> TranslationMode.SPEAK
        }
        Log.d(TAG, "Mode toggled to ${_mode.value}")
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioCapture()
        webSocketClient.disconnect()
        tts?.stop()
        tts?.shutdown()
        okHttpClient.dispatcher.executorService.shutdown()
    }

    private fun TranslationMode.toModeString() = when (this) {
        TranslationMode.SPEAK -> "speak"
        TranslationMode.READ -> "read"
    }
}
