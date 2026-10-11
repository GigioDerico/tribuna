package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Limites e guardas do Aligner (rodada 3 da 0002; TRD §Alinhamento 2 a 5; spec 0002 D5–D13).
 * Os roteiros são sintéticos (palavras de letras sorteadas, sempre as mesmas) e os parâmetros são montados em cada
 * teste para que a similaridade caia exatamente no limite: texto de teste, nunca discurso pessoal.
 * Cada teste diz qual quebra mínima do `Aligner` ele trava (tabela Q2 do impl_plan, quebras da rodada 3).
 */
class AlignerBoundaryTest {
    /** Palavra de 10 letras de 'a' a 'y' (sem 'z', que serve para estragar letras), estável por índice. */
    private fun word(i: Int): String {
        val random = Random(1000L + i)
        return String(CharArray(10) { 'a' + random.nextInt(25) })
    }

    /** Troca as [n] primeiras letras por 'z': similaridade 1 − n/10 contra a palavra original (2 → 0,8; 4 → 0,6). */
    private fun noisy(
        w: String,
        n: Int,
    ): String = "z".repeat(n) + w.drop(n)

    private fun script(
        size: Int,
        forms: Map<Int, List<List<String>>> = emptyMap(),
    ): List<Token> = List(size) { Token(it, it..it, forms[it] ?: listOf(listOf(word(it))), 1f, false) }

    private fun params(
        k: Int,
        short: Double,
        long: Double,
        window: Int = 40,
    ) = AlignParams(k = k, window = window, shortThreshold = short, longThreshold = long)

    private fun state(
        cursor: Int,
        highWater: Int = cursor,
        missStreak: Int = 0,
    ) = AlignState(cursor, highWater, missStreak, Move.Stay, 0.0)

    private fun next(
        tokens: List<Token>,
        params: AlignParams,
        from: AlignState,
        heard: List<String>,
    ) = Aligner(tokens, params).advance(from, heard)

    // ---------------------------------------------------------------- distância curta e limiares

    /** Q2 (L40a, K1, K1b): fica vermelho se "curto" deixar de ser exatamente até 5 tokens do cursor. */
    @Test
    fun avancoCurto_valeAteCincoTokensDoCursor() {
        val tokens = script(100)
        val p = params(k = 1, short = 0.6, long = 0.9)
        val em5 = next(tokens, p, state(10), listOf(noisy(word(15), 2)))
        assertEquals(AlignState(15, 15, 0, Move.Short, 0.8), em5)
        val em6 = next(tokens, p, state(10), listOf(noisy(word(16), 2)))
        assertEquals(AlignState(10, 10, 1, Move.Stay, em6.score), em6)
        assertEquals(0.8, em6.score, 1e-9)
    }

    /** Q2 (L39a, L40b): fica vermelho se o limiar curto passar de ">=" para ">" (similaridade e palavra mais nova). */
    @Test
    fun limiarCurto_iguaisPassam_maiorQueNaoPassa() {
        val tokens = script(100)
        val heard = listOf(noisy(word(12), 2))
        val exact = Similarity.of(heard[0], word(12))
        val passes = next(tokens, params(k = 1, short = exact, long = 0.99), state(10), heard)
        assertEquals(AlignState(12, 12, 0, Move.Short, exact), passes)
        val fails = next(tokens, params(k = 1, short = Math.nextUp(exact), long = 0.99), state(10), heard)
        assertEquals(1, fails.missStreak)
        assertEquals(10, fails.cursor)
    }

    /** Q2 (L40b): a similaridade média (0,8) está no limiar curto e a palavra mais nova (1,0) passa folgada. */
    @Test
    fun limiarCurto_mediaIgualAoLimiarPassa() {
        val tokens = script(100)
        // k = 2: palavra anterior com 0,8 e a mais nova exata: média 0,9
        val heard = listOf(noisy(word(11), 2), word(12))
        val avg = (Similarity.of(heard[0], word(11)) + Similarity.of(heard[1], word(12))) / 2.0
        val passes = next(tokens, params(k = 2, short = avg, long = 0.99), state(10), heard)
        assertEquals(12, passes.cursor)
        assertEquals(Move.Short, passes.lastMove)
        val fails = next(tokens, params(k = 2, short = Math.nextUp(avg), long = 0.99), state(10), heard)
        assertEquals(10, fails.cursor)
        assertEquals(1, fails.missStreak)
    }

