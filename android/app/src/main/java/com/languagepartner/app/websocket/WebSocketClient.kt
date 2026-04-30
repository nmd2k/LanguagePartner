package com.languagepartner.app.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
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

class WebSocketClient(private val okHttpClient: OkHttpClient) {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _translationResults = MutableSharedFlow<TranslationResult>(extraBufferCapacity = 64)
    val translationResults: SharedFlow<TranslationResult> = _translationResults.asSharedFlow()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var currentMode: String = "speak"

    fun connect(address: String, mode: String = "speak") {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING ||
            _connectionStatus.value == ConnectionStatus.CONNECTED) {
            Log.d(TAG, "Already connecting or connected, skipping")
            return
        }
        currentMode = mode
        _connectionStatus.value = ConnectionStatus.CONNECTING
        val url = "ws://$address/ws"
        Log.d(TAG, "Connecting to $url")
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, createListener(mode))
    }

    fun sendAudioChunk(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        ws.send(pcmBytes.toByteString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private fun createListener(mode: String): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")
                _connectionStatus.value = ConnectionStatus.CONNECTED
                val configJson = gson.toJson(
                    mapOf(
                        "type" to "config",
                        "sample_rate" to 16000,
                        "mode" to mode
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
                            _translationResults.tryEmit(result)
                        }
                        "error" -> {
                            val code = json.get("code")?.asString ?: "UNKNOWN"
                            val message = json.get("message")?.asString ?: ""
                            Log.e(TAG, "Server error [$code]: $message")
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
                Log.e(TAG, "WebSocket failure", t)
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }
}
