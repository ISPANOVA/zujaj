package com.smrh.zujaj.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class Fill { SOLID, GRADIENT, GLASS, AURORA, NONE }

@Serializable
enum class Texture { OFF, NOISE, GRAIN, LINES, DOTS, GRID, WAVES }

@Serializable
enum class Cal { GREGORIAN, HIJRI }

@Serializable
enum class Lang { AUTO, EN, AR }

@Serializable
enum class Numerals { AUTO, LATIN, ARABIC }

@Serializable
enum class Tap { DEFAULT, EDIT, OPEN_APP, NONE }

@Serializable
enum class ContactAction { CALL, SMS, WHATSAPP, OPEN }

@Serializable
data class ContactRef(
    val name: String,
    val phone: String,
    val photo: String = "",
    val action: ContactAction = ContactAction.CALL,
)

@Serializable
data class AppRef(val pkg: String, val cls: String, val label: String = "")

@Serializable
data class City(val name: String, val lat: Double, val lng: Double, val zone: String = "")

/**
 * Everything the user can customise on a single placed widget.
 * Stored as JSON per appWidgetId.
 */
@Serializable
data class WidgetConfig(
    val styleId: String,
    // Background
    val bg: Boolean = true,
    val fill: Fill = Fill.SOLID,
    val bgColor: Int = 0xFF1A1A1F.toInt(),
    val bgColor2: Int = 0xFF2A2A33.toInt(),
    val bgAlpha: Int = 100,
    val texture: Texture = Texture.OFF,
    val corner: Int = 26,
    val border: Boolean = true,
    // Content
    val color1: Int = 0xFFFFFFFF.toInt(),
    val color2: Int = 0xFF9C9CA6.toInt(),
    val accent: Int = 0xFFE5484D.toInt(),
    val size1: Int = 100,
    val size2: Int = 100,
    val gap: Int = 12,
    val spacing: Int = 0,
    val calendar: Cal = Cal.GREGORIAN,
    val hour24: Boolean = false,
    val seconds: Boolean = false,
    val title: String = "",
    val text: String = "",
    val author: String = "",
    val targetMillis: Long = 0L,
    val counter: Int = 0,
    val goal: Int = 33,
    val apps: List<AppRef> = emptyList(),
    val contacts: List<ContactRef> = emptyList(),
    val photos: List<String> = emptyList(),
    val cities: List<City> = emptyList(),
    val days: List<Long> = emptyList(),
    val variant: Int = 0,
    /** 0 Thuluth, 1 Ruqaa, 2 Nastaliq, 3 Kufi */
    val calli: Int = 0,
    /** Ornament (stars + rule) around Kufi display words; off by default. */
    val ornament: Boolean = false,
    val ornColor: Int = 0xFFE7B75F.toInt(),
    // Per-part colours (Prayer Hub)
    val accent2: Int = 0xFF7B4BB5.toInt(),
    val minGrad: Boolean = true,
    val dayColor: Int = 0xFFFFFFFF.toInt(),
    val hijriColor: Int = 0xFFFFFFFF.toInt(),
    val nameColor: Int = 0xFFB9A6E0.toInt(),
    val timeColor: Int = 0xFFFFFFFF.toInt(),
    val nextColor: Int = 0xFFF2865E.toInt(),
    val lineColor: Int = 0x55B9A6E0,
    /** Face of the main time (LiveFont name); blank = the design's own. */
    val font: String = "",
    // Region
    val language: Lang = Lang.AUTO,
    val numerals: Numerals = Numerals.AUTO,
    // Advanced
    val padding: Int = 16,
    val tap: Tap = Tap.DEFAULT,
    val tapApp: AppRef? = null,
) {
    fun toJson(): String = json.encodeToString(this)

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; isLenient = true }
        fun fromJson(s: String?): WidgetConfig? =
            s?.let { runCatching { json.decodeFromString<WidgetConfig>(it) }.getOrNull() }
    }
}
