package com.smrh.zujaj.catalog

import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.render.Kit
import com.smrh.zujaj.render.LiveR
import com.smrh.zujaj.render.RenderCtx
import com.smrh.zujaj.render.SignatureR

/**
 * The Live category: 50 animated widgets (prayer, weather, clock, Ramadan & Eid, Islamic, date).
 * Each has its own colours by default; everything — background, colours, glow and second effect colour,
 * motion on/off, speed and amount of effects, text sizes, language — is changeable in the editor.
 */
object LiveCatalog {
    private val S = Shape.SMALL
    private val W = Shape.WIDE
    private val L = Shape.LARGE
    private val pU = setOf(Upd.PRAYER, Upd.DAILY)
    private val wU = setOf(Upd.WEATHER, Upd.DAILY)
    private val dU = setOf(Upd.DAILY)

    private fun live(
        id: String, en: String, ar: String, shape: Shape, upd: Set<Upd>, pro: Boolean,
        bg1: Long, bg2: Long, acc: Long, acc2: Long, popular: Boolean = false,
        fx: (RenderCtx, Float) -> Unit, draw: (RenderCtx) -> Unit,
    ) = WidgetStyle(
        "live_$id", Cat.LIVE, en, ar, shape, pro = pro, isNew = true, popular = popular,
        feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = upd,
        defaults = { it.copy(bg = true, fill = Fill.GRADIENT, bgColor = bg1.toInt(), bgColor2 = bg2.toInt(), accent = acc.toInt(), accent2 = acc2.toInt(), border = true) },
        fx = fx, draw = draw,
    )

    private fun z(r: RenderCtx) = LiveR.zone(r)
    private fun weather(r: RenderCtx, t: Float) { val w = Kit.weather(r); LiveR.weatherObj(r, z(r), t, w.cond, w.night || LiveR.isNight(r)) }
    private fun rainy(r: RenderCtx) = Kit.weather(r).cond.let { it == com.smrh.zujaj.data.Cond.RAIN || it == com.smrh.zujaj.data.Cond.STORM }
    private fun progress(r: RenderCtx) = Kit.day(r).progress

