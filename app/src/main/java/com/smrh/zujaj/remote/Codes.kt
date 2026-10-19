package com.smrh.zujaj.remote

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smrh.zujaj.billing.Billing
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId

/** Units an admin can choose for how long a code grants Pro. */
enum class CodeUnit(val en: String, val ar: String) {
    DAYS("Days", "أيام"), WEEKS("Weeks", "أسابيع"), MONTHS("Months", "شهور"), YEARS("Years", "سنين"), FOREVER("Forever", "للأبد");
}

/**
 * A redeem code stored in Firestore `codes/{CODE}`.
 * [maxUses] 0 = unlimited people. Each phone can use a code only once
 * (its redemption lives in `codes/{CODE}/redemptions/{deviceId}`).
 * [expiresAt] optional last day the code can be redeemed (0 = no deadline).
 */
data class RedeemCode(
    val code: String,
    val amount: Int,
    val unit: CodeUnit,
    val maxUses: Long,
    val uses: Long,
    val active: Boolean,
    val expiresAt: Long,
    val note: String,
    val createdAt: Long,
    /** pro, wall or both */
    val grants: String = "pro",
) {
    fun grantsLabel(ar: Boolean) = when (grants) {
        "wall" -> if (ar) "الخلفيات" else "Wallpapers"
        "both" -> if (ar) "برو + الخلفيات" else "Pro + Wallpapers"
        else -> if (ar) "برو" else "Pro"
    }
    fun durationLabel(ar: Boolean): String =
        if (unit == CodeUnit.FOREVER) (if (ar) "للأبد" else "Forever") else "$amount ${if (ar) unit.ar else unit.en}"

    /** Pro end time for a redemption made at [from]. */
    fun until(from: Long): Long {
        if (unit == CodeUnit.FOREVER) return Long.MAX_VALUE
        val z = Instant.ofEpochMilli(from).atZone(ZoneId.systemDefault())
        val end = when (unit) {
            CodeUnit.DAYS -> z.plusDays(amount.toLong())
            CodeUnit.WEEKS -> z.plusWeeks(amount.toLong())
            CodeUnit.MONTHS -> z.plusMonths(amount.toLong())
            CodeUnit.YEARS -> z.plusYears(amount.toLong())
            CodeUnit.FOREVER -> z
        }
        return end.toInstant().toEpochMilli()
    }
}

sealed class RedeemResult {
    data class Ok(val until: Long, val restored: Boolean, val grants: String = "pro") : RedeemResult()
    data class Fail(val reason: Reason) : RedeemResult()
    enum class Reason { INVALID, DISABLED, EXPIRED, FULL, ALREADY_USED_EXPIRED, OFFLINE }
}

object Codes {
    private fun db() = FirebaseFirestore.getInstance()
    private fun col() = db().collection("codes")

    fun normalize(c: String) = c.trim().uppercase().filter { it.isLetterOrDigit() || it == '-' }

    @SuppressLint("HardwareIds")
    fun deviceId(ctx: Context): String {
        val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        return id.ifBlank { Prefs.getString("dev_id").ifBlank { java.util.UUID.randomUUID().toString().also { Prefs.setString("dev_id", it) } } }
    }

    private fun parse(id: String, d: Map<String, Any?>): RedeemCode = RedeemCode(
        code = id,
        amount = (d["amount"] as? Number)?.toInt() ?: 1,
        unit = runCatching { CodeUnit.valueOf(d["unit"] as String) }.getOrDefault(CodeUnit.MONTHS),
        maxUses = (d["maxUses"] as? Number)?.toLong() ?: 0,
        uses = (d["uses"] as? Number)?.toLong() ?: 0,
        active = d["active"] as? Boolean ?: true,
        expiresAt = (d["expiresAt"] as? Number)?.toLong() ?: 0,
        note = d["note"] as? String ?: "",
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0,
        grants = d["grants"] as? String ?: "pro",
    )