    /** Q2 (L41b): fica vermelho se o limiar longo passar de ">=" para ">". Salto a 20 tokens do cursor. */
    @Test
    fun limiarLongo_iguaisPassam_maiorQueNaoPassa() {
        val tokens = script(100)
        val heard = listOf(noisy(word(30), 1))
        val exact = Similarity.of(heard[0], word(30))
        val passes = next(tokens, params(k = 1, short = 0.5, long = exact), state(10), heard)
        assertEquals(AlignState(30, 30, 0, Move.LongJump, exact), passes)
        val fails = next(tokens, params(k = 1, short = 0.5, long = Math.nextUp(exact)), state(10), heard)
        assertEquals(AlignState(10, 10, 1, Move.Stay, fails.score), fails)
    }

    /** Q2 (L48): fica vermelho se um movimento não zerar missStreak. */
    @Test
    fun movimento_zeraMissStreak() {
        val tokens = script(100)
        val p = params(k = 1, short = 0.6, long = 0.9)
        assertEquals(AlignState(12, 12, 0, Move.Short, 1.0), next(tokens, p, state(10, missStreak = 1), listOf(word(12))))
        assertEquals(AlignState(40, 40, 0, Move.LongJump, 1.0), next(tokens, p, state(10, missStreak = 1), listOf(word(40))))
    }

    /** Q2 (L104b, L104c): fica vermelho se a janela passar de cursor + W em mais ou menos uma posição. */
    @Test
    fun janela_terminaExatamenteEmCursorMaisW() {
        val tokens = script(100)
        val p = params(k = 1, short = 0.6, long = 0.9, window = 10)
        assertEquals(AlignState(20, 20, 0, Move.LongJump, 1.0), next(tokens, p, state(10), listOf(word(20))))
        val fora = next(tokens, p, state(10), listOf(word(21)))
        assertEquals(10, fora.cursor)
        assertEquals(1, fora.missStreak)
    }

    /** Q2 (K2b): fica vermelho se o recuo permitido passar de highWater − 3 para highWater − 2 (ou menos). */
    @Test
    fun recuo_chegaAHighWaterMenos3EParaAli() {
        val tokens = script(100)
        val p = params(k = 1, short = 0.6, long = 0.9)
        assertEquals(AlignState(27, 30, 0, Move.Short, 1.0), next(tokens, p, state(30), listOf(word(27))))
        val abaixo = next(tokens, p, state(30), listOf(word(26)))
        assertEquals(30, abaixo.cursor)
        assertEquals(1, abaixo.missStreak)
    }

    /** Q2 (L159): fica vermelho se as palavras ouvidas que sobram antes do início do roteiro deixarem de pesar. */
    @Test
    fun palavrasQueSobramAntesDoInicio_contamComoErro() {
        val tokens = script(100)
        val p = params(k = 3, short = 0.6, long = 0.9)
        val result = next(tokens, p, state(0), listOf("qqqqqqqqqq", "rrrrrrrrrr", word(0)))
        assertEquals(AlignState(0, 0, 1, Move.Stay, 1.0 / 3.0), result.copy(score = 1.0 / 3.0))
        assertEquals(1.0 / 3.0, result.score, 1e-9)
    }

    /** Q2 (L28a, L182a): forma sem palavra (só pontuação) não quebra e a similaridade nunca fica negativa. */
    @Test
    fun tokenSemPalavras_naoQuebraENaoDaSimilaridadeNegativa() {
        val tokens = script(1, forms = mapOf(0 to listOf(listOf("—"))))
        val result = next(tokens, params(k = 1, short = 0.6, long = 0.9), state(0), listOf("qualquer"))
        assertEquals(AlignState(0, 0, 1, Move.Stay, 0.0), result)
    }

