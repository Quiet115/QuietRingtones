package com.quietringtones

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.quietringtones.core.ProfileResult
import com.quietringtones.core.QuietRingtones
import com.quietringtones.ui.LanguageStore
import com.quietringtones.ui.withLanguage

/** Tile de ajustes rápidos: un toque pasa al perfil siguiente y muestra cuál está activo. */
class ProfileTileService : TileService() {

    private fun quiet() = QuietRingtones(AndroidSoundSystem(this), PrefsStore(this))

    // El servicio no pasa por MainActivity: los textos se piden en el idioma elegido en la app.
    private fun text(res: Int): String = withLanguage(LanguageStore(this).load()).getString(res)

    override fun onStartListening() = refresh()

    override fun onClick() {
        val app = quiet()
        if (!app.canModify()) {
            openApp()
            return
        }
        when (app.switchToNextProfile()) {
            null -> openApp()
            ProfileResult.NeedsPermission, ProfileResult.NotFound -> openApp()
            is ProfileResult.Done -> refresh()
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val app = quiet()
        val active = app.activeProfile()
        tile.label = when {
            active == null -> text(R.string.tile_label)
            active.id == QuietRingtones.ORIGINAL_ID -> text(R.string.profile_original)
            else -> active.name
        }
        tile.subtitle = text(if (active == null) R.string.tile_pick else R.string.tile_switch)
        tile.state = if (active == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.updateTile()
    }

    /** Sin permiso o sin perfiles, lo útil es abrir la app, donde se resuelve. */
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}