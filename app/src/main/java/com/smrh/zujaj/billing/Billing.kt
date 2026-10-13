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

data class PlanPrice(val plan: Plan, val price: String, val details: ProductDetails, val offerToken: String?, val trial: Boolean)

object Billing {
    private var client: BillingClient? = null
    private var appCtx: Context? = null
    private val _prices = MutableStateFlow<Map<Plan, PlanPrice>>(emptyMap())
    val prices: StateFlow<Map<Plan, PlanPrice>> = _prices
    private val _pro = MutableStateFlow(false)
    val pro: StateFlow<Boolean> = _pro
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        _pro.value = Prefs.isPro
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

    fun restore() {
        val c = client ?: return
        var any = false
        var done = 0
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach {
                        any = true
                        acknowledge(it)
                        Prefs.proPlan = it.products.firstOrNull() ?: ""
                    }
                }
                done++
                if (done == 2 && result.responseCode == BillingClient.BillingResponseCode.OK) setPro(any)
            }
        }
    }

    private fun setPro(v: Boolean) {
        val changed = Prefs.isPro != v
        Prefs.isPro = v
        _pro.value = v
        if (changed) appCtx?.let { WidgetUpdater.updateAll(it) }
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
                if (it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledge(it)
                    Prefs.proPlan = it.products.firstOrNull() ?: ""
                    setPro(true)
                    _message.value = "pro_ok"
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

    private fun acknowledge(p: Purchase) {
        if (p.isAcknowledged) return
        client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
    }
}
