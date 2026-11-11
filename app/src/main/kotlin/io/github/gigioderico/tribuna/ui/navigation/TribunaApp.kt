package io.github.gigioderico.tribuna.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gigioderico.tribuna.data.AppSettings
import io.github.gigioderico.tribuna.data.ScrollMode
import io.github.gigioderico.tribuna.data.SettingsRepository
import io.github.gigioderico.tribuna.ui.editor.EditorScreen
import io.github.gigioderico.tribuna.ui.onboarding.OnboardingScreen
import io.github.gigioderico.tribuna.ui.prompter.PrompterScreen
import io.github.gigioderico.tribuna.ui.settings.SettingsScreen
import io.github.gigioderico.tribuna.ui.splash.SplashScreen
import io.github.gigioderico.tribuna.viewmodel.PrompterViewModel
import io.github.gigioderico.tribuna.viewmodel.ReadingPhase
import kotlinx.coroutines.launch

enum class Screen {
    Splash,
    Onboarding,
    Editor,
    Prompter,
    Settings,
}

/**
 * Ponto central de navegação e orquestração do aplicativo Tribuna.
 */
@Composable
fun TribunaApp(
    viewModel: PrompterViewModel,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    initialScreen: Screen = Screen.Editor,
    showSplash: Boolean = false,
) {
    var currentScreen by remember { mutableStateOf(if (showSplash) Screen.Splash else initialScreen) }
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                theme = settings.theme,
                onFinished = { currentScreen = initialScreen },
                modifier = modifier,
            )
        }

        Screen.Onboarding -> {
            OnboardingScreen(
                theme = settings.theme,
                onComplete = { currentScreen = Screen.Editor },
                modifier = modifier,
            )
        }

        Screen.Editor -> {
            EditorScreen(
                text = uiState.text,
                onTextChanged = { viewModel.setText(it) },
                mode = uiState.mode,
                onModeChanged = { viewModel.changeMode(it) },
                onStartReading = {
                    viewModel.startReading()
                    currentScreen = Screen.Prompter
                },
                onOpenSettings = { currentScreen = Screen.Settings },
                theme = settings.theme,
                modifier = modifier,
            )
        }

        Screen.Prompter -> {
            BackHandler {
                if (uiState.phase == ReadingPhase.Reading) {
                    viewModel.pauseReading()
                } else {
                    viewModel.endReading()
                    currentScreen = Screen.Editor
                }
            }

            PrompterScreen(
                uiState = uiState,
                settings = settings,
                onPause = { viewModel.pauseReading() },
                onResume = { viewModel.resumeReading() },
                onRestart = {
                    viewModel.reposition(0)
                    viewModel.resumeReading()
                },
                onFinish = {
                    viewModel.endReading()
                    currentScreen = Screen.Editor
                },
                onWordClick = { tokenIndex ->
                    viewModel.reposition(tokenIndex)
                    if (uiState.phase is ReadingPhase.Paused) {
                        viewModel.resumeReading()
                    }
                },
                onToggleMode = {
                    val nextMode = if (uiState.mode == ScrollMode.Voice) ScrollMode.Constant else ScrollMode.Voice
                    viewModel.changeMode(nextMode)
                },
                modifier = modifier,
            )
        }

        Screen.Settings -> {
            BackHandler {
                currentScreen = Screen.Editor
            }

            SettingsScreen(
                settings = settings,
                onUpdateSettings = { transform ->
                    scope.launch {
                        settingsRepository.update(transform)
                    }
                },
                onBack = { currentScreen = Screen.Editor },
                modifier = modifier,
            )
        }
    }
}
