package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Test

/** C6: JSON do Vosk → palavras, nos formatos da documentação do `org.vosk.Recognizer` 0.3.75. */
class VoskJsonTest {
    @Test
    fun parcial_viraPalavras() {
        assertEquals(listOf("ouvir", "bem"), VoskJson.words("{\"partial\" : \"ouvir bem\"}"))
    }

    @Test
    fun resultado_viraPalavras() {
        assertEquals(listOf("ouvir", "bem", "e", "uma"), VoskJson.words("{\"text\" : \"ouvir bem e uma\"}"))
    }

    @Test
    fun textoVazio_viraListaVazia() {
        assertEquals(emptyList<String>(), VoskJson.words("{\"partial\" : \"\"}"))
        assertEquals(emptyList<String>(), VoskJson.words("{\"text\" : \"\"}"))
        assertEquals(emptyList<String>(), VoskJson.words(""))
    }

    @Test
    fun jsonComQuebrasDeLinha_eAcentos() {
        assertEquals(listOf("ouvir", "é", "três"), VoskJson.words("{\n  \"partial\" : \"ouvir é três\"\n}"))
    }

    @Test
    fun escapesUnicode_saoDecodificados() {
        assertEquals(listOf("é", "três"), VoskJson.words("{\"text\" : \"\\u00e9 tr\\u00eas\"}"))
    }

    @Test
    fun resultadoComPalavrasComTempo_usaOTexto() {
        val json = "{\"result\" : [{\"conf\" : 1.0, \"word\" : \"ouvir\"}], \"text\" : \"ouvir\"}"
        assertEquals(listOf("ouvir"), VoskJson.words(json))
    }

    @Test
    fun gramatica_eUmArrayComUmaFrasePorPalavra() {
        assertEquals("[\"ouvir\",\"três\",\"[unk]\"]", VoskJson.grammar(listOf("ouvir", "três", "[unk]")))
    }

    @Test
    fun gramatica_escapaAspasEBarra() {
        assertEquals("[\"a\\\"b\",\"c\\\\d\"]", VoskJson.grammar(listOf("a\"b", "c\\d")))
    }
}
