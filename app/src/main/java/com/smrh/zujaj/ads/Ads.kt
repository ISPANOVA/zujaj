package com.smrh.zujaj.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.smrh.zujaj.billing.Billing
import com.smrh.zujaj.remote.Flags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** AdMob: small banners for free users and rewarded ads that unlock a wallpaper Apply/Save (a few per day). */
object Ads {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_REWARD = "ca-app-pub-3940256099942544/5224354917"
    private const val DAY = 24 * 60 * 60 * 1000L
    private lateinit var sp: android.content.SharedPreferences
    private var started = false
    private var reward: RewardedAd? = null
    private var loading = false
    private var retry = 0L
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private val _ver = MutableStateFlow(0)
    val changes: StateFlow<Int> = _ver

    fun init(ctx: Context) { sp = ctx.getSharedPreferences("ads", Context.MODE_PRIVATE) }

    /** Ask for consent where the law requires it (EU/UK), then start the SDK. Call from the activity. */
    fun start(a: Activity) {
        if (started) return
        started = true
        val ci = UserMessagingPlatform.getConsentInformation(a)
        ci.requestConsentInfoUpdate(a, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(a) { boot(a) }
        }, { boot(a) })
        if (ci.canRequestAds()) boot(a)
    }

    private var booted = false
    private fun boot(a: Activity) {
        if (booted) return
        booted = true
        runCatching { MobileAds.initialize(a.applicationContext) { preload(a.applicationContext); AdPolicy.preload(a.applicationContext); AdPolicy.preloadRewarded(a.applicationContext); Banners.ensure(a, 0); Banners.ensure(a, 1) } }
            .onFailure { Log.w("Zujaj", "ads init", it) }
    }

    fun on(): Boolean = Flags.flags.value.adsOn
    const val REAL_BANNER = "ca-app-pub-6729977170206961/5219173598"
    const val REAL_REWARD = "ca-app-pub-6729977170206961/6657123207"
    // debug builds always use Google's test ads so we never click our own real ads
    fun bannerId() = if (com.smrh.zujaj.BuildConfig.DEBUG) TEST_BANNER else Flags.flags.value.adBanner.ifBlank { REAL_BANNER }
    private fun rewardId() = if (com.smrh.zujaj.BuildConfig.DEBUG) TEST_REWARD else Flags.flags.value.adReward.ifBlank { REAL_REWARD }

    // ---------------------------------------------------------------- daily wallpaper quota
    private fun perDay() = Flags.flags.value.adWallPerDay.coerceIn(0, 20)
    private fun windowStart(): Long {
        val s = sp.getLong("w_start", 0)
        return if (s == 0L || System.currentTimeMillis() - s >= DAY) 0 else s
    }
    /** Uses left in the current 24-hour window. */
    fun left(): Int = if (windowStart() == 0L) perDay() else (perDay() - sp.getInt("w_used", 0)).coerceAtLeast(0)
    /** Milliseconds until the window resets (0 if uses are left). */
    /** One extra wallpaper use for the current 24-hour window (a reward). */
    fun grantWallUse() {
        if (windowStart() == 0L) sp.edit().putLong("w_start", System.currentTimeMillis()).putInt("w_used", -1).apply()
        else sp.edit().putInt("w_used", sp.getInt("w_used", 0) - 1).apply()
        _ver.value++
    }
    fun waitMs(): Long = if (left() > 0) 0 else (windowStart() + DAY - System.currentTimeMillis()).coerceAtLeast(0)
    private fun consume() {
        if (windowStart() == 0L) sp.edit().putLong("w_start", System.currentTimeMillis()).putInt("w_used", 1).apply()
        else sp.edit().putInt("w_used", sp.getInt("w_used", 0) + 1).apply()
        _ver.value++
    }

    // ---------------------------------------------------------------- rewarded
    fun preload(ctx: Context) {
        if (reward != null || loading || !on()) return
        loading = true
        runCatching {
            RewardedAd.load(ctx, rewardId(), AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { retry = 0L; reward = ad; loading = false; _ready.value = true }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    reward = null; loading = false; _ready.value = false; Log.w("Zujaj", "reward: ${e.code} ${e.message}")
                    // no ad right now (new account, no fill…) — try again a bit later
                    retry = (retry * 2).coerceIn(15_000L, 300_000L)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ preload(ctx) }, retry)
                }
            })
        }.onFailure { loading = false }
    }

    /** Shows a rewarded ad; [onEarned] runs only if the user watched it to the end. Returns false if no ad is ready. */
    fun showReward(a: Activity, quota: Boolean = true, onEarned: () -> Unit): Boolean {
        val ad = reward ?: run { preload(a); return false }
        if (quota && left() <= 0) return false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                reward = null; _ready.value = false; preload(a)
                if (earned) { if (quota) consume(); onEarned() }
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { reward = null; _ready.value = false; preload(a) }
        }
        ad.show(a) { earned = true }
        return true
    }
}

