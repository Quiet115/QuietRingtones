package com.quietringtones.ui

import android.content.Context

class AppearanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun load(): Appearance {
        val default = Appearance()
        val palette = runCatching { PaletteId.valueOf(prefs.getString("palette", null) ?: "") }
            .getOrDefault(default.palette)
        return Appearance(
            palette = palette,
            customAccent = prefs.getInt("accent", default.customAccent),
            customDark = prefs.getBoolean("dark", default.customDark),
            customBase = prefs.getInt("base", default.customBase),
        )
    }

    fun save(appearance: Appearance) {
        prefs.edit()
            .putString("palette", appearance.palette.name)
            .putInt("accent", appearance.customAccent)
            .putBoolean("dark", appearance.customDark)
            .putInt("base", appearance.customBase)
            .apply()
    }
}
