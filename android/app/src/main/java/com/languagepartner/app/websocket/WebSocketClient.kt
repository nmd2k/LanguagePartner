package com.languagepartner.app.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

private const val TAG = "WebSocketClient"

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class TranslationResult(
    val utteranceId: String,
    val sourceText: String,
    val translatedText: String
)

sealed class WebSocketEvent {
    data class Translation(val result: TranslationResult) : WebSocketEvent()
    data class Error(val code: String, val message: String) : WebSocketEvent()
}

class WebSocketClient(private val okHttpClient: OkHttpClient) {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _translationResults = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 64)
    val translationResults: SharedFlow<WebSocketEvent> = _translationResults.asSharedFlow()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var currentMode: String = "speak"
    private var currentSourceLang: String = "en"
    private var currentTargetLang: String = "zh"

    fun connect(
        address: String,
        mode: String = "speak",
        sourceLang: String = "en",
        targetLang: String = "zh"
    ) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING ||
            _connectionStatus.value == ConnectionStatus.CONNECTED) {
            Log.d(TAG, "Already connecting or connected, skipping")
            return
        }
        currentMode = mode
        currentSourceLang = sourceLang
        currentTargetLang = targetLang
        _connectionStatus.value = ConnectionStatus.CONNECTING
        val url = "ws://$address/ws"
        Log.d(TAG, "Connecting to $url with source=$sourceLang target=$targetLang")
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, createListener(mode, sourceLang, targetLang))
    }

    fun sendAudioChunk(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        ws.send(pcmBytes.toByteString())
    }

    fun sendConfigMessage(mode: String) {
        sendConfigFull(mode, currentSourceLang, currentTargetLang)
    }

    fun sendConfigFull(mode: String, sourceLang: String, targetLang: String) {
        val ws = webSocket ?: return
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        currentSourceLang = sourceLang
        currentTargetLang = targetLang
        val configJson = gson.toJson(
            mapOf(
                "type" to "config",
                "sample_rate" to 16000,
                "mode" to mode,
                "source_lang" to sourceLang,
                "target_lang" to targetLang
            )
        )
        ws.send(configJson)
        Log.d(TAG, "Sent config: $configJson")
    }

    fun sendPause() {
        val ws = webSocket ?: return
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        val msg = gson.toJson(mapOf("type" to "pause"))
        ws.send(msg)
        Log.d(TAG, "Sent pause")
    }

    fun sendResume() {
        val ws = webSocket ?: return
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        val msg = gson.toJson(mapOf("type" to "resume"))
        ws.send(msg)
        Log.d(TAG, "Sent resume")
    }

    fun sendTextInput(text: String, sourceLang: String, targetLang: String) {
        val ws = webSocket ?: return
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        val msg = gson.toJson(
            mapOf(
                "type" to "text_input",
                "text" to text,
                "source_lang" to sourceLang,
                "target_lang" to targetLang
            )
        )
        ws.send(msg)
        Log.d(TAG, "Sent text_input: $text")
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private fun createListener(
        mode: String,
        sourceLang: String,
        targetLang: String
    ): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")
                _connectionStatus.value = ConnectionStatus.CONNECTED
                val configJson = gson.toJson(
                    mapOf(
                        "type" to "config",
                        "sample_rate" to 16000,
                        "mode" to mode,
                        "source_lang" to sourceLang,
                        "target_lang" to targetLang
                    )
                )
                webSocket.send(configJson)
                Log.d(TAG, "Sent config: $configJson")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received text message: $text")
                try {
                    val json = JsonParser.parseString(text).asJsonObject
                    when (json.get("type")?.asString) {
                        "translation" -> {
                            val result = TranslationResult(
                                utteranceId = json.get("utterance_id")?.asString ?: "",
                                sourceText = json.get("source_text")?.asString ?: "",
                                translatedText = json.get("translated_text")?.asString ?: ""
                            )
                            _translationResults.tryEmit(WebSocketEvent.Translation(result))
                        }
                        "error" -> {
                            val code = json.get("code")?.asString ?: "UNKNOWN"
                            val message = json.get("message")?.asString ?: ""
                            Log.e(TAG, "Server error [$code]: $message")
                            _translationResults.tryEmit(WebSocketEvent.Error(code, message))
                        }
                        else -> {
                            Log.w(TAG, "Unknown message type: $text")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message: $text", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received binary message (${bytes.size} bytes)")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _connectionStatus.value = ConnectionStatus.ERROR
                _translationResults.tryEmit(
                    WebSocketEvent.Error(
                        "SERVER_UNREACHABLE",
                        t.message ?: "Unknown connection error"
                    )
                )
            }
        }
    }
}
