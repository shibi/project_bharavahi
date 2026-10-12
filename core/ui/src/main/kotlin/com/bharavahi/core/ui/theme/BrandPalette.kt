package com.bharavahi.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * UI-1: everything that makes an app look like itself. Swap the palette object to re-theme an app;
 * screens only read MaterialTheme.colorScheme and LocalStatusColors.
 */
@Immutable
data class BrandPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
    val statusLight: StatusColors,
    val statusDark: StatusColors,
)

/** App-specific colour roles that Material 3 has no slot for. */
@Immutable
data class StatusColors(
    val paid: Color,
    val partlyPaid: Color,
    val pending: Color,
    val income: Color,
    val expense: Color,
)

val LocalStatusColors = staticCompositionLocalOf<StatusColors> {
    error("StatusColors not provided. Wrap content in BharavahiTheme.")
}
