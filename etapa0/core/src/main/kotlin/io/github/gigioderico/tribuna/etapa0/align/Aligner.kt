package io.github.gigioderico.tribuna.etapa0.align

import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.MAX_BACK_FROM_HIGH_WATER
import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.MISSES_FOR_RECOVERY
import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.RECOVERY_WORDS
import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.SHORT_DISTANCE_TOKENS
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Função pura: recebe o estado e as palavras confirmadas e devolve o novo estado (TRD §Contratos, Aligner).
 * Não conhece Android nem o motor.
 *
 * Invariante: um movimento automático nunca leva o cursor abaixo de highWater − 3.
 * Só reposition move o cursor livremente, e ele redefine cursor e highWater para a nova posição.
 *
 * `words` é a cauda das palavras confirmadas desde o último reposition (ou início da sessão), em ordem,
 * terminando na mais nova; quem chama mantém a cauda e chama advance uma vez por lote de palavras novas
 * (impl_plan 0002, decisão A1). Só as últimas max(k, 4) palavras são lidas.
 * O cursor é o índice do último token falado (D2); a leitura começa com reposition(0) (D3).
 */
class Aligner(
    private val tokens: List<Token>,
    private val params: AlignParams,
) {
    private val forms: List<List<List<String>>> =
        tokens.map { token -> token.spoken.map { form -> form.flatMap(Normalizer::words) }.filter { it.isNotEmpty() } }
    private val weights: DoubleArray = DoubleArray(tokens.size) { tokens[it].weight.toDouble() }

    fun advance(
        state: AlignState,
        words: List<String>,
    ): AlignState {
        if (tokens.isEmpty()) return state
        val heard = recentWords(words)
        if (heard.size < params.k) return state.copy(lastMove = Move.Stay, score = 0.0) // D5
        val best = searchWindow(state, heard.takeLast(params.k))
        if (best.lastWord >= params.shortThreshold) { // decisão A2: a palavra mais nova precisa casar
            val short = abs(best.position - state.cursor) <= SHORT_DISTANCE_TOKENS && best.similarity >= params.shortThreshold
            if (short || best.similarity >= params.longThreshold) {
                if (best.position == state.cursor) {
                    return state.copy(missStreak = 0, lastMove = Move.Stay, score = best.similarity) // D6
                }
                return AlignState(
                    cursor = best.position,
                    highWater = max(state.highWater, best.position),
                    missStreak = 0,
                    lastMove = if (short) Move.Short else Move.LongJump,
                    score = best.similarity,
                )
            }
        }
        val misses = state.missStreak + 1 // D6: nenhum candidato passou
        if (misses >= MISSES_FOR_RECOVERY && heard.size >= RECOVERY_WORDS) {
            val recovery = recover(state.highWater, heard.takeLast(RECOVERY_WORDS))
            val jump = recovery.jump
            if (jump != null) {
                return AlignState(jump.position, jump.position, 0, Move.Recovery, jump.similarity) // D7
            }
            return state.copy(missStreak = misses, lastMove = Move.Stay, score = max(best.similarity, recovery.bestSimilarity))
        }
        return state.copy(missStreak = misses, lastMove = Move.Stay, score = best.similarity)
    }

    fun reposition(tokenIndex: Int): AlignState {
        // toque, Volume −, arrasto, troca de modo (D4: índice fora da faixa é limitado)
        val index = if (tokens.isEmpty()) 0 else tokenIndex.coerceIn(0, tokens.lastIndex)
        return AlignState(cursor = index, highWater = index, missStreak = 0, lastMove = Move.Reposition, score = 0.0)
    }

    private class Match(
        val position: Int,
        val similarity: Double,
        val lastWord: Double,
    )

    private class Recovery(
        val jump: Match?,
        val bestSimilarity: Double,
    )

    /** D13: normaliza a fala; [unk] vira a marca que nunca casa. Lê só o fim da cauda. */
    private fun recentWords(words: List<String>): List<String> {
        val needed = maxOf(params.k, RECOVERY_WORDS)
        val out = ArrayDeque<String>()
        var i = words.lastIndex
        while (i >= 0 && out.size < needed) {
            val raw = words[i]
            val normalized =
                if (raw.trim().lowercase(Locale.ROOT) == Similarity.UNKNOWN) listOf(Similarity.UNKNOWN) else Normalizer.words(raw)
            for (x in normalized.indices.reversed()) out.addFirst(normalized[x])
            i--
        }
        return out.toList().takeLast(needed)
    }

    /** TRD §Alinhamento 2 e D9: janela [max(cursor − 3, highWater − 3), cursor + W]; empate → mais perto do cursor. */
    private fun searchWindow(
        state: AlignState,
        heard: List<String>,
    ): Match {
        val from = maxOf(state.cursor - MAX_BACK_FROM_HIGH_WATER, state.highWater - MAX_BACK_FROM_HIGH_WATER, 0)
        val to = minOf(state.cursor + params.window, tokens.lastIndex)
        var best: Match? = null
        for (position in from..to) {
            val match = score(position, heard)
            val current = best
            if (current == null ||
                match.similarity > current.similarity + EPSILON ||
                (
                    abs(match.similarity - current.similarity) <= EPSILON &&
                        abs(position - state.cursor) < abs(current.position - state.cursor)
                )
            ) {
                best = match
            }
        }
        return best ?: Match(state.cursor, 0.0, 0.0)
    }

    /** TRD §Alinhamento 4, D7 e D8: só à frente de highWater; salta só com exatamente uma posição ≥ limite longo. */
    private fun recover(
        highWater: Int,
        heard: List<String>,
    ): Recovery {
        val candidates = mutableListOf<Match>()
        var bestSimilarity = 0.0
        for (position in highWater + 1..tokens.lastIndex) {
            val match = score(position, heard)
            bestSimilarity = max(bestSimilarity, match.similarity)
            if (match.similarity >= params.longThreshold) candidates += match
        }
        val only = candidates.singleOrNull()?.takeIf { it.lastWord >= params.shortThreshold }
        return Recovery(only, bestSimilarity)
    }

    /**
     * Similaridade média ponderada das palavras ouvidas terminando no token `position` (TRD §Alinhamento 1 e 2).
     * A palavra mais nova casa com a última palavra de uma forma falada do token; as anteriores andam para trás
     * pelo roteiro, e cada token usa a forma que der a maior média. Antes do início do roteiro, cada palavra
     * que sobra conta com similaridade 0 e peso 1. `lastWord` é a similaridade da palavra mais nova (decisão A2).
     */
    private fun score(
        position: Int,
        heard: List<String>,
    ): Match {
        var bestSimilarity = -1.0
        var bestLast = 0.0

        fun walk(
            token: Int,
            newest: Int,
            sum: Double,
            weight: Double,
            last: Double,
        ) {
            if (newest < 0 || token < 0) {
                val total = weight + (newest + 1)
                val similarity = if (total == 0.0) 0.0 else sum / total
                if (similarity > bestSimilarity + EPSILON) {
                    bestSimilarity = similarity
                    bestLast = last
                }
                return
            }
            val w = weights[token]
            for (form in forms[token]) {
                val used = minOf(form.size, newest + 1)
                var s = sum
                var total = weight
                for (x in 0 until used) {
                    s += w * Similarity.of(heard[newest - used + 1 + x], form[form.size - used + x])
                    total += w
                }
                val lastWord = if (token == position) Similarity.of(heard[newest], form.last()) else last
                walk(token - 1, newest - used, s, total, lastWord)
            }
        }

        walk(position, heard.lastIndex, 0.0, 0.0, 0.0)
        return Match(position, bestSimilarity.coerceAtLeast(0.0), bestLast)
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