    /** Q2 (L186): fica vermelho se a tolerância de empate subir a ponto de apagar diferenças pequenas (< 0,01). */
    @Test
    fun diferencaPequenaDeSimilaridade_naoViraEmpate() {
        val longo = "abcdefghijklmnopqrstuvwxy".repeat(6) // 150 letras
        val quase = "z" + longo.drop(1) // 1 troca: 1 − 1/150
        val tokens = script(60, forms = mapOf(11 to listOf(listOf(quase)), 14 to listOf(listOf(longo))))
        val p = params(k = 1, short = 0.6, long = 0.9)
        val result = next(tokens, p, state(10), listOf(longo))
        assertEquals(14, result.cursor) // a exata, mais longe, vence a quase exata, mais perto
        assertEquals(1.0, result.score, 1e-12)
    }

    /** Q2 (L113a): empate de similaridade a distâncias iguais do cursor fica com o menor índice. */
    @Test
    fun empateAMesmaDistancia_ficaComOMenorIndice() {
        val tokens = script(60, forms = mapOf(5 to listOf(listOf("repetida")), 9 to listOf(listOf("repetida"))))
        val result = next(tokens, params(k = 1, short = 0.6, long = 0.9), state(7), listOf("repetida"))
        assertEquals(AlignState(5, 7, 0, Move.Short, 1.0), result)
    }

    /**
     * Q2 (L110b): somas iguais a menos de 1e-9 são empate e vão para a posição mais próxima do cursor. As médias
     * 2,4/3 saem como 0,7999999999999999 (posição 11, mais perto) e 0,8000000000000002 (posição 15) só pela ordem das somas.
     */
    @Test
    fun ruidoDePontoFlutuante_naoDecideAJanela() {
        val h = listOf(word(100), word(101), word(102))
        val forms =
            mapOf(
                9 to listOf(listOf(noisy(h[0], 4))), // posição 11: (1,0 ; 0,8 ; 0,6) do mais novo ao mais velho
                10 to listOf(listOf(noisy(h[1], 2))),
                11 to listOf(listOf(h[2])),
                13 to listOf(listOf(noisy(h[0], 2))), // posição 15: (1,0 ; 0,6 ; 0,8)
                14 to listOf(listOf(noisy(h[1], 4))),
                15 to listOf(listOf(h[2])),
            )
        val result = next(script(40, forms), params(k = 3, short = 0.6, long = 0.95), state(10), h)
        assertEquals(11, result.cursor)
    }

    // ---------------------------------------------------------------- formas faladas e normalização da fala

    /** Q2 (L91a): [unk] em maiúsculas ou com espaços continua sendo a marca que nunca casa. */
    @Test
    fun unk_valeComCaixaEEspacos_D13() {
        val tokens = script(10, forms = mapOf(2 to listOf(listOf("unk"))))
        val p = params(k = 1, short = 0.6, long = 0.9)
        for (raw in listOf("[UNK]", " [unk] ")) {
            assertEquals(AlignState(1, 1, 1, Move.Stay, 0.0), next(tokens, p, state(1), listOf(raw)))
        }
    }

    /** Q2 (L92): um item da cauda com várias palavras entra na ordem em que foi dito. */
    @Test
    fun itemComVariasPalavras_mantemAOrdem() {
        val tokens = script(60)
        val item = "${word(5)} ${word(6)} ${word(7)}"
        val result = next(tokens, params(k = 3, short = 0.6, long = 0.9), state(4), listOf(item))
        assertEquals(AlignState(7, 7, 0, Move.Short, 1.0), result)
    }

    /** Q2 (L168): o token tem duas formas faladas e a certa é a segunda. */
    @Test
    fun segundaFormaFalada_tambemCasa() {
        val tokens = script(10, forms = mapOf(2 to listOf(listOf("dois"), listOf("duas"))))
        val result = next(tokens, params(k = 1, short = 0.6, long = 0.9), state(1), listOf("duas"))
        assertEquals(AlignState(2, 2, 0, Move.Short, 1.0), result)
    }

    /** Q2 (L173a, L173b, L176b): forma de 3 palavras casada só pelas 2 últimas, em ordem, e a mais nova com a última. */
    @Test
    fun formaFaladaLonga_casaPelasUltimasPalavrasEmOrdem() {
        val tokens = script(10, forms = mapOf(2 to listOf(listOf("vinte", "e", "um"))))
        val result = next(tokens, params(k = 2, short = 0.6, long = 0.9), state(1), listOf("e", "um"))
        assertEquals(AlignState(2, 2, 0, Move.Short, 1.0), result)
    }

