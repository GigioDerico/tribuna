package io.github.gigioderico.tribuna.etapa0.speech

import kotlin.math.sqrt

/** `Level.rms` = sqrt(média(s²)) / 32768, em [0, 1] (spec 0003, D2). */
object AudioLevel {
    fun rms(
        samples: ShortArray,
        count: Int,
    ): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        return (sqrt(sum / count) / 32768.0).toFloat()
    }
}
