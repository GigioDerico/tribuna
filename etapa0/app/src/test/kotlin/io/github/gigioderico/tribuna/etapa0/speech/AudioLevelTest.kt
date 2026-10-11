package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Test

/** C7: `Level.rms` = sqrt(média(s²)) / 32768 (spec 0003, D2). */
class AudioLevelTest {
    @Test
    fun blocoDeZeros_daZero() {
        assertEquals(0.0f, AudioLevel.rms(ShortArray(3200), 3200), 0.0f)
    }

    @Test
    fun ondaQuadradaEmFundoDeEscala_daUm() {
        val square = ShortArray(3200) { if (it % 2 == 0) 32767 else -32767 }
        assertEquals(1.0f, AudioLevel.rms(square, square.size), 1e-3f)
    }

    @Test
    fun meioDaEscala_daMeio() {
        assertEquals(0.5f, AudioLevel.rms(ShortArray(100) { 16384 }, 100), 1e-6f)
    }

    @Test
    fun soContaAsAmostrasLidas() {
        val samples = ShortArray(200) { if (it < 100) 16384 else 0 }
        assertEquals(0.5f, AudioLevel.rms(samples, 100), 1e-6f)
    }

    @Test
    fun contagemZero_daZero() {
        assertEquals(0.0f, AudioLevel.rms(ShortArray(10) { 1000 }, 0), 0.0f)
    }
}