    /** Q2 (L177): depois de uma forma de 2 palavras, o token anterior casa com a palavra que sobrou, não com a anterior. */
    @Test
    fun formaFalada_consomeSuasPalavrasEOAnteriorCasaComAResto() {
        val tokens = script(10, forms = mapOf(2 to listOf(listOf("e", "um"))))
        val result = next(tokens, params(k = 3, short = 0.9, long = 0.95), state(1), listOf(word(1), "e", "um"))
        assertEquals(AlignState(2, 2, 0, Move.Short, 1.0), result)
    }

    /** Q2 (L161a, L161c): duas formas com a mesma média (2,4/3): vale a primeira; a segunda tem a palavra mais nova melhor. */
    @Test
    fun formasEmpatadas_valeAPrimeira() {
        val h = listOf(word(200), word(201), word(202))
        val primeira = listOf(noisy(h[0], 2), h[1], noisy(h[2], 4)) // 0,8 ; 1,0 ; 0,6 (mais nova 0,6)
        val segunda = listOf(noisy(h[0], 4), noisy(h[1], 2), h[2]) // 0,6 ; 0,8 ; 1,0 (mais nova 1,0)
        val tokens = script(5, forms = mapOf(0 to listOf(primeira, segunda)))
        val result = next(tokens, params(k = 3, short = 0.7, long = 0.95, window = 5), state(0), h)
        assertEquals(AlignState(0, 0, 1, Move.Stay, result.score), result)
    }

    /** Q2 (L161b): a diferença de 4e-16 entre as formas é ruído, não vantagem. */
    @Test
    fun formasComRuidoDePontoFlutuante_valeAPrimeira() {
        val h = listOf(word(210), word(211), word(212))
        val primeira = listOf(h[0], noisy(h[1], 2), noisy(h[2], 4)) // 1,0 ; 0,8 ; 0,6 → 2,4 (mais nova 0,6)
        val segunda = listOf(h[0], noisy(h[1], 4), noisy(h[2], 2)) // 1,0 ; 0,6 ; 0,8 → 2,4000000000000004 (mais nova 0,8)
        val tokens = script(5, forms = mapOf(0 to listOf(primeira, segunda)))
        val result = next(tokens, params(k = 3, short = 0.7, long = 0.95, window = 5), state(0), h)
        assertEquals(AlignState(0, 0, 1, Move.Stay, result.score), result)
    }

    // ---------------------------------------------------------------- score e recuperação

    /** Q2 (L63a, L61b): o score do miss é a melhor similaridade da janela; com recuperação, o maior entre janela e recuperação. */
    @Test
    fun scoreDoMiss_eAMelhorSimilaridade() {
        val tokens = script(100)
        val p = params(k = 1, short = 0.9, long = 0.99)
        // a palavra mais nova é a última da cauda: só ela vale com k = 1; as 4 palavras já permitem a recuperação
        val newest = listOf("qqqqqqqqqq", "rrrrrrrrrr", "ssssssssss", noisy(word(11), 2))
        val primeiro = next(tokens, p, state(10), newest)
        assertEquals(AlignState(10, 10, 1, Move.Stay, 0.8), primeiro)
        val segundo = next(tokens, p, primeiro, newest)
        assertEquals(AlignState(10, 10, 2, Move.Stay, 0.8), segundo)
    }

    /**
     * Cenário da recuperação: 4 palavras ouvidas; no token [c], a palavra mais nova tem 0,9, a anterior 0,7 e as duas
     * mais velhas são exatas. Com k = 2 a janela vê (0,7 + 0,9) / 2 = 0,8 e erra; a recuperação vê a média de 4 (0,9).
     */
    private class Recovery(
        val tokens: List<Token>,
        val heard: List<String>,
        val c: Int,
    ) {
        val newestSimilarity = Similarity.of(heard[3], tokens[c].spoken[0][0])
    }

    private fun recovery(
        c: Int,
        size: Int,
    ): Recovery {
        val tokens = script(size)
        val heard = listOf(word(c - 3), word(c - 2), noisy(word(c - 1), 3), noisy(word(c), 1))
        return Recovery(tokens, heard, c)
    }

    private val agilLike = params(k = 2, short = 0.85, long = 0.89)

