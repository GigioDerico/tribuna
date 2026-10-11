package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechContractShapeTest {
    @Test
    fun engineState_temOsEstadosDoTrdNaOrdem() {
        assertEquals(
            listOf(
                EngineState.Idle,
                EngineState.Preparing,
                EngineState.Ready,
                EngineState.Capturing,
                EngineState.Recognizing,
                EngineState.Failed,
            ),
            EngineState.entries,
        )
    }

    @Test
    fun engineError_temOsErrosDoTrdNaOrdem() {
        assertEquals(
            listOf(
                EngineError.ModelMissing,
                EngineError.ModelCorrupt,
                EngineError.MicUnavailable,
                EngineError.PermissionRevoked,
                EngineError.Internal,
            ),
            EngineError.entries,
        )
    }

    @Test
    fun speechEvent_temQuatroVariantes_eSoPartialFinalEErrorCarregamSessao() {
        val id = SessionId(42L)
        val events: List<SpeechEvent> =
            listOf(
                SpeechEvent.Partial(id, listOf("ouvir")),
                SpeechEvent.Final(id, listOf("ouvir", "bem")),
                SpeechEvent.Level(0.5f),
                SpeechEvent.Error(null, EngineError.ModelMissing),
            )
        assertEquals(listOf(id, id, null, null), events.map(::sessionOf))
        assertEquals(42L, id.value)
    }

    // `when` exaustivo sem `else`: um subtipo novo de SpeechEvent quebra a compilação deste teste.
    private fun sessionOf(event: SpeechEvent): SessionId? =
        when (event) {
            is SpeechEvent.Partial -> event.session
            is SpeechEvent.Final -> event.session
            is SpeechEvent.Level -> null
            is SpeechEvent.Error -> event.session
        }
}
