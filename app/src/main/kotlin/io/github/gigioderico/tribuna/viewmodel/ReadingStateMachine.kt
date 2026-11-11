package io.github.gigioderico.tribuna.viewmodel

import io.github.gigioderico.tribuna.data.ScrollMode

sealed interface ReadingEvent {
    data object Start : ReadingEvent

    data object Pause : ReadingEvent

    data object Resume : ReadingEvent

    data class Reposition(
        val tokenIndex: Int,
    ) : ReadingEvent

    data object End : ReadingEvent

    data object Finish : ReadingEvent

    data class ModeChanged(
        val newMode: ScrollMode,
    ) : ReadingEvent

    data class BackgroundChanged(
        val isBackground: Boolean,
    ) : ReadingEvent
}

sealed interface SideEffect {
    data object StartCapture : SideEffect

    data object StopCapture : SideEffect

    data object BeginSession : SideEffect

    data object EndSession : SideEffect

    data class ResetPosition(
        val tokenIndex: Int,
    ) : SideEffect
}

data class TransitionResult(
    val newPhase: ReadingPhase,
    val newMode: ScrollMode,
    val sideEffects: List<SideEffect>,
)

object ReadingStateMachine {
    fun transition(
        currentPhase: ReadingPhase,
        currentMode: ScrollMode,
        event: ReadingEvent,
    ): TransitionResult =
        when (event) {
            is ReadingEvent.Start -> {
                when (currentPhase) {
                    is ReadingPhase.Idle, is ReadingPhase.Finished -> {
                        val effects =
                            if (currentMode == ScrollMode.Voice) {
                                listOf(SideEffect.StartCapture, SideEffect.BeginSession)
                            } else {
                                emptyList()
                            }
                        TransitionResult(ReadingPhase.Reading, currentMode, effects)
                    }

                    else -> {
                        TransitionResult(currentPhase, currentMode, emptyList())
                    }
                }
            }

            is ReadingEvent.Pause -> {
                when (currentPhase) {
                    is ReadingPhase.Reading -> {
                        val effects =
                            if (currentMode == ScrollMode.Voice) {
                                listOf(SideEffect.EndSession)
                            } else {
                                emptyList()
                            }
                        TransitionResult(ReadingPhase.Paused, currentMode, effects)
                    }

                    is ReadingPhase.Countdown -> {
                        TransitionResult(ReadingPhase.Paused, currentMode, emptyList())
                    }

                    else -> {
                        TransitionResult(currentPhase, currentMode, emptyList())
                    }
                }
            }

            is ReadingEvent.Resume -> {
                when (currentPhase) {
                    is ReadingPhase.Paused -> {
                        val effects =
                            if (currentMode == ScrollMode.Voice) {
                                listOf(SideEffect.BeginSession)
                            } else {
                                emptyList()
                            }
                        TransitionResult(ReadingPhase.Reading, currentMode, effects)
                    }

                    else -> {
                        TransitionResult(currentPhase, currentMode, emptyList())
                    }
                }
            }

            is ReadingEvent.Reposition -> {
                when (currentPhase) {
                    is ReadingPhase.Reading -> {
                        val effects =
                            if (currentMode == ScrollMode.Voice) {
                                listOf(
                                    SideEffect.EndSession,
                                    SideEffect.ResetPosition(event.tokenIndex),
                                    SideEffect.BeginSession,
                                )
                            } else {
                                listOf(SideEffect.ResetPosition(event.tokenIndex))
                            }
                        TransitionResult(ReadingPhase.Reading, currentMode, effects)
                    }

                    is ReadingPhase.Paused -> {
                        TransitionResult(
                            ReadingPhase.Paused,
                            currentMode,
                            listOf(SideEffect.ResetPosition(event.tokenIndex)),
                        )
                    }

                    else -> {
                        TransitionResult(currentPhase, currentMode, emptyList())
                    }
                }
            }

            is ReadingEvent.Finish -> {
                val effects =
                    if (currentMode == ScrollMode.Voice) {
                        listOf(SideEffect.EndSession, SideEffect.StopCapture)
                    } else {
                        emptyList()
                    }
                TransitionResult(ReadingPhase.Finished(0L), currentMode, effects)
            }

            is ReadingEvent.End -> {
                val effects =
                    if (currentMode == ScrollMode.Voice) {
                        listOf(SideEffect.EndSession, SideEffect.StopCapture)
                    } else {
                        emptyList()
                    }
                TransitionResult(ReadingPhase.Idle, currentMode, effects)
            }

            is ReadingEvent.ModeChanged -> {
                if (currentPhase is ReadingPhase.Paused || currentPhase is ReadingPhase.Idle) {
                    TransitionResult(currentPhase, event.newMode, emptyList())
                } else {
                    TransitionResult(currentPhase, currentMode, emptyList())
                }
            }

            is ReadingEvent.BackgroundChanged -> {
                if (event.isBackground) {
                    when (currentPhase) {
                        is ReadingPhase.Reading -> {
                            val effects =
                                if (currentMode == ScrollMode.Voice) {
                                    listOf(SideEffect.EndSession, SideEffect.StopCapture)
                                } else {
                                    emptyList()
                                }
                            TransitionResult(ReadingPhase.Paused, currentMode, effects)
                        }

                        is ReadingPhase.Paused -> {
                            val effects =
                                if (currentMode == ScrollMode.Voice) {
                                    listOf(SideEffect.StopCapture)
                                } else {
                                    emptyList()
                                }
                            TransitionResult(ReadingPhase.Paused, currentMode, effects)
                        }

                        else -> {
                            TransitionResult(currentPhase, currentMode, emptyList())
                        }
                    }
                } else {
                    TransitionResult(currentPhase, currentMode, emptyList())
                }
            }
        }
}