    private fun recoveryScore(r: Recovery): Double {
        // longo = 2,0: a recuperação não salta e devolve a melhor similaridade que viu
        val probe = next(r.tokens, params(k = 2, short = 0.85, long = 2.0), state(r.c - 1, missStreak = 1), r.heard)
        return probe.score
    }

    /** Q2 (L129b, K4): a recuperação salta para o token logo depois de highWater com exatamente 4 palavras ouvidas. */
    @Test
    fun recuperacao_saltaParaOTokenLogoDepoisDeHighWater() {
        val r = recovery(c = 21, size = 60)
        val score = recoveryScore(r)
        assertTrue("o caso exercita a recuperação (Q3)", score >= agilLike.longThreshold)
        val result = next(r.tokens, agilLike, state(20, missStreak = 1), r.heard)
        assertEquals(AlignState(21, 21, 0, Move.Recovery, score), result)
    }

    /** Q2 (L132a): fica vermelho se o limiar longo da recuperação passar de ">=" para ">". */
    @Test
    fun recuperacao_limiarLongoIgualPassa() {
        val r = recovery(c = 21, size = 60)
        val score = recoveryScore(r)
        val passes = next(r.tokens, params(k = 2, short = 0.85, long = score), state(20, missStreak = 1), r.heard)
        assertEquals(Move.Recovery, passes.lastMove)
        val fails = next(r.tokens, params(k = 2, short = 0.85, long = Math.nextUp(score)), state(20, missStreak = 1), r.heard)
        assertEquals(Move.Stay, fails.lastMove)
    }

    /** Q2 (L134b): fica vermelho se a palavra mais nova da recuperação passar de ">=" para ">" o limiar curto. */
    @Test
    fun recuperacao_palavraNovaIgualAoLimiarCurtoPassa() {
        val r = recovery(c = 21, size = 60)
        val score = recoveryScore(r)
        val short = r.newestSimilarity
        val passes = next(r.tokens, params(k = 2, short = short, long = score), state(20, missStreak = 1), r.heard)
        assertEquals(Move.Recovery, passes.lastMove)
        val fails = next(r.tokens, params(k = 2, short = Math.nextUp(short), long = score), state(20, missStreak = 1), r.heard)
        assertEquals(Move.Stay, fails.lastMove)
    }

    /** Q2 (L129): o token em highWater não é candidato da recuperação (só os à frente). */
    @Test
    fun recuperacao_naoOlhaOTokenEmHighWater() {
        val r = recovery(c = 21, size = 60)
        val result = next(r.tokens, agilLike, state(21, missStreak = 1), r.heard)
        assertEquals(21, result.cursor)
        assertEquals(2, result.missStreak)
        assertEquals(Move.Stay, result.lastMove)
    }

    /** Q2 (L56b): a recuperação parte de highWater, não do cursor (cursor 19, highWater 21, candidato em 20). */
    @Test
    fun recuperacao_partedeHighWaterNaoDoCursor() {
        val r = recovery(c = 20, size = 60)
        val result = next(r.tokens, agilLike, AlignState(19, 21, 1, Move.Stay, 0.0), r.heard)
        assertEquals(19, result.cursor)
        assertEquals(21, result.highWater)
        assertEquals(2, result.missStreak)
        assertEquals(Move.Stay, result.lastMove)
    }

    /** Q2 (L129c): o último token do roteiro também é candidato da recuperação. */
    @Test
    fun recuperacao_incluiOUltimoToken() {
        val r = recovery(c = 29, size = 30)
        val result = next(r.tokens, agilLike, state(28, missStreak = 1), r.heard)
        assertEquals(29, result.cursor)
        assertEquals(Move.Recovery, result.lastMove)
    }

    /** Q2 (D12): cauda de exatamente 4 palavras já dispara a recuperação, e de 5 ou mais também (só as 4 últimas valem). */
    @Test
    fun recuperacao_usaAsQuatroUltimasPalavras() {
        val r = recovery(c = 21, size = 60)
        val longa = listOf("qqqqqqqqqq", "rrrrrrrrrr") + r.heard
        val result = next(r.tokens, agilLike, state(20, missStreak = 1), longa)
        assertEquals(Move.Recovery, result.lastMove)
        assertEquals(21, result.cursor)
    }
}
