package com.smrh.zujaj.data

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

data class Prayer(val key: String, val en: String, val ar: String, val time: Long)

data class PrayerDay(
    val list: List<Prayer>, // fajr, sunrise, dhuhr, asr, maghrib, isha
    val nextIndex: Int, // index into list of the next prayer (may be tomorrow's fajr → 6)
    val next: Prayer,
    val prev: Prayer,
) {
    /** Five obligatory prayers (no sunrise) */
    val five get() = list.filter { it.key != "sunrise" }
    val progress: Float
        get() {
            val now = System.currentTimeMillis()
            val span = (next.time - prev.time).coerceAtLeast(1)
            return ((now - prev.time).toFloat() / span).coerceIn(0f, 1f)
        }
}

object PrayerRepo {
    private val names = listOf(
        Triple("fajr", "Fajr", "الفجر"),
        Triple("sunrise", "Sunrise", "الشروق"),
        Triple("dhuhr", "Dhuhr", "الظهر"),
        Triple("asr", "Asr", "العصر"),
        Triple("maghrib", "Maghrib", "المغرب"),
        Triple("isha", "Isha", "العشاء"),
    )

    private fun params() = run {
        val m = runCatching { CalculationMethod.valueOf(Prefs.prayerMethod) }.getOrDefault(CalculationMethod.EGYPTIAN)
        m.parameters.also { it.madhab = if (Prefs.hanafi) Madhab.HANAFI else Madhab.SHAFI }
    }

    fun timesFor(date: LocalDate, lat: Double = Prefs.lat, lng: Double = Prefs.lng): List<Prayer> {
        val dc = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val pt = PrayerTimes(Coordinates(lat, lng), dc, params())
        val ds: List<Date> = listOf(pt.fajr, pt.sunrise, pt.dhuhr, pt.asr, pt.maghrib, pt.isha)
        return ds.mapIndexed { i, d -> Prayer(names[i].first, names[i].second, names[i].third, d.time) }
    }

    fun today(now: Long = System.currentTimeMillis()): PrayerDay {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val today = timesFor(date)
        val tomorrow = timesFor(date.plusDays(1))
        val yesterday = timesFor(date.minusDays(1))
        val idx = today.indexOfFirst { it.time > now }
        return if (idx == -1) {
            PrayerDay(today, 6, tomorrow[0], today[5])
        } else {
            val prev = if (idx == 0) yesterday[5] else today[idx - 1]
            PrayerDay(today, idx, today[idx], prev)
        }
    }

    fun qibla(): Double = Qibla(Coordinates(Prefs.lat, Prefs.lng)).direction
}
