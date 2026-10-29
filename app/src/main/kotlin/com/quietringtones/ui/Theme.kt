package com.quietringtones.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun colorScheme(palette: Palette): ColorScheme {
    val c = derive(palette)
    val base = if (palette.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        tertiary = Color(c.secondary),
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

/** Colores de la donación: un dorado cálido, fuera del esquema para que no se mezcle con el acento. */
data class DonateColors(val container: Color, val content: Color)

@Composable
fun donateColors(): DonateColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DonateColors(Color(0xFF5A3F0A), Color(0xFFFFDFA6))
    else DonateColors(Color(0xFFF1D595), Color(0xFF4A3000))

@Composable
fun QuietTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val scheme = if (appearance.isWallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        colorScheme(appearance.palette())
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = QuietTypography,
        content = content,
    )
}
