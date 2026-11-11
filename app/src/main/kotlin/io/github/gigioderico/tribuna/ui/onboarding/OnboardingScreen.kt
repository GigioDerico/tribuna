package io.github.gigioderico.tribuna.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.gigioderico.tribuna.data.AppTheme
import io.github.gigioderico.tribuna.speech.ModelInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tela de boas-vindas e configuração inicial (PRD Telas 1a e 1b; TRD §ui.onboarding).
 *
 * - Apresenta o compromisso de privacidade inegociável (100% offline, zero armazenamento de texto).
 * - Solicita permissão de áudio (RECORD_AUDIO).
 * - Extrai os arquivos do modelo Vosk offline na primeira execução.
 */
@Composable
fun OnboardingScreen(
    theme: AppTheme,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }

    var isInstallingModel by remember { mutableStateOf(false) }
    var isModelReady by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            isPermissionGranted = granted
        }

    LaunchedEffect(Unit) {
        scope.launch {
            isInstallingModel = true
            val result =
                withContext(Dispatchers.IO) {
                    ModelInstaller.installIfNeeded(context)
                }
            isInstallingModel = false
            if (result.isSuccess) {
                isModelReady = true
            } else {
                installError = result.exceptionOrNull()?.message ?: "Falha ao preparar modelo de voz"
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Tribuna",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = theme.highlightColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Teleprompter com Reconhecimento de Voz",
                fontSize = 16.sp,
                color = theme.textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
            ) {
                Text(
                    text = "🔒 100% Offline & Seguro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = theme.textColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        "Seus discursos nunca são salvos em nenhum arquivo nem enviados pela internet. " +
                            "O aplicativo não requer permissão de rede.",
                    fontSize = 13.sp,
                    color = theme.textColor.copy(alpha = 0.7f),
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "🎤 Acompanhamento por Voz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = theme.textColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O teleprompter avança automaticamente conforme você fala, acompanhando seu ritmo natural e pausas.",
                    fontSize = 13.sp,
                    color = theme.textColor.copy(alpha = 0.7f),
                    lineHeight = 18.sp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isInstallingModel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = theme.highlightColor,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Preparando modelo de voz offline...",
                        color = theme.textColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
            } else if (installError != null) {
                Text(
                    text = "Erro: $installError",
                    color = Color.Red,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isPermissionGranted) {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = theme.highlightColor,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Autorizar Microfone", fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onComplete,
                enabled = !isInstallingModel,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = theme.highlightColor,
                        contentColor = theme.backgroundColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
            ) {
                Text(
                    text = if (isPermissionGranted) "Continuar para o Editor" else "Continuar (Modo Constante)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
