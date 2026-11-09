package com.smrh.zujaj.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * A notification campaign sent from the control panel (e.g. "Ramadan in 30 days — get your screen ready 🌙").
 * Stored as camp* fields of `app/catalog` (already readable by all, writable by the admin). Every phone shows it once (by id), from the
 * periodic refresh, until [until]; tapping it opens [open] ("cat:KEY" a collection, "style:ID" a widget).
 */
object Campaign {
    data class C(val id: Long, val title: String, val body: String, val open: String, val until: Long)
    private fun doc() = FirebaseFirestore.getInstance().collection("app").document("catalog")

    suspend fun get(): C? {
        if (!Flags.enabled) return null
        val s = doc().get().await()
        val id = s.getLong("campId") ?: return null
        if (id == 0L) return null
        return C(id, s.getString("campTitle").orEmpty(), s.getString("campBody").orEmpty(), s.getString("campOpen").orEmpty(), s.getLong("campUntil") ?: 0L)
    }

    /** Admin only. */
    suspend fun send(title: String, body: String, open: String, days: Int) {
        val now = System.currentTimeMillis()
        doc().update(mapOf("campId" to now, "campTitle" to title, "campBody" to body, "campOpen" to open, "campUntil" to now + days * 86_400_000L)).await()
    }
    suspend fun stop() { doc().update("campId", 0L).await() }
}
