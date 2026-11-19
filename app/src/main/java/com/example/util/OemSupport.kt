package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.R

enum class OemVendor {
    XIAOMI,
    SAMSUNG,
    ONEPLUS_OPPO,
    VIVO,
    HUAWEI_HONOR,
    ASUS,
    STANDARD
}

object OemSupport {

    fun detectVendor(): OemVendor {
        val mfr = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        val combined = "$mfr $brand"

        return when {
            combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco") -> OemVendor.XIAOMI
            combined.contains("samsung") -> OemVendor.SAMSUNG
            combined.contains("oneplus") || combined.contains("oppo") || combined.contains("realme") -> OemVendor.ONEPLUS_OPPO
            combined.contains("vivo") || combined.contains("iqoo") -> OemVendor.VIVO
            combined.contains("huawei") || combined.contains("honor") -> OemVendor.HUAWEI_HONOR
            combined.contains("asus") -> OemVendor.ASUS
            else -> OemVendor.STANDARD
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
    }

    fun backgroundHintForDevice(): String = when (detectVendor()) {
        OemVendor.XIAOMI ->
            "On Xiaomi / HyperOS devices, set Battery Saver to \"No restrictions\" and enable Background Autostart so the system does not pause Accessibility."
        OemVendor.SAMSUNG ->
            "On Samsung One UI, set Battery to \"Unrestricted\" and make sure BezelPulse is not listed under Sleeping or Deep Sleeping apps."
        OemVendor.ONEPLUS_OPPO ->
            "On OxygenOS / ColorOS, enable \"Allow background activity\" and \"Allow auto-launch\" in App Management so animations stay active after clearing recents."
        OemVendor.VIVO ->
            "On Vivo / iQOO devices, allow \"Autostart\" and set background power consumption to \"Unrestricted\"."
        OemVendor.HUAWEI_HONOR ->
            "On Huawei / Honor devices, switch App Launch to Manual and enable Auto-launch and Run in background."
        OemVendor.ASUS ->
            "On Asus devices, allow BezelPulse in Auto-start Manager and exclude it from battery optimization."
        OemVendor.STANDARD ->
            "Set battery usage to \"Unrestricted\" so Android keeps the hardware key listener responsive after clearing recent apps or rebooting."
    }

    fun hasDedicatedAutoStartScreen(): Boolean = detectVendor() != OemVendor.STANDARD

    fun openBatterySettings(context: Context) {
        val pkg = context.packageName
        val appName = context.getString(R.string.app_name)
        val candidates = buildList {
            if (detectVendor() == OemVendor.XIAOMI) {
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
            OemVendor.XIAOMI -> listOf(
                componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                Intent("miui.intent.action.OP_AUTO_START").apply { addCategory(Intent.CATEGORY_DEFAULT) },
                appDetailsIntent(pkg)
            )
            OemVendor.SAMSUNG -> listOf(
                componentIntent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                componentIntent("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                appDetailsIntent(pkg)
            )
            OemVendor.ONEPLUS_OPPO -> listOf(
                componentIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                componentIntent("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"),
                componentIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                appDetailsIntent(pkg)
            )
            OemVendor.VIVO -> listOf(
                componentIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                componentIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                appDetailsIntent(pkg)
            )
            OemVendor.HUAWEI_HONOR -> listOf(
                componentIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                appDetailsIntent(pkg)
            )
            OemVendor.ASUS -> listOf(
                componentIntent("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
                appDetailsIntent(pkg)
            )
            OemVendor.STANDARD -> listOf(appDetailsIntent(pkg))
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
