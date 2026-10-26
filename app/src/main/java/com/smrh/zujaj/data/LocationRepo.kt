package com.smrh.zujaj.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager

/**
 * The user's location for prayer times and weather.
 *
 * "Detect" turns on auto mode: from then on the location is refreshed in the background (boot, the
 * periodic refresh) from the phone's last known position, so it survives restarts and follows the user
 * when they travel. Picking a city by hand turns auto mode off. A failed reverse-geocode never wipes a
 * city name we already had for (roughly) the same place.
 */
object LocationRepo {
    fun permitted(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun lastKnown(ctx: Context): Location? {
        if (!permitted(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER, "fused")
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun cityOf(ctx: Context, lat: Double, lng: Double): String? = runCatching {
        @Suppress("DEPRECATION")
        Geocoder(ctx).getFromLocation(lat, lng, 1)?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun km(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Float {
        val out = FloatArray(1); Location.distanceBetween(aLat, aLng, bLat, bLng, out); return out[0] / 1000f
    }

    /** Settings → Detect. Blocking; call off the main thread. Returns false when the phone has no position yet. */
    fun detect(ctx: Context): Boolean {
        val loc = lastKnown(ctx) ?: return false
        save(ctx, loc.latitude, loc.longitude, auto = true)
        return true
    }

    /** Background refresh (boot, periodic work): only in auto mode, only when we may, only when moved or unnamed. */
    fun refreshQuietly(ctx: Context): Boolean {
        if (!Prefs.autoLocation || !permitted(ctx)) return false
        val loc = lastKnown(ctx) ?: return false
        val moved = !Prefs.hasLocation || km(Prefs.lat, Prefs.lng, loc.latitude, loc.longitude) > 3f
        if (!moved && Prefs.cityName.isNotBlank()) return false
        save(ctx, loc.latitude, loc.longitude, auto = true)
        return true
    }

    private fun save(ctx: Context, lat: Double, lng: Double, auto: Boolean) {
        val near = Prefs.hasLocation && km(Prefs.lat, Prefs.lng, lat, lng) < 15f
        val name = cityOf(ctx, lat, lng) ?: if (near) Prefs.cityName else ""
        Prefs.saveLocation(lat, lng, name, auto)
    }
}
