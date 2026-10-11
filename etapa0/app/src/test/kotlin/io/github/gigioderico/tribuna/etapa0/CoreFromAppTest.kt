package io.github.gigioderico.tribuna.etapa0

import io.github.gigioderico.tribuna.etapa0.speech.FakeSpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Prova que os contratos do :core e o fake (testFixtures) chegam ao classpath do módulo Android. */
class CoreFromAppTest {
    @Test
    fun contratosEFakeDoCore_chegamAoClasspathDoApp() =
        runTest {
            val fake = FakeSpeechEngine()
            fake.prepare()
            fake.startCapture()
            val id = fake.beginSession()
            fake.hearPartial(listOf("ouvir"))
            assertEquals(SpeechEvent.Partial(id, listOf("ouvir")), fake.events.first())
        }
}
