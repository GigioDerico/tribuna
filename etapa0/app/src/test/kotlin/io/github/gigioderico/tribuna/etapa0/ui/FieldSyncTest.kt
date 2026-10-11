package io.github.gigioderico.tribuna.etapa0.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** OPR-029: digitar rápido não perde caracteres com o estado do controlador atualizando depois. */
class FieldSyncTest {
    @Test
    fun ecosAtrasados_naoApagamOQueFoiDigitadoDepois() {
        val sync = FieldSync("")
        sync.typed("a")
        sync.typed("ab")
        sync.typed("abc")
        assertFalse(sync.received("a"))
        assertEquals("abc", sync.value)
        assertFalse(sync.received("ab"))
        assertEquals("abc", sync.value)
        assertFalse(sync.received("abc"))
        assertEquals("abc", sync.value)
    }

    @Test
    fun estadoConflado_soComOUltimoTexto_naoMudaOCampo() {
        val sync = FieldSync("")
        sync.typed("a")
        sync.typed("ab")
        assertFalse(sync.received("ab"))
        assertEquals("ab", sync.value)
        // O eco de "a" nunca chegou; o que sobrou não pode virar texto externo mais tarde.
        assertTrue(sync.received("T01"))
        assertEquals("T01", sync.value)
    }

    @Test
    fun textoQueNaoVeioDoCampo_substituiOCampo() {
        val sync = FieldSync("antigo")
        assertTrue(sync.received("texto do chip"))
        assertEquals("texto do chip", sync.value)
        assertFalse(sync.received("texto do chip"))
    }

    @Test
    fun textoDoChipDepoisDeDigitar_substituiOCampo_eEcosVelhosNaoVoltam() {
        val sync = FieldSync("")
        sync.typed("a")
        assertFalse(sync.received("a"))
        assertTrue(sync.received("texto do chip"))
        assertEquals("texto do chip", sync.value)
        sync.typed("texto do chipx")
        assertFalse(sync.received("texto do chipx"))
        assertEquals("texto do chipx", sync.value)
    }

    @Test
    fun apagarEDigitarDeNovo_ecosRepetidosSaoConsumidosEmOrdem() {
        val sync = FieldSync("")
        sync.typed("a")
        sync.typed("")
        sync.typed("a")
        assertFalse(sync.received("a"))
        assertFalse(sync.received(""))
        assertEquals("a", sync.value)
        assertFalse(sync.received("a"))
        assertEquals("a", sync.value)
    }

    @Test
    fun valorInicial_eOTextoDoEstado() {
        val sync = FieldSync("já carregado")
        assertEquals("já carregado", sync.value)
        assertFalse(sync.received("já carregado"))
    }
}
