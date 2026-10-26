package com.smrh.zujaj.data

import android.content.Context
import org.json.JSONObject

/**
 * Languages beyond English and Arabic. The app's texts are written as tr(english, arabic); for Urdu,
 * Indonesian, Turkish and French the English text is looked up in assets/i18n/<lang>.json. Anything
 * not translated yet falls back to English, so a missing line never shows a blank.
 */
object I18n {
    /** code, name in its own language, right-to-left */
    data class L(val code: String, val native: String, val rtl: Boolean)
    val all = listOf(
        L("en", "English", false), L("ar", "العربية", true), L("ur", "اردو", true),
        L("id", "Bahasa Indonesia", false), L("tr", "Türkçe", false), L("fr", "Français", false),
    )
    private var app: Context? = null
    private val tables = HashMap<String, Map<String, String>>()
    fun init(ctx: Context) { app = ctx.applicationContext }

    private fun table(code: String): Map<String, String> = tables.getOrPut(code) {
        runCatching {
            val o = JSONObject(app!!.assets.open("i18n/$code.json").bufferedReader().use { it.readText() })
            buildMap { o.keys().forEach { put(it, o.getString(it)) } }
        }.getOrDefault(emptyMap())
    }
    fun t(code: String, en: String): String = table(code)[en] ?: en
}
