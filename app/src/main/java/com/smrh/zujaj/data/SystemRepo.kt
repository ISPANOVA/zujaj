package com.smrh.zujaj.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.util.Locale
import java.util.TimeZone

data class SysInfo(
    val battery: Int,
    val charging: Boolean,
    val tempC: Float,
    val voltage: Float,
    val tech: String,
    val health: String,
    val storageUsedPct: Int,
    val storageFreeGb: Float,
    val storageTotalGb: Float,
    val ramUsedPct: Int,
    val ramFreeGb: Float,
    val model: String,
    val brand: String,
    val device: String,
    val android: String,
    val sdk: Int,
    val build: String,
    val patch: String,
    val uptime: String,
    val locale: String,
    val zone: String,
)

object SystemRepo {
    fun read(ctx: Context): SysInfo {
        val bi = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0) level * 100 / scale.coerceAtLeast(1) else 0
        val status = bi?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val volt = (bi?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
        val tech = bi?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        val health = when (bi?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Hot"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "OK"
        }
        val sf = StatFs(Environment.getDataDirectory().path)
        val total = sf.totalBytes.toFloat()
        val free = sf.availableBytes.toFloat()
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val up = SystemClock.elapsedRealtime() / 1000
        val upStr = "${up / 86400}d ${(up % 86400) / 3600}h"
        return SysInfo(
            battery = pct, charging = charging, tempC = temp, voltage = volt, tech = tech, health = health,
            storageUsedPct = if (total > 0) ((1 - free / total) * 100).toInt() else 0,
            storageFreeGb = free / 1e9f, storageTotalGb = total / 1e9f,
            ramUsedPct = if (mi.totalMem > 0) ((1 - mi.availMem.toFloat() / mi.totalMem) * 100).toInt() else 0,
            ramFreeGb = mi.availMem / 1e9f,
            model = Build.MODEL, brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            device = "${Build.DEVICE} · ${Build.HARDWARE}",
            android = Build.VERSION.RELEASE, sdk = Build.VERSION.SDK_INT,
            build = Build.DISPLAY.take(22), patch = Build.VERSION.SECURITY_PATCH,
            uptime = upStr, locale = Locale.getDefault().toLanguageTag(), zone = TimeZone.getDefault().id,
        )
    }
}
