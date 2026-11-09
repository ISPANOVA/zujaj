package com.smrh.zujaj.catalog

import android.graphics.RectF
import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.render.Kit
import com.smrh.zujaj.render.LiveR
import com.smrh.zujaj.render.RenderCtx

/**
 * The Live category. Prayer, Ramadan & Eid and Islamic widgets are built from Islamic art (mihrab arches,
 * khatam star tiles, rosettes, lanterns, mosque skylines, star garlands); weather widgets paint the real sky
 * for the real weather. Motion is slow and gentle: a 20-frame loop of about four seconds.
 * Everything (background, colours, glow / second colour, motion, speed, effects, sizes, language) is editable.
 */
object LiveCatalog {
    private val S = Shape.SMALL
    private val W = Shape.WIDE
    private val L = Shape.LARGE
    private val pU = setOf(Upd.PRAYER, Upd.DAILY)
    private val wU = setOf(Upd.WEATHER, Upd.DAILY)
    private val dU = setOf(Upd.DAILY)

    // calm palettes: (background top, background bottom, glow, second colour)
    private val NIGHT = longArrayOf(0xFF0E1426, 0xFF1B2340, 0xFFE3C07A, 0xFFC9A45C)
    private val EMERALD = longArrayOf(0xFF0C1C18, 0xFF15302A, 0xFFE3C07A, 0xFFB89A5E)
    private val PLUM = longArrayOf(0xFF17111F, 0xFF2A1F38, 0xFFE0B7A8, 0xFFC9A45C)
    private val TEAL = longArrayOf(0xFF0D1C22, 0xFF173039, 0xFF9CCFC8, 0xFFD3B27A)
    private val SAND = longArrayOf(0xFF1E1710, 0xFF33271A, 0xFFE6BE7E, 0xFFC4964F)
    private val SLATE = longArrayOf(0xFF12151C, 0xFF20252F, 0xFFA9BEDD, 0xFFD6C4A0)
    private val INDIGO = longArrayOf(0xFF0F1022, 0xFF1F1D3D, 0xFFB7A6E8, 0xFF8FB8D8)
    private val ROSE = longArrayOf(0xFF1C1218, 0xFF33202B, 0xFFE8B0B8, 0xFFE3C07A)
    private val SKY = longArrayOf(LiveR.SCENE.toLong() and 0xFFFFFFFFL, 0xFF2D6CB5, 0xFFFFD58A, 0xFFDCE6F0)

    private fun live(
        id: String, en: String, ar: String, shape: Shape, upd: Set<Upd>, pro: Boolean, pal: LongArray, popular: Boolean = false,
        fx: (RenderCtx, Float) -> Unit, draw: (RenderCtx) -> Unit,
    ) = WidgetStyle(
        "live_$id", Cat.LIVE, en, ar, shape, pro = pro, isNew = true, popular = popular,
        feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = upd,
        defaults = { it.copy(bg = true, fill = Fill.GRADIENT, bgColor = pal[0].toInt(), bgColor2 = pal[1].toInt(), accent = pal[2].toInt(), accent2 = pal[3].toInt(), color2 = 0xB3FFFFFF.toInt(), liveSpeed = 1) },
        fx = fx, draw = draw,
    )

    private fun z(r: RenderCtx) = LiveR.zone(r)
    private fun tiles(r: RenderCtx, a: Float = 0.06f) = LiveR.pattern(r, RectF(0f, 0f, r.w, r.h), r.cfg.accent2, a)
    private fun mihrabRect(r: RenderCtx): RectF { val zz = z(r); val w = zz.width() * 0.62f; return RectF(zz.centerX() - w / 2, zz.top + zz.height() * 0.04f, zz.centerX() + w / 2, zz.bottom) }
    private fun mihrabLantern(r: RenderCtx, t: Float) { val m = mihrabRect(r); LiveR.lantern(r, m.centerX(), m.top + m.height() * 0.2f, m.height() * 0.62f, t) }
    private fun progress(r: RenderCtx) = Kit.day(r).progress
    private fun skyline(r: RenderCtx) = LiveR.skyline(r, r.h * 0.52f, android.graphics.Color.argb(235, 6, 8, 16))
    private fun sky(r: RenderCtx) = LiveR.skyScene(r)

