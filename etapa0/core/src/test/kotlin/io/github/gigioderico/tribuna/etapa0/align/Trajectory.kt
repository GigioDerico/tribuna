package io.github.gigioderico.tribuna.etapa0.align

/**
 * Trajetória sintética (spec 0002, D21): uma palavra por chamada de advance, cauda mantida pelo teste (D1),
 * amostra depois de cada chamada. O esperado é o maior token lido do roteiro até ali (congela no improviso
 * e na repetição); reposition redefine o esperado para a nova posição.
 */
sealed interface Step {
    /** Palavra do roteiro; `token` é o token que termina nesta palavra, ou null no meio de um token composto. */
    data class Read(
        val word: String,
        val token: Int?,
    ) : Step

    /** Palavra fora do roteiro (improviso ou erro sem correspondente). */
    data class Improv(
        val word: String,
    ) : Step

    /** Nova sessão (pausa e retomada): quem chama zera a cauda; nenhuma chamada. */
    data object NewSession : Step

    /** Toque numa palavra: reposition e cauda zerada. */
    data class Reposition(
        val index: Int,
    ) : Step
}

enum class Kind { Read, Improv, Reposition }

data class Sample(
    val step: Int,
    val word: String?,
    val kind: Kind,
    val previousCursor: Int,
    val state: AlignState,
    val expected: Int,
) {
    /** Atraso em tokens (TESTES-ETAPA0, Métricas). */
    val delay: Int get() = expected - state.cursor

    /** Mais de 2 tokens à frente do esperado, ou qualquer movimento durante improviso. */
    val wrongJump: Boolean get() = state.cursor > expected + 2 || (kind == Kind.Improv && state.cursor != previousCursor)
}

/** Qual forma falada usar nas referências do T02 (protocolo: três takes). */
enum class Take { First, A, B, C }

object Speech {
    fun of(
        tokens: List<Token>,
        from: Int,
        toExclusive: Int,
        take: Take = Take.First,
    ): List<Step.Read> =
        (from until toExclusive).flatMap { i ->
            val form = pick(tokens[i], take)
            form.mapIndexed { n, word -> Step.Read(word, if (n == form.lastIndex) i else null) }
        }

    fun improv(words: List<String>): List<Step.Improv> = words.map { Step.Improv(it) }

    /** Todas as palavras faladas (primeira forma) de um texto, para usar como improviso. */
    fun wordsOf(tokens: List<Token>): List<String> = tokens.flatMap { it.spoken.first() }

    /** Índice do primeiro token de `phrase` (palavras simples), a partir de `from`. */
    fun indexOf(
        tokens: List<Token>,
        phrase: String,
        from: Int = 0,
    ): Int {
        val words = Normalizer.words(phrase)
        for (i in from..tokens.size - words.size) {
            if (words.indices.all { tokens[i + it].spoken.first() == listOf(words[it]) }) return i
        }
        error("frase não encontrada: $phrase")
    }

    /** Token de referência bíblica: tem uma forma "capítulo ... versículo(s) ...". */
    fun isReference(token: Token): Boolean = token.spoken.any { it.size > 1 && it.first() == "capitulo" }

    private fun pick(
        token: Token,
        take: Take,
    ): List<String> {
        if (take == Take.First || !isReference(token)) return token.spoken.first()
        val plain = token.spoken.filter { it.first() != "capitulo" }
        val shortest = plain.minOf { it.size }
        return when (take) {
            Take.A -> plain.first { it.size == shortest }

            // "três dezesseis", "treze quatro a sete"
            Take.B -> plain.first { it.size == shortest + 1 }

            // "três e dezesseis", "treze de quatro a sete"
            else -> token.spoken.first { it.first() == "capitulo" } // "capítulo três versículo dezesseis"
        }
    }
}

class TrajectoryRunner(
    tokens: List<Token>,
    params: AlignParams,
) {
    val aligner = Aligner(tokens, params)
    val advanceNanos = mutableListOf<Long>()

    fun run(steps: List<Step>): List<Sample> {
        var state = aligner.reposition(0) // D3
        val tail = mutableListOf<String>()
        var expected = 0
        val samples = mutableListOf<Sample>()
        steps.forEachIndexed { i, step ->
            val previous = state.cursor
            when (step) {
                is Step.NewSession -> {
                    tail.clear()
                }

                is Step.Reposition -> {
                    state = aligner.reposition(step.index)
                    tail.clear()
                    expected = state.cursor
                    samples += Sample(i, null, Kind.Reposition, previous, state, expected)
                }

                is Step.Read -> {
                    tail += step.word
                    state = timed(state, tail)
                    step.token?.let { expected = maxOf(expected, it) }
                    samples += Sample(i, step.word, Kind.Read, previous, state, expected)
                }

                is Step.Improv -> {
                    tail += step.word
                    state = timed(state, tail)
                    samples += Sample(i, step.word, Kind.Improv, previous, state, expected)
                }
            }
        }
        return samples
    }

    private fun timed(
        state: AlignState,
        tail: List<String>,
    ): AlignState {
        val start = System.nanoTime()
        val next = aligner.advance(state, tail)
        advanceNanos += System.nanoTime() - start
        return next
    }
}

fun List<Sample>.reads(): List<Sample> = filter { it.kind == Kind.Read }

fun List<Sample>.wrongJumps(): List<Sample> = filter { it.wrongJump }

fun List<Sample>.fractionWithinDelay(maxDelay: Int = 3): Double {
    require(isNotEmpty()) { "nenhuma amostra (Q3)" }
    return count { it.delay <= maxDelay }.toDouble() / size
}

fun List<Sample>.moveChanges(): Int = count { it.state.cursor != it.previousCursor }
