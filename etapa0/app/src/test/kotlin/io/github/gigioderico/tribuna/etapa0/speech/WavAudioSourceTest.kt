package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** C11: fonte WAV de teste (spec 0003, D8). O WAV é sintético, gerado aqui; nunca uma gravação de discurso. */
class WavAudioSourceTest {
    private fun wav(
        samples: ShortArray,
        audioFormat: Int = 1,
        sampleRate: Int = 16_000,
        channels: Int = 1,
        bits: Int = 16,
        extraChunk: Boolean = false,
    ): ByteArray {
        val data = ByteArrayOutputStream()
        samples.forEach {
            data.write(it.toInt() and 0xFF)
            data.write((it.toInt() shr 8) and 0xFF)
        }
        val body = ByteArrayOutputStream()

        fun int16(v: Int) {
            body.write(v and 0xFF)
            body.write((v shr 8) and 0xFF)
        }

        fun int32(v: Int) {
            int16(v and 0xFFFF)
            int16((v shr 16) and 0xFFFF)
        }
        body.write("WAVE".toByteArray())
        body.write("fmt ".toByteArray())
        int32(16)
        int16(audioFormat)
        int16(channels)
        int32(sampleRate)
        int32(sampleRate * channels * bits / 8)
        int16(channels * bits / 8)
        int16(bits)
        if (extraChunk) {
            body.write("LIST".toByteArray())
            int32(3)
            body.write(byteArrayOf(1, 2, 3, 0))
        }
        body.write("data".toByteArray())
        int32(data.size())
        body.write(data.toByteArray())
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        val size = body.size()
        out.write(
            byteArrayOf((size and 0xFF).toByte(), (size shr 8 and 0xFF).toByte(), (size shr 16 and 0xFF).toByte(), (size shr 24).toByte()),
        )
        out.write(body.toByteArray())
        return out.toByteArray()
    }

    private fun source(bytes: ByteArray) = WavAudioSource({ ByteArrayInputStream(bytes) }, Pacer { })

    private val ramp = ShortArray(10_000) { (it * 7 - 30_000).toShort() }

    @Test
    fun wav16kMonoPcm16_eAceito_eOsBlocosSaemEmOrdemESemPerda() {
        val wav = source(wav(ramp, extraChunk = true))
        wav.open()
        val buffer = ShortArray(3_200)
        val read = mutableListOf<Short>()
        while (!wav.exhausted) {
            val n = wav.read(buffer)
            read += buffer.copyOf(n).toList()
        }
        assertEquals(ramp.size, read.size)
        assertArrayEquals(ramp, read.toShortArray())
    }

    @Test
    fun depoisDoFim_entregaSilencio() {
        val wav = source(wav(ShortArray(100) { 500 }))
        wav.open()
        val buffer = ShortArray(3_200)
        assertEquals(100, wav.read(buffer))
        assertEquals(3_200, wav.read(buffer))
        assertTrue(buffer.all { it == 0.toShort() })
    }

    @Test
    fun ritmoReal_recebeOTotalEntregue() {
        val delivered = mutableListOf<Long>()
        val wav = WavAudioSource({ ByteArrayInputStream(wav(ShortArray(5_000))) }, Pacer { delivered += it })
        wav.open()
        val buffer = ShortArray(3_200)
        wav.read(buffer)
        wav.read(buffer)
        assertEquals(listOf(3_200L, 5_000L), delivered)
    }

    private fun refused(bytes: ByteArray): EngineError? =
        (runCatching { source(bytes).open() }.exceptionOrNull() as? AudioSourceException)?.error

    @Test
    fun wav44kHz_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, sampleRate = 44_100)))
    }

    @Test
    fun wavEstereo_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, channels = 2)))
    }

    @Test
    fun wav8Bits_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, bits = 8)))
    }

    @Test
    fun arquivoQueNaoEWav_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused("não é wav".toByteArray()))
    }

    @Test
    fun wavNaoPcm_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, audioFormat = 3)))
    }
}
