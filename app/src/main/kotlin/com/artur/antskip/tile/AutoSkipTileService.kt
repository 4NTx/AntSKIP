package com.artur.antskip.tile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.artur.antskip.accessibility.AntSkipAccessibilityService
import com.artur.antskip.data.PreferenceStore

class AutoSkipTileService : TileService() {
    private val preferences by lazy { PreferenceStore(this) }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isServiceRunning = AntSkipAccessibilityService.instance != null
        if (!isServiceRunning) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        val newState = !preferences.isAutomationEnabled
        preferences.setAutomationEnabled(newState)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isServiceRunning = AntSkipAccessibilityService.instance != null
        val isEnabled = preferences.isAutomationEnabled
        val isScheduleActive = preferences.isScheduleActiveNow()

        if (!isServiceRunning) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "AntSKIP"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Toque p/ ativar serviço"
            }
        } else if (!isEnabled) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "AntSKIP"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Pausado"
            }
        } else if (!isScheduleActive) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "AntSKIP"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Fora do horário"
            }
        } else {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "AntSKIP"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Pular ativo"
            }
        }
        tile.updateTile()
    }

    companion object {
        fun requestUpdate(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, AutoSkipTileService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
