package io.github.gigioderico.tribuna.etapa0.ui

import io.github.gigioderico.tribuna.etapa0.align.Token

/** Retângulo de uma palavra na tela, em px. */
data class WordBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Toque em palavra ou vazio (TRD Renderização e toque §Hit-test 1–4; CLAUDE.md "Erros comuns"): o caractere
 * de `getOffsetForPosition` só conta se o toque cair no retângulo da palavra (união dos `getBoundingBox`)
 * ampliado em 8 dp. Parte pura, testada na JVM; a tela fornece o layout.
 */
object HitTest {
    /** Token cujo intervalo exibido contém `offset` (busca binária, TRD §Hit-test 4), ou `null`. */
    fun tokenAt(
        offset: Int,
        tokens: List<Token>,
    ): Int? {
        var low = 0
        var high = tokens.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val range = tokens[mid].display
            when {
                offset < range.first -> high = mid - 1
                offset > range.last -> low = mid + 1
                else -> return mid
            }
        }
        return null
    }

    /** União dos retângulos dos caracteres; `null` se não houver nenhum. */
    fun union(boxes: List<WordBox>): WordBox? =
        boxes.reduceOrNull { a, b ->
            WordBox(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
        }

    /** O ponto está no retângulo ampliado de `slop` px para todos os lados? */
    fun inside(
        x: Float,
        y: Float,
        box: WordBox,
        slop: Float,
    ): Boolean = x >= box.left - slop && x <= box.right + slop && y >= box.top - slop && y <= box.bottom + slop

    /** Índice do token tocado, ou `null` (vazio). */
    fun tappedToken(
        x: Float,
        y: Float,
        offset: Int,
        tokens: List<Token>,
        boxOf: (Int) -> WordBox,
        slop: Float,
    ): Int? {
        val index = tokenAt(offset, tokens) ?: return null
        val box = union(tokens[index].display.map(boxOf)) ?: return null
        return if (inside(x, y, box, slop)) index else null
    }
}
