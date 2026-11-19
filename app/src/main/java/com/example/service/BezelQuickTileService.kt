package com.example.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BezelQuickTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val repo = BezelPreferencesRepository(applicationContext)
            val cfg = runCatching { repo.configFlow.first() }.getOrDefault(BezelRuntimeBus.config.value)
            BezelRuntimeBus.updateConfig(cfg)
            updateTileState(cfg.masterEnabled)
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repo = BezelPreferencesRepository(applicationContext)
            val current = runCatching { repo.configFlow.first() }.getOrDefault(BezelRuntimeBus.config.value)
            val updated = current.copy(masterEnabled = !current.masterEnabled)
            BezelRuntimeBus.updateConfig(updated)
            repo.saveConfig(updated)

            if (updated.masterEnabled && updated.keepAliveForeground) {
                BezelKeepAliveService.start(applicationContext)
            } else if (!updated.masterEnabled) {
                BezelKeepAliveService.stop(applicationContext)
            }
            updateTileState(updated.masterEnabled)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun updateTileState(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(com.example.R.string.app_name)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = if (enabled) "Active" else "Paused"
        }
        tile.updateTile()
    }
}