    val styles: List<WidgetStyle> = listOf(
        // ------------------------------------------------------------------ prayer (Islamic art)
        live("p_mihrab", "Mihrab · Prayer", "المحراب · الصلاة", W, pU, false, EMERALD, popular = true,
            fx = { r, t -> mihrabLantern(r, t) }) { tiles(it); LiveR.mihrab(it, mihrabRect(it)); LiveR.prayerNext(it) },
        live("p_mihrab_s", "Mihrab · Small", "المحراب · صغير", S, pU, false, EMERALD,
            fx = { r, t -> mihrabLantern(r, t) }) { tiles(it); LiveR.mihrab(it, mihrabRect(it)); LiveR.prayerNext(it) },
        live("p_mihrab_l", "Mihrab · Large", "المحراب · كبير", L, pU, true, EMERALD,
            fx = { r, t -> mihrabLantern(r, t) }) { tiles(it); LiveR.mihrab(it, mihrabRect(it)); LiveR.prayerNext(it) },
        live("p_skyline", "Mosques at Night", "مآذن الليل", L, pU, true, NIGHT, popular = true,
            fx = { r, t -> LiveR.stars(r, t); LiveR.crescent(r, RectF(r.w * 0.62f, r.h * 0.02f, r.w * 0.9f, r.h * 0.3f), t, scale = 0.8f); LiveR.skylineLights(r, r.h * 0.52f, t) }) { RectF(it.w * 0.62f, it.h * 0.02f, it.w * 0.9f, it.h * 0.3f).let { cz -> LiveR.crescentSparks(it, cz.centerX(), cz.centerY(), 0.8f, cz) }; skyline(it); LiveR.prayerNext(it) },
        live("p_rosette", "Golden Rosette", "الوردة الذهبية", S, pU, true, NIGHT,
            fx = { r, t -> LiveR.rosette(r, z(r), t) }) { LiveR.rosetteRing(it); tiles(it, 0.05f); LiveR.prayerNext(it) },
        live("p_rosette_l", "Rosette · Five Prayers", "الوردة · الصلوات الخمس", L, pU, true, PLUM,
            fx = { r, t -> LiveR.rosette(r, z(r), t) }) { LiveR.rosetteRing(it); tiles(it, 0.05f); LiveR.prayerNext(it) },
        live("p_moon", "Crescent & Tiles", "الهلال والزخرفة", W, pU, false, INDIGO,
            fx = { r, t -> LiveR.stars(r, t); LiveR.crescent(r, z(r), t) }) { LiveR.zone(it).let { zz -> LiveR.crescentSparks(it, zz.centerX(), zz.centerY(), 1f) }; tiles(it, 0.05f); LiveR.prayerNext(it) },
        live("p_mosque", "Mosque Night", "ليلة المسجد", L, pU, true, NIGHT,
            fx = { r, t -> LiveR.stars(r, t); LiveR.mosque(r, z(r), t) }) { tiles(it, 0.04f); LiveR.prayerNext(it) },
        live("p_dome", "Dome · Small", "القبة · صغيرة", S, pU, false, NIGHT,
            fx = { r, t -> LiveR.stars(r, t); LiveR.mosque(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_orb", "Time Orb · Prayer", "كرة الوقت · الصلاة", W, pU, false, TEAL,
            fx = { r, t -> LiveR.orb(r, z(r), t, progress(r)) }) { tiles(it, 0.05f); LiveR.prayerNext(it) },
        live("p_orb_s", "Time Orb · Small", "كرة الوقت · صغيرة", S, pU, false, TEAL,
            fx = { r, t -> LiveR.orb(r, z(r), t, progress(r)) }) { LiveR.prayerNext(it) },
        live("p_sun", "Sun & Moon · Prayer", "الشمس والقمر · الصلاة", W, pU, true, SAND,
            fx = { r, t -> if (LiveR.isNight(r)) { LiveR.stars(r, t); LiveR.crescent(r, z(r), t) } else LiveR.sun(r, z(r), t) }) { LiveR.zone(it).let { zz -> if (LiveR.isNight(it)) LiveR.crescentSparks(it, zz.centerX(), zz.centerY(), 1f) else LiveR.sunRays(it, zz.centerX(), zz.centerY(), 1f) }; tiles(it, 0.05f); LiveR.prayerNext(it) },
        live("p_lantern", "Lantern · Prayer", "فانوس · الصلاة", W, pU, true, PLUM,
            fx = { r, t -> val zz = z(r); LiveR.lantern(r, zz.centerX(), zz.top, zz.height() * 0.95f, t) }) { tiles(it, 0.05f); LiveR.prayerNext(it) },
        live("p_beam", "Light of the Mashrabiya", "نور المشربية", W, pU, true, SAND,
            fx = { r, t -> LiveR.beam(r, t, z(r).centerX()) }) { LiveR.pattern(it, z(it), it.cfg.accent2, 0.35f, z(it).width() * 0.28f); LiveR.prayerNext(it) },

        // ------------------------------------------------------------------ Ramadan & Eid
        live("r_lantern", "Ramadan Lantern", "فانوس رمضان", W, pU, false, PLUM, popular = true,
            fx = { r, t -> LiveR.stars(r, t); val zz = z(r); LiveR.lantern(r, zz.centerX(), zz.top, zz.height() * 0.95f, t) }) { tiles(it, 0.05f); LiveR.season(it) },
        live("r_lantern_s", "Lantern · Small", "فانوس · صغير", S, pU, false, PLUM,
            fx = { r, t -> val zz = z(r); LiveR.lantern(r, zz.centerX(), zz.top - zz.height() * 0.05f, zz.height() * 1.05f, t) }) { LiveR.season(it) },
        live("r_lanterns", "Lantern Street", "شارع الفوانيس", L, pU, true, PLUM,
            fx = { r, t ->
                LiveR.stars(r, t)
                val s = r.h * 0.42f
                LiveR.lantern(r, r.w * 0.2f, 0f, s * 0.85f, t, 0.15f)
                LiveR.lantern(r, r.w * 0.5f, 0f, s * 1.05f, t)
                LiveR.lantern(r, r.w * 0.8f, 0f, s * 0.85f, t, 0.4f)
            }) { tiles(it, 0.04f); LiveR.season(it) },
        live("r_garland", "Ramadan Garland", "زينة رمضان", L, pU, true, NIGHT, popular = true,
            fx = { r, t -> LiveR.garland(r, t); LiveR.skylineLights(r, r.h * 0.52f, t) }) { skyline(it); LiveR.season(it) },
        live("e_fireworks", "Eid Fireworks", "ألعاب العيد", W, pU, true, INDIGO,
            fx = { r, t -> LiveR.fireworks(r, z(r), t) }) { tiles(it, 0.04f); LiveR.season(it) },
        live("e_skyline", "Eid Night", "ليلة العيد", L, pU, true, NIGHT,
            fx = { r, t -> LiveR.fireworks(r, RectF(0f, 0f, r.w, r.h * 0.45f), t); LiveR.skylineLights(r, r.h * 0.52f, t) }) { skyline(it); LiveR.season(it) },
        live("e_balloons", "Eid Balloons", "بلالين العيد", S, pU, false, ROSE,
            fx = { r, t -> LiveR.balloons(r, z(r), t) }) { LiveR.season(it) },
        live("e_sparkle", "Eid Sparkle", "بريق العيد", W, pU, false, SAND,
            fx = { r, t -> LiveR.sparkles(r, t); LiveR.crescent(r, z(r), t) }) { LiveR.zone(it).let { zz -> LiveR.crescentSparks(it, zz.centerX(), zz.centerY(), 1f) }; tiles(it, 0.05f); LiveR.season(it) },

        // ------------------------------------------------------------------ Islamic
        live("i_crescent", "Crescent · Hijri Date", "الهلال · التاريخ الهجري", W, dU, false, INDIGO,
            fx = { r, t -> LiveR.stars(r, t); LiveR.crescent(r, z(r), t) }) { LiveR.zone(it).let { zz -> LiveR.crescentSparks(it, zz.centerX(), zz.centerY(), 1f) }; tiles(it, 0.05f); LiveR.dateCard(it, hijri = true) },
        live("i_crescent_s", "Crescent · Small", "الهلال · صغير", S, dU, false, INDIGO,
            fx = { r, t -> LiveR.crescent(r, z(r), t) }) { LiveR.zone(it).let { zz -> LiveR.crescentSparks(it, zz.centerX(), zz.centerY(), 1f) }; LiveR.dateCard(it, hijri = true) },
        live("i_rosette_w", "Rosette · Hijri Date", "الوردة · التاريخ الهجري", W, dU, true, EMERALD,
            fx = { r, t -> LiveR.rosette(r, z(r), t) }) { LiveR.rosetteRing(it); tiles(it, 0.05f); LiveR.dateCard(it, hijri = true) },
        live("i_qibla", "Living Qibla", "القبلة الحيّة", S, dU, false, EMERALD,
            fx = { r, t -> LiveR.compass(r, z(r), t, LiveR.qiblaBearing()) }) { LiveR.qiblaInfo(it) },
        live("i_qibla_w", "Living Qibla · Wide", "القبلة الحيّة · عريضة", W, dU, true, EMERALD,
            fx = { r, t -> LiveR.compass(r, z(r), t, LiveR.qiblaBearing()) }) { tiles(it, 0.05f); LiveR.qiblaInfo(it) },
        live("i_dhikr", "Dhikr Beads", "مسبحة الذكر", W, dU, false, SAND, popular = true,
            fx = { r, t -> LiveR.beadsRing(r, z(r), t) }) { tiles(it, 0.05f); LiveR.dhikrCard(it) },
        live("i_beads_s", "Beads · Small", "مسبحة · صغيرة", S, dU, true, SAND,
            fx = { r, t -> LiveR.beadsRing(r, z(r), t) }) { LiveR.dhikrCard(it) },
        live("i_friday", "Friday Mosque", "مسجد الجمعة", W, dU, true, NIGHT,
            fx = { r, t -> LiveR.stars(r, t); LiveR.mosque(r, z(r), t) }) { tiles(it, 0.04f); LiveR.friday(it) },
        live("i_verse", "Verse under the Arch", "آية تحت القوس", W, dU, true, EMERALD,
            fx = { r, t -> mihrabLantern(r, t) }) { tiles(it); LiveR.mihrab(it, mihrabRect(it)); LiveR.verse(it) },

        // ------------------------------------------------------------------ weather (the real sky)
        live("w_sky", "Living Sky", "السما الحيّة", W, wU, false, SKY, popular = true,
            fx = { r, t -> LiveR.weatherScene(r, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.weatherNow(it) },
        live("w_sky_s", "Living Sky · Small", "السما الحيّة · صغيرة", S, wU, false, SKY,
            fx = { r, t -> LiveR.weatherScene(r, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.weatherNow(it) },
        live("w_sky_l", "Living Sky · Forecast", "السما الحيّة · التوقعات", L, wU, true, SKY, popular = true,
            fx = { r, t -> LiveR.weatherScene(r, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.weatherNow(it) },
        live("w_city", "City Skyline Weather", "طقس المدينة", W, wU, true, SKY,
            fx = { r, t -> LiveR.weatherScene(r, t); LiveR.skylineLights(r, r.h * 0.97f, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.skyline(it, it.h * 0.97f, android.graphics.Color.argb(200, 8, 10, 18)); LiveR.weatherNow(it) },
        live("w_glass", "Rain on Glass", "مطر على الزجاج", W, wU, true, SKY,
            fx = { r, t -> LiveR.weatherScene(r, t); if (Kit.weather(r).cond !in setOf(com.smrh.zujaj.data.Cond.RAIN, com.smrh.zujaj.data.Cond.STORM)) LiveR.bokeh(r, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.weatherNow(it) },
        live("w_wind", "Wind Flow", "تيار الهوا", W, wU, true, SKY,
            fx = { r, t -> LiveR.weatherScene(r, t); LiveR.wind(r, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.weatherNow(it) },
        live("w_temp", "Sky Temperature", "حرارة السما", S, wU, false, SKY,
            fx = { r, t -> LiveR.weatherScene(r, t) }) { sky(it); LiveR.weatherSpins(it); LiveR.weatherNow(it) },
        live("w_aurora", "Northern Lights", "الشفق القطبي", S, wU, true, TEAL,
            fx = { r, t -> LiveR.aurora(r, t); LiveR.stars(r, t) }) { LiveR.weatherNow(it) },
        live("w_snowglobe", "Snow Globe", "كرة الثلج", S, wU, true, SLATE,
            fx = { r, t -> LiveR.snowGlobe(r, z(r), t) }) { LiveR.weatherNow(it) },
        live("w_sunset", "Sea Sunset", "غروب البحر", L, wU, true, ROSE,
            fx = { r, t -> LiveR.sunset(r, z(r), t) }) { LiveR.weatherNow(it) },

        // ------------------------------------------------------------------ clock
        live("c_cube", "Cube Clock", "ساعة المكعب", S, dU, false, INDIGO,
            fx = { r, t -> LiveR.cube(r, z(r), t) }) { LiveR.clock(it) },
        live("c_orbit", "Orbit Clock", "ساعة المدار", W, dU, true, SLATE,
            fx = { r, t -> LiveR.stars(r, t); LiveR.planet(r, z(r), t) }) { LiveR.clock(it) },
        live("c_hourglass", "Hourglass Clock", "ساعة الرملية", S, dU, true, SAND,
            fx = { r, t -> LiveR.hourglass(r, z(r), t) }) { LiveR.clock(it) },
        live("c_galaxy", "Galaxy Clock", "ساعة المجرة", L, dU, true, INDIGO,
            fx = { r, t -> LiveR.stars(r, t); LiveR.galaxy(r, z(r), t) }) { LiveR.galaxySpin(it); LiveR.clock(it) },
        live("c_helix", "Helix Clock", "ساعة الحلزون", W, dU, true, TEAL,
            fx = { r, t -> LiveR.helix(r, z(r), t) }) { LiveR.clock(it) },
        live("c_meteor", "Meteor Clock", "ساعة الشهب", W, dU, true, NIGHT,
            fx = { r, t -> LiveR.stars(r, t); LiveR.meteors(r, t); LiveR.crescent(r, z(r), t, scale = 0.7f) }) { LiveR.zone(it).let { zz -> LiveR.crescentSparks(it, zz.centerX(), zz.centerY(), 0.7f) }; LiveR.clock(it) },
        live("c_pulse", "Pulse Clock", "ساعة النبض", S, dU, false, ROSE,
            fx = { r, t -> LiveR.pulse(r, z(r), t) }) { LiveR.clock(it) },

        // ------------------------------------------------------------------ date & more
        live("d_planet", "Planet Date", "كوكب التاريخ", L, dU, true, SLATE,
            fx = { r, t -> LiveR.stars(r, t); LiveR.planet(r, z(r), t) }) { LiveR.dateCard(it, hijri = false) },
        live("d_crystal", "Crystal Date", "تاريخ الكريستال", W, dU, true, INDIGO,
            fx = { r, t -> LiveR.sparkles(r, t); LiveR.cube(r, z(r), t, octa = true) }) { LiveR.dateCard(it, hijri = false) },
        live("d_bubbles", "Bubbles Date", "تاريخ الفقاعات", S, dU, false, TEAL,
            fx = { r, t -> LiveR.bubbles(r, t) }) { LiveR.dateCard(it, hijri = false) },
        live("d_aurora", "Aurora Date", "تاريخ الشفق", W, dU, false, TEAL,
            fx = { r, t -> LiveR.aurora(r, t); LiveR.globe(r, z(r), t) }) { LiveR.dateCard(it, hijri = false) },
        live("m_newyear", "New Year Countdown", "العد للسنة الجديدة", S, dU, true, INDIGO,
            fx = { r, t -> LiveR.fireworks(r, z(r), t) }) { LiveR.newYear(it) },
        live("m_progress", "Month Ring", "حلقة الشهر", S, dU, false, PLUM,
            fx = { r, t -> LiveR.progressRing(r, z(r), t, LiveR.monthProgressInfo(r)) }) { LiveR.ringDots(it); LiveR.monthProgress(it) },
    )
}
