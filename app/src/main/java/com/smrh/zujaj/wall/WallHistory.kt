package com.smrh.zujaj.wall

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/** Every wallpaper the user saved or applied, so it can be opened and edited again. */
object WallHistory {
    data class Item(val dir: File, val thumb: File, val time: Long)

    private fun root(ctx: Context) = File(WallDesign.dir(ctx), "history").apply { mkdirs() }

    fun list(ctx: Context): List<Item> = root(ctx).listFiles()?.filter { File(it, "design.json").exists() }
        ?.map { Item(it, File(it, "thumb.jpg"), it.name.toLongOrNull() ?: it.lastModified()) }
        ?.sortedByDescending { it.time } ?: emptyList()

    /** Copies the photo + mask with the design, plus a small preview. */
    fun add(ctx: Context, d: WallDesign, preview: Bitmap) {
        val dir = File(root(ctx), System.currentTimeMillis().toString()).apply { mkdirs() }
        fun copy(path: String, name: String): String {
            if (path.isBlank() || !File(path).exists()) return ""
            val f = File(dir, name); File(path).copyTo(f, overwrite = true); return f.path
        }
        val saved = d.copy(image = copy(d.image, "photo.jpg"), mask = copy(d.mask, "mask.png"))
        File(dir, "design.json").writeText(saved.toJson())
        val w = 360; val h = (preview.height * w / preview.width.toFloat()).toInt()
        File(dir, "thumb.jpg").outputStream().use { Bitmap.createScaledBitmap(preview, w, h, true).compress(Bitmap.CompressFormat.JPEG, 85, it) }
        // keep the newest 60
        list(ctx).drop(60).forEach { it.dir.deleteRecursively() }
    }

    /** Opens a saved wallpaper as the current design (its own copy, so the history stays intact). */
    fun open(ctx: Context, item: Item): WallDesign {
        val d = runCatching { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(WallDesign.serializer(), File(item.dir, "design.json").readText()) }.getOrDefault(WallDesign())
        val base = WallDesign.dir(ctx)
        val stamp = System.currentTimeMillis()
        fun copy(path: String, name: String): String {
            if (path.isBlank() || !File(path).exists()) return ""
            val f = File(base, name); File(path).copyTo(f, overwrite = true); return f.path
        }
        val cur = d.copy(image = copy(d.image, "photo_$stamp.jpg"), mask = copy(d.mask, "mask_$stamp.png"))
        WallDesign.save(ctx, cur)
        return cur
    }

    fun delete(item: Item) { item.dir.deleteRecursively() }
}
