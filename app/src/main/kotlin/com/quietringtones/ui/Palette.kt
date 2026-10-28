package com.quietringtones.ui

import kotlin.math.abs
import kotlin.math.pow

/** Matemática de color sobre enteros ARGB, sin dependencias de Android ni de Compose (se prueba en JVM). */
object PaletteMath {
    const val MIN_CONTRAST = 4.5

    fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    fun mix(from: Int, to: Int, amount: Float): Int {
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * amount).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** Color a partir de tono (0-360), saturación y luminosidad (0-1). */
    fun hsl(hue: Float, saturation: Float, lightness: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = lightness - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun to255(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (to255(r) shl 16) or (to255(g) shl 8) or to255(b)
    }

    /** Texto sobre botones y zonas de color: blanco o negro puro, el de más contraste (siempre ≥ 4,5). */
    fun readableOn(background: Int): Int {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        return if (contrast(background, white) >= contrast(background, black)) white else black
    }

    /** Texto principal sobre un fondo de pantalla: blanco hueso en fondos oscuros, grafito en claros. */
    fun textOn(background: Int): Int =
        if (luminance(background) < 0.4) 0xFFE8ECF2.toInt() else 0xFF14181D.toInt()

    /** Acerca [accent] al blanco (fondo oscuro) o al negro (fondo claro) hasta que se lea sobre [background]. */
    fun ensureContrast(accent: Int, background: Int, dark: Boolean): Int {
        val target = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        var result = accent
        var step = 0
        while (contrast(result, background) < MIN_CONTRAST && step < 10) {
            step++
            result = mix(accent, target, step / 10f)
        }
        return result
    }
}

/** Colores ya derivados de una paleta, listos para armar el esquema de Material. */
data class PaletteColors(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val outlineVariant: Int,
)

/** Todo sale de los seis valores de la paleta: modo, fondo, tarjeta, acento y segundo acento. */
fun derive(palette: Palette): PaletteColors {
    val bg = palette.background
    val text = PaletteMath.textOn(bg)
    val primary = PaletteMath.ensureContrast(palette.accent, bg, palette.dark)
    val container = PaletteMath.mix(bg, primary, if (palette.dark) 0.30f else 0.18f)
    return PaletteColors(
        primary = primary,
        onPrimary = PaletteMath.readableOn(primary),
        primaryContainer = container,
        onPrimaryContainer = PaletteMath.readableOn(container),
        secondary = PaletteMath.ensureContrast(palette.accent2, bg, palette.dark),
        background = bg,
        onBackground = text,
        surface = palette.card,
        surfaceVariant = PaletteMath.mix(palette.card, text, 0.12f),
        onSurfaceVariant = PaletteMath.mix(text, palette.card, 0.30f),
        outlineVariant = PaletteMath.mix(bg, text, 0.20f),
    )
}
