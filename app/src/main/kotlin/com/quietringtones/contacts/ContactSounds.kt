package com.quietringtones.contacts

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract.Contacts

/** Un contacto con un tono propio. Nada de esto se guarda en la app: se lee del teléfono cuando se muestra. */
data class ContactSound(val id: Long, val name: String, val soundName: String)

/** El contacto elegido en el selector del sistema, a la espera de que el usuario elija su sonido. */
data class PendingContact(val uri: Uri, val name: String)

/** Sonido por contacto: interruptor de la función, desactivada de serie. */
class ContactPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("contacts", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit().putBoolean("enabled", value).apply()
}

/** Lee y cambia el campo «tono» de los contactos del teléfono (ContactsContract), sin copiar ningún dato. */
class ContactSounds(private val context: Context) {
    private val resolver = context.contentResolver

    fun hasPermission(): Boolean = PERMISSIONS.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    fun assigned(fallbackName: String): List<ContactSound> {
        if (!hasPermission()) return emptyList()
        val result = mutableListOf<ContactSound>()
        resolver.query(
            Contacts.CONTENT_URI,
            arrayOf(Contacts._ID, Contacts.DISPLAY_NAME_PRIMARY, Contacts.CUSTOM_RINGTONE),
            "${Contacts.CUSTOM_RINGTONE} IS NOT NULL",
            null,
            "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val tone = cursor.getString(2) ?: continue
                result += ContactSound(cursor.getLong(0), cursor.getString(1).orEmpty(), titleOf(tone) ?: fallbackName)
            }
        }
        return result
    }

    fun nameOf(contactUri: Uri): String? =
        resolver.query(contactUri, arrayOf(Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    /** Pone [sound] como tono del contacto elegido en el selector. */
    fun assign(contactUri: Uri, sound: Uri) {
        val target = Contacts.lookupContact(resolver, contactUri) ?: contactUri
        update(target, sound.toString())
    }

    /** Quita el tono propio: el contacto vuelve a sonar con el tono predeterminado. */
    fun remove(contactId: Long) = update(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId), null)

    private fun update(contact: Uri, ringtone: String?) {
        val values = ContentValues().apply { put(Contacts.CUSTOM_RINGTONE, ringtone) }
        check(resolver.update(contact, values, null, null) > 0) { "Couldn't change the contact" }
    }

    private fun titleOf(uri: String): String? =
        runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }
            .getOrNull()?.takeIf { it.isNotBlank() }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
    }
}