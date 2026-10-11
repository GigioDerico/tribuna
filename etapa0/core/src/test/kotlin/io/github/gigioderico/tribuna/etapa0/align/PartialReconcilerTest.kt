package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Test

/** Suíte "Reconciliação de hipóteses" (TRD Testes; Motor de voz §Reconciliação de hipóteses). Frases de etapa0/textos/. */
class PartialReconcilerTest {
    private val r = PartialReconciler()

    private fun w(text: String) = if (text.isEmpty()) emptyList() else text.split(' ')

    private fun p(text: String) = r.onPartial(w(text))

    private fun f(text: String) = r.onFinal(w(text))

    @Test
    fun parciaisQueSoCrescem_entregamSoOPrefixoEstavelETotalIgualAoFinal() {
        val out = mutableListOf<String>()
        out += p("ouvir").also { assertEquals(emptyList<String>(), it) }
        out += p("ouvir bem").also { assertEquals(w("ouvir"), it) }
        out += p("ouvir bem é uma").also { assertEquals(w("bem"), it) }
        out += f("ouvir bem é uma habilidade").also { assertEquals(w("é uma habilidade"), it) }
        assertEquals(w("ouvir bem é uma habilidade"), out)
    }

    @Test
    fun correcaoNoComecoDaFrase_naoRepeteNemRetira() {
        val out = mutableListOf<String>()
        out += p("a")
        out += p("a maioria")
        out += p("a maioria de")
        out += p("a maioria de nós")
        assertEquals(w("a maioria de"), out)
        out += p("a maior parte de nós").also { assertEquals(emptyList<String>(), it) }
        out += p("a maior parte de nós escuta").also { assertEquals(w("nós"), it) }
        out += f("a maior parte de nós escuta apenas").also { assertEquals(w("escuta apenas"), it) }
        assertEquals(w("a maioria de nós escuta apenas"), out)
    }

    @Test
    fun insercaoNoComecoDaFrase_entregaSoODepoisDoPontoAlinhado() {
        p("ouvir bem")
        assertEquals(w("ouvir bem"), p("ouvir bem é"))
        assertEquals(w("é"), p("ouvir bem é uma"))
        assertEquals(emptyList<String>(), p("então ouvir bem é uma"))
        assertEquals(w("uma"), p("então ouvir bem é uma habilidade"))
    }

    @Test
    fun remocaoNoComecoDaFrase_entregaSoODepoisDoPontoAlinhado() {
        p("a maioria de")
        assertEquals(w("a maioria de"), p("a maioria de nós"))
        assertEquals(emptyList<String>(), p("maioria de nós escuta"))
        assertEquals(w("nós escuta"), p("maioria de nós escuta apenas"))
    }

    @Test
    fun finalComMaisPalavras_entregaOQueFaltaEZera() {
        p("ouvir bem")
        assertEquals(w("ouvir bem"), p("ouvir bem é"))
        assertEquals(w("é uma habilidade"), f("ouvir bem é uma habilidade"))
        assertEquals(emptyList<String>(), p("que"))
        assertEquals(w("que"), p("que quase"))
    }

    @Test
    fun finalComMenosPalavras_naoRetiraNada() {
        p("ouvir bem é uma")
        assertEquals(w("ouvir bem é uma"), p("ouvir bem é uma habilidade"))
        assertEquals(emptyList<String>(), f("ouvir bem é"))
        assertEquals(emptyList<String>(), p("que quase"))
    }

    @Test
    fun finalComPalavraTrocada_entregaSoODepois() {
        p("ouvir vem é")
        assertEquals(w("ouvir vem é"), p("ouvir vem é uma"))
        assertEquals(w("uma habilidade"), f("ouvir bem é uma habilidade"))
    }

    @Test
    fun fraseRepetida_mesmoFinalDuasVezesEntregaNasDuas_T08() {
        assertEquals(w("isso tem um custo"), f("isso tem um custo"))
        assertEquals(w("isso tem um custo"), f("isso tem um custo"))
        p("isso tem")
        assertEquals(w("isso tem"), p("isso tem um"))
        assertEquals(w("um custo"), f("isso tem um custo"))
    }

    @Test
    fun parcialVazioEPrimeiroParcial_naoEntregamNada() {
        assertEquals(emptyList<String>(), p(""))
        assertEquals(emptyList<String>(), p("ouvir bem"))
        assertEquals(emptyList<String>(), p(""))
        assertEquals(emptyList<String>(), p("ouvir"))
    }

    @Test
    fun reset_sessaoNovaNaoComparaComHipotesesAnteriores() {
        p("ouvir bem")
        assertEquals(w("ouvir bem"), p("ouvir bem é"))
        r.reset()
        assertEquals(emptyList<String>(), p("ouvir bem é"))
        assertEquals(w("ouvir bem é"), p("ouvir bem é uma"))
    }
}
