package com.quietringtones.ui

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

    fun parseHex(text: String): Int? {
        val clean = text.trim().removePrefix("#")
        if (clean.length != 6) return null
        val value = clean.toIntOrNull(16) ?: return null
        return (0xFF shl 24) or value
    }

    fun toHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)
}

/** Colores base de un tema: con estos dos y el modo se deriva todo el esquema. */
data class PaletteSeed(val accent: Int, val background: Int, val dark: Boolean)

data class PaletteColors(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val outlineVariant: Int,
)

fun derive(seed: PaletteSeed): PaletteColors {
    val bg = seed.background
    val text = PaletteMath.textOn(bg)
    val primary = PaletteMath.ensureContrast(seed.accent, bg, seed.dark)
    val container = PaletteMath.mix(bg, primary, if (seed.dark) 0.30f else 0.18f)
    return PaletteColors(
        primary = primary,
        onPrimary = PaletteMath.readableOn(primary),
        primaryContainer = container,
        onPrimaryContainer = PaletteMath.readableOn(container),
        background = bg,
        onBackground = text,
        surface = PaletteMath.mix(bg, text, 0.06f),
        surfaceVariant = PaletteMath.mix(bg, text, 0.14f),
        onSurfaceVariant = PaletteMath.mix(text, bg, 0.30f),
        outlineVariant = PaletteMath.mix(bg, text, 0.20f),
    )
}
