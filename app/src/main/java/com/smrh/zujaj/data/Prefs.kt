package com.smrh.zujaj.data

import android.content.Context
import android.content.SharedPreferences
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.security.Guard
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
        Guard.init(ctx)
        I18n.init(ctx)
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

    /** The language the app shows: the chosen one, or the phone's if supported, else English. */
    val uiLang: String
        get() = appLanguage.takeIf { it != "system" } ?: Locale.getDefault().language.let { l -> if (I18n.all.any { it.code == l }) l else "en" }
    val isArabicUi: Boolean get() = uiLang == "ar"
    /** Arabic and Urdu read right to left. */
    val isRtlUi: Boolean get() = uiLang == "ar" || uiLang == "ur"

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
    /** Location follows the phone (set by Detect; off after choosing a city by hand). */
    var autoLocation: Boolean
        get() = sp.getBoolean("auto_loc", false)
        set(v) = put("auto_loc", v)
    /** All location fields in ONE synchronous write, so a restart right after can never lose half of it. */
    fun saveLocation(lat: Double, lng: Double, city: String, auto: Boolean) {
        sp.edit().putString("lat", lat.toString()).putString("lng", lng.toString()).putString("city", city)
            .putBoolean("has_loc", true).putBoolean("auto_loc", auto).commit()
        bump()
    }

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
    /** Pro bought through Google Play (sealed against editing, see Guard). */
    var purchasedPro: Boolean
        get() = sp.getBoolean("pro", false) && Guard.verify("pro|1", str("pro_sig", ""))
        set(v) { sp.edit().putBoolean("pro", v).putString("pro_sig", if (v) Guard.seal("pro|1") else "").apply(); bump() }
    /** Pro granted by a redeem code: epoch ms it ends, Long.MAX_VALUE = forever, 0 = none. */
    var codeProUntil: Long
        get() {
            val u = sp.getLong("code_pro_until", 0L)
            return if (u > 0 && Guard.verify("code|$u", str("code_sig", ""))) u else 0L
        }
        set(v) { sp.edit().putLong("code_pro_until", v).putString("code_sig", if (v > 0) Guard.seal("code|$v") else "").apply(); bump() }
    var codeUsed: String
        get() = str("code_used", "")
        set(v) = put("code_used", v)
    val codePro: Boolean get() = codeProUntil > System.currentTimeMillis()
    /** Pro is on when bought or redeemed — and never on a tampered copy of the app. */
    var isPro: Boolean
        get() = (purchasedPro || codePro) && !Guard.compromised()
        set(v) { purchasedPro = v }
    // ---------- Wallpapers (one-time purchase or code) ----------
    var purchasedWall: Boolean
        get() = sp.getBoolean("wall", false) && Guard.verify("wall|1", str("wall_sig", ""))
        set(v) { sp.edit().putBoolean("wall", v).putString("wall_sig", if (v) Guard.seal("wall|1") else "").apply(); bump() }
    var codeWallUntil: Long
        get() {
            val u = sp.getLong("code_wall_until", 0L)
            return if (u > 0 && Guard.verify("cwall|$u", str("cwall_sig", ""))) u else 0L
        }
        set(v) { sp.edit().putLong("code_wall_until", v).putString("cwall_sig", if (v > 0) Guard.seal("cwall|$v") else "").apply(); bump() }
    /** What the redeemed code unlocks: pro, wall or both. */
    var codeGrants: String
        get() = str("code_grants", "pro")
        set(v) = put("code_grants", v)
    val wallOwned: Boolean get() = (purchasedWall || codeWallUntil > System.currentTimeMillis()) && !Guard.compromised()

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
