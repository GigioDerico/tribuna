package io.github.gigioderico.tribuna.etapa0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import io.github.gigioderico.tribuna.etapa0.ui.ReaderRoute
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderViewModel

/**
 * Activity única da etapa0, só Compose (CLAUDE.md, regra 8). Declarada no manifesto conforme a CIA 0003
 * (portão 1). Sem `configChanges`: a rotação recria a Activity e o ViewModel segue vivo (TRD §Segundo plano).
 */
class MainActivity : ComponentActivity() {
    private val viewModel: ReaderViewModel by viewModels { ReaderViewModel.factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ReaderRoute(viewModel.controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.controller.onActivityStart()
    }

    override fun onStop() {
        super.onStop()
        // Rotação não é ida ao segundo plano: com isChangingConfigurations, nada muda (TRD §Segundo plano).
        viewModel.controller.onActivityStop(isChangingConfigurations)
    }
}
