package io.github.gigioderico.tribuna.etapa0.align

/**
 * Constantes fixas do alinhamento (PRD, Algoritmo de alinhamento; TRD Motor de voz §Alinhamento).
 * Não variam por sensibilidade (spec 0002, D12). O peso das palavras comuns é PROVISÓRIO (D10).
 */
object AlignConstants {
    /** Avanço curto: distância até 5 tokens do cursor (TRD §Alinhamento 3). */
    const val SHORT_DISTANCE_TOKENS = 5

    /** Nenhum movimento automático abaixo de highWater − 3 (TRD §Alinhamento 2 e 5; CLAUDE.md regra 6). */
    const val MAX_BACK_FROM_HIGH_WATER = 3

    /** A recuperação dispara quando o missStreak chega a 2 (TRD §Alinhamento 4; D7). */
    const val MISSES_FOR_RECOVERY = 2

    /** Palavras comparadas na recuperação (TRD §Alinhamento 4: "4 ou mais"; D7). */
    const val RECOVERY_WORDS = 4

    /** Palavras comuns do PRD, na forma normalizada; "é" também conta, porque normaliza para "e" (D10). */
    val COMMON_WORDS: Set<String> = setOf("de", "que", "e", "o", "a")

    /** PROVISÓRIO: decisão do analista para a Etapa 0 (D10), a calibrar na 0005 com o T09 e o L30. */
    const val COMMON_WORD_WEIGHT = 0.5f
}
