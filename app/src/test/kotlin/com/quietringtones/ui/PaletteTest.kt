package com.quietringtones.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaletteTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    private fun customs(): List<Palette> =
        listOf(true, false).flatMap { dark ->
            (0 until 360 step 40).flatMap { accentHue ->
                (0 until 360 step 90).flatMap { bgHue ->
                    listOf(0f, 0.5f, 1f).map { tint ->
                        Palettes.custom(CustomPalette("c", "c", dark, accentHue.toFloat(), bgHue.toFloat(), tint))
                    }
                }
            }
        }

    @Test
    fun `contrast between black and white is the maximum`() {
        assertEquals(21.0, PaletteMath.contrast(black, white), 0.01)
    }

    @Test
    fun `hsl produces the expected primaries`() {
        assertEquals(0xFFFF0000.toInt(), PaletteMath.hsl(0f, 1f, 0.5f))
        assertEquals(0xFF00FF00.toInt(), PaletteMath.hsl(120f, 1f, 0.5f))
        assertEquals(0xFF0000FF.toInt(), PaletteMath.hsl(240f, 1f, 0.5f))
        assertEquals(0xFF808080.toInt(), PaletteMath.hsl(200f, 0f, 0.5f))
    }

    @Test
    fun `a dim accent on a dark background is lightened until it can be read`() {
        val bg = 0xFF0D0F12.toInt()
        val result = PaletteMath.ensureContrast(0xFF1A2A6C.toInt(), bg, dark = true)
        assertTrue(PaletteMath.contrast(result, bg) >= PaletteMath.MIN_CONTRAST)
    }

    @Test
    fun `a pale accent on a light background is darkened until it can be read`() {
        val bg = 0xFFF7F7F4.toInt()
        val result = PaletteMath.ensureContrast(0xFFFFF3B0.toInt(), bg, dark = false)
        assertTrue(PaletteMath.contrast(result, bg) >= PaletteMath.MIN_CONTRAST)
    }

    @Test
    fun `text on a button picks black or white and always passes 4 point 5`() {
        assertEquals(white, PaletteMath.readableOn(0xFF1A2A6C.toInt()))
        assertEquals(black, PaletteMath.readableOn(0xFFF1D595.toInt()))
        assertTrue(PaletteMath.contrast(PaletteMath.readableOn(0xFF4A5CD0.toInt()), 0xFF4A5CD0.toInt()) >= 4.5)
    }

    @Test
    fun `obsidian is the default and uses the family values`() {
        val obsidian = Appearance().palette()
        assertEquals("obsidian", obsidian.id)
        assertEquals(0xFF0D0D0F.toInt(), obsidian.background)
        assertEquals(0xFF18181C.toInt(), obsidian.card)
        assertEquals(0xFFE2B857.toInt(), obsidian.accent)
        assertEquals(0xFFD98E73.toInt(), obsidian.accent2)
    }

    @Test
    fun `an unknown selection falls back to obsidian`() {
        assertEquals("obsidian", Appearance(selectedId = "custom:gone").palette().id)
    }

    @Test
    fun `every built-in and custom palette passes the contrast rules`() {
        (Palettes.builtIn + customs()).forEach { p ->
            val c = derive(p)
            val label = "${p.id} dark=${p.dark} bg=${PaletteMath_hex(p.background)}"
            assertTrue(PaletteMath.contrast(c.onBackground, c.background) >= 7.0, "text on background: $label")
            assertTrue(PaletteMath.contrast(c.onBackground, c.surface) >= 7.0, "text on card: $label")
            assertTrue(PaletteMath.contrast(c.onSurfaceVariant, c.surface) >= 4.5, "muted text on card: $label")
            assertTrue(PaletteMath.contrast(c.primary, c.background) >= 3.0, "accent on background: $label")
            assertTrue(PaletteMath.contrast(c.onPrimary, c.primary) >= 4.5, "text on accent: $label")
        }
    }

    @Test
    fun `custom palettes survive a save and load`() {
        val list = listOf(CustomPalette("custom:1", "Mine", true, 200f, 250f, 0.4f))
        assertEquals(list, AppearanceCodec.decode(AppearanceCodec.encode(list)))
        assertTrue(AppearanceCodec.decode("not json").isEmpty())
        assertTrue(AppearanceCodec.decode(null).isEmpty())
    }

    @Test
    fun `at most twelve custom palettes are kept`() {
        val many = (1..15).map { CustomPalette("custom:$it", "P$it", true, 10f, 10f, 0.1f) }
        assertEquals(Palettes.MAX_CUSTOM, AppearanceCodec.decode(AppearanceCodec.encode(many)).size)
    }

    private fun PaletteMath_hex(argb: Int) = "#%06X".format(argb and 0xFFFFFF)
}
