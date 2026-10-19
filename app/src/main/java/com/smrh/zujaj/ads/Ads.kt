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
        runCatching { MobileAds.initialize(a.applicationContext) { preload(a.applicationContext) } }
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
                override fun onAdLoaded(ad: RewardedAd) { reward = ad; loading = false; _ready.value = true }
                override fun onAdFailedToLoad(e: LoadAdError) { reward = null; loading = false; _ready.value = false; Log.w("Zujaj", "reward: ${e.message}") }
            })
        }.onFailure { loading = false }
    }

    /** Shows a rewarded ad; [onEarned] runs only if the user watched it to the end. Returns false if no ad is ready. */
    fun showReward(a: Activity, onEarned: () -> Unit): Boolean {
        val ad = reward ?: run { preload(a); return false }
        if (left() <= 0) return false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                reward = null; _ready.value = false; preload(a)
                if (earned) { consume(); onEarned() }
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { reward = null; _ready.value = false; preload(a) }
        }
        ad.show(a) { earned = true }
        return true
    }
}

/** A small banner that only free users see. Collapses to nothing if no ad loads. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val pro by Billing.pro.collectAsState()
    val wall by Billing.wall.collectAsState()
    val flags by Flags.flags.collectAsState()
    if (pro || wall || !flags.adsOn) return
    val w = LocalConfiguration.current.screenWidthDp - 32
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        AndroidView(factory = { c ->
            AdView(c).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(c, w))
                adUnitId = Ads.bannerId()
                adListener = object : AdListener() {}
                loadAd(AdRequest.Builder().build())
            }
        }, onRelease = { it.destroy() })
    }
}
