package com.smrh.zujaj.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Plan(val productId: String, val type: String) {
    MONTHLY("zujaj_pro_monthly", BillingClient.ProductType.SUBS),
    YEARLY("zujaj_pro_yearly", BillingClient.ProductType.SUBS),
    LIFETIME("zujaj_pro_lifetime", BillingClient.ProductType.INAPP),
}

/** Wallpapers (Apply / Save): monthly, yearly or lifetime — separate from Pro. */
const val WALL_PRODUCT = "zujaj_wallpapers_lifetime"
const val WALL_MONTHLY = "zujaj_wall_monthly"
const val WALL_YEARLY = "zujaj_wall_yearly"
val WALL_IDS = setOf(WALL_PRODUCT, WALL_MONTHLY, WALL_YEARLY)

/** A wallpapers plan as shown in the purchase sheet. */
data class WallPlan(val id: String, val price: String, val details: ProductDetails, val offerToken: String?)

data class PlanPrice(val plan: Plan, val price: String, val details: ProductDetails, val offerToken: String?, val trial: Boolean)

object Billing {
    private var client: BillingClient? = null
    private var appCtx: Context? = null
    private val _prices = MutableStateFlow<Map<Plan, PlanPrice>>(emptyMap())
    val prices: StateFlow<Map<Plan, PlanPrice>> = _prices
    private val _pro = MutableStateFlow(false)
    val pro: StateFlow<Boolean> = _pro
    private val _wallPlans = MutableStateFlow<Map<String, WallPlan>>(emptyMap())
    val wallPlans: StateFlow<Map<String, WallPlan>> = _wallPlans
    private val _wall = MutableStateFlow(false)
    val wall: StateFlow<Boolean> = _wall
    fun refreshWall() { _wall.value = Prefs.wallOwned }
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        _pro.value = Prefs.isPro
        _wall.value = Prefs.wallOwned
        val c = BillingClient.newBuilder(ctx.applicationContext)
            .setListener { result, purchases -> onPurchases(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        connect()
    }

    private fun connect() {
        val c = client ?: return
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restore()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProducts() {
        val c = client ?: return
        for ((type, ids) in listOf(BillingClient.ProductType.INAPP to listOf(WALL_PRODUCT), BillingClient.ProductType.SUBS to listOf(WALL_MONTHLY, WALL_YEARLY))) {
            c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(ids.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(type).build()
            }).build()) { r, res ->
                if (r.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val map = _wallPlans.value.toMutableMap()
                for (pd in res.productDetailsList) {
                    if (type == BillingClient.ProductType.INAPP) {
                        val price = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: continue
                        map[pd.productId] = WallPlan(pd.productId, price, pd, null)
                    } else {
                        val offer = pd.subscriptionOfferDetails?.firstOrNull() ?: continue
                        map[pd.productId] = WallPlan(pd.productId, offer.pricingPhases.pricingPhaseList.last().formattedPrice, pd, offer.offerToken)
                    }
                }
                _wallPlans.value = map
            }
        }
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            val plans = Plan.values().filter { it.type == type }
            val params = QueryProductDetailsParams.newBuilder().setProductList(
                plans.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(type).build() }
            ).build()
            c.queryProductDetailsAsync(params) { result, detailsResult ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val map = _prices.value.toMutableMap()
                for (pd in detailsResult.productDetailsList) {
                    val plan = Plan.values().firstOrNull { it.productId == pd.productId } ?: continue
                    if (type == BillingClient.ProductType.SUBS) {
                        val offers = pd.subscriptionOfferDetails ?: continue
                        // Prefer an offer with a free trial, else the base plan
                        val offer = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } } ?: offers.first()
                        val price = offer.pricingPhases.pricingPhaseList.last().formattedPrice
                        val trial = offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                        map[plan] = PlanPrice(plan, price, pd, offer.offerToken, trial)
                    } else {
                        val price = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: continue
                        map[plan] = PlanPrice(plan, price, pd, null, false)
                    }
                }
                _prices.value = map
            }
        }
    }

    /** Background re-check with Google Play (from the refresh worker), so an ended subscription locks Pro widgets. */
    fun recheck() {
        val c = client ?: return
        if (c.isReady) restore() else connect()
    }

    fun restore() {
        val c = client ?: return
        var any = false
        var anyWall = false
        var done = 0
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && genuine(it) }.forEach {
                        acknowledge(it)
                        if (it.products.any { p -> p in WALL_IDS }) anyWall = true
                        else { any = true; Prefs.proPlan = it.products.firstOrNull() ?: "" }
                    }
                }
                done++
                if (done == 2 && result.responseCode == BillingClient.BillingResponseCode.OK) {
                    setPro(any)
                    if (Prefs.purchasedWall != anyWall) Prefs.purchasedWall = anyWall
                    refreshWall()
                }
            }
        }
    }

    private fun setPro(v: Boolean) {
        Prefs.purchasedPro = v
        refreshPro()
    }

    /** Re-evaluates Pro (purchase or redeem code) and refreshes widgets when it changes. */
    fun refreshPro() {
        _wall.value = Prefs.wallOwned
        val now = Prefs.isPro
        val changed = _pro.value != now
        _pro.value = now
        if (changed) appCtx?.let { WidgetUpdater.updateAll(it) }
    }

    fun buyWall(activity: Activity, id: String): Boolean {
        val c = client ?: return false
        val p = _wallPlans.value[id] ?: return false
        val pp = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(p.details)
        p.offerToken?.let { pp.setOfferToken(it) }
        val r = c.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pp.build())).build())
        return r.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun buy(activity: Activity, plan: Plan): Boolean {
        val c = client ?: return false
        val p = _prices.value[plan] ?: return false
        val pp = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(p.details)
        p.offerToken?.let { pp.setOfferToken(it) }
        val r = c.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pp.build())).build())
        return r.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach {
                if (it.purchaseState == Purchase.PurchaseState.PURCHASED && genuine(it)) {
                    acknowledge(it)
                    if (it.products.any { p -> p in WALL_IDS }) {
                        Prefs.purchasedWall = true; refreshWall(); _message.value = "wall_ok"
                    } else {
                        Prefs.proPlan = it.products.firstOrNull() ?: ""
                        setPro(true)
                        _message.value = "pro_ok"
                    }
                } else if (it.purchaseState == Purchase.PurchaseState.PENDING) {
                    _message.value = "pending"
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            else -> _message.value = "error"
        }
    }

    fun consumeMessage() { _message.value = null }

    /** Checks Google's signature on a purchase with the app's Play licensing key (when configured). */
    private fun genuine(p: Purchase): Boolean {
        val ctx = appCtx ?: return false
        val key = ctx.getString(com.smrh.zujaj.R.string.play_license_key).trim()
        if (key.isEmpty()) return true
        return runCatching {
            val pub = java.security.KeyFactory.getInstance("RSA")
                .generatePublic(java.security.spec.X509EncodedKeySpec(android.util.Base64.decode(key, android.util.Base64.DEFAULT)))
            val sig = java.security.Signature.getInstance("SHA1withRSA")
            sig.initVerify(pub)
            sig.update(p.originalJson.toByteArray())
            sig.verify(android.util.Base64.decode(p.signature, android.util.Base64.DEFAULT))
        }.getOrDefault(false) && p.packageName == ctx.packageName
    }

    private fun acknowledge(p: Purchase) {
        if (p.isAcknowledged) return
        client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
    }
}
