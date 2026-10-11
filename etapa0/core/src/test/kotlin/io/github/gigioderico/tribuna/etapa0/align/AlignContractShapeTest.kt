package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AlignContractShapeTest {
    @Test
    fun token_representaOExemploDoTrd316() {
        // "João 3:16": a referência ocupa os caracteres 5..8 e é um único token (TRD §Contratos, Token).
        val token =
            Token(
                index = 1,
                display = 5..8,
                spoken =
                    listOf(
                        listOf("tres", "dezesseis"),
                        listOf("tres", "e", "dezesseis"),
                        listOf("capitulo", "tres", "versiculo", "dezesseis"),
                    ),
                weight = 1.0f,
                sentenceStart = false,
            )
        assertEquals(5..8, token.display)
        assertEquals(3, token.spoken.size)
        assertEquals(listOf("tres", "e", "dezesseis"), token.spoken[1])
    }

    @Test
    fun alignState_temOsCamposDoContrato() {
        val state = AlignState(cursor = 4, highWater = 7, missStreak = 0, lastMove = Move.Short, score = 0.5)
        assertEquals(AlignState(5, 7, 0, Move.Short, 0.5), state.copy(cursor = 5))
    }

    @Test
    fun move_temOsMovimentosDoTrdNaOrdem() {
        assertEquals(
            listOf(Move.Stay, Move.Short, Move.LongJump, Move.Recovery, Move.Reposition),
            Move.entries,
        )
    }

    @Test
    fun aligner_eAlignParams_seConstroemComOsArgumentosDoContrato() {
        // Valores sintéticos, só para construir: não são parâmetros calibrados (vêm da Etapa 0).
        // advance e reposition não são chamados nesta tarefa (spec, decisão 3).
        val params = AlignParams(k = 1, window = 1, shortThreshold = 0.0, longThreshold = 0.0)
        val aligner = Aligner(tokens = emptyList(), params = params)
        assertEquals(1, params.k)
        assertNotNull(aligner)
    }
}
