package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizerTest {
    @Test
    fun normalize_minusculasSemAcentosSemPontuacao_palavrasDoT01() {
        assertEquals("as", Normalizer.normalize("Às"))
        assertEquals("e", Normalizer.normalize("É"))
        assertEquals("conversa", Normalizer.normalize("conversa."))
        assertEquals("comeca", Normalizer.normalize("começa"))
        assertEquals("dificil", Normalizer.normalize("difícil,"))
        assertEquals("ouvir bem e uma habilidade", Normalizer.normalize("Ouvir bem é uma habilidade"))
    }

    @Test
    fun normalize_palavrasDoT09ESemPontuacaoSolta() {
        assertEquals("e entao", Normalizer.normalize("E então?"))
        assertEquals("so esperei", Normalizer.normalize("Só esperei."))
        assertEquals("nao e facil", Normalizer.normalize("Não é fácil,"))
        assertEquals("", Normalizer.normalize(" — … "))
    }

    @Test
    fun normalize_roteiroEFalaDoMesmoJeito() {
        assertEquals(Normalizer.normalize("três"), Normalizer.normalize("Três"))
        assertEquals("tres", Normalizer.normalize("TRÊS"))
        assertEquals("escolha", Normalizer.normalize("ESCOLHA"))
    }

    @Test
    fun normalize_hifenEApostrofoUnemAsPartes_D18() {
        assertEquals("bomdia", Normalizer.normalize("bom-dia"))
        assertEquals("escrevelos", Normalizer.normalize("escrevê-los"))
        assertEquals("dagua", Normalizer.normalize("d'água"))
    }

    @Test
    fun normalize_eIdempotenteEmTodosOsTextos() {
        for (name in TestTexts.names) {
            val once = Normalizer.normalize(TestTexts.read(name))
            assertEquals(name, once, Normalizer.normalize(once))
        }
    }

    @Test
    fun words_separaPorEspacoEDescartaVazios() {
        assertEquals(listOf("isso", "tem", "um", "custo"), Normalizer.words("  Isso tem um custo.  "))
        assertEquals(emptyList<String>(), Normalizer.words("—"))
    }
}
