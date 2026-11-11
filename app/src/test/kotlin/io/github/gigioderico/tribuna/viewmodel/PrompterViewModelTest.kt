package io.github.gigioderico.tribuna.viewmodel

import io.github.gigioderico.tribuna.data.AppSettings
import io.github.gigioderico.tribuna.data.SettingsRepository
import io.github.gigioderico.tribuna.speech.EngineState
import io.github.gigioderico.tribuna.speech.FakeSpeechEngine
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
}
