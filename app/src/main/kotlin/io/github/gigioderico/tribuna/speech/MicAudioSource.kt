package io.github.gigioderico.tribuna.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.core.content.ContextCompat

/**
 * Microfone por `AudioRecord`, 16 kHz, mono, PCM 16 bits (TRD Motor de voz Ã‚Â§Captura e sessÃƒÂµes; spec 0003, D15).
 * SÃƒÂ³ lÃƒÂª para o reconhecedor; nada ÃƒÂ© gravado (CLAUDE.md, regra 3). Validado no aparelho pela 0005.
 */
class MicAudioSource(
    private val context: Context,
) : AudioSource {
    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission") // a permissÃƒÂ£o ÃƒÂ© conferida logo antes
    override fun open() {
        if (!hasPermission()) throw AudioSourceException(EngineError.PermissionRevoked, "sem permissÃƒÂ£o de microfone")
        val minBytes = AudioRecord.getMinBufferSize(VoskEngine.SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBytes <= 0) throw AudioSourceException(EngineError.MicUnavailable, "AudioRecord.getMinBufferSize=$minBytes")
        val created =
            try {
                AudioRecord(VOICE_RECOGNITION, VoskEngine.SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBytes, BUFFER_BYTES))
            } catch (e: SecurityException) {
                throw AudioSourceException(EngineError.PermissionRevoked, "permissÃƒÂ£o negada ao abrir o microfone", e)
            } catch (e: IllegalArgumentException) {
                throw AudioSourceException(EngineError.MicUnavailable, "parÃƒÂ¢metros do AudioRecord recusados", e)
            }
        if (created.state != AudioRecord.STATE_INITIALIZED) {
            created.release()
            throw AudioSourceException(EngineError.MicUnavailable, "AudioRecord nÃƒÂ£o inicializou")
        }
        created.startRecording()
        if (created.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            created.release()
            throw AudioSourceException(EngineError.MicUnavailable, "microfone ocupado")
        }
        record = created
    }

    override fun read(buffer: ShortArray): Int {
        val active = record ?: throw AudioSourceException(EngineError.MicUnavailable, "microfone fechado")
        val count = active.read(buffer, 0, buffer.size)
        if (count < 0) {
            val cause = if (hasPermission()) EngineError.MicUnavailable else EngineError.PermissionRevoked
            throw AudioSourceException(cause, "AudioRecord.read=$count")
        }
        return count
    }

    override fun close() {
        val active = record ?: return
        record = null
        try {
            active.stop()
        } catch (e: IllegalStateException) {
            // jÃƒÂ¡ parado
        }
        active.release()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private companion object {
        /** `AudioSource.VOICE_RECOGNITION` da API do Android (6), a mesma fonte do Vosk 0.3.75. */
        const val VOICE_RECOGNITION = 6
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Dois blocos de 16 bits, como o laÃƒÂ§o de referÃƒÂªncia do vosk-android 0.3.75 (bufferSize * 2). */
        const val BUFFER_BYTES = VoskEngine.BLOCK_SAMPLES * 2 * 2
    }
}
