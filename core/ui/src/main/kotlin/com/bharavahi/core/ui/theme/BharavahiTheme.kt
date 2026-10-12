package com.bharavahi.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.bharavahi.core.domain.model.ThemeMode

/**
 * UI-2: wrap each app's content once. Dynamic colour is deliberately not used (UI-4),
 * so the brand palette always shows.
 */
@Composable
fun BharavahiTheme(
    palette: BrandPalette,
    mode: ThemeMode = ThemeMode.SYSTEM,
    typography: Typography = DefaultTypography,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalStatusColors provides if (dark) palette.statusDark else palette.statusLight) {
        MaterialTheme(
            colorScheme = if (dark) palette.dark else palette.light,
            typography = typography,
            shapes = BharavahiShapes,
            content = content,
        )
    }
}
