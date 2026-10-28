package com.smrh.zujaj.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.smrh.zujaj.ui.WidgetSet
import com.smrh.zujaj.ui.WidgetSets
import kotlinx.coroutines.tasks.await

/** Seasonal sets kept in Firestore (`sets/{id}`), so new ones appear without an app update. */
object RemoteSets {
    private fun col() = FirebaseFirestore.getInstance().collection("sets")

    private fun from(d: com.google.firebase.firestore.DocumentSnapshot): WidgetSet? = runCatching {
        WidgetSet(
            id = "r_" + d.id, en = d.getString("en") ?: "", ar = d.getString("ar") ?: "",
            tagEn = d.getString("tagEn") ?: "", tagAr = d.getString("tagAr") ?: "",
            pro = d.getBoolean("pro") ?: true, wall = d.getString("wall") ?: "midnight",
            items = (d.get("items") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            accent = (d.getLong("accent") ?: 0xFFE7B75F).toInt(), card = (d.getLong("card") ?: 0xFF15151B).toInt(), card2 = (d.getLong("card2") ?: 0xFF1D1D25).toInt(),
            image = d.getString("image") ?: "", order = (d.getLong("order") ?: 0).toInt(), remote = true, visible = d.getBoolean("visible") ?: true,
        )
    }.getOrNull()?.takeIf { it.items.isNotEmpty() && (it.en.isNotBlank() || it.ar.isNotBlank()) }

    /** Everything (admin sees hidden ones too); the app shows the visible ones. */
    suspend fun load(): List<WidgetSet> = col().get().await().documents.mapNotNull { from(it) }.also { WidgetSets.remote.value = it }

    suspend fun save(s: WidgetSet): String {
        val data = mapOf("en" to s.en, "ar" to s.ar, "tagEn" to s.tagEn, "tagAr" to s.tagAr, "pro" to s.pro, "wall" to s.wall,
            "items" to s.items, "accent" to (s.accent.toLong() and 0xFFFFFFFFL), "card" to (s.card.toLong() and 0xFFFFFFFFL), "card2" to (s.card2.toLong() and 0xFFFFFFFFL),
            "image" to s.image, "order" to s.order, "visible" to s.visible)
        val id = s.id.removePrefix("r_")
        if (s.remote && id.isNotBlank()) { col().document(id).set(data).await(); return id }
        return col().add(data).await().id
    }
    suspend fun delete(s: WidgetSet) { col().document(s.id.removePrefix("r_")).delete().await() }
}
