package com.smrh.zujaj.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Many brands (Xiaomi/Redmi/POCO, Huawei/Honor, OPPO/Realme/OnePlus, Vivo/iQOO, Samsung,
 * Asus, Meizu, Tecno/Infinix…) stop background work unless the app is whitelisted.
 * This finds the brand's own "auto-start / background" screen so widgets keep updating.
 */
object Oem {
    private val screens = listOf(
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
        "com.samsung.android.sm" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        "com.asus.mobilemanager" to "com.asus.mobilemanager.entry.FunctionActivity",
        "com.meizu.safe" to "com.meizu.safe.security.SHOW_APPSEC",
        "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity",
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
    )

    val brand: String get() = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

    fun autoStartIntent(ctx: Context): Intent? = screens.asSequence()
        .map { (p, c) -> Intent().setComponent(ComponentName(p, c)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        .firstOrNull { ctx.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }

    fun openAutoStart(ctx: Context): Boolean = autoStartIntent(ctx)?.let { runCatching { ctx.startActivity(it) }.isSuccess } ?: false
}
