package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Automatically starts BezelKeepAliveService after device reboot, quick boot, or app update
 * whenever autoStartOnBoot and masterEnabled are enabled.
 */
class BezelBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val isSupportedAction = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"

        if (!isSupportedAction) return

        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cfg = runCatching {
                    BezelPreferencesRepository(appCtx).configFlow.first()
                }.getOrNull()
                if (cfg != null) {
                    BezelRuntimeBus.updateConfig(cfg)
                    if (cfg.masterEnabled && cfg.autoStartOnBoot && cfg.keepAliveForeground) {
                        BezelKeepAliveService.start(appCtx)
                    }
                } else {
                    // Direct boot before user unlock: start keep-alive proactively
                    BezelKeepAliveService.start(appCtx)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
