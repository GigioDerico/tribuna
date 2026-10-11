package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * SpeechEngine falso para testes (TRD §Módulos e responsabilidades; §Contratos, regras do SpeechEngine).
 *
 * - Cada beginSession cria um [FakeRecognizer] com o SessionId capturado na criação. O reconhecedor
 *   emite sempre com esse id, mesmo depois de endSession: é o callback atrasado do TRD.
 * - [hearPartial] e [hearFinal] simulam áudio chegando: vão ao reconhecedor ativo; sem sessão, são descartados.
 * - [emitLevel] só chega com a captura aberta.
 * - [events] tem um coletor só e buffer ilimitado; [release] encerra o fluxo.
 * - Uso fora de ordem lança IllegalStateException, para expor erro de quem chama. Isso é do fake, não do contrato.
 */
class FakeSpeechEngine(
    private val prepareResult: Result<Unit> = Result.success(Unit),
) : SpeechEngine {
    private val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(EngineState.Idle)
    private val created = mutableListOf<FakeRecognizer>()
    private var nextSession = 1L
    private var active: FakeRecognizer? = null
    private var released = false

    override val events: Flow<SpeechEvent> = channel.receiveAsFlow()
    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    /** Reconhecedores criados, em ordem; o último é o da sessão mais recente. */
    val recognizers: List<FakeRecognizer>
        get() = created.toList()

    override suspend fun prepare(): Result<Unit> {
        checkNotReleased()
        mutableState.value = EngineState.Preparing
        mutableState.value = if (prepareResult.isSuccess) EngineState.Ready else EngineState.Failed
        return prepareResult
    }

    override fun startCapture() {
        checkNotReleased()
        check(mutableState.value == EngineState.Ready) { "startCapture exige Ready; estado: ${mutableState.value}" }
        mutableState.value = EngineState.Capturing
    }

    override fun stopCapture() {
        checkNotReleased()
        check(active == null) { "endSession antes de stopCapture" }
        check(mutableState.value == EngineState.Capturing) { "stopCapture exige Capturing; estado: ${mutableState.value}" }
        mutableState.value = EngineState.Ready
    }

    override fun beginSession(vocabulary: List<String>?): SessionId {
        checkNotReleased()
        check(mutableState.value == EngineState.Capturing && active == null) {
            "beginSession exige captura aberta e nenhuma sessão ativa; estado: ${mutableState.value}"
        }
        val recognizer = FakeRecognizer(SessionId(nextSession++), vocabulary)
        created += recognizer
        active = recognizer
        mutableState.value = EngineState.Recognizing
        return recognizer.session
    }

    override fun endSession() {
        if (active == null) return
        active = null
        mutableState.value = EngineState.Capturing
    }

    override fun release() {
        released = true
        active = null
        mutableState.value = EngineState.Idle
        channel.close()
    }

    /** Fala chegando ao motor: vira Partial da sessão ativa; sem sessão, é descartada. */
    fun hearPartial(words: List<String>) {
        active?.partial(words)
    }

    /** Fala chegando ao motor: vira Final da sessão ativa; sem sessão, é descartada. */
    fun hearFinal(words: List<String>) {
        active?.final(words)
    }

    /** Nível de áudio da captura; só chega com o microfone aberto. */
    fun emitLevel(rms: Float) {
        val current = mutableState.value
        if (current == EngineState.Capturing || current == EngineState.Recognizing) {
            channel.trySend(SpeechEvent.Level(rms))
        }
    }

    /** Erro da captura, sem sessão (ex.: MicUnavailable, PermissionRevoked). */
    fun emitCaptureError(cause: EngineError) {
        channel.trySend(SpeechEvent.Error(null, cause))
    }

    private fun checkNotReleased() = check(!released) { "FakeSpeechEngine já liberado" }

    /** Reconhecedor de uma sessão. O id é capturado na criação; nenhum método lê a sessão ativa do motor. */
    inner class FakeRecognizer internal constructor(
        val session: SessionId,
        val vocabulary: List<String>?,
    ) {
        fun partial(words: List<String>) {
            channel.trySend(SpeechEvent.Partial(session, words))
        }

        fun final(words: List<String>) {
            channel.trySend(SpeechEvent.Final(session, words))
        }

        fun error(cause: EngineError) {
            channel.trySend(SpeechEvent.Error(session, cause))
        }
    }
}
