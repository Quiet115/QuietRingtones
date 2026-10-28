package com.quietringtones.ui

/** Paletas incluidas. Obsidiana es la de serie. */
enum class PaletteId { OBSIDIANA, FIELTRO, PERSONALIZADA }

/** Fondos que se pueden elegir en la paleta personalizada, según sea oscura o clara. */
val DarkBases = listOf(0xFF0D0F12.toInt(), 0xFF12171E.toInt(), 0xFF0F1512.toInt(), 0xFF181111.toInt())
val LightBases = listOf(0xFFE9EBEE.toInt(), 0xFFF7F7F4.toInt(), 0xFFE5EEE9.toInt(), 0xFFF1E8E8.toInt())

val AccentChoices = listOf(
    0xFF6FD3E6.toInt(), 0xFF3B4FE0.toInt(), 0xFFE0566B.toInt(), 0xFF4CAF7A.toInt(),
    0xFFE0A03B.toInt(), 0xFFA66CF0.toInt(), 0xFFE07B39.toInt(), 0xFFC9CED6.toInt(),
)

data class Appearance(
    val palette: PaletteId = PaletteId.OBSIDIANA,
    val customAccent: Int = AccentChoices[0],
    val customDark: Boolean = true,
    val customBase: Int = 0,
) {
    /** Colores de partida de la paleta elegida. */
    fun seed(): PaletteSeed = when (palette) {
        PaletteId.OBSIDIANA -> PaletteSeed(AccentChoices[0], DarkBases[0], dark = true)
        PaletteId.FIELTRO -> PaletteSeed(AccentChoices[1], LightBases[0], dark = false)
        PaletteId.PERSONALIZADA -> {
            val bases = if (customDark) DarkBases else LightBases
            PaletteSeed(customAccent, bases[customBase.coerceIn(bases.indices)], customDark)
        }
    }
}
