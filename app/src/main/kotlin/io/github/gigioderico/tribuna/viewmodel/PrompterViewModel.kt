package io.github.gigioderico.tribuna.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gigioderico.tribuna.align.AlignConstants
import io.github.gigioderico.tribuna.align.AlignParams
import io.github.gigioderico.tribuna.align.AlignPresets
import io.github.gigioderico.tribuna.align.AlignState
import io.github.gigioderico.tribuna.align.Aligner
import io.github.gigioderico.tribuna.align.Move
import io.github.gigioderico.tribuna.align.PartialReconciler
import io.github.gigioderico.tribuna.align.Tokenizer
import io.github.gigioderico.tribuna.data.AppSettings
import io.github.gigioderico.tribuna.data.ScrollMode
import io.github.gigioderico.tribuna.data.SettingsRepository
import io.github.gigioderico.tribuna.speech.EngineState
import io.github.gigioderico.tribuna.speech.SessionId
import io.github.gigioderico.tribuna.speech.SpeechEngine
import io.github.gigioderico.tribuna.speech.SpeechEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class PrompterViewModel(
    private val speechEngine: SpeechEngine,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PrompterUiState())
    val uiState: StateFlow<PrompterUiState> = _uiState.asStateFlow()

    private var activeSessionId: SessionId? = null
    private var aligner: Aligner? = null
    private var alignState = AlignState(0, 0, 0, Move.Stay, 0.0)
    private var reconciler = PartialReconciler()
    private val tail = ArrayDeque<String>()
    private var timerJob: Job? = null
    private var appSettings = AppSettings()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                appSettings = settings
                updateAlignerParams()
                _uiState.update { current ->
                    current.copy(
                        targetMs = if (settings.targetTimeMinutes > 0) settings.targetTimeMinutes * 60 * 1000L else null,
                    )
                }
            }
        }

        viewModelScope.launch {
            speechEngine.events.collect { event ->
                handleSpeechEvent(event)
            }
        }

        viewModelScope.launch {
            speechEngine.state.collect { state ->
                _uiState.update { it.copy(micAvailable = state != EngineState.Failed) }
            }
        }

        viewModelScope.launch {
            speechEngine.prepare()
        }
    }

    fun setText(newText: String) {
        val tokens = Tokenizer.tokenize(newText)
        _uiState.update {
            it.copy(
                text = newText,
                tokens = tokens,
                cursor = 0,
            )
        }
        updateAlignerParams()
    }

    private fun updateAlignerParams() {
        val tokens = _uiState.value.tokens
        val params = resolveSensitivityParams(appSettings.sensitivity)
        aligner = Aligner(tokens, params)
        alignState = AlignState(0, 0, 0, Move.Stay, 0.0)
    }

    fun startReading() {
        if (_uiState.value.mode == ScrollMode.Constant || speechEngine.state.value == EngineState.Ready) {
            dispatch(ReadingEvent.Start)
        } else {
            viewModelScope.launch {
                val currentState = speechEngine.state.value
                if (currentState == EngineState.Idle || currentState == EngineState.Failed) {
                    speechEngine.prepare()
                }
                if (speechEngine.state.value == EngineState.Preparing) {
                    speechEngine.state.first { it != EngineState.Preparing }
                }
                if (speechEngine.state.value == EngineState.Ready) {
                    dispatch(ReadingEvent.Start)
                }
            }
        }
    }

    fun pauseReading() {
        dispatch(ReadingEvent.Pause)
    }

    fun resumeReading() {
        dispatch(ReadingEvent.Resume)
    }

    fun reposition(tokenIndex: Int) {
        dispatch(ReadingEvent.Reposition(tokenIndex))
    }

    fun finishReading() {
        dispatch(ReadingEvent.Finish)
    }

    fun endReading() {
        dispatch(ReadingEvent.End)
    }

    fun changeMode(newMode: ScrollMode) {
        dispatch(ReadingEvent.ModeChanged(newMode))
    }

    fun onActivityStop(isChangingConfigurations: Boolean) {
        if (!isChangingConfigurations) {
            dispatch(ReadingEvent.BackgroundChanged(isBackground = true))
        }
    }

    fun onActivityStart() {
        dispatch(ReadingEvent.BackgroundChanged(isBackground = false))
    }

    private fun dispatch(event: ReadingEvent) {
        val current = _uiState.value
        val result = ReadingStateMachine.transition(current.phase, current.mode, event)

        _uiState.update {
            it.copy(
                phase = result.newPhase,
                mode = result.newMode,
            )
        }

        executeSideEffects(result.sideEffects)
        manageTimer(result.newPhase)
    }

    private fun executeSideEffects(effects: List<SideEffect>) {
        for (effect in effects) {
            when (effect) {
                is SideEffect.StartCapture -> {
                    speechEngine.startCapture()
                    val ok = speechEngine.state.value == EngineState.Capturing
                    _uiState.update { it.copy(micAvailable = ok) }
                    if (!ok) {
                        _uiState.update { it.copy(phase = ReadingPhase.Paused) }
                        return
                    }
                }

                is SideEffect.StopCapture -> {
                    val state = speechEngine.state.value
                    if (state == EngineState.Capturing || state == EngineState.Recognizing) {
                        speechEngine.stopCapture()
                    }
                }

                is SideEffect.BeginSession -> {
                    if (speechEngine.state.value == EngineState.Ready) {
                        speechEngine.startCapture()
                    }
                    if (speechEngine.state.value != EngineState.Capturing) {
                        _uiState.update { it.copy(micAvailable = false, phase = ReadingPhase.Paused) }
                        return
                    }
                    _uiState.update { it.copy(micAvailable = true) }
                    // Configuração oficial calibrada na Etapa 0 (D10): vocabulary=off (null)
                    // Vocabulário restrito com gramática Kaldi prejudica fala contínua natural
                    val sessionId = speechEngine.beginSession(vocabulary = null)
                    activeSessionId = sessionId
                    reconciler.reset()
                    tail.clear()
                }

                is SideEffect.EndSession -> {
                    speechEngine.endSession()
                    activeSessionId = null
                    reconciler.reset()
                    tail.clear()
                }

                is SideEffect.ResetPosition -> {
                    aligner?.let {
                        alignState = it.reposition(effect.tokenIndex)
                        reconciler.reset()
                        tail.clear()
                        _uiState.update { s -> s.copy(cursor = alignState.cursor) }
                    }
                }
            }
        }
    }

    private fun handleSpeechEvent(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.Level -> {
                _uiState.update { it.copy(micLevel = event.rms) }
            }

            is SpeechEvent.Partial -> {
                if (event.session != activeSessionId) return
                if (_uiState.value.phase != ReadingPhase.Reading) return
                val words = reconciler.onPartial(event.words)
                if (words.isNotEmpty()) {
                    advanceCursor(words)
                }
            }

            is SpeechEvent.Final -> {
                if (event.session != activeSessionId) return
                if (_uiState.value.phase != ReadingPhase.Reading) return
                val words = reconciler.onFinal(event.words)
                if (words.isNotEmpty()) {
                    advanceCursor(words)
                }
            }

            is SpeechEvent.Error -> {
                if (event.session == null || event.session == activeSessionId) {
                    if (event.session == null) {
                        _uiState.update { it.copy(micAvailable = false) }
                    }
                    dispatch(ReadingEvent.Pause)
                }
            }
        }
    }

    private fun advanceCursor(fresh: List<String>) {
        val currentAligner = aligner ?: return
        fresh.forEach(tail::addLast)
        val params = resolveSensitivityParams(appSettings.sensitivity)
        val keep = maxOf(params.k, AlignConstants.RECOVERY_WORDS)
        while (tail.size > keep) tail.removeFirst()
        alignState = currentAligner.advance(alignState, tail.toList())
        _uiState.update { it.copy(cursor = alignState.cursor) }

        if (alignState.cursor >= _uiState.value.tokens.size - 1 && _uiState.value.tokens.isNotEmpty()) {
            finishReading()
        }
    }

    private fun manageTimer(phase: ReadingPhase) {
        if (phase is ReadingPhase.Reading) {
            if (timerJob == null || timerJob?.isActive != true) {
                timerJob =
                    viewModelScope.launch {
                        while (true) {
                            delay(100L)
                            _uiState.update { current ->
                                val newElapsed = current.elapsedMs + 100L
                                val target = current.targetMs
                                val warning =
                                    if (target != null && target > 0) {
                                        val remaining = target - newElapsed
                                        when {
                                            remaining <= 60_000L -> TimeWarning.OneMin
                                            remaining <= 300_000L -> TimeWarning.FiveMin
                                            else -> TimeWarning.None
                                        }
                                    } else {
                                        TimeWarning.None
                                    }
                                current.copy(
                                    elapsedMs = newElapsed,
                                    warning = warning,
                                )
                            }
                        }
                    }
            }
        } else if (phase !is ReadingPhase.Paused) {
            timerJob?.cancel()
            timerJob = null
            if (phase is ReadingPhase.Idle) {
                _uiState.update { it.copy(elapsedMs = 0L, warning = TimeWarning.None) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
    }

    companion object {
        /**
         * Parâmetros calibrados para fala natural contínua no celular Android.
         * Reduz a latência inicial para 2 palavras no modo Normal e tolera pequenas variações fonéticas.
         */
        fun resolveSensitivityParams(sensitivity: String): AlignParams =
            when (sensitivity.lowercase(Locale.ROOT)) {
                "ágil", "agil" -> AlignParams(k = 2, window = 60, shortThreshold = 0.50, longThreshold = 0.78)
                "conservador" -> AlignParams(k = 3, window = 30, shortThreshold = 0.65, longThreshold = 0.88)
                else -> AlignParams(k = 2, window = 45, shortThreshold = 0.55, longThreshold = 0.82)
            }
    }
}
