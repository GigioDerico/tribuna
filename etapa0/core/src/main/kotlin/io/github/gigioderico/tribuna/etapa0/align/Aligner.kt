package io.github.gigioderico.tribuna.etapa0.align

/**
 * Função pura: recebe o estado e as palavras confirmadas e devolve o novo estado (TRD §Contratos, Aligner).
 * Não conhece Android nem o motor.
 *
 * Invariante: um movimento automático nunca leva o cursor abaixo de highWater − 3.
 * Só reposition move o cursor livremente, e ele redefine cursor e highWater para a nova posição.
 *
 * Esqueleto da tarefa 0001: a implementação é da tarefa 0002 (TRD §Motor de voz, Alinhamento).
 */
class Aligner(
    private val tokens: List<Token>,
    private val params: AlignParams,
) {
    fun advance(
        state: AlignState,
        words: List<String>,
    ): AlignState = throw NotImplementedError("Aligner.advance é implementado na tarefa 0002")

    fun reposition(tokenIndex: Int): AlignState =
        // toque, Volume −, arrasto, troca de modo
        throw NotImplementedError("Aligner.reposition é implementado na tarefa 0002")
}
