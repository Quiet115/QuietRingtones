package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeAdvisorTest {
    private fun advise(
        kind: SoundKind = SoundKind.NOTIFICATION,
        index: Int = 10,
        max: Int = 15,
        ringer: RingerMode = RingerMode.NORMAL,
        dnd: DoNotDisturb = DoNotDisturb.OFF,
    ) = VolumeAdvisor.advise(kind, Volume(index, max), ringer, dnd)

    @Test
    fun `a comfortable volume needs no warning`() {
        assertEquals(VolumeAdvice.OK, advise(index = 10))
    }

    @Test
    fun `zero volume is reported as muted`() {
        assertEquals(VolumeAdvice.MUTED, advise(index = 0))
    }

    @Test
    fun `a quarter of the volume or less is reported as low`() {
        assertEquals(VolumeAdvice.LOW, advise(index = 3, max = 15))
        assertEquals(VolumeAdvice.LOW, advise(index = 1, max = 15))
        assertEquals(VolumeAdvice.OK, advise(index = 4, max = 15))
    }

    @Test
    fun `silent and vibrate mode silence ringtones and notifications whatever the volume`() {
        assertEquals(VolumeAdvice.SILENT_MODE, advise(SoundKind.RINGTONE, index = 15, ringer = RingerMode.SILENT))
        assertEquals(VolumeAdvice.VIBRATE_MODE, advise(SoundKind.NOTIFICATION, index = 15, ringer = RingerMode.VIBRATE))
    }

    @Test
    fun `the ringer mode does not affect alarms`() {
        assertEquals(VolumeAdvice.OK, advise(SoundKind.ALARM, index = 10, ringer = RingerMode.SILENT))
    }

    @Test
    fun `do not disturb that allows only alarms silences calls and notifications but not alarms`() {
        assertEquals(VolumeAdvice.DO_NOT_DISTURB, advise(SoundKind.RINGTONE, dnd = DoNotDisturb.ALARMS_ONLY))
        assertEquals(VolumeAdvice.OK, advise(SoundKind.ALARM, dnd = DoNotDisturb.ALARMS_ONLY))
    }

    @Test
    fun `total silence blocks everything, alarms included`() {
        SoundKind.entries.forEach {
            assertEquals(VolumeAdvice.DO_NOT_DISTURB, advise(it, dnd = DoNotDisturb.TOTAL_SILENCE))
        }
    }

    @Test
    fun `priority-only do not disturb gives no warning of its own`() {
        assertEquals(VolumeAdvice.OK, advise(dnd = DoNotDisturb.PRIORITY))
    }

    @Test
    fun `the mode that silences the sound wins over a low volume`() {
        assertEquals(VolumeAdvice.SILENT_MODE, advise(index = 0, ringer = RingerMode.SILENT))
    }

    @Test
    fun `a stream with no known range only warns when it is at zero`() {
        assertEquals(VolumeAdvice.OK, advise(index = 1, max = 0))
        assertEquals(VolumeAdvice.MUTED, advise(index = 0, max = 0))
    }
}
