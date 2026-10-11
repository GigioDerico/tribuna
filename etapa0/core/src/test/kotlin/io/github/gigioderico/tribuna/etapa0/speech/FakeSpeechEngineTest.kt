package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSpeechEngineTest {
    // Palavras de etapa0/textos/T01.txt (CLAUDE.md: textos de teste só de etapa0/textos/).
    private val ouvirBem = listOf("ouvir", "bem")
    private val habilidade = listOf("habilidade")

    private suspend fun capturing(): FakeSpeechEngine =
        FakeSpeechEngine().also {
            it.prepare()
            it.startCapture()
        }

    @Test
    fun prepare_sucesso_vaiDeIdleParaReady() =
        runTest {
            val fake = FakeSpeechEngine()
            assertEquals(EngineState.Idle, fake.state.value)
            assertTrue(fake.prepare().isSuccess)
            assertEquals(EngineState.Ready, fake.state.value)
        }

    @Test
    fun prepare_falha_vaiParaFailed_eDevolveAFalha() =
        runTest {
            val cause = IllegalStateException("modelo ausente")
            val fake = FakeSpeechEngine(prepareResult = Result.failure(cause))
            assertEquals(cause, fake.prepare().exceptionOrNull())
            assertEquals(EngineState.Failed, fake.state.value)
        }

    @Test
    fun cicloDeVida_capturaESessao_atualizamOEstado() =
        runTest {
            val fake = FakeSpeechEngine()
            fake.prepare()
            fake.startCapture()
            assertEquals(EngineState.Capturing, fake.state.value)
            fake.beginSession()
            assertEquals(EngineState.Recognizing, fake.state.value)
            fake.endSession()
            assertEquals(EngineState.Capturing, fake.state.value)
            fake.stopCapture()
            assertEquals(EngineState.Ready, fake.state.value)
        }

    @Test
    fun release_vaiParaIdle_eEncerraOFluxoDeEventos() =
        runTest {
            val fake = capturing()
            fake.emitLevel(0.2f)
            fake.release()
            assertEquals(EngineState.Idle, fake.state.value)
            assertEquals(listOf<SpeechEvent>(SpeechEvent.Level(0.2f)), fake.events.toList())
        }

    @Test
    fun sessionId_cresceACadaBeginSession_eNuncaSeRepete() =
        runTest {
            val fake = capturing()
            val ids =
                List(3) {
                    fake.beginSession().also { fake.endSession() }
                }
            assertTrue(ids.zipWithNext().all { (antes, depois) -> depois.value > antes.value })
            assertEquals(3, ids.toSet().size)
        }

    @Test
    fun beginSession_guardaOVocabularioNoReconhecedorDaSessao() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession(vocabulary = listOf("ouvir", "bem", "[unk]"))
            val recognizer = fake.recognizers.last()
            assertEquals(id, recognizer.session)
            assertEquals(listOf("ouvir", "bem", "[unk]"), recognizer.vocabulary)
        }

    @Test
    fun partial_chegaComOIdDaSessaoAtiva() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession()
            fake.hearPartial(ouvirBem)
            assertEquals(SpeechEvent.Partial(id, ouvirBem), fake.events.first())
        }

    @Test
    fun final_chegaComOIdDaSessaoAtiva() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession()
            fake.hearFinal(ouvirBem)
            assertEquals(SpeechEvent.Final(id, ouvirBem), fake.events.first())
        }

    @Test
    fun level_chegaSemSessao_comACapturaAberta() =
        runTest {
            val fake = capturing()
            fake.emitLevel(0.7f)
            assertEquals(SpeechEvent.Level(0.7f), fake.events.first())
        }

    @Test
    fun error_doReconhecedorTrazASessao_eODaCapturaVemSemSessao() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession()
            fake.recognizers.last().error(EngineError.Internal)
            fake.emitCaptureError(EngineError.MicUnavailable)
            assertEquals(
                listOf(
                    SpeechEvent.Error(id, EngineError.Internal),
                    SpeechEvent.Error(null, EngineError.MicUnavailable),
                ),
                fake.events.take(2).toList(),
            )
        }

    // Spec, critério 3; alvo da quebra A (Q2).
    @Test
    fun callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo() =
        runTest {
            val fake = capturing()
            val antiga = fake.beginSession()
            val reconhecedorAntigo = fake.recognizers.last()
            fake.endSession()
            val nova = fake.beginSession()
            assertNotEquals(antiga, nova)

            reconhecedorAntigo.partial(habilidade)
            reconhecedorAntigo.final(habilidade)

            assertEquals(
                listOf(
                    SpeechEvent.Partial(antiga, habilidade),
                    SpeechEvent.Final(antiga, habilidade),
                ),
                fake.events.take(2).toList(),
            )
        }

    @Test
    fun callbackAtrasado_semSessaoAtiva_chegaComOIdAntigo() =
        runTest {
            val fake = capturing()
            val antiga = fake.beginSession()
            val reconhecedorAntigo = fake.recognizers.last()
            fake.endSession()
            reconhecedorAntigo.final(habilidade)
            assertEquals(SpeechEvent.Final(antiga, habilidade), fake.events.first())
        }

    // Spec, critério 4; alvo da quebra B (Q2). O Level serve de sentinela: se algo
    // tivesse sido emitido antes dele, `first()` devolveria esse algo.
    @Test
    fun endSession_naoEmiteMaisPartialNemFinal_eLevelContinua() =
        runTest {
            val fake = capturing()
            fake.beginSession()
            fake.endSession()
            fake.hearPartial(ouvirBem)
            fake.hearFinal(ouvirBem)
            fake.emitLevel(0.4f)
            assertEquals(SpeechEvent.Level(0.4f), fake.events.first())
            assertEquals(EngineState.Capturing, fake.state.value)
        }

    @Test
    fun level_comMicrofoneFechado_naoChega() =
        runTest {
            val fake = capturing()
            fake.stopCapture()
            fake.emitLevel(0.9f)
            fake.emitCaptureError(EngineError.PermissionRevoked)
            assertEquals(SpeechEvent.Error(null, EngineError.PermissionRevoked), fake.events.first())
        }

    @Test
    fun beginSession_semCaptura_falha() =
        runTest {
            val fake = FakeSpeechEngine()
            fake.prepare()
            assertThrows(IllegalStateException::class.java) { fake.beginSession() }
        }

    @Test
    fun beginSession_comSessaoAtiva_falha() =
        runTest {
            val fake = capturing()
            fake.beginSession()
            assertThrows(IllegalStateException::class.java) { fake.beginSession() }
        }

    @Test
    fun stopCapture_comSessaoAtiva_falha() =
        runTest {
            val fake = capturing()
            fake.beginSession()
            assertThrows(IllegalStateException::class.java) { fake.stopCapture() }
        }
}
