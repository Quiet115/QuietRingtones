package com.quietringtones.online

import android.content.Context

class CatalogPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("catalog", Context.MODE_PRIVATE)

    /** Mostrar el catálogo online en cada pestaña: activado de serie. */
    var showOnline: Boolean
        get() = prefs.getBoolean("show_online", true)
        set(value) = prefs.edit().putBoolean("show_online", value).apply()
}