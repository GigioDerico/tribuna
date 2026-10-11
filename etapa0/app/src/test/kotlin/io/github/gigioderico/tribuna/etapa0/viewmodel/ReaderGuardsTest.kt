package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.AlignPresets
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guardas e valores do controlador, um por um (varredura de quebras da rodada 2; CLAUDE.md, regra 6 e 7). */
class ReaderGuardsTest {
    @Test
    fun semNivelMedidoAinda_comLimiarLigado_parcialEIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            assertEquals(0, h.state.cursor)
            assertEquals(emptyList<String>(), h.state.debug.tail)
        }

    @Test
    fun limiarDeNivel_ficaEntreZeroEUm() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setLevelThreshold(5f)
            h.settle()
            assertEquals(1f, h.state.settings.levelThreshold, 0f)
            h.controller.setLevelThreshold(-1f)
            h.settle()
            assertEquals(0f, h.state.settings.levelThreshold, 0f)
            h.controller.setLevelThreshold(0.3f)
            h.settle()
            assertEquals(0.3f, h.state.settings.levelThreshold, 0f)
        }

    @Test
    fun duracaoDaAnimacao_ficaNaFaixaDoR5() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setAnimationMs(10)
            h.settle()
            assertEquals(150, h.state.settings.animationMs)
            h.controller.setAnimationMs(9_999)
            h.settle()
            assertEquals(600, h.state.settings.animationMs)
            h.controller.setAnimationMs(200)
            h.settle()
            assertEquals(200, h.state.settings.animationMs)
        }

    @Test
    fun ultimoParcialEUltimoFinal_vaoCadaUmParaOSeuCampo() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            assertEquals(listOf("ouvir", "bem"), h.state.debug.lastPartial)
            assertEquals(emptyList<String>(), h.state.debug.lastFinal)
            h.final("ouvir", "bem", "é")
            assertEquals(listOf("ouvir", "bem", "é"), h.state.debug.lastFinal)
            assertEquals(listOf("ouvir", "bem"), h.state.debug.lastPartial)
        }

    @Test
    fun parcialSemPalavraNova_naoPublicaDeNovoOAlinhador() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            assertEquals(Move.Reposition, h.state.debug.lastMove)
            h.partial("ouvir")
            assertEquals(Move.Reposition, h.state.debug.lastMove)
            assertEquals(null, h.state.debug.latency2Ms)
            h.partial("ouvir", "bem")
            assertTrue(h.state.debug.lastMove != Move.Reposition)
        }

    @Test
    fun nivelDoMicrofone_vaiParaOPainelEParaOArquivoDeNivel() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.fake.emitLevel(0.25f)
            h.settle()
            assertEquals(0.25f, h.state.debug.level, 0f)
            h.export()
            assertEquals(listOf("0,0.250000"), h.rows("T01.level.csv"))
        }

    @Test
    fun erroDeSessaoAtivaEDeSessaoAntiga_entramNaExportacaoComAccepted() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.listen()
            h.fake.recognizers
                .first()
                .error(EngineError.Internal)
            h.fake.recognizers
                .last()
                .error(EngineError.Internal)
            h.settle()
            h.export()
            assertEquals(
                listOf("0,1,error,\"Internal\",0", "0,2,error,\"Internal\",1"),
                h.rows("T01.hypotheses.csv"),
            )
        }

    @Test
    fun textoVazio_ouvirNaoFazNada() =
        runTest {
            val h = ReaderHarness(this)
            h.listen()
            assertEquals(ReaderPhase.Idle, h.state.phase)
            assertEquals(emptyList<String>(), h.engine.calls)
        }

    @Test
    fun pausarForaDaLeitura_naoFazNada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.pause()
            assertEquals(ReaderPhase.Idle, h.state.phase)
            assertEquals(emptyList<String>(), h.engine.calls)
            h.listen()
            h.pause()
            h.pause()
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertEquals(1, h.engine.calls.count { it.startsWith("endSession") })
            h.export()
            assertEquals(1, h.rows("T01.events.csv").count { it.contains("pausa_toque") })
        }

    @Test
    fun pausarEnquantoOModeloCarrega_eIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.pause()
            h.controller.onStopReading()
            h.settle()
            assertEquals(ReaderPhase.Preparing, h.state.phase)
            gate.complete(Unit)
            h.settle()
            assertEquals(ReaderPhase.Reading, h.state.phase)
        }

    @Test
    fun parar_fechaASessaoEACaptura_soUmaVez() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            val before = h.engine.calls.size
            h.controller.onStopReading()
            h.settle()
            assertEquals(listOf("stopCapture"), h.engine.calls.drop(before))
            assertEquals(ReaderPhase.Idle, h.state.phase)
            assertFalse(h.state.capturing)
            assertEquals(null, h.state.debug.activeSession)
        }

    @Test
    fun pararComASessaoAtiva_fechaASessaoAntesDaCaptura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onStopReading()
            h.settle()
            assertEquals(listOf("endSession@0", "stopCapture"), h.engine.calls.takeLast(2))
        }

    @Test
    fun trocarAFonte_recriaOMotor_masPresetEVocabularioNao() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setPreset(PresetChoice.Agil)
            h.controller.setVocabulary(true)
            h.settle()
            assertEquals(1, h.engines.size)
            assertEquals(PresetChoice.Agil, h.state.settings.preset)
            assertEquals(true, h.state.settings.vocabulary)
            h.controller.setSource(AudioChoice.Wav("T01_giorgio_sala_t1.wav"))
            h.settle()
            assertEquals(2, h.engines.size)
            assertEquals(AudioChoice.Wav("T01_giorgio_sala_t1.wav"), h.engine.config.source)
            assertEquals(RecognizerStrategy.NewPerSession, h.engine.config.strategy)
        }

    @Test
    fun motorRecriado_continuaEntregandoEventosAoControlador() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setStrategy(RecognizerStrategy.Reset)
            h.settle()
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun fecharOControlador_liberaOMotor() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.close()
            assertEquals("release", h.engine.calls.last())
        }

    @Test
    fun ajustesSoMudamComALeituraParada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.setPreset(PresetChoice.Agil)
            h.controller.setVocabulary(true)
            h.controller.setStrategy(RecognizerStrategy.Reset)
            h.settle()
            assertEquals(PresetChoice.Normal, h.state.settings.preset)
            assertEquals(false, h.state.settings.vocabulary)
            assertEquals(1, h.engines.size)
        }

    @Test
    fun falhaNaGravacao_marcaExportFailed_eSucessoLimpa() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.failWrite = true
            assertTrue(h.export().isEmpty())
            assertTrue(h.state.exportFailed)
            assertEquals(emptyList<String>(), h.state.lastExport)
            h.failWrite = false
            h.export()
            assertFalse(h.state.exportFailed)
            assertEquals(5, h.state.lastExport.size)
        }

    @Test
    fun toqueNaPalavra_gravaOIndiceNoDetalheDoEvento() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onWordTapped(7)
            h.settle()
            h.export()
            assertEquals(listOf("0,toque_palavra,7"), h.rows("T01.events.csv"))
        }

    @Test
    fun cargaTerminadaEmSegundoPlano_comTextoDeTeste_deixaAExportacaoPronta_ecomTextoLivre_nao() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.controller.onActivityStop(changingConfigurations = false)
            h.settle()
            gate.complete(Unit)
            h.settle()
            assertTrue(h.state.canExport)
            val free = ReaderHarness(this)
            free.controller.setText("texto livre que não é de teste")
            free.settle()
            val freeGate = CompletableDeferred<Unit>()
            free.engine.prepareGate = freeGate
            free.listen()
            free.controller.onActivityStop(changingConfigurations = false)
            free.settle()
            freeGate.complete(Unit)
            free.settle()
            assertEquals(ReaderPhase.Paused, free.state.phase)
            assertFalse(free.state.canExport)
        }

    @Test
    fun presetsDaTela_sao_osDoNucleo() {
        assertEquals(AlignPresets.conservador, PresetChoice.Conservador.params)
        assertEquals(AlignPresets.normal, PresetChoice.Normal.params)
        assertEquals(AlignPresets.agil, PresetChoice.Agil.params)
        assertEquals(3, PresetChoice.entries.map { it.params }.distinct().size)
    }

    @Test
    fun estadoInicial_temOsValoresPadraoDaEtapa0() {
        val state = ReaderUiState()
        assertEquals("", state.text)
        assertEquals(0, state.cursor)
        assertEquals(0L, state.cursorAtMs)
        assertEquals(ReaderPhase.Idle, state.phase)
        assertEquals(false, state.capturing)
        assertEquals(null, state.testTextId)
        assertEquals(false, state.canExport)
        assertEquals(false, state.exportFailed)
        assertEquals(null, state.error)
        val settings = state.settings
        assertEquals(PresetChoice.Normal, settings.preset)
        assertEquals(false, settings.vocabulary)
        assertEquals(RecognizerStrategy.NewPerSession, settings.strategy)
        assertEquals(AudioChoice.Microphone, settings.source)
        assertEquals(0f, settings.levelThreshold, 0f)
        assertEquals(375, settings.animationMs)
        assertEquals(150, DebugSettings.ANIMATION_MIN_MS)
        assertEquals(600, DebugSettings.ANIMATION_MAX_MS)
        assertEquals(375, DebugSettings.ANIMATION_DEFAULT_MS)
        val debug = state.debug
        assertEquals(0.0, debug.score, 0.0)
        assertEquals(null, debug.lastMove)
        assertEquals(0, debug.highWater)
        assertEquals(0, debug.missStreak)
        assertEquals(null, debug.activeSession)
        assertEquals(io.github.gigioderico.tribuna.etapa0.speech.EngineState.Idle, debug.engineState)
        assertEquals(0f, debug.level, 0f)
        assertEquals(null, debug.latency2Ms)
        assertEquals(null, debug.latency3Ms)
        assertEquals(null, debug.vocabularySize)
    }

    @Test
    fun capturaPerdidaComOMotorJaEmReady_naoPedeStopCaptureDeNovo() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            val before = h.engine.calls.toList()
            h.engine.loseCapture(EngineError.MicUnavailable)
            h.settle()
            assertEquals(before, h.engine.calls)
            assertFalse(h.state.capturing)
            assertEquals(EngineError.MicUnavailable, h.state.error)
            assertEquals(ReaderPhase.Paused, h.state.phase)
        }

    @Test
    fun erroDeSessaoAntigaSemSessaoAtiva_eIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.fake.recognizers
                .single()
                .error(EngineError.Internal)
            h.settle()
            assertEquals(null, h.state.error)
            assertEquals(ReaderPhase.Paused, h.state.phase)
            h.export()
            assertEquals(listOf("0,1,error,\"Internal\",0"), h.rows("T01.hypotheses.csv"))
        }

    @Test
    fun parcialDeSessaoAntigaSemSessaoAtiva_eIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            val old = h.fake.recognizers.single()
            old.partial(listOf("ouvir", "bem"))
            old.final(listOf("ouvir", "bem", "é"))
            h.settle()
            assertEquals(0, h.state.cursor)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            h.export()
            assertEquals(
                listOf("0,1,partial,\"ouvir bem\",0", "0,1,final,\"ouvir bem é\",0"),
                h.rows("T01.hypotheses.csv"),
            )
        }

    @Test
    fun recuoSeguidoDeRuido_naoGravaTrajetoriaNemRenovaOInstanteDoCursor() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.clockSkew = 100
            h.final("ouvir", "bem", "é", "uma", "habilidade", "que")
            h.final("bem", "é", "uma")
            assertEquals(3, h.state.cursor)
            assertEquals(5, h.state.debug.highWater)
            h.export()
            val rows = h.rows("T01.trajectory.csv").size
            h.clockSkew = 500
            h.final("zzz")
            assertEquals(3, h.state.cursor)
            assertEquals(5, h.state.debug.highWater)
            h.export()
            assertEquals(rows, h.rows("T01.trajectory.csv").size)
            h.clockSkew = 900
            h.controller.onScrollSettled(3)
            h.settle()
            assertEquals(800L, h.state.debug.latency3Ms)
        }
}
