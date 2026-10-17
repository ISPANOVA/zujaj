package com.smrh.zujaj.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Anti-tamper checks protecting Pro.
 *
 * A modified ("cracked") copy of the app must be re-signed with someone else's key, usually
 * gets a new package name, and/or runs with a hooking framework (Xposed/LSPosed, Frida) or a
 * debugger attached. When any of that is detected Pro is simply not granted on that copy.
 * Stored Pro state is sealed with a key that lives in the phone's hardware keystore, so
 * editing the app's settings file can't switch Pro on either.
 */
object Guard {
    private const val PKG = "com.smrh.zujaj"

    /** SHA-256 of certificates allowed to sign the app (upload key). The Play app-signing key is added remotely. */
    private val BUILT_IN_CERTS = setOf(
        "211F5AC39B13460901F6883437D6954FE873C804734FDA8E55003BAC305D33AC",
    )

    @Volatile private var cached: Boolean? = null
    @Volatile var reason: String = ""
        private set
    private var appCtx: Context? = null

    /** Extra certificates published by the admin (e.g. the Google Play app-signing key). */
    @Volatile var remoteCerts: Set<String> = emptySet()
        set(v) { field = v.map { norm(it) }.filter { it.length == 64 }.toSet(); cached = null }

    private fun norm(s: String) = s.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    val isTest: Boolean get() = Build.FINGERPRINT == "robolectric" || Build.FINGERPRINT.contains("robolectric", true)

    /** True when this copy of the app can't be trusted with Pro. */
    fun compromised(): Boolean {
        if (isTest) return false
        cached?.let { return it }
        val ctx = appCtx ?: return false
        val r = runCatching { check(ctx) }.getOrElse { "error" }
        reason = r ?: ""
        val bad = r != null
        cached = bad
        return bad
    }

    private fun check(ctx: Context): String? {
        if (ctx.packageName != PKG) return "pkg"
        val dbg = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (dbg && !com.smrh.zujaj.BuildConfig.DEBUG) return "debuggable"
        if (!com.smrh.zujaj.BuildConfig.DEBUG && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) return "debugger"
        if (hooked()) return "hook"
        if (!signatureOk(ctx)) return "sig"
        return null
    }

    // ------------------------------------------------------------------ signature
    @SuppressLint("PackageManagerGetSignatures")
    fun certHashes(ctx: Context): List<String> {
        val pm = ctx.packageManager
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return emptyList()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures
        } ?: return emptyList()
        return sigs.map { s -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02X".format(it) } }
    }

    private fun installer(ctx: Context): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) ctx.packageManager.getInstallSourceInfo(ctx.packageName).installingPackageName
        else @Suppress("DEPRECATION") ctx.packageManager.getInstallerPackageName(ctx.packageName)
    }.getOrNull()

    private fun signatureOk(ctx: Context): Boolean {
        val mine = certHashes(ctx)
        if (mine.isEmpty()) return false
        val allowed = BUILT_IN_CERTS + remoteCerts
        if (mine.any { it in allowed }) return true
        // Until the Play app-signing certificate is published by the admin, a copy installed by
        // Google Play is trusted (Play re-signs apps with its own key).
        return remoteCerts.isEmpty() && installer(ctx) == "com.android.vending"
    }

    // ------------------------------------------------------------------ hooks
    private fun hooked(): Boolean {
        // Xposed / LSPosed / EdXposed
        val xposed = listOf("de.robv.android.xposed.XposedBridge", "de.robv.android.xposed.XC_MethodHook", "org.lsposed.lspd.core.Main")
        if (xposed.any { runCatching { Class.forName(it, false, ClassLoader.getSystemClassLoader()) }.isSuccess }) return true
        if (Throwable().stackTrace.any { it.className.contains("xposed", true) || it.className.contains("lsposed", true) }) return true
        // Frida (agent or gadget mapped into our process)
        val maps = runCatching { File("/proc/self/maps").readText() }.getOrDefault("")
        val bad = listOf("frida", "gum-js-loop", "libxposed", "liblspd", "substrate")
        if (bad.any { maps.contains(it, true) }) return true
        val threads = runCatching { File("/proc/self/task").listFiles()?.mapNotNull { runCatching { File(it, "comm").readText().trim() }.getOrNull() } }.getOrNull().orEmpty()
        if (threads.any { it.contains("gum-js", true) || it.contains("gmain", true) || it.contains("frida", true) }) return true
        return false
    }

    // ------------------------------------------------------------------ sealing local state
    private const val ALIAS = "zujaj_seal_v1"
    private var key: SecretKey? = null

    @SuppressLint("HardwareIds")
    private fun deviceId(): String {
        val c = appCtx ?: return "x"
        return Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "x"
    }

    private fun sealKey(): SecretKey {
        key?.let { return it }
        val k = runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN).build())
                generateKey()
            }
        }.getOrElse {
            // no hardware keystore (tests / very old ROMs): bind to this install instead
            SecretKeySpec(MessageDigest.getInstance("SHA-256").digest("zujaj|${deviceId()}|$PKG".toByteArray()), "HmacSHA256")
        }
        key = k
        return k
    }

    /** Tamper-evident signature of [value] for this phone and this install. */
    fun seal(value: String): String = runCatching {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(sealKey())
        mac.doFinal("$value|${deviceId()}".toByteArray()).joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    private val verified = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    fun verify(value: String, sig: String): Boolean {
        if (sig.isEmpty()) return false
        return verified.getOrPut("$value#$sig") { MessageDigest.isEqual(seal(value).toByteArray(), sig.toByteArray()) }
    }
}
