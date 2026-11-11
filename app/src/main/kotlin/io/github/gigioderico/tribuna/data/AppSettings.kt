package io.github.gigioderico.tribuna.data

import androidx.compose.ui.graphics.Color
import io.github.gigioderico.tribuna.align.AlignPresets

enum class AppTheme(
    val textColor: Color,
    val backgroundColor: Color,
    val highlightColor: Color,
) {
    DarkTealMint(
        textColor = Color.White,
        backgroundColor = Color(0xFF0F1E24), // Azul-petrÃ³leo escuro (PadrÃ£o TRD)
        highlightColor = Color(0xFF64FFDA), // Verde-menta
    ),
    BlackWhite(
        textColor = Color.Black,
        backgroundColor = Color.White,
        highlightColor = Color(0xFF1E88E5),
    ),
    WhiteBlack(
        textColor = Color.White,
        backgroundColor = Color.Black,
        highlightColor = Color(0xFF00E676),
    ),
    YellowBlack(
        textColor = Color(0xFFFFEB3B),
        backgroundColor = Color.Black,
        highlightColor = Color.White,
    ),
    GreenBlack(
        textColor = Color(0xFF00E676),
        backgroundColor = Color.Black,
        highlightColor = Color(0xFFFFEB3B),
    ),
}

enum class ScrollMode {
    Voice,
    Constant,
}

data class AppSettings(
    val fontSizeSp: Float = 44f,
    val lineSpacingMultiplier: Float = 1.4f,
    val horizontalMarginPercent: Float = 8f,
    val readingLinePercent: Float = 30f,
    val readTextOpacity: Float = 0.4f,
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val constantSpeedLevel: Int = 5,
    val scrollDurationMs: Long = 375L, // OPR-022 aprovado
    val theme: AppTheme = AppTheme.DarkTealMint,
    val sensitivity: String = AlignPresets.NORMAL_NAME,
    val targetTimeMinutes: Int = 0, // 0 = desativado/livre (PRD D3)
)
