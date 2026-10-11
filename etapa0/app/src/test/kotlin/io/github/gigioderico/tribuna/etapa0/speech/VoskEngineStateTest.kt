package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** C8 estados e erros (D15) e C9 modelo carregado uma vez (TRD §Instalação do modelo 4). */
class VoskEngineStateTest {
    @Test
    fun sequenciaDeEstados() =
        runTest {
            val seen = mutableListOf<EngineState>()
            lateinit var h: EngineHarness
            val factory = FakeDecoderFactory()
            h =
                EngineHarness(
                    this,
                    factory = factory,
                    loader =
                        ModelLoader {
                            seen += h.engine.state.value
                            factory
                        },
                )
            seen += h.engine.state.value
            h.engine.prepare()
            seen += h.engine.state.value
            h.engine.startCapture()
            seen += h.engine.state.value
            h.engine.beginSession()
            seen += h.engine.state.value
            h.engine.endSession()
            seen += h.engine.state.value
            h.engine.stopCapture()
            seen += h.engine.state.value
            assertEquals(
                listOf(
                    EngineState.Idle,
                    EngineState.Preparing,
                    EngineState.Ready,
                    EngineState.Capturing,
                    EngineState.Recognizing,
                    EngineState.Capturing,
                    EngineState.Ready,
                ),
                seen,
            )
        }

    @Test
    fun falhaAoAbrirAFonte_geraMicUnavailableSemSessao() =
        runTest {
            val h = EngineHarness(this, source = FakeAudioSource(openError = EngineError.MicUnavailable))
            h.engine.prepare()
            h.engine.startCapture()
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.MicUnavailable)), h.events)
            assertEquals(EngineState.Ready, h.engine.state.value)
        }

    @Test
    fun semPermissao_geraPermissionRevokedSemSessao() =
        runTest {
            val h = EngineHarness(this, source = FakeAudioSource(openError = EngineError.PermissionRevoked))
            h.engine.prepare()
            h.engine.startCapture()
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.PermissionRevoked)), h.events)
        }

    @Test
    fun permissaoPerdidaDuranteACaptura_geraPermissionRevoked_eFechaACaptura() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.source.readError = EngineError.PermissionRevoked
            h.driver.pump()
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.PermissionRevoked)), h.recognitionEvents())
            assertEquals(EngineState.Ready, h.engine.state.value)
            assertTrue(
                h.factory.created
                    .single()
                    .closed,
            )
            assertEquals(1, h.source.closed)
        }

    @Test
    fun excecaoDoReconhecedor_geraErroInternoDaSessao() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val id = h.engine.beginSession()
            h.factory.created
                .single()
                .failOnAccept = true
            h.feed()
            assertEquals(listOf(SpeechEvent.Error(id, EngineError.Internal)), h.recognitionEvents())
        }

    @Test
    fun modeloAusente_prepareFalha_comFailedEModelMissing() =
        runTest {
            val h = EngineHarness(this, loader = ModelLoader { throw ModelLoadException(EngineError.ModelMissing, "ausente") })
            val result = h.engine.prepare()
            assertTrue(result.isFailure)
            assertEquals(EngineError.ModelMissing, (result.exceptionOrNull() as ModelLoadException).error)
            assertEquals(EngineState.Failed, h.engine.state.value)
        }

    @Test
    fun stopCapture_paraOLaco_eFechaAFonte() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.engine.stopCapture()
            assertEquals(false, h.driver.pump())
            assertEquals(1, h.source.closed)
            assertEquals(1, h.driver.stops)
            assertTrue(
                h.factory.created
                    .single()
                    .closed,
            )
        }

    @Test
    fun duasChamadasDePrepare_carregamOModeloUmaVez() =
        runTest {
            var loads = 0
            val factory = FakeDecoderFactory()
            val shared =
                CachingModelLoader {
                    loads++
                    factory
                }
            val a = EngineHarness(this, factory = factory, loader = shared)
            a.engine.prepare()
            a.engine.prepare()
            val b = EngineHarness(this, factory = factory, loader = shared)
            b.engine.prepare()
            assertEquals(1, loads)
            assertEquals(EngineState.Ready, b.engine.state.value)
        }

    @Test
    fun falhaDeCargaNaoFicaEmCache_tentarDeNovoCarrega() =
        runTest {
            var loads = 0
            val factory = FakeDecoderFactory()
            val shared =
                CachingModelLoader {
                    loads++
                    if (loads == 1) throw ModelLoadException(EngineError.ModelCorrupt, "falhou")
                    factory
                }
            val h = EngineHarness(this, factory = factory, loader = shared)
            assertTrue(h.engine.prepare().isFailure)
            assertTrue(h.engine.prepare().isSuccess)
            assertEquals(2, loads)
        }

    @Test
    fun prepareComOMotorAberto_naoRecarregaNemMudaOEstado() =
        runTest {
            var loads = 0
            val factory = FakeDecoderFactory()
            val h =
                EngineHarness(
                    this,
                    factory = factory,
                    loader =
                        ModelLoader {
                            loads++
                            factory
                        },
                )
            h.open()
            h.engine.beginSession()
            assertTrue(h.engine.prepare().isSuccess)
            assertEquals(1, loads)
            assertEquals(EngineState.Recognizing, h.engine.state.value)
        }
}
