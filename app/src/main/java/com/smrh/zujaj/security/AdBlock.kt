package com.smrh.zujaj.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Finds ad blockers: blocking DNS (AdGuard DNS, NextDNS, Pi-hole, "private DNS"…), VPN-style blockers
 * (AdGuard, Blokada, RethinkDNS, NetGuard…) and hosts-file blockers. Only says "blocked" when the
 * internet clearly works but the ad servers are made unreachable — so no false alarm when offline.
 */
object AdBlock {
    private val adHosts = listOf("googleads.g.doubleclick.net", "pagead2.googlesyndication.com", "securepubads.g.doubleclick.net")
    private val controlHosts = listOf("www.google.com", "firebaseinstallations.googleapis.com")
    private val blockerApps = listOf(
        "com.adguard.android", "com.adguard.android.contentblocker", "org.blokada.fem.fdroid", "org.blokada.alarm",
        "org.blokada.alarm.dnschanger", "com.celzero.bravedns", "dnsfilter.android", "org.jak_linux.dns66",
        "eu.faircode.netguard", "org.adaway", "io.nextdns.NextDNS", "com.frostnerd.dnschanger",
    )
    private val blockingDnsWords = listOf("adguard", "nextdns", "blokada", "adblock", "ads", "block", "rethink", "controld", "pihole")

    enum class Why { NONE, DNS, PRIVATE_DNS, APP }

    private val _state = MutableStateFlow(Why.NONE)
    val state: StateFlow<Why> = _state

    private fun sinkhole(a: InetAddress): Boolean = when (a) {
        is Inet4Address -> a.isAnyLocalAddress || a.isLoopbackAddress || a.hostAddress == "0.0.0.0" || a.hostAddress?.startsWith("127.") == true || a.isSiteLocalAddress
        is Inet6Address -> a.isAnyLocalAddress || a.isLoopbackAddress || a.isLinkLocalAddress
        else -> false
    }

    /** null = couldn't resolve at all; true = resolved to a real address; false = sinkholed (0.0.0.0 etc.). */
    private suspend fun lookup(host: String): Boolean? = withTimeoutOrNull(3500) {
        withContext(Dispatchers.IO) {
            runCatching { InetAddress.getAllByName(host).toList() }.getOrNull()?.let { list -> list.isNotEmpty() && list.none { sinkhole(it) } }
        }
    }

    private fun vpnOn(ctx: Context): Boolean = runCatching {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }.getOrDefault(false)

    private fun privateDnsHost(ctx: Context): String = runCatching {
        val mode = Settings.Global.getString(ctx.contentResolver, "private_dns_mode") ?: ""
        if (mode == "hostname") Settings.Global.getString(ctx.contentResolver, "private_dns_specifier") ?: "" else ""
    }.getOrDefault("")

    private fun blockerInstalled(ctx: Context): Boolean = blockerApps.any { p ->
        runCatching { ctx.packageManager.getPackageInfo(p, 0); true }.getOrDefault(false)
    }

    /** Runs all checks (network, a few seconds at most) and updates [state]. */
    suspend fun check(ctx: Context): Why = coroutineScope {
        val dns = privateDnsHost(ctx).lowercase()
        val control = controlHosts.map { async { lookup(it) } }.awaitAll()
        val online = control.any { it == true }
        val why = if (!online) Why.NONE else {
            val ads = adHosts.map { async { lookup(it) } }.awaitAll()
            val adsBlocked = ads.any { it != true }
            when {
                adsBlocked && dns.isNotBlank() -> Why.PRIVATE_DNS
                adsBlocked -> if (vpnOn(ctx) && blockerInstalled(ctx)) Why.APP else Why.DNS
                dns.isNotBlank() && blockingDnsWords.any { dns.contains(it) } -> Why.PRIVATE_DNS
                else -> Why.NONE
            }
        }
        _state.value = why
        why
    }
}
