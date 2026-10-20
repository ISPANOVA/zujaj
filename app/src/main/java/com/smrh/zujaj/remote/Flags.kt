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
    /** SHA-256 of extra trusted signing certificates (the Google Play app-signing key). */
    val certs: List<String> = emptyList(),
    /** Web-app URL of the admin email-code service (tools/admin-otp.gs). */
    val otpUrl: String = "",
    /** true = the message pops up once per phone; false = it stays on the home page. */
    val noticeOnce: Boolean = false,
    /** Newest versionCode on Google Play (0 = no update prompt). */
    val latestCode: Long = 0,
    /** Older copies can't be used until they update. */
    val forceUpdate: Boolean = false,
    val updateNote: String = "",
    /** Ads for free users (banners on the home page + rewarded ads for wallpapers). */
    val adsOn: Boolean = true,
    val adBanner: String = "",
    val adReward: String = "",
    /** How many wallpaper Apply/Save a free user gets per 24 h by watching ads. */
    val adWallPerDay: Int = 3,
    /** Watch this many rewarded ads to open one Pro widget (0 = off)… */
    val adUnlockAds: Int = 3,
    /** …for this many days. */
    val adUnlockDays: Int = 3,
    /** "Editor's choice" on the home page, in order (empty = the app's own picks). */
    val featured: List<String> = emptyList(),
    /** Lock the app for free users who block ads (AdGuard, private DNS, …). */
    val adblockLock: Boolean = true,
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
        com.smrh.zujaj.security.Guard.remoteCerts = _flags.value.certs.toSet()
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
            // App Check: once enforced in the Firebase console, only the genuine app from Google Play
            // (verified by Play Integrity) can talk to the database — modified copies are refused.
            runCatching {
                com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
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
                certs = (snap.get("certs") as? List<String>) ?: emptyList(),
                otpUrl = snap.getString("otpUrl") ?: "",
                noticeOnce = snap.getBoolean("noticeOnce") ?: false,
                latestCode = snap.getLong("latestCode") ?: 0,
                forceUpdate = snap.getBoolean("forceUpdate") ?: false,
                updateNote = snap.getString("updateNote") ?: "",
                adsOn = snap.getBoolean("adsOn") ?: true,
                adBanner = snap.getString("adBanner") ?: "",
                adReward = snap.getString("adReward") ?: "",
                adWallPerDay = snap.getLong("adWallPerDay")?.toInt() ?: 3,
                adUnlockAds = snap.getLong("adUnlockAds")?.toInt() ?: 3,
                adUnlockDays = snap.getLong("adUnlockDays")?.toInt() ?: 3,
                featured = (snap.get("featured") as? List<String>) ?: emptyList(),
                adblockLock = snap.getBoolean("adblockLock") ?: true,
            )
            com.smrh.zujaj.security.Guard.remoteCerts = f.certs.toSet()
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

    fun locked(s: WidgetStyle): Boolean = isPro(s) && !Prefs.isPro && !com.smrh.zujaj.ads.AdUnlock.open(s.id)

    // ---------------- admin ----------------
    fun auth(): FirebaseAuth? = if (enabled) FirebaseAuth.getInstance() else null

    suspend fun save(f: CatalogFlags) {
        val data = hashMapOf<String, Any>(
            "pro" to (f.pro ?: Catalog.all.filter { it.pro }.map { it.id }),
            "fresh" to (f.fresh ?: Catalog.all.filter { it.isNew }.map { it.id }),
            "hidden" to f.hidden,
            "freeAll" to f.freeAll,
            "notice" to f.notice,
            "certs" to f.certs,
            "otpUrl" to f.otpUrl,
            "noticeOnce" to f.noticeOnce,
            "latestCode" to f.latestCode,
            "forceUpdate" to f.forceUpdate,
            "updateNote" to f.updateNote,
            "adsOn" to f.adsOn,
            "adBanner" to f.adBanner,
            "adReward" to f.adReward,
            "adWallPerDay" to f.adWallPerDay,
            "adUnlockAds" to f.adUnlockAds,
            "adUnlockDays" to f.adUnlockDays,
            "featured" to f.featured,
            "adblockLock" to f.adblockLock,
            "updatedAt" to System.currentTimeMillis(),
        )
        doc().set(data, SetOptions.merge()).await()
    }
}
