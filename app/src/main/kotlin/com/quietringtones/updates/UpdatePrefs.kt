package com.quietringtones.updates

import android.content.Context

class UpdatePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /** Buscar al abrir y descargar con Wi-Fi: activado de serie. */
    var automatic: Boolean
        get() = prefs.getBoolean("automatic", true)
        set(value) = prefs.edit().putBoolean("automatic", value).apply()

    var lastChecked: Long
        get() = prefs.getLong("last_checked", 0L)
        set(value) = prefs.edit().putLong("last_checked", value).apply()

    var skippedVersion: String?
        get() = prefs.getString("skipped", null)
        set(value) = prefs.edit().putString("skipped", value).apply()
}
