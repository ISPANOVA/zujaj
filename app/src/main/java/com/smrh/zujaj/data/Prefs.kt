package com.smrh.zujaj.data

import android.content.Context
import android.content.SharedPreferences
import com.smrh.zujaj.model.WidgetConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** App-wide settings (not per widget). */
object Prefs {
    private lateinit var sp: SharedPreferences
    private lateinit var wp: SharedPreferences

    /** Bumped whenever something visual changes so the UI can re-render previews. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("zujaj_settings", Context.MODE_PRIVATE)
        wp = ctx.getSharedPreferences("zujaj_widgets", Context.MODE_PRIVATE)
    }

    fun bump() { _version.value = _version.value + 1 }

    private fun str(k: String, d: String) = sp.getString(k, d) ?: d
    private fun put(k: String, v: Any?) {
        sp.edit().apply {
            when (v) {
                null -> remove(k)
                is String -> putString(k, v)
                is Int -> putInt(k, v)
                is Long -> putLong(k, v)
                is Boolean -> putBoolean(k, v)
                is Float -> putFloat(k, v)
                is Double -> putString(k, v.toString())
                is Set<*> -> putStringSet(k, v.map { it.toString() }.toSet())
            }
        }.apply()
        bump()
    }

    // ---------- UI ----------
    /** "system", "en" or "ar" */
    var appLanguage: String
        get() = str("app_lang", "system")
        set(v) = put("app_lang", v)

    val isArabicUi: Boolean
        get() = when (appLanguage) {
            "ar" -> true
            "en" -> false
            else -> Locale.getDefault().language == "ar"
        }

    var accentIndex: Int
        get() = sp.getInt("accent", 0)
        set(v) = put("accent", v)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = put("onboarded", v)

    var favorites: Set<String>
        get() = sp.getStringSet("favs", emptySet()) ?: emptySet()
        set(v) = put("favs", v)

    // ---------- Location ----------
    var lat: Double
        get() = str("lat", "30.0444").toDouble()
        set(v) = put("lat", v)
    var lng: Double
        get() = str("lng", "31.2357").toDouble()
        set(v) = put("lng", v)
    var cityName: String
        get() = str("city", "")
        set(v) = put("city", v)
    var hasLocation: Boolean
        get() = sp.getBoolean("has_loc", false)
        set(v) = put("has_loc", v)

    // ---------- Prayer ----------
    /** Adhan CalculationMethod enum name. */
    var prayerMethod: String
        get() = str("p_method", "EGYPTIAN")
        set(v) = put("p_method", v)
    var hanafi: Boolean
        get() = sp.getBoolean("p_hanafi", false)
        set(v) = put("p_hanafi", v)
    var hijriOffset: Int
        get() = sp.getInt("hijri_off", 0)
        set(v) = put("hijri_off", v)

    // ---------- Units ----------
    var fahrenheit: Boolean
        get() = sp.getBoolean("fahrenheit", false)
        set(v) = put("fahrenheit", v)

    // ---------- Pro ----------
    /** Pro bought through Google Play. */
    var purchasedPro: Boolean
        get() = sp.getBoolean("pro", false)
        set(v) = put("pro", v)
    /** Pro granted by a redeem code: epoch ms it ends, Long.MAX_VALUE = forever, 0 = none. */
    var codeProUntil: Long
        get() = sp.getLong("code_pro_until", 0L)
        set(v) = put("code_pro_until", v)
    var codeUsed: String
        get() = str("code_used", "")
        set(v) = put("code_used", v)
    val codePro: Boolean get() = codeProUntil > System.currentTimeMillis()
    /** Writing sets the Play purchase flag (kept for tests and older callers). */
    var isPro: Boolean
        get() = purchasedPro || codePro
        set(v) { purchasedPro = v }
    var proPlan: String
        get() = str("pro_plan", "")
        set(v) = put("pro_plan", v)

    /** Cached remote catalog flags (JSON) */
    var remoteFlags: String
        get() = str("remote_flags", "")
        set(v) = put("remote_flags", v)

    // ---------- Weather cache ----------
    var weatherCache: String
        get() = str("weather_cache", "")
        set(v) = put("weather_cache", v)

    // ---------- Per-widget config ----------
    fun widgetConfig(id: Int): WidgetConfig? = WidgetConfig.fromJson(wp.getString("w_$id", null))
    fun saveWidgetConfig(id: Int, cfg: WidgetConfig) {
        wp.edit().putString("w_$id", cfg.toJson()).apply()
        bump()
    }
    fun deleteWidgetConfig(id: Int) { wp.edit().remove("w_$id").apply() }

    /** Config waiting for the launcher to hand us an appWidgetId (pin flow). */
    fun setPending(token: String, cfg: WidgetConfig) {
        wp.edit().putString("pending_$token", cfg.toJson()).putString("pending_last", token).apply()
    }
    fun takePending(token: String?): WidgetConfig? {
        val key = "pending_" + (token ?: wp.getString("pending_last", "") ?: "")
        val c = WidgetConfig.fromJson(wp.getString(key, null))
        if (c != null) wp.edit().remove(key).apply()
        return c
    }

    // Generic small key-value storage for features (habit, counters…)
    fun getLong(k: String, d: Long = 0L) = sp.getLong(k, d)
    fun setLong(k: String, v: Long) = put(k, v)
    fun getString(k: String, d: String = "") = str(k, d)
    fun setString(k: String, v: String) = put(k, v)
}
