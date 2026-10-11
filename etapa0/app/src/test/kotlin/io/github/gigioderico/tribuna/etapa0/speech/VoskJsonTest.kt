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

    @Test
    fun espacosAoRedorDosDoisPontos_variam() {
        assertEquals(listOf("ouvir"), VoskJson.words("{\"partial\":\"ouvir\"}"))
        assertEquals(listOf("ouvir", "bem"), VoskJson.words("{\"text\":\"ouvir bem\"}"))
        assertEquals(listOf("ouvir"), VoskJson.words("{\"partial\"  :  \"ouvir\"}"))
        assertEquals(listOf("ouvir"), VoskJson.words("{\"text\"\n:\t\"ouvir\"}"))
    }

    @Test
    fun espacosRepetidosNoTexto_naoViramPalavraVazia() {
        assertEquals(listOf("a", "b"), VoskJson.words("{\"text\" : \" a  b \"}"))
    }

    @Test
    fun escapesDeUmCaractere_saoDecodificados() {
        assertEquals(listOf("a\nb"), VoskJson.words("{\"text\" : \"a\\nb\"}"))
        assertEquals(listOf("a\tb"), VoskJson.words("{\"text\" : \"a\\tb\"}"))
        assertEquals(listOf("a\rb"), VoskJson.words("{\"text\" : \"a\\rb\"}"))
        assertEquals(listOf("a\bb"), VoskJson.words("{\"text\" : \"a\\bb\"}"))
        assertEquals(listOf("a\u000Cb"), VoskJson.words("{\"text\" : \"a\\fb\"}"))
        assertEquals(listOf("a\"b"), VoskJson.words("{\"text\" : \"a\\\"b\"}"))
        assertEquals(listOf("a\\b"), VoskJson.words("{\"text\" : \"a\\\\b\"}"))
        assertEquals(listOf("a/b"), VoskJson.words("{\"text\" : \"a\\/b\"}"))
    }

    @Test
    fun jsonCortado_naoLancaEDevolveListaVazia() {
        assertEquals(emptyList<String>(), VoskJson.words("{\"text\" : \"abc"))
        assertEquals(emptyList<String>(), VoskJson.words("{\"text\" : \"abc\\"))
        assertEquals(emptyList<String>(), VoskJson.words("{\"text\" : \"\\u00e"))
        assertEquals(emptyList<String>(), VoskJson.words("{\"text\" : \"\\u00"))
    }

    @Test
    fun aspasDeFechamentoComoUltimoCaractere_aindaFecham() {
        assertEquals(listOf("abc"), VoskJson.words("{\"text\" : \"abc\""))
    }

    @Test
    fun gramatica_controleEEspaco_naoViramEscape() {
        assertEquals("[\"a b\"]", VoskJson.grammar(listOf("a b")))
        assertEquals("[\"\\u0001\\u001f\"]", VoskJson.grammar(listOf("\u0001\u001f")))
        assertEquals("[]", VoskJson.grammar(emptyList()))
    }
}