    /** Redeems [raw] on this phone and turns Pro on locally when it works. */
    suspend fun redeem(ctx: Context, raw: String): RedeemResult {
        if (!Flags.enabled) return RedeemResult.Fail(RedeemResult.Reason.OFFLINE)
        val code = normalize(raw)
        if (code.length < 3) return RedeemResult.Fail(RedeemResult.Reason.INVALID)
        val dev = deviceId(ctx)
        val ref = col().document(code)
        val redRef = ref.collection("redemptions").document(dev)
        val now = System.currentTimeMillis()
        val result = try {
            // a phone that already used this code gets its time back (e.g. after reinstalling)
            val prev = redRef.get().await()
            if (prev.exists()) {
                val until = prev.getLong("until") ?: 0
                if (until > now) RedeemResult.Ok(until, restored = true, grants = prev.getString("grants") ?: "pro")
                else RedeemResult.Fail(RedeemResult.Reason.ALREADY_USED_EXPIRED)
            } else db().runTransaction { tx ->
                val snap = tx.get(ref)
                if (!snap.exists()) return@runTransaction RedeemResult.Fail(RedeemResult.Reason.INVALID)
                val c = parse(code, snap.data ?: emptyMap())
                when {
                    !c.active -> RedeemResult.Fail(RedeemResult.Reason.DISABLED)
                    c.expiresAt in 1 until now -> RedeemResult.Fail(RedeemResult.Reason.EXPIRED)
                    c.maxUses > 0 && c.uses >= c.maxUses -> RedeemResult.Fail(RedeemResult.Reason.FULL)
                    else -> {
                        val until = c.until(now)
                        tx.update(ref, mapOf("uses" to c.uses + 1, "lastDevice" to dev))
                        tx.set(redRef, mapOf("at" to now, "until" to until, "model" to "${Build.MANUFACTURER} ${Build.MODEL}", "grants" to c.grants))
                        RedeemResult.Ok(until, restored = false, grants = c.grants)
                    }
                }
            }.await()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PERMISSION_DENIED", true)) RedeemResult.Fail(RedeemResult.Reason.FULL)
            else RedeemResult.Fail(RedeemResult.Reason.OFFLINE)
        }
        if (result is RedeemResult.Ok) {
            apply(result.grants, result.until)
            Prefs.codeUsed = code
            Billing.refreshPro()
            watch(ctx)
            com.smrh.zujaj.widget.Scheduler.scheduleAlarms(ctx)
        }
        return result
    }

    /**
     * Re-checks this phone's redemption with the server (on app start). The local value is
     * replaced by what the server recorded, and a redemption the server doesn't know about
     * (while its code still exists) is revoked.
     */
    suspend fun revalidate(ctx: Context) {
        val code = Prefs.codeUsed
        if (code.isBlank() || !Flags.enabled) return
        runCatching {
            val ref = col().document(code)
            val red = ref.collection("redemptions").document(deviceId(ctx)).get().await()
            if (red.exists()) {
                val until = red.getLong("until") ?: 0
                val g = red.getString("grants") ?: "pro"
                if (g != "wall") { if (until != Prefs.codeProUntil) Prefs.codeProUntil = until }
                if (g != "pro") { if (until != Prefs.codeWallUntil) Prefs.codeWallUntil = until }
                Prefs.codeGrants = g
            } else if (ref.get().await().exists()) {
                Prefs.codeProUntil = 0; Prefs.codeWallUntil = 0
            }
            Billing.refreshWall()
            Billing.refreshPro()
        }
    }

    /** Sets the local Pro / Wallpapers time from what a code unlocks. */
    private fun apply(grants: String, until: Long) {
        Prefs.codeGrants = grants
        if (grants != "wall") Prefs.codeProUntil = maxOf(Prefs.codeProUntil, until)
        if (grants != "pro") Prefs.codeWallUntil = if (until == 0L) 0L else maxOf(Prefs.codeWallUntil, until)
        Billing.refreshWall()
    }

