package com.quietringtones.core

/** Sonidos del sistema que Android deja activar o desactivar, pero no sustituir por otro. */
enum class SystemSwitch { TOUCH, DIAL_PAD, LOCK }

interface SystemSwitches {
    fun canModify(): Boolean
    fun isOn(switch: SystemSwitch): Boolean
    fun set(switch: SystemSwitch, on: Boolean)
}

sealed interface SwitchResult {
    data object NeedsPermission : SwitchResult
    data class Changed(val on: Boolean) : SwitchResult
}

class OtherSounds(private val switches: SystemSwitches) {
    fun states(): Map<SystemSwitch, Boolean> = SystemSwitch.entries.associateWith(switches::isOn)

    /** Devuelve el valor que quedó de verdad: si el fabricante ignora el cambio, la pantalla no miente. */
    fun set(switch: SystemSwitch, on: Boolean): SwitchResult {
        if (!switches.canModify()) return SwitchResult.NeedsPermission
        switches.set(switch, on)
        return SwitchResult.Changed(switches.isOn(switch))
    }
}
