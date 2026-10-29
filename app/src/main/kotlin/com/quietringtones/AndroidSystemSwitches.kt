package com.quietringtones

import android.content.Context
import android.provider.Settings
import com.quietringtones.core.SystemSwitch
import com.quietringtones.core.SystemSwitches

/** Interruptores de sonido guardados en Settings.System: se escriben con el mismo permiso que los tonos. */
class AndroidSystemSwitches(private val context: Context) : SystemSwitches {
    // «lockscreen_sounds_enabled» no tiene constante pública en el SDK, pero la tabla del sistema la usa en todos los móviles.
    private fun key(switch: SystemSwitch) = when (switch) {
        SystemSwitch.TOUCH -> Settings.System.SOUND_EFFECTS_ENABLED
        SystemSwitch.DIAL_PAD -> Settings.System.DTMF_TONE_WHEN_DIALING
        SystemSwitch.LOCK -> "lockscreen_sounds_enabled"
    }

    override fun canModify() = Settings.System.canWrite(context)

    override fun isOn(switch: SystemSwitch) =
        Settings.System.getInt(context.contentResolver, key(switch), 1) != 0

    override fun set(switch: SystemSwitch, on: Boolean) {
        Settings.System.putInt(context.contentResolver, key(switch), if (on) 1 else 0)
    }
}
