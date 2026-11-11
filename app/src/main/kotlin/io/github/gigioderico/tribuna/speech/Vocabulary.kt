package io.github.gigioderico.tribuna.speech

import io.github.gigioderico.tribuna.align.Normalizer
import io.github.gigioderico.tribuna.align.Token

/** LÃƒÂ©xico do modelo indexado pela forma normalizada (OPR-020). Sem sÃƒÂ­mbolos internos (`<eps>`, `!SIL`, `#0`). */
class Lexicon(
    words: Collection<String>,
) {
    private val byNormalized: Map<String, List<String>> =
        words
            .filter { it.isNotEmpty() && it != Vocabulary.UNKNOWN && it[0] !in INTERNAL_PREFIXES }
            .groupBy(Normalizer::normalize)
            .mapValues { (_, spellings) -> spellings.sorted() }

    val size: Int = byNormalized.values.sumOf { it.size }

    /** Grafias do lÃƒÂ©xico cuja forma normalizada ÃƒÂ© `normalized`; vazia se nenhuma. */
    fun spellings(normalized: String): List<String> = byNormalized[normalized].orEmpty()

    private companion object {
        const val INTERNAL_PREFIXES = "<#!"
    }
}

/**
 * Lista do vocabulÃƒÂ¡rio restrito (TRD Motor de voz Ã‚Â§VocabulÃƒÂ¡rio restrito; spec 0003, D5, C5; OPR-020).
 *
 * Primeiro, cada palavra de `Token.spoken`, uma vez, na ordem do roteiro (a forma do TRD). Depois, as grafias
 * do lÃƒÂ©xico do modelo que normalizam para uma dessas palavras ou para uma parte de palavra com hÃƒÂ­fen do texto
 * ("trÃƒÂªs" para "tres", "bom" e "dia" para "bom-dia"): o lÃƒÂ©xico do `vosk-model-small-pt-0.3` tem a grafia com
 * acento e o Vosk ignora palavra fora do lÃƒÂ©xico. Por ÃƒÂºltimo, `[unk]`. Sem lÃƒÂ©xico, a lista ÃƒÂ© a do TRD.
 * Nada aqui sai da memÃƒÂ³ria: a lista vai sÃƒÂ³ ao reconhecedor.
 */
object Vocabulary {
    const val UNKNOWN = "[unk]"
    private val joiners = Regex("[-'Ã¢â‚¬â„¢]")

    fun build(
        text: String,
        tokens: List<Token>,
        lexicon: Lexicon?,
    ): List<String> {
        val spoken = LinkedHashSet<String>()
        tokens.forEach { token -> token.spoken.forEach { form -> spoken.addAll(form) } }
        if (lexicon == null) return spoken.toList() + UNKNOWN
        val keys = LinkedHashSet<String>(spoken)
        tokens.forEach { token ->
            val shown = text.substring(token.display.first, token.display.last + 1)
            keys.addAll(Normalizer.words(shown.replace(joiners, " ")))
        }
        val extra = sortedSetOf<String>()
        keys.forEach { key -> lexicon.spellings(key).filterTo(extra) { it !in spoken } }
        return spoken.toList() + extra + UNKNOWN
    }
}
