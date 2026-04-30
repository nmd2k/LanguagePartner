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
import com.languagepartner.app.websocket.WebSocketEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

data class LogEntry(
    val timestamp: String,
    val level: String,
    val message: String
)

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
) {
    companion object {
        val SUPPORTED = listOf(
            Language("en", "English", "English"),
            Language("zh", "Chinese (Simplified)", "普通话"),
            Language("vi", "Vietnamese", "Tiếng Việt"),
            Language("si", "Sinhala", "සිංහල")
        )

        fun fromCode(code: String): Language =
            SUPPORTED.find { it.code == code } ?: SUPPORTED[0]

        fun defaultSource() = SUPPORTED[0]
        fun defaultTarget() = SUPPORTED[1]
    }
}

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val webSocketClient = WebSocketClient(okHttpClient)
    private val settingsRepository = SettingsRepository(application)

    val connectionStatus: StateFlow<ConnectionStatus> = webSocketClient.connectionStatus

    private val _utterances = MutableStateFlow<List<Utterance>>(emptyList())
    val utterances: StateFlow<List<Utterance>> = _utterances.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _logEntries = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry("—", "INFO", "Debug console ready. Connect to a server to receive logs."),
            LogEntry("—", "INFO", "Waiting for server connection...")
        )
    )
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    private val _mode = MutableStateFlow(TranslationMode.SPEAK)
    val mode: StateFlow<TranslationMode> = _mode.asStateFlow()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(Language.defaultSource())
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(Language.defaultTarget())
    val targetLanguage: StateFlow<Language> = _targetLanguage.asStateFlow()

    val serverAddress: StateFlow<String> = settingsRepository.serverAddress
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private var audioCapture: AudioCapture? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var retryJob: Job? = null
    private val backoffMs = listOf(1000L, 2000L, 4000L, 8000L, 30000L)
    private var userDisconnected = false

    init {
        initTts()
        collectTranslationResults()
        watchConnectionStatus()
        loadLanguagePrefs()
    }

    private fun loadLanguagePrefs() {
        viewModelScope.launch {
            settingsRepository.sourceLanguage.collect { code ->
                if (code.isNotEmpty()) {
                    _sourceLanguage.value = Language.fromCode(code)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.targetLanguage.collect { code ->
                if (code.isNotEmpty()) {
                    _targetLanguage.value = Language.fromCode(code)
                }
            }
        }
    }

    private fun ttsLocaleForLanguage(code: String): Locale = when (code) {
        "en" -> Locale.ENGLISH
        "zh" -> Locale.CHINESE
        "vi" -> Locale("vi")
        "si" -> Locale("si")
        else -> Locale.ENGLISH
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val targetLocale = ttsLocaleForLanguage(_targetLanguage.value.code)
                val result = tts?.setLanguage(targetLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(TAG, "TTS: $targetLocale language not supported, falling back to English")
                    val fallback = tts?.setLanguage(Locale.ENGLISH)
                    if (fallback == TextToSpeech.LANG_MISSING_DATA ||
                        fallback == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        ttsReady = false
                        if (_mode.value == TranslationMode.SPEAK) {
                            _mode.value = TranslationMode.READ
                        }
                    } else {
                        ttsReady = true
                    }
                } else {
                    ttsReady = true
                    Log.d(TAG, "TTS initialized with locale $targetLocale")
                }
            } else {
                Log.e(TAG, "TTS init failed: $status")
                ttsReady = false
                if (_mode.value == TranslationMode.SPEAK) {
                    _mode.value = TranslationMode.READ
                }
            }
        }
    }

    private fun updateTtsLocale() {
        val targetLocale = ttsLocaleForLanguage(_targetLanguage.value.code)
        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.e(TAG, "TTS: $targetLocale not supported, falling back to English")
            tts?.setLanguage(Locale.ENGLISH)
        } else {
            Log.d(TAG, "TTS locale updated to $targetLocale")
        }
    }

    private fun collectTranslationResults() {
        viewModelScope.launch {
            webSocketClient.translationResults.collect { event ->
                when (event) {
                    is WebSocketEvent.Translation -> {
                        val result = event.result
                        val utterance = Utterance(
                            id = result.utteranceId,
                            sourceText = result.sourceText,
                            translatedText = result.translatedText
                        )
                        _utterances.value = _utterances.value + listOf(utterance)
                        if (_mode.value == TranslationMode.SPEAK && ttsReady) {
                            tts?.speak(
                                result.translatedText,
                                TextToSpeech.QUEUE_ADD,
                                null,
                                result.utteranceId
                            )
                        }
                    }
                    is WebSocketEvent.Error -> {
                        val errorMessage = formatErrorMessage(event.code, event.message)
                        _errorEvents.tryEmit(errorMessage)
                        _logEntries.value = _logEntries.value + LogEntry(
                            timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                                .format(java.util.Date()),
                            level = "ERROR",
                            message = errorMessage
                        )
                    }
                    is WebSocketEvent.Log -> {
                        _logEntries.value = _logEntries.value + LogEntry(
                            timestamp = event.timestamp,
                            level = event.level,
                            message = event.message
                        )
                    }
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
                        if (!_paused.value) {
                            startAudioCapture()
                        }
                    }
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                        Log.d(TAG, "Disconnected/Error — stopping audio capture")
                        stopAudioCapture()
                        if (!userDisconnected) {
                            scheduleReconnect()
                        }
                    }
                    ConnectionStatus.CONNECTING -> {}
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
                webSocketClient.connect(
                    address,
                    _mode.value.toModeString(),
                    _sourceLanguage.value.code,
                    _targetLanguage.value.code
                )
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
        webSocketClient.connect(
            address,
            _mode.value.toModeString(),
            _sourceLanguage.value.code,
            _targetLanguage.value.code
        )
    }

    fun saveServerAddress(address: String) {
        viewModelScope.launch { settingsRepository.saveServerAddress(address) }
        connect(address)
    }

    fun testConnection(address: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val testClient = WebSocketClient(
                OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
            )
            var completed = false
            val job = launch {
                testClient.connectionStatus.collect { status ->
                    if (!completed) {
                        when (status) {
                            ConnectionStatus.CONNECTED -> {
                                completed = true
                                onResult(true)
                                testClient.disconnect()
                            }
                            ConnectionStatus.ERROR -> {
                                completed = true
                                onResult(false)
                                testClient.disconnect()
                            }
                            else -> {}
                        }
                    }
                }
            }
            testClient.connect(address, "read", _sourceLanguage.value.code, _targetLanguage.value.code)
            delay(6000)
            if (!completed) {
                completed = true
                onResult(false)
                testClient.disconnect()
                job.cancel()
            }
        }
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
        sendConfigIfConnected()
    }

    fun togglePause() {
        _paused.value = !_paused.value
        if (_paused.value) {
            stopAudioCapture()
            if (connectionStatus.value == ConnectionStatus.CONNECTED) {
                webSocketClient.sendPause()
            }
            Log.d(TAG, "Translation paused")
        } else {
            if (connectionStatus.value == ConnectionStatus.CONNECTED) {
                webSocketClient.sendResume()
                startAudioCapture()
            }
            Log.d(TAG, "Translation resumed")
        }
    }

    fun sendTextInput(text: String) {
        if (text.isBlank()) return
        webSocketClient.sendTextInput(
            text,
            _sourceLanguage.value.code,
            _targetLanguage.value.code
        )
    }

    fun setSourceLanguage(code: String) {
        if (code == _targetLanguage.value.code) {
            swapLanguages()
            return
        }
        val lang = Language.fromCode(code)
        _sourceLanguage.value = lang
        viewModelScope.launch { settingsRepository.saveSourceLanguage(code) }
        sendConfigIfConnected()
    }

    fun setTargetLanguage(code: String) {
        if (code == _sourceLanguage.value.code) {
            swapLanguages()
            return
        }
        val lang = Language.fromCode(code)
        _targetLanguage.value = lang
        viewModelScope.launch { settingsRepository.saveTargetLanguage(code) }
        updateTtsLocale()
        sendConfigIfConnected()
    }

    fun swapLanguages() {
        val src = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = src
        viewModelScope.launch {
            settingsRepository.saveSourceLanguage(_sourceLanguage.value.code)
            settingsRepository.saveTargetLanguage(_targetLanguage.value.code)
        }
        updateTtsLocale()
        sendConfigIfConnected()
    }

    private fun sendConfigIfConnected() {
        if (connectionStatus.value == ConnectionStatus.CONNECTED) {
            webSocketClient.sendConfigFull(
                _mode.value.toModeString(),
                _sourceLanguage.value.code,
                _targetLanguage.value.code
            )
        }
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

    private fun formatErrorMessage(code: String, message: String): String {
        return when (code) {
            "SERVER_UNREACHABLE" -> "Server unreachable: $message"
            "ASR_FAILED" -> "Speech recognition failed: $message"
            "MODEL_LOAD_FAIL" -> "Model loading failed: $message"
            else -> "Error: $message"
        }
    }
}
