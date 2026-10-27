package com.smrh.zujaj.remote

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.ui.ShareDesign
import kotlinx.coroutines.tasks.await

/**
 * Community gallery of widget designs. Anyone can submit a design (as its share code); it stays hidden
 * until the admin approves it in the control panel. Approved designs are public and can be liked.
 * Firestore: `gallery/{id}` = { code, styleId, title, status: pending|approved, likes, createdAt }.
 */
object Gallery {
    data class Item(val id: String, val code: String, val styleId: String, val title: String, val likes: Long, val createdAt: Long, val status: String)

    private fun col() = FirebaseFirestore.getInstance().collection("gallery")
    private fun item(d: com.google.firebase.firestore.DocumentSnapshot) = runCatching {
        Item(d.id, d.getString("code") ?: "", d.getString("styleId") ?: "", d.getString("title") ?: "",
            d.getLong("likes") ?: 0, d.getLong("createdAt") ?: 0, d.getString("status") ?: "pending")
    }.getOrNull()?.takeIf { it.code.isNotBlank() && ShareDesign.decode(it.code) != null }

    /** Approved designs, most liked or newest first. */
    suspend fun approved(byLikes: Boolean): List<Item> {
        // filter-only query (no composite index to set up); sorted here
        val all = col().whereEqualTo("status", "approved").limit(300).get().await().documents.mapNotNull { item(it) }
        return if (byLikes) all.sortedWith(compareByDescending<Item> { it.likes }.thenByDescending { it.createdAt }) else all.sortedByDescending { it.createdAt }
    }

    /** Admin only (rules enforce it). */
    suspend fun pending(): List<Item> =
        col().whereEqualTo("status", "pending").limit(100).get().await().documents.mapNotNull { item(it) }.sortedByDescending { it.createdAt }
    /** Admin: everything already public, newest first, so anything can be taken down. */
    suspend fun published(): List<Item> =
        col().whereEqualTo("status", "approved").limit(300).get().await().documents.mapNotNull { item(it) }.sortedByDescending { it.createdAt }
    suspend fun approve(id: String) { col().document(id).update("status", "approved").await() }
    suspend fun remove(id: String) { col().document(id).delete().await() }

    /** At most 3 submissions per day from one phone. */
    fun canSubmit(): Boolean {
        val day = java.time.LocalDate.now().toEpochDay().toString()
        val (d, n) = Prefs.getString("gal_sub").split(":").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        return d != day || n < 3
    }
    suspend fun submit(cfg: com.smrh.zujaj.model.WidgetConfig, title: String) {
        col().add(mapOf(
            "code" to ShareDesign.encode(cfg), "styleId" to cfg.styleId, "title" to title.trim().take(40),
            "status" to "pending", "likes" to 0L, "createdAt" to System.currentTimeMillis(),
        )).await()
        val day = java.time.LocalDate.now().toEpochDay().toString()
        val (d, n) = Prefs.getString("gal_sub").split(":").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        Prefs.setString("gal_sub", "$day:${if (d == day) n + 1 else 1}")
    }

    fun liked(id: String) = id in Prefs.getString("gal_likes").split(",")
    suspend fun like(id: String) {
        if (liked(id)) return
        col().document(id).update("likes", FieldValue.increment(1)).await()
        Prefs.setString("gal_likes", (Prefs.getString("gal_likes").split(",").filter { it.isNotBlank() }.takeLast(500) + id).joinToString(","))
    }
}
