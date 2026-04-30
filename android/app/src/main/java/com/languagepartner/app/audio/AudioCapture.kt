package com.languagepartner.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "AudioCapture"
private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val CHUNK_SAMPLES = 512
private const val CHUNK_BYTES = CHUNK_SAMPLES * 2 // 16-bit = 2 bytes per sample

/**
 * Captures microphone audio at 16kHz, mono, PCM 16-bit.
 * Reads in 512-sample (1024-byte) chunks and delivers them as little-endian ByteArrays.
 *
 * Caller must ensure RECORD_AUDIO permission is granted before calling [start].
 */
class AudioCapture(private val onChunk: (ByteArray) -> Unit) {

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (captureJob?.isActive == true) {
            Log.w(TAG, "AudioCapture already running")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, CHUNK_BYTES * 4)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            record.release()
            return
        }

        audioRecord = record
        record.startRecording()
        Log.d(TAG, "AudioRecord started (bufferSize=$bufferSize, minBufferSize=$minBufferSize)")

        captureJob = scope.launch {
            val shortBuffer = ShortArray(CHUNK_SAMPLES)
            while (isActive) {
                val read = record.read(shortBuffer, 0, CHUNK_SAMPLES)
                if (read > 0) {
                    val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until read) {
                        byteBuffer.putShort(shortBuffer[i])
                    }
                    onChunk(byteBuffer.array())
                } else if (read < 0) {
                    Log.e(TAG, "AudioRecord.read error: $read")
                    break
                }
            }
            Log.d(TAG, "Audio capture loop ended")
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        audioRecord?.let { record ->
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioRecord", e)
            }
        }
        audioRecord = null
        Log.d(TAG, "AudioCapture stopped")
    }
}
