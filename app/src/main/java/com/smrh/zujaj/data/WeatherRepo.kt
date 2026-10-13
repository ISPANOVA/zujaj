package com.smrh.zujaj.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import kotlin.math.roundToInt

enum class Cond { CLEAR, PARTLY, CLOUDY, RAIN, SNOW, STORM, FOG }

@Serializable
data class Weather(
    val city: String,
    val lat: Double,
    val lng: Double,
    val temp: Double,
    val hi: Double,
    val lo: Double,
    val symbol: String,
    val humidity: Double = 0.0,
    val wind: Double = 0.0,
    val updated: Long,
) {
    val cond: Cond get() = WeatherRepo.condOf(symbol)
    val night: Boolean get() = symbol.endsWith("_night")
}

@Serializable
private data class WeatherCache(val items: List<Weather> = emptyList())

object WeatherRepo {
    private val json = Json { ignoreUnknownKeys = true }

    val sample = Weather("Cairo", 30.04, 31.23, 28.0, 31.0, 19.0, "fair_day", 40.0, 3.0, System.currentTimeMillis())
    val sample2 = Weather("Dubai", 25.2, 55.27, 33.0, 36.0, 27.0, "clearsky_day", 50.0, 4.0, System.currentTimeMillis())

    private fun load(): List<Weather> =
        runCatching { json.decodeFromString<WeatherCache>(Prefs.weatherCache).items }.getOrDefault(emptyList())

    private fun key(lat: Double, lng: Double) = "${(lat * 10).roundToInt()},${(lng * 10).roundToInt()}"

    fun cached(lat: Double = Prefs.lat, lng: Double = Prefs.lng): Weather? =
        load().firstOrNull { key(it.lat, it.lng) == key(lat, lng) }

    /** Main location weather (cached) or null */
    fun main(): Weather? = cached()

    fun condOf(symbol: String): Cond = when {
        symbol.startsWith("clearsky") -> Cond.CLEAR
        symbol.startsWith("fair") || symbol.startsWith("partlycloudy") -> Cond.PARTLY
        symbol.contains("thunder") -> Cond.STORM
        symbol.contains("snow") || symbol.contains("sleet") -> Cond.SNOW
        symbol.contains("rain") -> Cond.RAIN
        symbol.startsWith("fog") -> Cond.FOG
        else -> Cond.CLOUDY
    }

    fun condName(c: Cond, ar: Boolean): String = when (c) {
        Cond.CLEAR -> if (ar) "صافٍ" else "Sunny"
        Cond.PARTLY -> if (ar) "غائم جزئياً" else "Partly cloudy"
        Cond.CLOUDY -> if (ar) "غائم" else "Clouds"
        Cond.RAIN -> if (ar) "مطر" else "Rain"
        Cond.SNOW -> if (ar) "ثلج" else "Snow"
        Cond.STORM -> if (ar) "عاصفة" else "Storm"
        Cond.FOG -> if (ar) "ضباب" else "Fog"
    }

    fun fmt(t: Double): String {
        val v = if (Prefs.fahrenheit) t * 9 / 5 + 32 else t
        return "${v.roundToInt()}°"
    }

    /** Blocking network call – run on a background thread. */
    fun fetch(name: String, lat: Double, lng: Double): Weather? = runCatching {
        val la = String.format(java.util.Locale.US, "%.3f", lat)
        val lo = String.format(java.util.Locale.US, "%.3f", lng)
        val url = URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$la&lon=$lo")
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "ZujajWidgets/1.0 github.com/ISPANOVA/zujaj")
        c.connectTimeout = 15000
        c.readTimeout = 15000
        val body = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        val root = json.parseToJsonElement(body).jsonObject
        val series = root["properties"]!!.jsonObject["timeseries"]!!.jsonArray
        val first = series[0].jsonObject
        val data = first["data"]!!.jsonObject
        val details = data["instant"]!!.jsonObject["details"]!!.jsonObject
        val temp = details["air_temperature"]!!.jsonPrimitive.double
        val hum = details["relative_humidity"]?.jsonPrimitive?.double ?: 0.0
        val wind = details["wind_speed"]?.jsonPrimitive?.double ?: 0.0
        val symbol = (data["next_1_hours"] ?: data["next_6_hours"])?.jsonObject
            ?.get("summary")?.jsonObject?.get("symbol_code")?.jsonPrimitive?.content ?: "cloudy"
        val start = OffsetDateTime.parse(first["time"]!!.jsonPrimitive.content)
        val temps = series.mapNotNull { e ->
            val o = e.jsonObject
            val t = OffsetDateTime.parse(o["time"]!!.jsonPrimitive.content)
            if (t.isAfter(start.plusHours(24))) null
            else (o["data"]!!.jsonObject["instant"]!!.jsonObject["details"]!!.jsonObject["air_temperature"]
                ?.jsonPrimitive?.double)
        }
        val w = Weather(name, lat, lng, temp, temps.maxOrNull() ?: temp, temps.minOrNull() ?: temp, symbol, hum, wind, System.currentTimeMillis())
        save(w)
        w
    }.getOrNull()

    private fun save(w: Weather) {
        val list = load().filterNot { key(it.lat, it.lng) == key(w.lat, w.lng) } + w
        Prefs.weatherCache = json.encodeToString(WeatherCache(list.takeLast(8)))
    }
}

@Suppress("unused")
private fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.content
