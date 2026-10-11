package io.github.gigioderico.tribuna.etapa0.speech

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** Formato lido do cabeçalho de um WAV. */
data class WavFormat(
    val audioFormat: Int,
    val channels: Int,
    val sampleRate: Int,
    val bitsPerSample: Int,
    /** Bytes do bloco `data`. */
    val dataSize: Long,
)

/** Cabeçalho RIFF/WAVE (spec 0003, D8, C11). Só lê; nunca escreve. */
object WavHeader {
    private const val PCM = 1

    /** Lê o cabeçalho até o início do bloco `data`; o stream fica posicionado na primeira amostra. */
    fun read(input: DataInputStream): WavFormat {
        if (tag(input) != "RIFF") throw IOException("não é RIFF")
        int32(input)
        if (tag(input) != "WAVE") throw IOException("não é WAVE")
        var format: IntArray? = null
        while (true) {
            val id = tag(input)
            val size = int32(input).toLong() and 0xFFFFFFFFL
            when (id) {
                "fmt " -> {
                    val audioFormat = int16(input)
                    val channels = int16(input)
                    val sampleRate = int32(input)
                    int32(input) // byteRate
                    int16(input) // blockAlign
                    val bits = int16(input)
                    skip(input, size - 16)
                    format = intArrayOf(audioFormat, channels, sampleRate, bits)
                }

                "data" -> {
                    val f = format ?: throw IOException("bloco data antes de fmt")
                    return WavFormat(f[0], f[1], f[2], f[3], size)
                }

                else -> {
                    skip(input, size + (size and 1L))
                }
            }
        }
    }

    /** 16 kHz, mono, PCM 16 bits, o formato da captura (TRD §Captura e sessões; TESTES-ETAPA0). */
    fun isSpeechFormat(format: WavFormat): Boolean =
        format.audioFormat == PCM && format.channels == 1 && format.sampleRate == 16_000 && format.bitsPerSample == 16

    private fun tag(input: DataInputStream): String {
        val bytes = ByteArray(4)
        input.readFully(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun int16(input: DataInputStream): Int {
        val b0 = input.readUnsignedByte()
        val b1 = input.readUnsignedByte()
        return b0 or (b1 shl 8)
    }

    private fun int32(input: DataInputStream): Int = int16(input) or (int16(input) shl 16)

    private fun skip(
        input: DataInputStream,
        count: Long,
    ) {
        var left = count
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) throw EOFException("WAV truncado")
            left -= skipped
        }
    }
}

/** Ritmo da fonte WAV: espera até o tempo real de `delivered` amostras (spec 0003, D8). */
fun interface Pacer {
    fun awaitSamples(delivered: Long)
}

/** Ritmo real, por relógio monotônico. */
class RealTimePacer(
    private val sampleRate: Int = VoskEngine.SAMPLE_RATE,
) : Pacer {
    private var startNanos = -1L

    override fun awaitSamples(delivered: Long) {
        if (startNanos < 0) startNanos = System.nanoTime()
        val due = startNanos + delivered * 1_000_000_000L / sampleRate
        val waitMs = (due - System.nanoTime()) / 1_000_000L
        if (waitMs > 0) Thread.sleep(waitMs)
    }
}

/**
 * WAV de teste como fonte de áudio (spec 0003, D8): lido no ritmo real, nunca gravado. Depois do fim dos dados,
 * entrega silêncio (zeros) no mesmo ritmo, como um microfone mudo: o reconhecedor fecha a frase sozinho.
 * Fora de 16 kHz, mono, PCM 16 bits, `open()` recusa com [EngineError.MicUnavailable].
 */
class WavAudioSource(
    private val open: () -> InputStream,
    private val pacer: Pacer = RealTimePacer(),
) : AudioSource {
    constructor(file: File) : this({ FileInputStream(file) })

    private var input: DataInputStream? = null
    private var remainingSamples = 0L
    private var delivered = 0L

    /** `true` depois que todas as amostras do arquivo foram entregues. */
    val exhausted: Boolean
        get() = remainingSamples == 0L

    override fun open() {
        val stream =
            try {
                DataInputStream(BufferedInputStream(open.invoke()))
            } catch (e: IOException) {
                throw AudioSourceException(EngineError.MicUnavailable, "WAV não abriu: ${e.message}", e)
            }
        val format =
            try {
                WavHeader.read(stream)
            } catch (e: IOException) {
                stream.close()
                throw AudioSourceException(EngineError.MicUnavailable, "WAV inválido: ${e.message}", e)
            }
        if (!WavHeader.isSpeechFormat(format)) {
            stream.close()
            throw AudioSourceException(EngineError.MicUnavailable, "WAV fora de 16 kHz, mono, PCM 16 bits: $format")
        }
        input = stream
        remainingSamples = format.dataSize / 2
        delivered = 0L
    }

    override fun read(buffer: ShortArray): Int {
        val stream = input ?: throw AudioSourceException(EngineError.MicUnavailable, "WAV fechado")
        val count: Int
        if (remainingSamples > 0) {
            count = minOf(buffer.size.toLong(), remainingSamples).toInt()
            try {
                for (i in 0 until count) {
                    val lo = stream.readUnsignedByte()
                    val hi = stream.readByte().toInt()
                    buffer[i] = ((hi shl 8) or lo).toShort()
                }
            } catch (e: IOException) {
                throw AudioSourceException(EngineError.MicUnavailable, "WAV truncado", e)
            }
            remainingSamples -= count
        } else {
            count = buffer.size
            buffer.fill(0)
        }
        delivered += count
        pacer.awaitSamples(delivered)
        return count
    }

    override fun close() {
        input?.close()
        input = null
    }
}
