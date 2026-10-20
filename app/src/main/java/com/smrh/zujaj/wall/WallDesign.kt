package com.smrh.zujaj.wall

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** One thing placed on the wallpaper. Position/size are fractions of the screen. */
@Serializable
data class WallLayer(
    val id: Long = System.nanoTime(),
    /** clock_tall, clock_thin, clock_stack, day_calli, month_greg, month_hijri, date_pair, date_greg, date_hijri,
     *  hijri_full, dual_full, prayer_next, prayer_in, prayer_row, location, text, widget */
    val kind: String,
    val styleId: String = "",
    val text: String = "",
    val color: Int = 0xFFFFFFFF.toInt(),
    val cx: Float = 0.5f,
    val cy: Float = 0.3f,
    /** width as a fraction of the screen width */
    val w: Float = 0.6f,
    val behind: Boolean = false,
    val alpha: Int = 255,
    val shadow: Boolean = true,
    /** second colour; 0 = solid colour */
    val color2: Int = 0,
    /** gradient direction: 0 top→bottom, 1 left→right, 2 diagonal */
    val gDir: Int = 0,
    /** custom width/height ratio when stretched from the edges (0 = natural shape) */
    val ar: Float = 0f,
    /** frosted-glass plate behind the layer (iPhone style) */
    val glass: Boolean = false,
    /** the widget/text itself is made of frosted glass (no plate) */
    val glassFill: Boolean = false,
    /** clock grows down to the top of the subject (iPhone depth style); needs the subject to be found */
    val fit: Boolean = false,
    /** how much of a fitted clock hides behind the subject (0–0.7 of its height) */
    val hide: Float = 0.3f,
    /** date line: stays just above the clock */
    val stick: Boolean = true,
)

@Serializable
data class WallDesign(
    val image: String = "",
    val mask: String = "",
    val layers: List<WallLayer> = emptyList(),
    val blur: Int = 0,
    val dim: Int = 0,
    /** photo framing: zoom relative to "fill the screen" (below 1 = smaller, edges filled with a blurred copy) and offset as a fraction of the screen */
    val zoom: Float = 1f,
    val px: Float = 0f,
    val py: Float = 0f,
    /** how empty space around a smaller photo is filled: 0 extend the edges, 1 mirror, 2 soft blur, 3 colour gradient */
    val fillMode: Int = 0,
    /** colour look of the photo: see WallRender.filters */
    val filter: Int = 0,
    /** darkens the top of the photo (0–80 %) so the clock stands out */
    val topShade: Int = 0,
) {
    fun toJson() = J.encodeToString(this)

    companion object {
        private val J = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private fun file(ctx: Context) = File(ctx.filesDir, "wall/design.json")
        fun dir(ctx: Context) = File(ctx.filesDir, "wall").apply { mkdirs() }
        fun load(ctx: Context): WallDesign = runCatching { J.decodeFromString<WallDesign>(file(ctx).readText()) }.getOrDefault(WallDesign())
        fun save(ctx: Context, d: WallDesign) { dir(ctx); file(ctx).writeText(d.toJson()) }
    }
}