    val styles: List<WidgetStyle> = listOf(
        // ------------------------------------------------------------------ prayer
        live("p_orb", "Liquid Orb · Prayer", "كرة السائل · الصلاة", W, pU, false, 0xFF0B1630, 0xFF1B1140, 0xFF4FC3F7, 0xFF7C5CFF, popular = true,
            fx = { r, t -> LiveR.stars(r, t); LiveR.orb(r, z(r), t, progress(r)) }) { LiveR.prayerNext(it) },
        live("p_orb_s", "Liquid Orb · Small", "كرة السائل · صغيرة", S, pU, false, 0xFF0B1630, 0xFF1B1140, 0xFF4FC3F7, 0xFF7C5CFF,
            fx = { r, t -> LiveR.orb(r, z(r), t, progress(r)) }) { LiveR.prayerNext(it) },
        live("p_radar", "Prayer Radar", "رادار الصلاة", W, pU, true, 0xFF06171A, 0xFF0B2B2E, 0xFF3DDCB5, 0xFF8BF5FF,
            fx = { r, t -> LiveR.pulse(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_mosque", "Mosque Night", "ليلة المسجد", L, pU, true, 0xFF0A0E26, 0xFF221640, 0xFFFFC56B, 0xFFE7B75F, popular = true,
            fx = { r, t -> LiveR.stars(r, t); LiveR.mosque(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_moon", "Moonlit Prayer", "صلاة على ضوء القمر", W, pU, false, 0xFF0B1024, 0xFF1C2350, 0xFFFFE6A8, 0xFF9DB4FF,
            fx = { r, t -> LiveR.stars(r, t); LiveR.crescent(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_gyro", "Gyro Prayer", "جيرو الصلاة", S, pU, true, 0xFF120B26, 0xFF2A1450, 0xFFB388FF, 0xFFFF7AB6,
            fx = { r, t -> LiveR.gyro(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_planet", "Planet Prayer", "كوكب الصلاة", L, pU, true, 0xFF070B1A, 0xFF16123A, 0xFF7C9CFF, 0xFFFFC27A,
            fx = { r, t -> LiveR.aurora(r, t); LiveR.planet(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_sun", "Sun & Moon Prayer", "الشمس والقمر · الصلاة", W, pU, true, 0xFF102040, 0xFF2B1B3F, 0xFFFFB74D, 0xFFFFE08A, popular = true,
            fx = { r, t -> if (LiveR.isNight(r)) { LiveR.stars(r, t); LiveR.crescent(r, z(r), t) } else LiveR.sun(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_fireflies", "Fireflies Prayer", "يراعات الصلاة", L, pU, true, 0xFF07140F, 0xFF0F2A20, 0xFFC6FF7A, 0xFF5CE1A6,
            fx = { r, t -> LiveR.fireflies(r, t); LiveR.crescent(r, z(r), t, scale = 0.8f) }) { LiveR.prayerNext(it) },
        live("p_neon", "Neon Prayer", "نيون الصلاة", W, pU, false, 0xFF0A0A12, 0xFF14101F, 0xFFFF4FD8, 0xFF4FD8FF,
            fx = { r, t -> LiveR.neon(r, t); LiveR.pulse(r, z(r), t) }) { LiveR.prayerNext(it) },
        live("p_crystal", "Crystal Prayer", "كريستال الصلاة", S, pU, true, 0xFF0D1426, 0xFF1A2A4A, 0xFF7FE7FF, 0xFF9C7CFF,
            fx = { r, t -> LiveR.sparkles(r, t); LiveR.cube(r, z(r), t, octa = true) }) { LiveR.prayerNext(it) },
        live("p_galaxy", "Galaxy Prayer", "مجرة الصلاة", W, pU, true, 0xFF05060F, 0xFF1A0F33, 0xFFB07BFF, 0xFF6EC8FF,
            fx = { r, t -> LiveR.stars(r, t); LiveR.galaxy(r, z(r), t) }) { LiveR.prayerNext(it) },

        // ------------------------------------------------------------------ weather
        live("w_sky", "Living Sky · Weather", "سماء حيّة · الطقس", W, wU, false, 0xFF1E4C8F, 0xFF3A7BD5, 0xFFFFC34D, 0xFF9AD0FF, popular = true,
            fx = { r, t -> weather(r, t) }) { LiveR.weatherNow(it) },
        live("w_sky_s", "Living Sky · Small", "سماء حيّة · صغيرة", S, wU, false, 0xFF1E4C8F, 0xFF3A7BD5, 0xFFFFC34D, 0xFF9AD0FF,
            fx = { r, t -> weather(r, t) }) { LiveR.weatherNow(it) },
        live("w_forecast", "Living Sky · Forecast", "سماء حيّة · التوقعات", L, wU, true, 0xFF173A70, 0xFF2D5FA8, 0xFFFFC34D, 0xFF9AD0FF,
            fx = { r, t -> weather(r, t) }) { LiveR.weatherNow(it) },
        live("w_glass", "Rain on Glass", "مطر على الزجاج", W, wU, true, 0xFF1A2230, 0xFF2C3A4F, 0xFF9AD0FF, 0xFFB8C7DB,
            fx = { r, t -> if (rainy(r)) LiveR.rainStreaks(r, t) else LiveR.bokeh(r, t); weather(r, t) }) { LiveR.weatherNow(it) },
        live("w_globe", "Weather Globe", "كرة الطقس", W, wU, true, 0xFF071A33, 0xFF0F2E57, 0xFF4FC3F7, 0xFF7CF5B0,
            fx = { r, t -> LiveR.stars(r, t); LiveR.globe(r, z(r), t) }) { LiveR.weatherNow(it) },
        live("w_aurora", "Aurora Temperature", "حرارة الشفق", S, wU, false, 0xFF04121A, 0xFF0B2233, 0xFF5CFFB0, 0xFFB07BFF,
            fx = { r, t -> LiveR.aurora(r, t); weather(r, t) }) { LiveR.weatherNow(it) },
        live("w_snowglobe", "Snow Globe", "كرة الثلج", S, wU, true, 0xFF0F1D33, 0xFF22385E, 0xFF9AD0FF, 0xFFC0392B,
            fx = { r, t -> LiveR.snowGlobe(r, z(r), t) }) { LiveR.weatherNow(it) },
        live("w_sunset", "Sea Sunset", "غروب البحر", L, wU, true, 0xFF2A1240, 0xFFB0475A, 0xFFFF9E5C, 0xFFFFD27A,
            fx = { r, t -> LiveR.sunset(r, z(r), t) }) { LiveR.weatherNow(it) },
        live("w_wind", "Wind Flow", "تيار الهوا", W, wU, true, 0xFF0F2333, 0xFF1D4058, 0xFF8BE3FF, 0xFFFFFFFF,
            fx = { r, t -> LiveR.wind(r, t); weather(r, t) }) { LiveR.weatherNow(it) },
        live("w_bokeh", "Bokeh Weather", "طقس بوكيه", W, wU, false, 0xFF1A1030, 0xFF3A1F55, 0xFFFF8FB1, 0xFFFFC56B,
            fx = { r, t -> LiveR.bokeh(r, t); weather(r, t) }) { LiveR.weatherNow(it) },

        // ------------------------------------------------------------------ clock
        live("c_cube", "Cube Clock", "ساعة المكعب", S, dU, false, 0xFF0E0B1F, 0xFF241A45, 0xFF8C7CFF, 0xFFFF7AB6, popular = true,
            fx = { r, t -> LiveR.cube(r, z(r), t) }) { LiveR.clock(it) },
        live("c_orbit", "Orbit Clock", "ساعة المدار", W, dU, true, 0xFF060A18, 0xFF141B3A, 0xFF7C9CFF, 0xFFFFC27A,
            fx = { r, t -> LiveR.stars(r, t); LiveR.planet(r, z(r), t) }) { LiveR.clock(it) },
        live("c_hourglass", "Hourglass Clock", "ساعة الرملية", S, dU, true, 0xFF1A120A, 0xFF33220F, 0xFFE7B75F, 0xFFB98A3E,
            fx = { r, t -> LiveR.hourglass(r, z(r), t) }) { LiveR.clock(it) },
        live("c_galaxy", "Galaxy Clock", "ساعة المجرة", L, dU, true, 0xFF05060F, 0xFF1A0F33, 0xFFB07BFF, 0xFF6EC8FF, popular = true,
            fx = { r, t -> LiveR.stars(r, t); LiveR.galaxy(r, z(r), t) }) { LiveR.clock(it) },
        live("c_neon", "Neon Clock", "ساعة النيون", W, dU, false, 0xFF0A0A12, 0xFF120E1E, 0xFF4FD8FF, 0xFFFF4FD8,
            fx = { r, t -> LiveR.neon(r, t); LiveR.gyro(r, z(r), t) }) { LiveR.clock(it) },
        live("c_helix", "Helix Clock", "ساعة الحلزون", W, dU, true, 0xFF071418, 0xFF0E2A33, 0xFF3DDCB5, 0xFFB388FF,
            fx = { r, t -> LiveR.helix(r, z(r), t) }) { LiveR.clock(it) },
        live("c_meteor", "Meteor Clock", "ساعة الشهب", W, dU, true, 0xFF060812, 0xFF10183A, 0xFF9DB4FF, 0xFFFFE6A8,
            fx = { r, t -> LiveR.stars(r, t); LiveR.meteors(r, t); LiveR.crescent(r, z(r), t, scale = 0.7f) }) { LiveR.clock(it) },
        live("c_pulse", "Pulse Clock", "ساعة النبض", S, dU, false, 0xFF190A12, 0xFF33102A, 0xFFFF5C8A, 0xFFFFB86B,
            fx = { r, t -> LiveR.pulse(r, z(r), t) }) { LiveR.clock(it) },

        // ------------------------------------------------------------------ Ramadan & Eid
        live("r_lantern", "Ramadan Lantern", "فانوس رمضان", W, pU, false, 0xFF1A0F2E, 0xFF3A1A3F, 0xFFFFB347, 0xFFE7B75F, popular = true,
            fx = { r, t -> LiveR.stars(r, t); val zz = z(r); LiveR.lantern(r, zz.centerX(), zz.top, zz.height() * 0.95f, t) }) { LiveR.season(it) },
        live("r_lantern_s", "Lantern · Small", "فانوس · صغير", S, pU, false, 0xFF1A0F2E, 0xFF3A1A3F, 0xFFFFB347, 0xFFE7B75F,
            fx = { r, t -> val zz = z(r); LiveR.lantern(r, zz.centerX(), zz.top - zz.height() * 0.05f, zz.height() * 1.05f, t) }) { LiveR.season(it) },
        live("r_lanterns", "Lantern Street", "شارع الفوانيس", L, pU, true, 0xFF140C26, 0xFF35173D, 0xFFFFB347, 0xFFE7B75F,
            fx = { r, t ->
                LiveR.stars(r, t)
                val s = r.h * 0.42f
                LiveR.lantern(r, r.w * 0.2f, 0f, s * 0.85f, t, 0.15f)
                LiveR.lantern(r, r.w * 0.5f, 0f, s * 1.05f, t)
                LiveR.lantern(r, r.w * 0.8f, 0f, s * 0.85f, t, 0.4f)
            }) { LiveR.season(it) },
        live("e_fireworks", "Eid Fireworks", "ألعاب العيد", W, pU, true, 0xFF0A0820, 0xFF1F1240, 0xFFFFC56B, 0xFFFF5C8A, popular = true,
            fx = { r, t -> LiveR.fireworks(r, z(r), t) }) { LiveR.season(it) },
        live("e_fireworks_l", "Eid Sky", "سماء العيد", L, pU, true, 0xFF0A0820, 0xFF1F1240, 0xFFFFC56B, 0xFF5CE1FF,
            fx = { r, t -> LiveR.stars(r, t); LiveR.fireworks(r, android.graphics.RectF(0f, 0f, r.w, r.h * 0.55f), t) }) { LiveR.season(it) },
        live("e_balloons", "Eid Balloons", "بلالين العيد", S, pU, false, 0xFF2A1240, 0xFF4A1F5E, 0xFFFF6B9A, 0xFFFFC56B,
            fx = { r, t -> LiveR.balloons(r, z(r), t) }) { LiveR.season(it) },
        live("e_sparkle", "Eid Sparkle", "بريق العيد", W, pU, false, 0xFF1C1206, 0xFF3A2610, 0xFFFFD27A, 0xFFFFFFFF,
            fx = { r, t -> LiveR.sparkles(r, t); LiveR.crescent(r, z(r), t) }) { LiveR.season(it) },

        // ------------------------------------------------------------------ Islamic
        live("i_crescent", "Glowing Crescent · Hijri", "الهلال المضيء · هجري", W, dU, false, 0xFF0A1022, 0xFF1F2A55, 0xFFFFE6A8, 0xFF9DB4FF,
            fx = { r, t -> LiveR.stars(r, t); LiveR.crescent(r, z(r), t) }) { LiveR.dateCard(it, hijri = true) },
        live("i_crescent_s", "Crescent · Small", "الهلال · صغير", S, dU, false, 0xFF0A1022, 0xFF1F2A55, 0xFFFFE6A8, 0xFF9DB4FF,
            fx = { r, t -> LiveR.crescent(r, z(r), t) }) { LiveR.dateCard(it, hijri = true) },
        live("i_qibla", "Living Qibla", "القبلة الحيّة", S, dU, false, 0xFF0B1F18, 0xFF123A2C, 0xFF3DDC97, 0xFFE7B75F,
            fx = { r, t -> LiveR.compass(r, z(r), t, LiveR.qiblaBearing()) }) { LiveR.qiblaInfo(it) },
        live("i_qibla_w", "Living Qibla · Wide", "القبلة الحيّة · عريضة", W, dU, true, 0xFF0B1F18, 0xFF123A2C, 0xFF3DDC97, 0xFFE7B75F,
            fx = { r, t -> LiveR.fireflies(r, t); LiveR.compass(r, z(r), t, LiveR.qiblaBearing()) }) { LiveR.qiblaInfo(it) },
        live("i_dhikr", "Dhikr Beads", "مسبحة الذكر", W, dU, false, 0xFF1C120B, 0xFF3A2416, 0xFFE7B75F, 0xFFFFD27A, popular = true,
            fx = { r, t -> LiveR.beadsRing(r, z(r), t) }) { LiveR.dhikrCard(it) },
        live("i_friday", "Friday Mosque", "مسجد الجمعة", W, dU, true, 0xFF0A1428, 0xFF1C2D50, 0xFFFFC56B, 0xFFE7B75F,
            fx = { r, t -> LiveR.stars(r, t); LiveR.mosque(r, z(r), t) }) { LiveR.friday(it) },
        live("i_verse", "Verse in Light", "آية في النور", W, dU, true, 0xFF120D24, 0xFF26184A, 0xFFFFD27A, 0xFFB388FF,
            fx = { r, t -> LiveR.bokeh(r, t); LiveR.crescent(r, z(r), t, scale = 0.8f) }) { LiveR.verse(it) },
        live("i_beads_s", "Beads · Small", "مسبحة · صغيرة", S, dU, true, 0xFF1C120B, 0xFF3A2416, 0xFFE7B75F, 0xFFFFD27A,
            fx = { r, t -> LiveR.beadsRing(r, z(r), t) }) { LiveR.dhikrCard(it) },

        // ------------------------------------------------------------------ date
        live("d_planet", "Planet Date", "كوكب التاريخ", L, dU, true, 0xFF070B1A, 0xFF16123A, 0xFFFF9E5C, 0xFF7C9CFF,
            fx = { r, t -> LiveR.stars(r, t); LiveR.planet(r, z(r), t) }) { LiveR.dateCard(it, hijri = false) },
        live("d_crystal", "Crystal Date", "تاريخ الكريستال", W, dU, true, 0xFF0D1426, 0xFF1A2A4A, 0xFF7FE7FF, 0xFFFF9CE0,
            fx = { r, t -> LiveR.sparkles(r, t); LiveR.cube(r, z(r), t, octa = true) }) { LiveR.dateCard(it, hijri = false) },
        live("d_bubbles", "Bubbles Date", "تاريخ الفقاعات", S, dU, false, 0xFF06283D, 0xFF0B4A66, 0xFF7FE7FF, 0xFFFFFFFF,
            fx = { r, t -> LiveR.bubbles(r, t) }) { LiveR.dateCard(it, hijri = false) },
        live("d_aurora", "Aurora Date", "تاريخ الشفق", W, dU, false, 0xFF04121A, 0xFF0B2233, 0xFF5CFFB0, 0xFFB07BFF,
            fx = { r, t -> LiveR.aurora(r, t); LiveR.globe(r, z(r), t) }) { LiveR.dateCard(it, hijri = false) },
        live("d_sparkle", "Sparkle Date", "تاريخ البريق", S, dU, false, 0xFF1A0F24, 0xFF33163F, 0xFFFFC56B, 0xFFFF7AB6,
            fx = { r, t -> LiveR.sparkles(r, t) }) { LiveR.dateCard(it, hijri = false) },

        // ------------------------------------------------------------------ more
        live("m_newyear", "New Year Countdown", "العد للسنة الجديدة", S, dU, true, 0xFF0A0820, 0xFF1F1240, 0xFFFFC56B, 0xFF5CE1FF,
            fx = { r, t -> LiveR.fireworks(r, z(r), t) }) { LiveR.newYear(it) },
        live("m_progress", "Month Ring", "حلقة الشهر", S, dU, false, 0xFF0E0B1F, 0xFF241A45, 0xFF8C7CFF, 0xFF4FD8FF,
            fx = { r, t -> LiveR.progressRing(r, z(r), t, LiveR.monthProgressInfo(r)) }) { LiveR.monthProgress(it) },
    )
}
