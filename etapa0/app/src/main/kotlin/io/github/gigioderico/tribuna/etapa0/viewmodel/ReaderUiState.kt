package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.AlignParams
import io.github.gigioderico.tribuna.etapa0.align.AlignPresets
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.align.Token
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.EngineState
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy

/** Fase da tela mínima da etapa0 (só o que ela usa; não é o `ReadingPhase` do app). */
enum class ReaderPhase { Idle, Preparing, Reading, Paused }

/** Presets do PRD (spec 0002, D11; spec 0003, D17: padrão Normal). */
enum class PresetChoice(
    val params: AlignParams,
) {
    Conservador(AlignPresets.conservador),
    Normal(AlignPresets.normal),
    Agil(AlignPresets.agil),
}

/** Fonte de áudio (spec 0003, D8). */
sealed interface AudioChoice {
    data object Microphone : AudioChoice

    data class Wav(
        val name: String,
    ) : AudioChoice
}

/** Ajustes do painel, só em memória (spec 0003, D16: a etapa0 não grava ajustes em disco). */
data class DebugSettings(
    val preset: PresetChoice = PresetChoice.Normal,
    val vocabulary: Boolean = false,
    val strategy: RecognizerStrategy = RecognizerStrategy.NewPerSession,
    val source: AudioChoice = AudioChoice.Microphone,
    /** Limiar de nível; 0 = desligado (D3). */
    val levelThreshold: Float = 0f,
    /** PROVISÓRIO: ponto médio da faixa do R5 (150–600 ms), só na etapa0 (D13; OPR-022). */
    val animationMs: Int = ANIMATION_DEFAULT_MS,
) {
    companion object {
        const val ANIMATION_MIN_MS = 150
        const val ANIMATION_MAX_MS = 600
        const val ANIMATION_DEFAULT_MS = 375
    }
}

/** O que o painel de depuração mostra (spec 0003, D18). Tudo em memória. */
data class DebugInfo(
    val lastPartial: List<String> = emptyList(),
    val lastFinal: List<String> = emptyList(),
    val tail: List<String> = emptyList(),
    val score: Double = 0.0,
    val lastMove: Move? = null,
    val highWater: Int = 0,
    val missStreak: Int = 0,
    val activeSession: Long? = null,
    val engineState: EngineState = EngineState.Idle,
    val level: Float = 0f,
    /** Latência 2: recepção do Partial → cursor publicado. */
    val latency2Ms: Long? = null,
    /** Latência 3: cursor publicado → fim da animação. */
    val latency3Ms: Long? = null,
    val installMs: Long? = null,
    val prepareMs: Long? = null,
    val beginSessionMs: Long? = null,
    /** Início da escuta: beginSession → primeiro parcial não vazio. */
    val firstPartialMs: Long? = null,
    val vocabularySize: Int? = null,
)

data class ReaderUiState(
    val text: String = "",
    val tokens: List<Token> = emptyList(),
    val cursor: Int = 0,
    /** Quando o cursor atual foi publicado (relógio do controlador), para a latência 3. */
    val cursorAtMs: Long = 0L,
    val phase: ReaderPhase = ReaderPhase.Idle,
    val capturing: Boolean = false,
    /** ID do texto de teste idêntico ao texto atual; só com ele a exportação existe (D9). */
    val testTextId: String? = null,
    val testTextIds: List<String> = emptyList(),
    val wavFiles: List<String> = emptyList(),
    val settings: DebugSettings = DebugSettings(),
    val exportName: String = "",
    val canExport: Boolean = false,
    val lastExport: List<String> = emptyList(),
    val exportFailed: Boolean = false,
    val error: EngineError? = null,
    val debug: DebugInfo = DebugInfo(),
)