    // ------------------------------------------------------------------ admin
    suspend fun list(): List<RedeemCode> =
        col().orderBy("createdAt", Query.Direction.DESCENDING).get().await().documents.map { parse(it.id, it.data ?: emptyMap()) }

    suspend fun exists(code: String): Boolean = col().document(normalize(code)).get().await().exists()

    suspend fun create(c: RedeemCode) {
        col().document(normalize(c.code)).set(
            mapOf(
                "amount" to c.amount, "unit" to c.unit.name, "maxUses" to c.maxUses, "uses" to 0L,
                "active" to true, "expiresAt" to c.expiresAt, "note" to c.note,
                "createdAt" to System.currentTimeMillis(), "lastDevice" to "", "grants" to c.grants,
            )
        ).await()
    }

    suspend fun setActive(code: String, active: Boolean) {
        col().document(code).update("active", active).await()
    }

    suspend fun delete(code: String) {
        val ref = col().document(code)
        // remove the redemptions first so the code id can be reused cleanly
        ref.collection("redemptions").get().await().documents.forEach { it.reference.delete().await() }
        ref.delete().await()
    }

    /** One phone that redeemed a code (as the admin sees it). */
    data class Redemption(val device: String, val model: String, val at: Long, val until: Long, val revoked: Boolean, val prevUntil: Long)

    suspend fun redemptions(code: String): List<Redemption> =
        col().document(code).collection("redemptions").get().await().documents.map {
            Redemption(it.id, it.getString("model") ?: it.id.take(6), it.getLong("at") ?: 0, it.getLong("until") ?: 0,
                it.getBoolean("revoked") ?: false, it.getLong("prevUntil") ?: 0)
        }.sortedByDescending { it.at }

    /**
     * Takes Pro away from one phone. The record stays (with until = 0) so the same phone can't
     * redeem the code again, and older app versions drop Pro the next time they open.
     */
    suspend fun revoke(code: String, r: Redemption) {
        col().document(code).collection("redemptions").document(r.device)
            .update(mapOf("until" to 0L, "revoked" to true, "prevUntil" to (if (r.revoked) r.prevUntil else r.until))).await()
    }

    /** Gives a revoked phone its time back. */
    suspend fun restore(code: String, r: Redemption) {
        col().document(code).collection("redemptions").document(r.device)
            .update(mapOf("until" to r.prevUntil, "revoked" to false)).await()
    }

    suspend fun revokeAll(code: String) {
        val list = redemptions(code).filter { !it.revoked }
        list.chunked(400).forEach { part ->
            val b = db().batch()
            part.forEach { r ->
                b.update(col().document(code).collection("redemptions").document(r.device),
                    mapOf("until" to 0L, "revoked" to true, "prevUntil" to r.until))
            }
            b.commit().await()
        }
        setActive(code, false)
    }

    // ------------------------------------------------------------------ live watch
    private var watchReg: com.google.firebase.firestore.ListenerRegistration? = null

    /** Follows this phone's redemption live, so a revoke from the control panel applies within seconds. */
    fun watch(ctx: Context) {
        val code = Prefs.codeUsed
        watchReg?.remove(); watchReg = null
        if (code.isBlank() || !Flags.enabled) return
        val app = ctx.applicationContext
        watchReg = col().document(code).collection("redemptions").document(deviceId(app)).addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            val g = snap.getString("grants") ?: Prefs.codeGrants
            val until = if (snap.exists()) snap.getLong("until") ?: 0 else Prefs.codeProUntil
            if (g != "pro" && until != Prefs.codeWallUntil) { Prefs.codeWallUntil = until; Billing.refreshWall() }
            if (g != "wall" && until != Prefs.codeProUntil) {
                Prefs.codeProUntil = until
                Billing.refreshPro()
                com.smrh.zujaj.widget.Scheduler.scheduleAlarms(app)
            }
        }
    }

    fun randomCode(): String {
        val abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = java.security.SecureRandom()
        return (1..3).joinToString("-") { (1..4).map { abc[r.nextInt(abc.length)] }.joinToString("") }
    }

}
