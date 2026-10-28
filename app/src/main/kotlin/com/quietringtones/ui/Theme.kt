package com.quietringtones.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun colorScheme(seed: PaletteSeed): ColorScheme {
    val c = derive(seed)
    val base = if (seed.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(c.primary),
        onPrimary = Color(c.onPrimary),
        primaryContainer = Color(c.primaryContainer),
        onPrimaryContainer = Color(c.onPrimaryContainer),
        secondaryContainer = Color(c.primaryContainer),
        onSecondaryContainer = Color(c.onPrimaryContainer),
        background = Color(c.background),
        onBackground = Color(c.onBackground),
        surface = Color(c.surface),
        onSurface = Color(c.onBackground),
        surfaceVariant = Color(c.surfaceVariant),
        onSurfaceVariant = Color(c.onSurfaceVariant),
        outlineVariant = Color(c.outlineVariant),
    )
}

private val QuietTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)

/** Colores de la donación: un dorado de buñuelo, fuera del esquema para que no se mezcle con el acento. */
data class DonateColors(val container: Color, val content: Color)

@Composable
fun donateColors(): DonateColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DonateColors(Color(0xFF5A3F0A), Color(0xFFFFDFA6))
    else DonateColors(Color(0xFFF1D595), Color(0xFF4A3000))

@Composable
fun QuietTheme(appearance: Appearance, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme(appearance.seed()),
        typography = QuietTypography,
        content = content,
    )
}