/**
 * One place that decides where ads may appear and how often (all numbers come from the control panel).
 * Rules: never on the wallpaper canvas, the editor, the paywalls or the widget-picker (configure) flow;
 * favourites only when the list is long; interstitials only after a finished action, with a cooldown and a per-session cap.
 */
object AdPolicy {
    enum class Place { HOME, CATALOG, SEARCH, FAVORITES, SETTINGS }
    private fun f() = Flags.flags.value
    /** Banner after every N rows in this list (0 = no banners). */
    fun every(place: Place, listSize: Int = 0): Int = if (!Ads.on()) 0 else when (place) {
        Place.HOME -> f().adHomeEvery.coerceIn(0, 50)
        Place.CATALOG -> f().adCatalogEvery.coerceIn(0, 50)
        Place.SEARCH -> if (listSize >= 6) (f().adCatalogEvery.coerceIn(0, 50)) else 0
        Place.FAVORITES -> if (listSize >= 8) (f().adCatalogEvery.coerceIn(0, 50) + 2) else 0
        Place.SETTINGS -> 1
    }

    // ---------------------------------------------------------------- interstitials
    private var inter: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    private var loading = false
    private var shownThisSession = 0
    private var lastShown = 0L
    private val sessionStart = System.currentTimeMillis()
    private fun unit(): String = if (com.smrh.zujaj.BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else f().adInterstitial

    fun preload(ctx: Context) {
        if (inter != null || loading || !Ads.on() || unit().isBlank() || Billing.pro.value || Billing.wall.value) return
        loading = true
        runCatching {
            com.google.android.gms.ads.interstitial.InterstitialAd.load(ctx, unit(), AdRequest.Builder().build(),
                object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: com.google.android.gms.ads.interstitial.InterstitialAd) { inter = ad; loading = false }
                    override fun onAdFailedToLoad(e: LoadAdError) { inter = null; loading = false; Log.w("Zujaj", "inter: ${e.code}") }
                })
        }.onFailure { loading = false }
    }

    // ---------------------------------------------------------------- rewarded interstitial
    // Offered at the same natural break as the interstitial and counted against the same caps. Google requires an
    // intro screen that names the reward and lets people skip — see RewardedInterIntro (shown from MainActivity).
    const val REAL_REWARD_INTER = "ca-app-pub-6729977170206961/8398462161"
    private fun rUnit(): String = when {
        com.smrh.zujaj.BuildConfig.DEBUG -> "ca-app-pub-3940256099942544/5354046379"   // Google's test unit
        f().adRewardInter.trim().equals("off", true) -> ""
        else -> f().adRewardInter.trim().ifBlank { REAL_REWARD_INTER }
    }
    private var rInter: com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd? = null
    private var rLoading = false
    /** What the next rewarded interstitial gives: a step towards this Pro widget, or (null) one extra wallpaper use today. */
    var rewardTarget: String? = null
    /** The intro waiting to be shown (non-null = show it). */
    val offer = MutableStateFlow<String?>(null)

    fun preloadRewarded(ctx: Context) {
        if (rInter != null || rLoading || !Ads.on() || rUnit().isBlank() || Billing.pro.value || Billing.wall.value) return
        rLoading = true
        runCatching {
            com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd.load(ctx, rUnit(), AdRequest.Builder().build(),
                object : com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd) { rInter = ad; rLoading = false }
                    override fun onAdFailedToLoad(e: LoadAdError) { rInter = null; rLoading = false; Log.w("Zujaj", "rinter: ${e.code}") }
                })
        }.onFailure { rLoading = false }
    }

    /** The user let the intro run (or tapped Watch): play the ad; the reward is granted only from the SDK's reward callback. */
    fun playRewarded(a: Activity) {
        offer.value = null
        val ad = rInter ?: return
        rInter = null
        val target = rewardTarget
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadRewarded(a) }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { preloadRewarded(a) }
        }
        var granted = false   // the SDK may call back more than once on some devices — grant once
        ad.show(a) {
            if (granted) return@show
            granted = true
            if (target != null && AdUnlock.enabled() && !AdUnlock.open(target)) AdUnlock.add(a, target) else Ads.grantWallUse()
        }
    }
    /** "No thanks" on the intro: nothing is shown, nothing is granted. */
    fun declineRewarded() { offer.value = null }

    /** Call at a natural break (after a widget was added, after saving). Shows only if every rule allows it. */
    fun atBreak(a: Activity) {
        val now = System.currentTimeMillis()
        val capsOk = Ads.on() && !Billing.pro.value && !Billing.wall.value &&
            now - sessionStart > 90_000L &&                                   // not in the first minute and a half
            shownThisSession < f().adInterPerSession.coerceIn(0, 10) &&
            now - lastShown > f().adInterGapMin.coerceIn(1, 120) * 60_000L
        // prefer the rewarded kind: people get something for their time
        if (capsOk && rInter != null) {
            shownThisSession++; lastShown = now
            offer.value = rewardTarget?.takeIf { AdUnlock.enabled() && !AdUnlock.open(it) } ?: ""
            return
        }
        preloadRewarded(a)
        val ok = capsOk && unit().isNotBlank()
        val ad = inter
        if (!ok || ad == null) { preload(a); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { inter = null; preload(a) }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { inter = null }
        }
        shownThisSession++; lastShown = now
        ad.show(a)
    }
}

