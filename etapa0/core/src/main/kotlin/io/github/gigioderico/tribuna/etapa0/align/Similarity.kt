package io.github.gigioderico.tribuna.etapa0.align

/** Similaridade de palavras: 1 − Levenshtein / maior comprimento (TRD §Stack "Levenshtein próprio"; §Alinhamento 2). */
internal object Similarity {
    /** Marca do vocabulário restrito do Vosk: nunca casa com palavra do roteiro (D13). */
    const val UNKNOWN = "[unk]"

    fun levenshtein(
        a: String,
        b: String,
    ): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    fun of(
        a: String,
        b: String,
    ): Double {
        if (a == UNKNOWN || b == UNKNOWN) return 0.0
        val longest = maxOf(a.length, b.length)
        if (longest == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }
}
