package com.spendsense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

// ── Color Schemes ───────────────────────────────────────────────────────────

private val SpendSenseDarkColors = darkColorScheme(
    primary            = PrimaryIndigo,
    onPrimary          = OnDark,
    primaryContainer   = DarkSurfaceVar,
    onPrimaryContainer = PrimaryIndigoVar,
    secondary          = SecondaryEmerald,
    onSecondary        = OnDark,
    background         = DarkBackground,
    onBackground       = OnDark,
    surface            = DarkSurface,
    onSurface          = OnDark,
    surfaceVariant     = DarkSurfaceVar,
    onSurfaceVariant   = OnDarkVariant,
    error              = ErrorColor,
    onError            = OnDark,
    outline            = OnDarkVariant
)

private val SpendSenseLightColors = lightColorScheme(
    primary            = LightPrimary,
    onPrimary          = LightSurface,
    primaryContainer   = LightSurfaceVar,
    onPrimaryContainer = LightPrimaryVar,
    secondary          = LightSecondary,
    onSecondary        = LightSurface,
    background         = LightBackground,
    onBackground       = OnLight,
    surface            = LightSurface,
    onSurface          = OnLight,
    surfaceVariant     = LightSurfaceVar,
    onSurfaceVariant   = OnLightVariant,
    error              = LightError,
    onError            = LightSurface,
    outline            = OnLightVariant
)

// ── Theme Toggle Hook ────────────────────────────────────────────────────────
// FUTURE: Wire useDarkTheme to a user preference stored in DataStore/Room
// For now we default to system theme, with darkTheme param for easy override.

/**
 * Local composition value to allow child composables to know the theme mode.
 * FUTURE: Expose this as a MutableState driven by user preference in Settings.
 */
val LocalUseDarkTheme = compositionLocalOf { true }

@Composable
fun SpendSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpendSenseDarkColors else SpendSenseLightColors

    CompositionLocalProvider(LocalUseDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = SpendSenseTypography,
            content     = content
        )
    }
}
