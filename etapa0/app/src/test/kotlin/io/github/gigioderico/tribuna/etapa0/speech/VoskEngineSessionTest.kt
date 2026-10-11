package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do contrato do SpeechEngine no VoskEngine (TRD §Contratos; Motor de voz §Captura e sessões):
 * C1 sessão presa ao reconhecedor, C2 fora de sessão, C3 endSession, C4 estratégias, C5 vocabulário.
 */
class VoskEngineSessionTest {
    @Test
    fun todoEventoDoReconhecedor_levaOIdDele() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val id = h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.partial = "{\"partial\" : \"ouvir bem\"}"
            h.feed()
            decoder.endOfUtterance = true
            decoder.result = "{\"text\" : \"ouvir bem e uma\"}"
            h.feed()
            decoder.failOnAccept = true
            h.feed()
            assertEquals(
                listOf(
                    SpeechEvent.Partial(id, listOf("ouvir", "bem")),
                    SpeechEvent.Final(id, listOf("ouvir", "bem", "e", "uma")),
                    SpeechEvent.Error(id, EngineError.Internal),
                ),
                h.recognitionEvents(),
            )
        }

    @Test
    fun resultadoProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo() =
        runTest {
            // C1 (Q2): o reconhecedor antigo ainda processa o bloco quando a sessão troca; o evento sai com o id
            // dele. Se o código ler a sessão atual do motor na hora de emitir, sai o id novo e o teste falha.
            val h = EngineHarness(this)
            h.open()
            val first = h.engine.beginSession()
            val old = h.factory.created.single()
            old.partial = "{\"partial\" : \"ouvir bem\"}"
            var second: SessionId? = null
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                second = h.engine.beginSession()
            }
            h.feed()
            assertEquals(listOf(SpeechEvent.Partial(first, listOf("ouvir", "bem"))), h.recognitionEvents())
            assertNotEquals(first, second)
        }

    @Test
    fun reconhecedorAntigoSoELiberadoDepoisDeTerminarOBloco() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val old = h.factory.created.single()
            var closedDuringAccept = true
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                closedDuringAccept = old.closed
            }
            h.feed()
            assertFalse(closedDuringAccept)
            assertTrue(old.closed)
        }

    @Test
    fun foraDeSessao_nenhumBlocoChegaAoReconhecedor_eLevelContinua() =
        runTest {
            // C2: com a captura aberta e sem sessão, um Level por bloco lido e nenhum bloco a reconhecedor nenhum.
            val h = EngineHarness(this)
            h.open()
            repeat(3) { h.feed(block(1000 + it)) }
            assertEquals(3, h.events.filterIsInstance<SpeechEvent.Level>().size)
            assertTrue(h.factory.created.isEmpty())

            h.engine.beginSession()
            h.feed()
            h.engine.endSession()
            val decoder = h.factory.created.single()
            repeat(2) { h.feed(block(7)) }
            assertEquals(1, decoder.fed.size)
            assertEquals(6, h.events.filterIsInstance<SpeechEvent.Level>().size)
        }

    @Test
    fun blocoLidoAntesDoEndSessionENaoEntregue_naoChegaAoReconhecedor() =
        runTest {
            // C3: o endSession acontece entre a leitura do bloco e a entrega.
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            h.source.onRead = {
                h.source.onRead = null
                h.engine.endSession()
            }
            h.feed(block(42))
            assertTrue(decoder.fed.isEmpty())
            assertTrue(decoder.closed)
        }

    @Test
    fun endSession_naoEmiteFinal_eNaoPedeOResultado() =
        runTest {
            // C3 e D14: a hipótese pendente é descartada.
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.partial = "{\"partial\" : \"ouvir bem\"}"
            decoder.result = "{\"text\" : \"ouvir bem\"}"
            h.feed()
            h.engine.endSession()
            h.feed()
            assertEquals(0, decoder.resultCalls)
            assertTrue(h.recognitionEvents().none { it is SpeechEvent.Final })
            assertTrue(decoder.closed)
        }

    @Test
    fun estrategiaReset_endSessionZeraEMantemOReconhecedor() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            h.feed()
            h.engine.endSession()
            assertEquals(1, decoder.resets)
            assertFalse(decoder.closed)
        }

    @Test
    fun estrategiaNova_cadaSessaoCriaUmReconhecedor_comIdsCrescentes() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val ids =
                List(3) {
                    h.engine.beginSession().also { h.engine.endSession() }
                }
            assertEquals(3, h.factory.created.size)
            assertTrue(h.factory.created.all { it.closed })
            assertEquals(ids.sortedBy { it.value }, ids)
            assertEquals(3, ids.map { it.value }.distinct().size)
        }

    @Test
    fun estrategiaReset_reaproveitaOReconhecedor_comIdNovo() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            val first = h.engine.beginSession(listOf("ouvir", "[unk]"))
            h.engine.endSession()
            val second = h.engine.beginSession(listOf("ouvir", "[unk]"))
            assertEquals(1, h.factory.created.size)
            assertTrue(second.value > first.value)
            val decoder = h.factory.created.single()
            decoder.partial = "{\"partial\" : \"ouvir\"}"
            h.feed()
            assertEquals(listOf(SpeechEvent.Partial(second, listOf("ouvir"))), h.recognitionEvents())
        }

    @Test
    fun estrategiaReset_vocabularioMudou_criaReconhecedorNovo() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession(listOf("ouvir", "[unk]"))
            h.engine.endSession()
            h.engine.beginSession(null)
            assertEquals(2, h.factory.created.size)
            assertTrue(h.factory.created[0].closed)
            assertEquals(null, h.factory.created[1].vocabulary)
        }

    @Test
    fun vocabulario_vaiParaOReconhecedorCriado_esemListaSemVocabulario() =
        runTest {
            // C5: beginSession(vocabulary) passa a lista; desligado, o reconhecedor é criado sem lista.
            val h = EngineHarness(this)
            h.open()
            val words = listOf("ouvir", "bem", "[unk]")
            h.engine.beginSession(words)
            h.engine.endSession()
            h.engine.beginSession()
            assertEquals(words, h.factory.created[0].vocabulary)
            assertEquals(null, h.factory.created[1].vocabulary)
        }

    @Test
    fun reconhecedorQueNaoCria_emiteErroInternoDaSessao() =
        runTest {
            val failing =
                object : DecoderFactory {
                    override val info = FakeDecoderFactory().info

                    override fun create(vocabulary: List<String>?): Decoder = throw java.io.IOException("Failed to create a recognizer")
                }
            val h = EngineHarness(this, loader = ModelLoader { failing })
            h.open()
            val id = h.engine.beginSession()
            assertEquals(listOf(SpeechEvent.Error(id, EngineError.Internal)), h.recognitionEvents())
        }

    @Test
    fun finalProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val first = h.engine.beginSession()
            val old = h.factory.created.single()
            old.endOfUtterance = true
            old.result = "{\"text\" : \"ouvir bem\"}"
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                h.engine.beginSession()
            }
            h.feed()
            assertEquals(listOf(SpeechEvent.Final(first, listOf("ouvir", "bem"))), h.recognitionEvents())
        }

    @Test
    fun reconhecedorComExcecao_naoRecebeMaisAudio() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val id = h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.failOnAccept = true
            h.feed()
            h.feed()
            assertEquals(1, decoder.fed.size)
            assertEquals(listOf(SpeechEvent.Error(id, EngineError.Internal)), h.recognitionEvents())
            assertTrue(decoder.closed)
        }

    @Test
    fun stopCaptureDuranteALeitura_descartaOBloco() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.source.onRead = {
                h.source.onRead = null
                h.engine.stopCapture()
            }
            h.feed()
            assertTrue(h.events.isEmpty())
            assertEquals(1, h.source.closed)
        }

    @Test
    fun estrategiaReset_sessaoAntigaAtrasada_guardaSoOReconhecedorMaisNovo() =
        runTest {
            // A sessão A termina durante um bloco (liberação adiada); B cria outro reconhecedor e termina antes.
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val first = h.factory.created.single()
            first.onAccept = {
                first.onAccept = null
                h.engine.endSession()
                h.engine.beginSession()
                h.engine.endSession()
            }
            h.feed()
            val second = h.factory.created[1]
            assertTrue(first.closed)
            assertFalse(second.closed)
            h.engine.beginSession()
            h.feed()
            assertEquals(2, h.factory.created.size)
            assertEquals(1, second.fed.size)
        }

    @Test
    fun releaseDuranteUmBloco_fechaOReconhecedorQuandoOBlocoTermina() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.onAccept = {
                decoder.onAccept = null
                h.engine.release()
            }
            h.feed()
            assertTrue(decoder.closed)
        }

    @Test
    fun erroDoReconhecedorComSessaoTrocadaNoMeioDoBloco_saiComOIdAntigo() =
        runTest {
            // C1 (Q2, rodada 2): o accept lança depois que a sessão trocou; o Error sai com o id do reconhecedor
            // que falhou, nunca com o da sessão atual do motor nem com o último id emitido.
            val h = EngineHarness(this)
            h.open()
            val first = h.engine.beginSession()
            val old = h.factory.created.single()
            old.failOnAccept = true
            var second: SessionId? = null
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                second = h.engine.beginSession()
            }
            h.feed()
            assertEquals(listOf(SpeechEvent.Error(first, EngineError.Internal)), h.recognitionEvents())
            assertNotEquals(first, second)
            assertTrue(old.closed)
        }

    @Test
    fun release_fechaOReconhecedorGuardadoDaEstrategiaReset() =
        runTest {
            // OPR-028: sem isto, cada troca de estratégia ou de fonte vaza um Recognizer nativo.
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            h.engine.endSession()
            val decoder = h.factory.created.single()
            assertFalse(decoder.closed)
            h.engine.release()
            assertTrue(decoder.closed)
        }
}
