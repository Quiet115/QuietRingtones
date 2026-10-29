package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeSwitches(
    var allowed: Boolean = true,
    var obeys: Boolean = true,
    private val values: MutableMap<SystemSwitch, Boolean> = SystemSwitch.entries.associateWith { true }.toMutableMap(),
) : SystemSwitches {
    override fun canModify() = allowed
    override fun isOn(switch: SystemSwitch) = values.getValue(switch)
    override fun set(switch: SystemSwitch, on: Boolean) {
        if (obeys) values[switch] = on
    }
}

class OtherSoundsTest {
    @Test
    fun `states lists every switch as the system reports it`() {
        val fake = FakeSwitches()
        fake.set(SystemSwitch.DIAL_PAD, false)

        assertEquals(
            mapOf(SystemSwitch.TOUCH to true, SystemSwitch.DIAL_PAD to false, SystemSwitch.LOCK to true),
            OtherSounds(fake).states(),
        )
    }

    @Test
    fun `turning a switch off changes only that switch`() {
        val fake = FakeSwitches()
        val result = OtherSounds(fake).set(SystemSwitch.TOUCH, false)

        assertEquals(SwitchResult.Changed(false), result)
        assertEquals(false, fake.isOn(SystemSwitch.TOUCH))
        assertEquals(true, fake.isOn(SystemSwitch.LOCK))
    }

    @Test
    fun `without the permission nothing is changed`() {
        val fake = FakeSwitches(allowed = false)
        val result = OtherSounds(fake).set(SystemSwitch.LOCK, false)

        assertEquals(SwitchResult.NeedsPermission, result)
        assertEquals(true, fake.isOn(SystemSwitch.LOCK))
    }

    @Test
    fun `if the phone ignores the change the result reports the real value`() {
        val fake = FakeSwitches(obeys = false)

        assertEquals(SwitchResult.Changed(true), OtherSounds(fake).set(SystemSwitch.DIAL_PAD, false))
    }
}
