package com.quietringtones.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** JSON de las paletas propias; aparte de Android para poder probarlo en JVM. */
object AppearanceCodec {
    fun encode(customs: List<CustomPalette>): String {
        val array = JSONArray()
        customs.forEach { c ->
            array.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("dark", c.dark)
                    .put("accentHue", c.accentHue.toDouble())
                    .put("backgroundHue", c.backgroundHue.toDouble())
                    .put("tint", c.tint.toDouble()),
            )
        }
        return array.toString()
    }

    fun decode(text: String?): List<CustomPalette> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(text)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                CustomPalette(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    dark = o.getBoolean("dark"),
                    accentHue = o.getDouble("accentHue").toFloat(),
                    backgroundHue = o.getDouble("backgroundHue").toFloat(),
                    tint = o.getDouble("tint").toFloat(),
                )
            }
        }.getOrDefault(emptyList()).take(Palettes.MAX_CUSTOM)
    }
}

class AppearanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun load(): Appearance = Appearance(
        selectedId = prefs.getString("palette", null) ?: Palettes.DEFAULT,
        customs = AppearanceCodec.decode(prefs.getString("custom_palettes", null)),
    )

    fun save(appearance: Appearance) {
        prefs.edit()
            .putString("palette", appearance.selectedId)
            .putString("custom_palettes", AppearanceCodec.encode(appearance.customs))
            .apply()
    }
}
