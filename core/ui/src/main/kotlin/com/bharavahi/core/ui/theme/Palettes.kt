package com.bharavahi.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The only file allowed to contain raw colour values (UI-3).

private val SharedStatusLight = StatusColors(
    paid = Color(0xFF2E7D5B),
    partlyPaid = Color(0xFFA15C00),
    pending = Color(0xFFB3261E),
    income = Color(0xFF1B4A5A),
    expense = Color(0xFFFEAE57),
)

private val SharedStatusDark = StatusColors(
    paid = Color(0xFF7FD3A8),
    partlyPaid = Color(0xFFFFC27A),
    pending = Color(0xFFFFB4AB),
    income = Color(0xFF8FCFE3),
    expense = Color(0xFFFEAE57),
)

/** Organization app: deep teal and warm orange from the reference design (section 14.2). */
val TealOrangePalette = BrandPalette(
    light = lightColorScheme(
        primary = Color(0xFF1B4A5A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF1B4A5A),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFFEAE57),
        onSecondary = Color(0xFF1B4A5A),
        secondaryContainer = Color(0xFFFFDDB8),
        onSecondaryContainer = Color(0xFF2B1700),
        tertiary = Color(0xFFB26100),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFEEDE),
        onTertiaryContainer = Color(0xFF17323C),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF17323C),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF17323C),
        surfaceVariant = Color(0xFFEEF1F2),
        onSurfaceVariant = Color(0xFF6B7478),
        outline = Color(0xFFD2D2D2),
        outlineVariant = Color(0xFFE6E6E6),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
    ),
    dark = darkColorScheme(
        primary = Color(0xFF8FCFE3),
        onPrimary = Color(0xFF00344A),
        primaryContainer = Color(0xFF1B4A5A),
        onPrimaryContainer = Color(0xFFE3F2F7),
        secondary = Color(0xFFFEAE57),
        onSecondary = Color(0xFF3A2300),
        secondaryContainer = Color(0xFF5C3B10),
        onSecondaryContainer = Color(0xFFFFDDB8),
        tertiary = Color(0xFFFEAE57),
        onTertiary = Color(0xFF3A2300),
        tertiaryContainer = Color(0xFF3D2E20),
        onTertiaryContainer = Color(0xFFFFDCC0),
        background = Color(0xFF0F1E24),
        onBackground = Color(0xFFE3EEF1),
        surface = Color(0xFF16282F),
        onSurface = Color(0xFFE3EEF1),
        surfaceVariant = Color(0xFF1D333B),
        onSurfaceVariant = Color(0xFF9DB0B6),
        outline = Color(0xFF2E444C),
        outlineVariant = Color(0xFF243840),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    ),
    statusLight = SharedStatusLight,
    statusDark = SharedStatusDark,
)

/** Super Admin app: plain slate, Material 3 neutral surfaces (section 14.4). */
val SlatePalette = BrandPalette(
    light = lightColorScheme(
        primary = Color(0xFF455A64),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCFE6F1),
        onPrimaryContainer = Color(0xFF071E26),
        secondary = Color(0xFF4F616B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD2E5F0),
        onSecondaryContainer = Color(0xFF0B1E26),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFB0C4CE),
        onPrimary = Color(0xFF1B3038),
        primaryContainer = Color(0xFF334A54),
        onPrimaryContainer = Color(0xFFCFE6F1),
        secondary = Color(0xFFB6C9D4),
        onSecondary = Color(0xFF20333C),
        secondaryContainer = Color(0xFF374953),
        onSecondaryContainer = Color(0xFFD2E5F0),
    ),
    statusLight = SharedStatusLight,
    statusDark = SharedStatusDark,
)
