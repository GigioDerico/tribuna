package io.github.gigioderico.tribuna.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tribuna_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {
    private object PreferencesKeys {
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_SPACING_MULTIPLIER = floatPreferencesKey("line_spacing_multiplier")
        val HORIZONTAL_MARGIN_PERCENT = floatPreferencesKey("horizontal_margin_percent")
        val READING_LINE_PERCENT = floatPreferencesKey("reading_line_percent")
        val READ_TEXT_OPACITY = floatPreferencesKey("read_text_opacity")
        val MIRROR_HORIZONTAL = booleanPreferencesKey("mirror_horizontal")
        val MIRROR_VERTICAL = booleanPreferencesKey("mirror_vertical")
        val CONSTANT_SPEED_LEVEL = intPreferencesKey("constant_speed_level")
        val SCROLL_DURATION_MS = longPreferencesKey("scroll_duration_ms")
        val THEME_NAME = stringPreferencesKey("theme_name")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
    }

    override val settings: Flow<AppSettings> =
        context.dataStore.data.map { preferences ->
            val default = AppSettings()
            val themeName = preferences[PreferencesKeys.THEME_NAME]
            val theme =
                themeName?.let {
                    runCatching { AppTheme.valueOf(it) }.getOrNull()
                } ?: default.theme

            AppSettings(
                fontSizeSp = preferences[PreferencesKeys.FONT_SIZE_SP] ?: default.fontSizeSp,
                lineSpacingMultiplier = preferences[PreferencesKeys.LINE_SPACING_MULTIPLIER] ?: default.lineSpacingMultiplier,
                horizontalMarginPercent = preferences[PreferencesKeys.HORIZONTAL_MARGIN_PERCENT] ?: default.horizontalMarginPercent,
                readingLinePercent = preferences[PreferencesKeys.READING_LINE_PERCENT] ?: default.readingLinePercent,
                readTextOpacity = preferences[PreferencesKeys.READ_TEXT_OPACITY] ?: default.readTextOpacity,
                mirrorHorizontal = preferences[PreferencesKeys.MIRROR_HORIZONTAL] ?: default.mirrorHorizontal,
                mirrorVertical = preferences[PreferencesKeys.MIRROR_VERTICAL] ?: default.mirrorVertical,
                constantSpeedLevel = preferences[PreferencesKeys.CONSTANT_SPEED_LEVEL] ?: default.constantSpeedLevel,
                scrollDurationMs = preferences[PreferencesKeys.SCROLL_DURATION_MS] ?: default.scrollDurationMs,
                theme = theme,
                sensitivity = preferences[PreferencesKeys.SENSITIVITY] ?: default.sensitivity,
            )
        }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            val default = AppSettings()
            val themeName = preferences[PreferencesKeys.THEME_NAME]
            val theme =
                themeName?.let {
                    runCatching { AppTheme.valueOf(it) }.getOrNull()
                } ?: default.theme

            val current =
                AppSettings(
                    fontSizeSp = preferences[PreferencesKeys.FONT_SIZE_SP] ?: default.fontSizeSp,
                    lineSpacingMultiplier = preferences[PreferencesKeys.LINE_SPACING_MULTIPLIER] ?: default.lineSpacingMultiplier,
                    horizontalMarginPercent = preferences[PreferencesKeys.HORIZONTAL_MARGIN_PERCENT] ?: default.horizontalMarginPercent,
                    readingLinePercent = preferences[PreferencesKeys.READING_LINE_PERCENT] ?: default.readingLinePercent,
                    readTextOpacity = preferences[PreferencesKeys.READ_TEXT_OPACITY] ?: default.readTextOpacity,
                    mirrorHorizontal = preferences[PreferencesKeys.MIRROR_HORIZONTAL] ?: default.mirrorHorizontal,
                    mirrorVertical = preferences[PreferencesKeys.MIRROR_VERTICAL] ?: default.mirrorVertical,
                    constantSpeedLevel = preferences[PreferencesKeys.CONSTANT_SPEED_LEVEL] ?: default.constantSpeedLevel,
                    scrollDurationMs = preferences[PreferencesKeys.SCROLL_DURATION_MS] ?: default.scrollDurationMs,
                    theme = theme,
                    sensitivity = preferences[PreferencesKeys.SENSITIVITY] ?: default.sensitivity,
                )

            val updated = transform(current)
            preferences[PreferencesKeys.FONT_SIZE_SP] = updated.fontSizeSp
            preferences[PreferencesKeys.LINE_SPACING_MULTIPLIER] = updated.lineSpacingMultiplier
            preferences[PreferencesKeys.HORIZONTAL_MARGIN_PERCENT] = updated.horizontalMarginPercent
            preferences[PreferencesKeys.READING_LINE_PERCENT] = updated.readingLinePercent
            preferences[PreferencesKeys.READ_TEXT_OPACITY] = updated.readTextOpacity
            preferences[PreferencesKeys.MIRROR_HORIZONTAL] = updated.mirrorHorizontal
            preferences[PreferencesKeys.MIRROR_VERTICAL] = updated.mirrorVertical
            preferences[PreferencesKeys.CONSTANT_SPEED_LEVEL] = updated.constantSpeedLevel
            preferences[PreferencesKeys.SCROLL_DURATION_MS] = updated.scrollDurationMs
            preferences[PreferencesKeys.THEME_NAME] = updated.theme.name
            preferences[PreferencesKeys.SENSITIVITY] = updated.sensitivity
        }
    }
}
