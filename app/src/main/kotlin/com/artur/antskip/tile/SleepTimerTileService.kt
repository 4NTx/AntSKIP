package com.artur.antskip.tile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.service.SleepTimerService

class SleepTimerTileService : TileService() {
    private val preferences by lazy { PreferenceStore(this) }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (SleepTimerService.isRunning) {
            val stopIntent = Intent(this, SleepTimerService::class.java).apply {
                action = SleepTimerService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            val durationMinutes = preferences.sleepTimerDurationMinutes
            val startIntent = Intent(this, SleepTimerService::class.java).apply {
                action = SleepTimerService.ACTION_START
                putExtra(SleepTimerService.EXTRA_DURATION_SECONDS, durationMinutes * 60L)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        if (SleepTimerService.isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Sono: ${SleepTimerService.formatSeconds(SleepTimerService.remainingSeconds)}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Toque para parar"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Temporizador"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val totalMin = preferences.sleepTimerDurationMinutes
                val h = totalMin / 60
                val m = totalMin % 60
                val formatted = when {
                    h > 0 && m > 0 -> "${h}h ${m}m"
                    h > 0 -> "${h}h"
                    else -> "$totalMin min"
                }
                tile.subtitle = formatted
            }
        }
        tile.updateTile()
    }

    companion object {
        fun requestUpdate(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, SleepTimerTileService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
