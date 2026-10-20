package com.smrh.zujaj.wall

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** "Auto-change": the live wallpaper cycles through some saved designs every hour or every day. */
@Serializable
data class WallRotation(
    /** folder names inside wall/history */
    val items: List<String> = emptyList(),
    /** minutes between changes */
    val every: Int = 1440,
    val start: Long = System.currentTimeMillis(),
) {
    companion object {
        private val J = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private fun file(ctx: Context) = File(WallDesign.dir(ctx), "rotation.json")
        fun load(ctx: Context): WallRotation? = runCatching { J.decodeFromString<WallRotation>(file(ctx).readText()) }.getOrNull()
            ?.takeIf { it.items.size >= 2 }
        fun save(ctx: Context, r: WallRotation) { WallDesign.dir(ctx); file(ctx).writeText(J.encodeToString(r)) }
        fun clear(ctx: Context) { file(ctx).delete() }

        /** Which saved design is showing now (its folder), or null when auto-change is off. */
        fun current(ctx: Context, now: Long = System.currentTimeMillis()): File? {
            val r = load(ctx) ?: return null
            val root = File(WallDesign.dir(ctx), "history")
            val alive = r.items.map { File(root, it) }.filter { File(it, "design.json").exists() }
            if (alive.size < 2) return alive.firstOrNull()
            val step = ((now - r.start).coerceAtLeast(0) / (r.every.coerceAtLeast(1) * 60_000L)).toInt()
            return alive[step % alive.size]
        }

        fun designIn(dir: File): WallDesign? = runCatching { J.decodeFromString<WallDesign>(File(dir, "design.json").readText()) }.getOrNull()
    }
}
