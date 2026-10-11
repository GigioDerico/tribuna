package io.github.gigioderico.tribuna.etapa0.viewmodel

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/** O ViewModel guarda o controlador e o fecha ao sair de cena (spec 0003, D16; TRD §Onde o texto vive). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun oControladorVemDaFabrica_eSaiDeCenaLiberaOMotor() {
        val scope = TestScope()
        val h = ReaderHarness(scope)
        var received: Any? = null
        val viewModel =
            ReaderViewModel { viewModelScope ->
                received = viewModelScope
                h.controller
            }
        assertSame(h.controller, viewModel.controller)
        assertEquals(false, received == null)
        assertEquals(emptyList<String>(), h.engine.calls)
        val store = ViewModelStore()
        store.put("leitor", viewModel)
        store.clear()
        assertEquals("release", h.engine.calls.last())
    }
}
