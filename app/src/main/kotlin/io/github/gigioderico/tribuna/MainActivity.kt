package io.github.gigioderico.tribuna

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.gigioderico.tribuna.data.SettingsRepositoryImpl
import io.github.gigioderico.tribuna.speech.MicAudioSource
import io.github.gigioderico.tribuna.speech.ModelInstaller
import io.github.gigioderico.tribuna.speech.RecognizerStrategy
import io.github.gigioderico.tribuna.speech.VoskEngine
import io.github.gigioderico.tribuna.speech.VoskModels
import io.github.gigioderico.tribuna.ui.navigation.Screen
import io.github.gigioderico.tribuna.ui.navigation.TribunaApp
import io.github.gigioderico.tribuna.viewmodel.PrompterViewModel

/**
 * Activity única do aplicativo Tribuna (TRD §Arquitetura; uma Activity, só Compose).
 *
 * - Tela sempre ligada via FLAG_KEEP_SCREEN_ON (sem WAKE_LOCK, respeitando a Regra 1).
 * - O texto vive exclusivamente no PrompterViewModel em memória (Regra 2).
 * - Notifica onActivityStart e onActivityStop(isChangingConfigurations) para pausa correta (TRD §Segundo plano).
 */
class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy {
        SettingsRepositoryImpl(applicationContext)
    }

    private val prompterViewModel by viewModels<PrompterViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val engine =
                        VoskEngine(
                            modelLoader = VoskModels.loader(applicationContext),
                            audioSource = { MicAudioSource(applicationContext) },
                            strategy = RecognizerStrategy.NewPerSession,
                        )
                    return PrompterViewModel(engine, settingsRepository) as T
                }
            }
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantém a tela acesa durante a apresentação sem requerer WAKE_LOCK (Regra 1)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val initialScreen =
            if (ModelInstaller.isInstalled(this)) {
                Screen.Editor
            } else {
                Screen.Onboarding
            }

        setContent {
            TribunaApp(
                viewModel = prompterViewModel,
                settingsRepository = settingsRepository,
                initialScreen = initialScreen,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        prompterViewModel.onActivityStart()
    }

    override fun onStop() {
        super.onStop()
        // Rotação não é ida ao segundo plano: com isChangingConfigurations = true, a leitura não pausa
        prompterViewModel.onActivityStop(isChangingConfigurations)
    }
}
