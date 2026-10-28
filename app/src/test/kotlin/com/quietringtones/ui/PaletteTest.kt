package com.quietringtones.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaletteTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun `contrast between black and white is the maximum`() {
        assertEquals(21.0, PaletteMath.contrast(black, white), 0.01)
    }

    @Test
    fun `a dim accent on a dark background is lightened until it can be read`() {
        val dim = 0xFF1A2A6C.toInt()
        val bg = 0xFF0D0F12.toInt()

        val result = PaletteMath.ensureContrast(dim, bg, dark = true)

        assertTrue(PaletteMath.contrast(result, bg) >= PaletteMath.MIN_CONTRAST)
    }

    @Test
    fun `a pale accent on a light background is darkened until it can be read`() {
        val pale = 0xFFFFF3B0.toInt()
        val bg = 0xFFF7F7F4.toInt()

        val result = PaletteMath.ensureContrast(pale, bg, dark = false)

        assertTrue(PaletteMath.contrast(result, bg) >= PaletteMath.MIN_CONTRAST)
    }

    @Test
    fun `text on a button picks black or white by contrast`() {
        assertEquals(white, PaletteMath.readableOn(0xFF1A2A6C.toInt()))
        assertEquals(black, PaletteMath.readableOn(0xFFF1D595.toInt()))
        // En la zona intermedia, donde ni el blanco ni el negro llegan a 7, sigue superando 4,5.
        assertTrue(PaletteMath.contrast(PaletteMath.readableOn(0xFF4A5CD0.toInt()), 0xFF4A5CD0.toInt()) >= 4.5)
    }

    @Test
    fun `every shipped palette and every custom accent keeps readable text and accents`() {
        val seeds = PaletteId.entries.map { Appearance(palette = it).seed() } +
            AccentChoices.flatMap { accent ->
                listOf(true, false).flatMap { dark ->
                    val bases = if (dark) DarkBases else LightBases
                    bases.indices.map { i ->
                        Appearance(PaletteId.PERSONALIZADA, accent, dark, i).seed()
                    }
                }
            }
        seeds.forEach { seed ->
            val c = derive(seed)
            assertTrue(PaletteMath.contrast(c.onBackground, c.background) >= 7.0, "texto sobre fondo $seed")
            assertTrue(PaletteMath.contrast(c.primary, c.background) >= PaletteMath.MIN_CONTRAST, "acento $seed")
            assertTrue(PaletteMath.contrast(c.onPrimary, c.primary) >= PaletteMath.MIN_CONTRAST, "texto de botón $seed")
            assertTrue(PaletteMath.contrast(c.onSurfaceVariant, c.surface) >= PaletteMath.MIN_CONTRAST, "texto secundario $seed")
        }
    }

    @Test
    fun `hex colors are parsed strictly`() {
        assertEquals(0xFF3B4FE0.toInt(), PaletteMath.parseHex("#3b4fe0"))
        assertEquals(0xFF3B4FE0.toInt(), PaletteMath.parseHex(" 3B4FE0 "))
        assertNull(PaletteMath.parseHex("#3B4F"))
        assertNull(PaletteMath.parseHex("#GGGGGG"))
        assertEquals("#3B4FE0", PaletteMath.toHex(0xFF3B4FE0.toInt()))
    }
}
