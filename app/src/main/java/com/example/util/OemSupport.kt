package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.R

/**
 * Resolves standard and vendor-specific system settings screens for battery optimization
 * and background auto-start across Android devices, falling back cleanly to App Details.
 */
object OemSupport {

    private enum class Vendor {
        XIAOMI,
        SAMSUNG,
        ONEPLUS_OPPO,
        VIVO,
        HUAWEI_HONOR,
        ASUS,
        GENERIC
    }

    private fun detectVendor(): Vendor {
        val combined = "${Build.MANUFACTURER.orEmpty()} ${Build.BRAND.orEmpty()}".lowercase()
        return when {
            combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco") -> Vendor.XIAOMI
            combined.contains("samsung") -> Vendor.SAMSUNG
            combined.contains("oneplus") || combined.contains("oppo") || combined.contains("realme") -> Vendor.ONEPLUS_OPPO
            combined.contains("vivo") || combined.contains("iqoo") -> Vendor.VIVO
            combined.contains("huawei") || combined.contains("honor") -> Vendor.HUAWEI_HONOR
            combined.contains("asus") -> Vendor.ASUS
            else -> Vendor.GENERIC
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
    }

    fun universalBackgroundHint(): String =
        "For reliable background operation on any Android device, set Battery usage to Unrestricted and allow Auto-Start / Background activity in system settings."

    fun openBatterySettings(context: Context) {
        val pkg = context.packageName
        val appName = context.getString(R.string.app_name)
        val candidates = buildList {
            if (detectVendor() == Vendor.XIAOMI) {
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.powerkeeper",
                            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                        )
                        putExtra("package_name", pkg)
                        putExtra("package_label", appName)
                    }
                )
            }
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(appDetailsIntent(pkg))
        }
        launchFirstAvailable(context, candidates)
    }

    fun openAutoStartSettings(context: Context) {
        val pkg = context.packageName
        val candidates = when (detectVendor()) {
            Vendor.XIAOMI -> listOf(
                componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                Intent("miui.intent.action.OP_AUTO_START").apply { addCategory(Intent.CATEGORY_DEFAULT) },
                appDetailsIntent(pkg)
            )
            Vendor.SAMSUNG -> listOf(
                componentIntent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                componentIntent("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                appDetailsIntent(pkg)
            )
            Vendor.ONEPLUS_OPPO -> listOf(
                componentIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                componentIntent("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"),
                componentIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                appDetailsIntent(pkg)
            )
            Vendor.VIVO -> listOf(
                componentIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                componentIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                appDetailsIntent(pkg)
            )
            Vendor.HUAWEI_HONOR -> listOf(
                componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                appDetailsIntent(pkg)
            )
            Vendor.ASUS -> listOf(
                componentIntent("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
                appDetailsIntent(pkg)
            )
            Vendor.GENERIC -> listOf(appDetailsIntent(pkg))
        }
        launchFirstAvailable(context, candidates)
    }

    fun openAccessibilitySettings(context: Context) {
        launchFirstAvailable(context, listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
    }

    private fun componentIntent(pkg: String, cls: String): Intent = Intent().apply {
        component = ComponentName(pkg, cls)
    }

    private fun appDetailsIntent(pkg: String): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", pkg, null)
    )

    private fun launchFirstAvailable(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val started = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (started) return
        }
    }
}
