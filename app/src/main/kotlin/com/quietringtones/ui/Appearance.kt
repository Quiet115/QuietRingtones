package com.quietringtones.ui

/**
 * Una paleta son seis valores: modo, fondo, tarjeta, acento y segundo acento.
 * Los ids y nombres son los mismos que en las demás apps Quiet, para que la elección se sienta igual.
 */
data class Palette(
    val id: String,
    val name: String,
    val dark: Boolean,
    val background: Int,
    val card: Int,
    val accent: Int,
    val accent2: Int,
    val group: PaletteGroup = if (dark) PaletteGroup.DARK else PaletteGroup.LIGHT,
)

enum class PaletteGroup { DARK, COLORFUL, LIGHT, SYSTEM }

/** Paleta propia: se guardan los parámetros y los colores se calculan, así siempre se leen bien. */
data class CustomPalette(
    val id: String,
    val name: String,
    val dark: Boolean,
    val accentHue: Float,
    val backgroundHue: Float,
    val tint: Float,
)

object Palettes {
    const val DEFAULT = "obsidian"
    const val WALLPAPER = "wallpaper"
    const val MAX_CUSTOM = 12

    val builtIn = listOf(
        Palette("midnight", "Midnight", true, 0xFF0B0F14.toInt(), 0xFF141A22.toInt(), 0xFF4FD1C5.toInt(), 0xFF8F9BFF.toInt()),
        Palette("obsidian", "Obsidian", true, 0xFF0D0D0F.toInt(), 0xFF18181C.toInt(), 0xFFE2B857.toInt(), 0xFFD98E73.toInt()),
        Palette("sunset", "Sunset", true, 0xFF150B20.toInt(), 0xFF22122F.toInt(), 0xFFFF6B9D.toInt(), 0xFFFFB86B.toInt(), PaletteGroup.COLORFUL),
        Palette("neon", "Neon", true, 0xFF0A0B1F.toInt(), 0xFF14173B.toInt(), 0xFFB06CFF.toInt(), 0xFF3DDCFF.toInt(), PaletteGroup.COLORFUL),
        Palette("paper", "Paper", false, 0xFFF4F6F8.toInt(), 0xFFFFFFFF.toInt(), 0xFF0B7A71.toInt(), 0xFF5B5BD6.toInt()),
        Palette("sky", "Sky", false, 0xFFF0F5FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF2A5FE0.toInt(), 0xFFD9542F.toInt()),
    )

    fun custom(c: CustomPalette): Palette {
        val bgSaturation = 0.02f + 0.28f * c.tint
        val background = PaletteMath.hsl(c.backgroundHue, bgSaturation, if (c.dark) 0.07f else 0.96f)
        val card = PaletteMath.hsl(c.backgroundHue, bgSaturation, if (c.dark) 0.11f else 0.995f)
        val lightness = if (c.dark) 0.64f else 0.40f
        return Palette(
            id = c.id,
            name = c.name,
            dark = c.dark,
            background = background,
            card = card,
            accent = PaletteMath.ensureContrast(PaletteMath.hsl(c.accentHue, 0.72f, lightness), background, c.dark),
            accent2 = PaletteMath.ensureContrast(PaletteMath.hsl(c.accentHue + 45f, 0.65f, lightness), background, c.dark),
            group = PaletteGroup.COLORFUL,
        )
    }
}

data class Appearance(
    val selectedId: String = Palettes.DEFAULT,
    val customs: List<CustomPalette> = emptyList(),
) {
    val isWallpaper: Boolean get() = selectedId == Palettes.WALLPAPER

    /** La paleta elegida; si ya no existe (por ejemplo, una propia borrada), Obsidian. */
    fun palette(): Palette =
        Palettes.builtIn.firstOrNull { it.id == selectedId }
            ?: customs.firstOrNull { it.id == selectedId }?.let(Palettes::custom)
            ?: Palettes.builtIn.first { it.id == Palettes.DEFAULT }

    /** Con "Wallpaper" el modo claro u oscuro lo manda el sistema. */
    fun isDark(systemDark: Boolean): Boolean = if (isWallpaper) systemDark else palette().dark
}