/**
 * Watch a few rewarded ads to open ONE Pro widget for a few days (both numbers set from the admin panel).
 * Progress and the open-until time are kept per widget, so the ads only open the widget they were watched for.
 */
object AdUnlock {
    private const val DAY = 24 * 60 * 60 * 1000L
    private lateinit var sp: android.content.SharedPreferences
    private val _ver = MutableStateFlow(0)
    val changes: StateFlow<Int> = _ver

    fun init(ctx: Context) { sp = ctx.getSharedPreferences("unlock", Context.MODE_PRIVATE) }
    private fun ok() = ::sp.isInitialized

    fun needed() = Flags.flags.value.adUnlockAds.coerceIn(0, 20)
    fun days() = Flags.flags.value.adUnlockDays.coerceIn(1, 60)
    /** Offered at all (ads on and a number of ads set). */
    fun enabled() = Flags.flags.value.adsOn && needed() > 0

    fun until(id: String): Long = if (ok()) sp.getLong("u_$id", 0L) else 0L
    fun open(id: String): Boolean = until(id) > System.currentTimeMillis()
    fun leftMs(id: String): Long = (until(id) - System.currentTimeMillis()).coerceAtLeast(0)
    fun watched(id: String): Int = if (ok()) sp.getInt("p_$id", 0) else 0

    /** One more ad watched for [id]; returns true when that finished the unlock. */
    fun add(ctx: Context, id: String): Boolean {
        val n = watched(id) + 1
        val done = n >= needed()
        if (done) sp.edit().putInt("p_$id", 0).putLong("u_$id", System.currentTimeMillis() + days() * DAY).apply()
        else sp.edit().putInt("p_$id", n).apply()
        _ver.value++
        if (done) runCatching { com.smrh.zujaj.widget.WidgetUpdater.updateAll(ctx) }
        return done
    }

    /** "2 days 5 h" / "3 h 20 min" left. */
    fun leftText(id: String, ar: Boolean): String {
        val m = leftMs(id) / 60_000L
        val d = m / 1440; val h = (m % 1440) / 60; val mm = m % 60
        return when {
            d > 0 -> if (ar) "$d يوم و$h ساعة" else "${d}d ${h}h"
            h > 0 -> if (ar) "$h ساعة و$mm دقيقة" else "${h}h ${mm}m"
            else -> if (ar) "$mm دقيقة" else "${mm}m"
        }
    }
}

/** A small banner that only free users see. Banners are loaded ahead of time and kept, so they show at once while scrolling. */
@Composable
fun AdBanner(slot: Int = 0, modifier: Modifier = Modifier) {
    val pro by Billing.pro.collectAsState()
    val wall by Billing.wall.collectAsState()
    val flags by Flags.flags.collectAsState()
    val loaded by Banners.loaded.collectAsState()
    if (pro || wall || !flags.adsOn) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val k = slot % Banners.SLOTS
    androidx.compose.runtime.LaunchedEffect(k) { Banners.ensure(ctx, k); Banners.ensure(ctx, (k + 1) % Banners.SLOTS) }
    if (k !in loaded) return
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        AndroidView(factory = { Banners.view(it, k).also { v -> (v.parent as? android.view.ViewGroup)?.removeView(v) } },
            onRelease = { (it.parent as? android.view.ViewGroup)?.removeView(it) })
    }
}

/** A small pool of banners that stay loaded (and auto-refresh) for the whole session. */
object Banners {
    const val SLOTS = 4
    private val views = arrayOfNulls<AdView>(SLOTS)
    private val _loaded = MutableStateFlow<Set<Int>>(emptySet())
    val loaded: StateFlow<Set<Int>> = _loaded
    private val retry = LongArray(SLOTS)

    fun view(c: Context, k: Int): AdView = views[k] ?: AdView(c.applicationContext).also { v ->
        views[k] = v
        val w = (c.resources.displayMetrics.widthPixels / c.resources.displayMetrics.density).toInt() - 32
        v.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(c.applicationContext, w))
        v.adUnitId = Ads.bannerId()
        v.adListener = object : AdListener() {
            override fun onAdLoaded() { retry[k] = 0; _loaded.value = _loaded.value + k }
            override fun onAdFailedToLoad(e: LoadAdError) {
                Log.w("Zujaj", "banner $k: ${e.code} ${e.message}")
                retry[k] = (retry[k] * 2).coerceIn(20_000L, 300_000L)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ v.loadAd(AdRequest.Builder().build()) }, retry[k])
            }
        }
        v.loadAd(AdRequest.Builder().build())
    }

    fun ensure(c: Context, k: Int) { if (Ads.on()) view(c, k) }
}
