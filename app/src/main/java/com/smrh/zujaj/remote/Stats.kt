package com.smrh.zujaj.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Anonymous daily counts for the control panel: how many phones opened the app, new installs, widgets added
 * (and which designs), and Pro purchases. Only numbers are sent — no ID, no account, nothing about the person.
 * Firestore: `stats/{yyyy-mm-dd}` = { users, opens, installs, added, pro, wall, s_<styleId>... }.
 */
object Stats {
    private val io = CoroutineScope(Dispatchers.IO)
    private fun day() = LocalDate.now().toString()
    private fun bump(vararg fields: String) {
        if (fields.isEmpty()) return
        io.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("stats").document(day())
                    .set(fields.associateWith { FieldValue.increment(1) }, SetOptions.merge()).await()
            }
        }
    }

    /** Once per launch; counts the phone once per day and once ever. */
    fun opened() {
        val d = day()
        val f = mutableListOf("opens")
        if (Prefs.getString("st_day") != d) { Prefs.setString("st_day", d); f += "users" }
        if (Prefs.getString("st_installed") != "1") { Prefs.setString("st_installed", "1"); f += "installs" }
        bump(*f.toTypedArray())
    }
    fun widgetAdded(styleId: String) = bump("added", "s_" + styleId.filter { it.isLetterOrDigit() || it == '_' }.take(40))
    fun wallpaperUsed() = bump("walls")
    fun purchase(kind: String) = bump(if (kind == "wall") "wall" else "pro")

    data class Day(val date: String, val values: Map<String, Long>)
    /** Admin only (rules enforce it): the last [days] days, oldest first. */
    suspend fun last(days: Int): List<Day> {
        val today = LocalDate.now()
        val ids = (days - 1 downTo 0).map { today.minusDays(it.toLong()).toString() }
        val col = FirebaseFirestore.getInstance().collection("stats")
        return ids.map { id ->
            val d = runCatching { col.document(id).get().await() }.getOrNull()
            Day(id, d?.data?.mapNotNull { (k, v) -> (v as? Number)?.let { k to it.toLong() } }?.toMap().orEmpty())
        }
    }
}
