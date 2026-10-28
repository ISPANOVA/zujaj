package com.smrh.zujaj.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Wallpapers people can browse in the Wallpapers tab.
 * - `walls/{id}` = { thumb (small base64 JPEG), by, kind: home|community, status: pending|approved, createdAt }
 * - `wallsFull/{id}` = { img } — the full picture, loaded only when someone opens it.
 * - `app/walls` = { hidden: [built-in wallpaper ids the admin removed] }
 * "home" = uploaded by the admin (shown as SMRH); "community" = anyone, after the admin approves it.
 */
object Walls {
    data class Item(val id: String, val thumb: String, val by: String, val kind: String, val status: String, val createdAt: Long)

    private fun db() = FirebaseFirestore.getInstance()
    private fun col() = db().collection("walls")
    private fun full() = db().collection("wallsFull")

    private fun item(d: com.google.firebase.firestore.DocumentSnapshot) = runCatching {
        Item(d.id, d.getString("thumb") ?: "", d.getString("by") ?: "", d.getString("kind") ?: "community",
            d.getString("status") ?: "pending", d.getLong("createdAt") ?: 0)
    }.getOrNull()?.takeIf { it.thumb.isNotBlank() }

    /** Approved wallpapers of one kind, newest first. */
    suspend fun approved(kind: String): List<Item> =
        col().whereEqualTo("status", "approved").limit(400).get().await().documents.mapNotNull { item(it) }
            .filter { it.kind == kind }.sortedByDescending { it.createdAt }

    /** Admin only. */
    suspend fun pending(): List<Item> =
        col().whereEqualTo("status", "pending").limit(100).get().await().documents.mapNotNull { item(it) }.sortedByDescending { it.createdAt }

    suspend fun fullImage(id: String): Bitmap? {
        val s = full().document(id).get().await().getString("img") ?: return null
        return withContext(Dispatchers.Default) { decode(s) }
    }

    suspend fun approve(id: String) { col().document(id).update("status", "approved").await() }
    suspend fun remove(id: String) { col().document(id).delete().await(); runCatching { full().document(id).delete().await() } }

    // ------------------------------------------------------------ built-in wallpapers the admin removed
    suspend fun hiddenBuiltIn(): Set<Int> =
        (db().collection("app").document("walls").get().await().get("hidden") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet().orEmpty()
    suspend fun hideBuiltIn(id: Int) {
        db().collection("app").document("walls").set(mapOf("hidden" to FieldValue.arrayUnion(id)), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    // ------------------------------------------------------------ uploading
    /** At most 3 community uploads a day from one phone. */
    fun canSubmit(): Boolean {
        val day = java.time.LocalDate.now().toEpochDay().toString()
        val (d, n) = Prefs.getString("wall_sub").split(":").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        return d != day || n < 3
    }

    /** Admin → kind "home", approved at once, shown as SMRH. Anyone else → "community", waits for review. */
    suspend fun upload(photo: Bitmap, by: String, admin: Boolean) {
        val (img, thumb) = withContext(Dispatchers.Default) { encode(photo, 1600, 640_000) to encode(photo, 420, 45_000) }
        val ref = col().document()
        // the full picture first, so an approved card never points at a missing image
        full().document(ref.id).set(mapOf("img" to img)).await()
        ref.set(mapOf("thumb" to thumb, "by" to (if (admin) "SMRH" else by.trim().take(30)), "kind" to (if (admin) "home" else "community"),
            "status" to (if (admin) "approved" else "pending"), "createdAt" to System.currentTimeMillis())).await()
        if (!admin) {
            val day = java.time.LocalDate.now().toEpochDay().toString()
            val (d, n) = Prefs.getString("wall_sub").split(":").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
            Prefs.setString("wall_sub", "$day:${if (d == day) n + 1 else 1}")
        }
    }

    fun decode(s: String): Bitmap? = runCatching {
        val b = Base64.decode(s, Base64.DEFAULT); BitmapFactory.decodeByteArray(b, 0, b.size)
    }.getOrNull()

    /** Scales so the long side is at most [maxSide] and compresses to JPEG under [maxBytes]. */
    private fun encode(src: Bitmap, maxSide: Int, maxBytes: Int): String {
        val k = minOf(1f, maxSide / maxOf(src.width, src.height).toFloat())
        val bmp = if (k < 1f) Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true) else src
        var q = 88; var bytes: ByteArray
        do { val o = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, q, o); bytes = o.toByteArray(); q -= 8 } while (bytes.size > maxBytes && q > 30)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Reads a picked photo, downscaled while decoding so huge camera shots don't run out of memory. */
    fun readPicked(ctx: android.content.Context, uri: android.net.Uri): Bitmap? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1; while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= 1600) s *= 2
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) }
    }.getOrNull()
}
