package com.quietringtones.trim

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class WavHeaderTest {
    @Test
    fun `the WAV header describes 16-bit stereo PCM and the data size`() {
        val header = AudioTrimmer.wavHeader(sampleRate = 44_100, channels = 2, dataBytes = 1_000)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(44, header.size)
        assertEquals("RIFF", String(header, 0, 4))
        assertEquals(1_036, buffer.getInt(4))
        assertEquals("WAVEfmt ", String(header, 8, 8))
        assertEquals(1, buffer.getShort(20).toInt())
        assertEquals(2, buffer.getShort(22).toInt())
        assertEquals(44_100, buffer.getInt(24))
        assertEquals(176_400, buffer.getInt(28))
        assertEquals(4, buffer.getShort(32).toInt())
        assertEquals(16, buffer.getShort(34).toInt())
        assertEquals("data", String(header, 36, 4))
        assertEquals(1_000, buffer.getInt(40))
    }
}
