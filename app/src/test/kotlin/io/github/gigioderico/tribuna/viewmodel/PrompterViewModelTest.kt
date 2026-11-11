package io.github.gigioderico.tribuna.viewmodel

import io.github.gigioderico.tribuna.data.AppSettings
import io.github.gigioderico.tribuna.data.SettingsRepository
import io.github.gigioderico.tribuna.speech.EngineError
import io.github.gigioderico.tribuna.speech.EngineState
import io.github.gigioderico.tribuna.speech.FakeSpeechEngine
import io.github.gigioderico.tribuna.speech.SpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrompterViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeEngine: FakeSpeechEngine
    private lateinit var fakeSettings: FakeSettingsRepository
    private lateinit var viewModel: PrompterViewModel

    @Before
    fun setup() =
        runBlocking {
            Dispatchers.setMain(testDispatcher)
            fakeEngine = FakeSpeechEngine()
            fakeEngine.prepare() // Passa o motor para Ready
            fakeSettings = FakeSettingsRepository()
            viewModel = PrompterViewModel(fakeEngine, fakeSettings)
        }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setText_populatesTokensAndResetsCursor() {
        viewModel.setText("Primeiro discurso de teste no Tribuna")

        val state = viewModel.uiState.value
        assertEquals("Primeiro discurso de teste no Tribuna", state.text)
        assertEquals(6, state.tokens.size)
        assertEquals(0, state.cursor)
    }

    @Test
    fun readingLifecycle_idleToReadingAndPaused() {
        viewModel.setText("Um dois tres quatro cinco")
        assertEquals(ReadingPhase.Idle, viewModel.uiState.value.phase)

        viewModel.startReading()

        assertEquals(ReadingPhase.Reading, viewModel.uiState.value.phase)
        assertEquals(EngineState.Recognizing, fakeEngine.state.value)

        viewModel.pauseReading()
        assertEquals(ReadingPhase.Paused, viewModel.uiState.value.phase)
        assertEquals(EngineState.Capturing, fakeEngine.state.value)

        viewModel.resumeReading()
        assertEquals(ReadingPhase.Reading, viewModel.uiState.value.phase)
    }

    @Test
    fun speechEvent_oldSessionEventIsIgnored() {
        viewModel.setText("palavra um dois tres quatro cinco")
        viewModel.startReading()

        val firstRecognizer = fakeEngine.recognizers.last()
        val oldSession = firstRecognizer.session

        // Pausar e retomar gera uma sessão nova
        viewModel.pauseReading()
        viewModel.resumeReading()

        val secondRecognizer = fakeEngine.recognizers.last()
        val newSession = secondRecognizer.session
        assertNotEquals(oldSession, newSession)

        // Evento atrasado da sessão antiga não move o cursor (mesmo sendo Final ou múltiplos parciais)
        firstRecognizer.final(listOf("palavra", "um", "dois"))
        assertEquals(0, viewModel.uiState.value.cursor)

        // Eventos da sessão atual reconciliam e movem o cursor
        secondRecognizer.partial(listOf("palavra", "um", "dois"))
        secondRecognizer.partial(listOf("palavra", "um", "dois", "tres"))
        assertTrue(viewModel.uiState.value.cursor > 0)
    }

    @Test
    fun reposition_movesCursorToTargetToken() {
        viewModel.setText("zero um dois tres quatro cinco seis sete")
        viewModel.startReading()

        viewModel.reposition(4)
        assertEquals(4, viewModel.uiState.value.cursor)
    }

    @Test
    fun onActivityStop_pausesReadingIfNotChangingConfigurations() {
        viewModel.setText("teste de ciclo de vida")
        viewModel.startReading()

        assertEquals(ReadingPhase.Reading, viewModel.uiState.value.phase)

        // Rotação: isChangingConfigurations = true (não pausa)
        viewModel.onActivityStop(isChangingConfigurations = true)
        assertEquals(ReadingPhase.Reading, viewModel.uiState.value.phase)

        // Ida real a segundo plano: isChangingConfigurations = false (pausa)
        viewModel.onActivityStop(isChangingConfigurations = false)
        assertEquals(ReadingPhase.Paused, viewModel.uiState.value.phase)
    }

    @Test
    fun startReading_whenEngineIdle_preparesAndStartsReading() =
        runBlocking {
            val unreadyEngine = FakeSpeechEngine()
            val vm = PrompterViewModel(unreadyEngine, fakeSettings)
            vm.setText("Teste de preparo do motor")
            vm.startReading()
            assertEquals(ReadingPhase.Reading, vm.uiState.value.phase)
            assertEquals(EngineState.Recognizing, unreadyEngine.state.value)
        }

    @Test
    fun targetTime_updatesTargetMsAndCalculatesWarnings() =
        runBlocking {
            fakeSettings.update { it.copy(targetTimeMinutes = 10) }
            assertEquals(10 * 60 * 1000L, viewModel.uiState.value.targetMs)
            assertEquals(TimeWarning.None, viewModel.uiState.value.warning)
        }

    @Test
    fun startReading_whenEngineFailedOnce_retriesPrepareAndStartsReading() =
        runBlocking {
            val delegate = FakeSpeechEngine()
            var failPrepare = true
            val stateFlow = MutableStateFlow(EngineState.Idle)
            val engine =
                object : SpeechEngine by delegate {
                    override val state = stateFlow.asStateFlow()

                    override suspend fun prepare(): Result<Unit> =
                        if (failPrepare) {
                            stateFlow.value = EngineState.Failed
                            Result.failure(RuntimeException("falha na primeira tentativa"))
                        } else {
                            delegate.prepare().also { stateFlow.value = delegate.state.value }
                        }

                    override fun startCapture() {
                        delegate.startCapture()
                        stateFlow.value = delegate.state.value
                    }

                    override fun beginSession(vocabulary: List<String>?) =
                        delegate.beginSession(vocabulary).also {
                            stateFlow.value = delegate.state.value
                        }
                }
            val vm = PrompterViewModel(engine, fakeSettings)
            assertEquals(EngineState.Failed, engine.state.value)
            assertFalse(vm.uiState.value.micAvailable)

            // Na segunda tentativa (ao tocar em Iniciar Leitura), prepare() funciona
            failPrepare = false
            vm.setText("Discurso apos recuperacao do motor")
            vm.startReading()

            assertEquals(ReadingPhase.Reading, vm.uiState.value.phase)
            assertEquals(EngineState.Recognizing, engine.state.value)
            assertTrue(vm.uiState.value.micAvailable)
        }

    @Test
    fun startReading_whenStartCaptureFails_pausesGracefullyWithoutCrashing() {
        val delegate = FakeSpeechEngine()
        runBlocking { delegate.prepare() }
        val engine =
            object : SpeechEngine by delegate {
                override fun startCapture() {
                    // Simula falha ao abrir AudioRecord: permanece em Ready e emite erro
                    delegate.emitCaptureError(EngineError.PermissionRevoked)
                }
            }
        val vm = PrompterViewModel(engine, fakeSettings)
        vm.setText("Texto sem permissao de microfone")

        vm.startReading()

        assertEquals(ReadingPhase.Paused, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.micAvailable)
    }
}
