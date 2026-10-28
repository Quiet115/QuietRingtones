package com.quietringtones

import com.quietringtones.core.Profile
import com.quietringtones.core.Selection
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundKind
import org.json.JSONArray
import org.json.JSONObject

object StoreCodec {

    fun encodeSelections(selections: Map<SoundKind, Selection>): String =
        selectionsJson(selections).toString()

    fun decodeSelections(text: String?): Map<SoundKind, Selection> =
        if (text.isNullOrBlank()) emptyMap() else selectionsFrom(JSONObject(text))

    fun encodeProfiles(profiles: List<Profile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("selections", selectionsJson(profile.selections)),
            )
        }
        return array.toString()
    }

    fun decodeProfiles(text: String?): List<Profile> {
        if (text.isNullOrBlank()) return emptyList()
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            Profile(item.getString("id"), item.getString("name"), selectionsFrom(item.getJSONObject("selections")))
        }
    }

    fun encodeImported(imported: Map<SoundKind, List<String>>): String {
        val json = JSONObject()
        imported.forEach { (kind, ids) ->
            val array = JSONArray()
            ids.forEach { array.put(it) }
            json.put(kind.name, array)
        }
        return json.toString()
    }

    fun decodeImported(text: String?): Map<SoundKind, List<String>> {
        if (text.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(text)
            SoundKind.entries.mapNotNull { kind ->
                val array = json.optJSONArray(kind.name) ?: return@mapNotNull null
                kind to (0 until array.length()).map { SoundId.normalize(array.getString(it)) }.distinct()
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun selectionsJson(selections: Map<SoundKind, Selection>): JSONObject {
        val json = JSONObject()
        selections.forEach { (kind, selection) ->
            json.put(
                kind.name,
                when (selection) {
                    Selection.Silent -> JSONObject().put("silent", true)
                    is Selection.Chosen -> JSONObject()
                        .put("id", selection.sound.id)
                        .put("name", selection.sound.name)
                },
            )
        }
        return json
    }

    private fun selectionsFrom(json: JSONObject): Map<SoundKind, Selection> =
        SoundKind.entries.mapNotNull { kind ->
            val item = json.optJSONObject(kind.name) ?: return@mapNotNull null
            kind to if (item.optBoolean("silent")) {
                Selection.Silent
            } else {
                Selection.Chosen(Sound(SoundId.normalize(item.getString("id")), item.getString("name")))
            }
        }.toMap()
}
