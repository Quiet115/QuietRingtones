package com.quietringtones

import android.content.Context
import com.quietringtones.core.Profile
import com.quietringtones.core.Selection
import com.quietringtones.core.SoundKind
import com.quietringtones.core.Store

class PrefsStore(context: Context) : Store {
    private val prefs = context.getSharedPreferences("quietringtones", Context.MODE_PRIVATE)

    override fun loadOriginal(): Map<SoundKind, Selection> =
        StoreCodec.decodeSelections(prefs.getString(KEY_ORIGINAL, null))

    override fun saveOriginal(original: Map<SoundKind, Selection>) {
        prefs.edit().putString(KEY_ORIGINAL, StoreCodec.encodeSelections(original)).commit()
    }

    override fun loadProfiles(): List<Profile> =
        StoreCodec.decodeProfiles(prefs.getString(KEY_PROFILES, null))

    override fun saveProfiles(profiles: List<Profile>) {
        prefs.edit().putString(KEY_PROFILES, StoreCodec.encodeProfiles(profiles)).commit()
    }

    private companion object {
        const val KEY_ORIGINAL = "original"
        const val KEY_PROFILES = "profiles"
    }
}
