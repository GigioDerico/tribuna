package io.github.gigioderico.tribuna.viewmodel

import io.github.gigioderico.tribuna.data.ScrollMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingStateMachineTest {
    @Test
    fun start_fromIdleInVoiceMode_startsCaptureAndSession() {
        val result =
            ReadingStateMachine.transition(
                currentPhase = ReadingPhase.Idle,
                currentMode = ScrollMode.Voice,
                event = ReadingEvent.Start,
            )

        assertEquals(ReadingPhase.Reading, result.newPhase)
        assertEquals(ScrollMode.Voice, result.newMode)
        assertTrue(result.sideEffects.contains(SideEffect.StartCapture))
        assertTrue(result.sideEffects.contains(SideEffect.BeginSession))
    }

    @Test
    fun pause_fromReadingInVoiceMode_pausesAndEndsSession() {
        val result =
            ReadingStateMachine.transition(
                currentPhase = ReadingPhase.Reading,
                currentMode = ScrollMode.Voice,
                event = ReadingEvent.Pause,
            )

        assertEquals(ReadingPhase.Paused, result.newPhase)
        assertTrue(result.sideEffects.contains(SideEffect.EndSession))
    }

    @Test
    fun resume_fromPausedInVoiceMode_resumesAndStartsSession() {
        val result =
            ReadingStateMachine.transition(
                currentPhase = ReadingPhase.Paused,
                currentMode = ScrollMode.Voice,
                event = ReadingEvent.Resume,
            )

        assertEquals(ReadingPhase.Reading, result.newPhase)
        assertTrue(result.sideEffects.contains(SideEffect.BeginSession))
    }

    @Test
    fun reposition_inReading_resetsPositionAndRestartsSession() {
        val result =
            ReadingStateMachine.transition(
                currentPhase = ReadingPhase.Reading,
                currentMode = ScrollMode.Voice,
                event = ReadingEvent.Reposition(tokenIndex = 5),
            )

        assertEquals(ReadingPhase.Reading, result.newPhase)
        assertTrue(result.sideEffects.contains(SideEffect.EndSession))
        assertTrue(result.sideEffects.contains(SideEffect.ResetPosition(5)))
        assertTrue(result.sideEffects.contains(SideEffect.BeginSession))
    }

    @Test
    fun background_whenReading_pausesAndStopsCapture() {
        val result =
            ReadingStateMachine.transition(
                currentPhase = ReadingPhase.Reading,
                currentMode = ScrollMode.Voice,
                event = ReadingEvent.BackgroundChanged(isBackground = true),
            )

        assertEquals(ReadingPhase.Paused, result.newPhase)
        assertTrue(result.sideEffects.contains(SideEffect.EndSession))
        assertTrue(result.sideEffects.contains(SideEffect.StopCapture))
    }
}
