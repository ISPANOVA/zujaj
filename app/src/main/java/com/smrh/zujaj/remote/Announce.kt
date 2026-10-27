package com.smrh.zujaj.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.tasks.await

/**
 * A pop-up the admin writes in the control panel (`app/popup`): title, text, optional picture, a button
 * that opens a part of the app or a link, who sees it and how often. Switched on/off any time.
 */
object Announce {
    data class Popup(
        val on: Boolean = false, val id: Long = 0, val title: String = "", val body: String = "", val image: String = "",
        val button: String = "", val action: String = "none", val url: String = "",
        /** once | daily | always */ val freq: String = "once",
        /** all | free | pro */ val audience: String = "all",
    )
    private fun doc() = FirebaseFirestore.getInstance().collection("app").document("popup")

    suspend fun load(): Popup? = runCatching {
        val d = doc().get().await()
        if (!d.exists()) return null
        Popup(d.getBoolean("on") ?: false, d.getLong("id") ?: 0, d.getString("title") ?: "", d.getString("body") ?: "", d.getString("image") ?: "",
            d.getString("button") ?: "", d.getString("action") ?: "none", d.getString("url") ?: "", d.getString("freq") ?: "once", d.getString("audience") ?: "all")
    }.getOrNull()

    suspend fun save(p: Popup) {
        doc().set(mapOf("on" to p.on, "id" to System.currentTimeMillis(), "title" to p.title, "body" to p.body, "image" to p.image,
            "button" to p.button, "action" to p.action, "url" to p.url, "freq" to p.freq, "audience" to p.audience)).await()
    }

    /** Should this phone see it now? Remembers what was shown per pop-up id. */
    fun due(p: Popup): Boolean {
        if (!p.on || (p.title.isBlank() && p.body.isBlank() && p.image.isBlank())) return false
        if (p.audience == "free" && Prefs.isPro) return false
        if (p.audience == "pro" && !Prefs.isPro) return false
        val last = Prefs.getString("pop_${p.id}").toLongOrNull() ?: 0L
        return when (p.freq) {
            "always" -> true
            "daily" -> System.currentTimeMillis() - last > 20 * 3600_000L
            else -> last == 0L
        }
    }
    fun markShown(p: Popup) = Prefs.setString("pop_${p.id}", System.currentTimeMillis().toString())
}
