package com.bharavahi.org.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.bharavahi.org.R

/** Plus Jakarta Sans, the font of the Stitch design, from one bundled variable font file. */
@OptIn(ExperimentalTextApi::class)
private val PlusJakartaSans = FontFamily(
    listOf(
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
    ).map { weight ->
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

/** The Material 3 type scale with Plus Jakarta Sans applied to every style. */
val OrgTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = PlusJakartaSans),
        displayMedium = base.displayMedium.copy(fontFamily = PlusJakartaSans),
        displaySmall = base.displaySmall.copy(fontFamily = PlusJakartaSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = PlusJakartaSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakartaSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = PlusJakartaSans),
        titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans),
        titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans),
        titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = PlusJakartaSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
        bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
        labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans),
        labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans),
        labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans),
    )
}
