package io.github.gigioderico.tribuna.etapa0.align

/**
 * Transforma parciais e finais do reconhecedor em palavras confirmadas, sem repetir nem perder palavra
 * (TRD Motor de voz §Reconciliação de hipóteses, passos 1–6). O passo 7 (descartar evento de sessão
 * antiga) é de quem chama, antes deste objeto; quem chama também chama reset() ao abrir cada sessão.
 * Não é thread-safe: um objeto por leitura, usado de um só lugar.
 */
class PartialReconciler {
    private val confirmed = mutableListOf<String>()
    private var previous: List<String>? = null

    /** Passos 2–4: só o prefixo comum com o parcial anterior é candidato; devolve as palavras novas. */
    fun onPartial(words: List<String>): List<String> {
        val last = previous
        previous = words
        if (last == null || words.isEmpty()) return emptyList()
        return deliver(commonPrefix(last, words))
    }

    /** Passo 5: entrega o que faltar do resultado final e zera o estado da frase. */
    fun onFinal(words: List<String>): List<String> {
        val delivered = deliver(words)
        reset()
        return delivered
    }

    /** Zera a frase em curso; chamado no Final e no começo de cada sessão. */
    fun reset() {
        confirmed.clear()
        previous = null
    }

    private fun deliver(sequence: List<String>): List<String> {
        val from = endOfConfirmed(sequence)
        val fresh = sequence.subList(from, sequence.size).toList()
        confirmed += fresh // passo 6: nada já entregue é retirado
        return fresh
    }

    private fun commonPrefix(
        a: List<String>,
        b: List<String>,
    ): List<String> {
        var n = 0
        while (n < a.size && n < b.size && same(a[n], b[n])) n++
        return b.subList(0, n).toList()
    }

    /**
     * Passo 3: distância de edição por palavra entre `confirmed` e cada prefixo de `sequence`.
     * Devolve o tamanho do prefixo mais próximo; no empate, o maior (prefere não repetir palavra).
     */
    private fun endOfConfirmed(sequence: List<String>): Int {
        if (confirmed.isEmpty()) return 0
        var row = IntArray(sequence.size + 1) { it }
        for (i in 1..confirmed.size) {
            val next = IntArray(sequence.size + 1)
            next[0] = i
            for (j in 1..sequence.size) {
                val cost = if (same(confirmed[i - 1], sequence[j - 1])) 0 else 1
                next[j] = minOf(row[j] + 1, next[j - 1] + 1, row[j - 1] + cost)
            }
            row = next
        }
        var best = 0
        for (j in row.indices) if (row[j] <= row[best]) best = j
        return best
    }

    private fun same(
        a: String,
        b: String,
    ): Boolean = Normalizer.normalize(a) == Normalizer.normalize(b)
}
