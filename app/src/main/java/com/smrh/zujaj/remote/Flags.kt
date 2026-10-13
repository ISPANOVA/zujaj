package com.smrh.zujaj.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.smrh.zujaj.R
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.WidgetStyle
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Remote catalogue flags controlled from the in-app admin dashboard.
 * Stored in Firestore document `app/catalog`.
 */
@Serializable
data class CatalogFlags(
    val pro: List<String>? = null,
    val fresh: List<String>? = null,
    val hidden: List<String> = emptyList(),
    val freeAll: Boolean = false,
    val notice: String = "",
    val updatedAt: Long = 0,
)

object Flags {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _flags = MutableStateFlow(CatalogFlags())
    val flags: StateFlow<CatalogFlags> = _flags
    private var reg: ListenerRegistration? = null
    var enabled = false
        private set

    fun init(ctx: Context) {
        _flags.value = runCatching { json.decodeFromString<CatalogFlags>(Prefs.remoteFlags) }.getOrDefault(CatalogFlags())
        val apiKey = ctx.getString(R.string.fb_api_key)
        val appId = ctx.getString(R.string.fb_app_id)
        val project = ctx.getString(R.string.fb_project_id)
        if (apiKey.isBlank() || appId.isBlank() || project.isBlank()) return
        runCatching {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                FirebaseApp.initializeApp(
                    ctx,
                    FirebaseOptions.Builder().setApiKey(apiKey).setApplicationId(appId).setProjectId(project)
                        .setStorageBucket("$project.appspot.com").build()
                )
            }
            enabled = true
            listen()
        }.onFailure { Log.w("Zujaj", "Firebase init failed", it) }
    }

    private fun doc() = FirebaseFirestore.getInstance().collection("app").document("catalog")

    private fun listen() {
        reg?.remove()
        reg = doc().addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val f = CatalogFlags(
                pro = snap.get("pro") as? List<String>,
                fresh = snap.get("fresh") as? List<String>,
                hidden = (snap.get("hidden") as? List<String>) ?: emptyList(),
                freeAll = snap.getBoolean("freeAll") ?: false,
                notice = snap.getString("notice") ?: "",
                updatedAt = snap.getLong("updatedAt") ?: 0,
            )
            _flags.value = f
            Prefs.remoteFlags = json.encodeToString(f)
        }
    }

    fun isPro(s: WidgetStyle): Boolean {
        val f = _flags.value
        if (f.freeAll) return false
        return f.pro?.contains(s.id) ?: s.pro
    }

    fun isNew(s: WidgetStyle): Boolean = _flags.value.fresh?.contains(s.id) ?: s.isNew
    fun isHidden(s: WidgetStyle): Boolean = _flags.value.hidden.contains(s.id)

    fun visible(): List<WidgetStyle> = Catalog.all.filterNot { isHidden(it) }

    fun locked(s: WidgetStyle): Boolean = isPro(s) && !Prefs.isPro

    // ---------------- admin ----------------
    fun auth(): FirebaseAuth? = if (enabled) FirebaseAuth.getInstance() else null

    suspend fun save(f: CatalogFlags) {
        val data = hashMapOf<String, Any>(
            "pro" to (f.pro ?: Catalog.all.filter { it.pro }.map { it.id }),
            "fresh" to (f.fresh ?: Catalog.all.filter { it.isNew }.map { it.id }),
            "hidden" to f.hidden,
            "freeAll" to f.freeAll,
            "notice" to f.notice,
            "updatedAt" to System.currentTimeMillis(),
        )
        doc().set(data, SetOptions.merge()).await()
    }
}
