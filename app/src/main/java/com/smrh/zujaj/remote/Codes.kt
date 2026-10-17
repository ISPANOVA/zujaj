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
) {
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
    data class Ok(val until: Long, val restored: Boolean) : RedeemResult()
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
                if (until > now) RedeemResult.Ok(until, restored = true)
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
                        tx.set(redRef, mapOf("at" to now, "until" to until, "model" to "${Build.MANUFACTURER} ${Build.MODEL}"))
                        RedeemResult.Ok(until, restored = false)
                    }
                }
            }.await()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PERMISSION_DENIED", true)) RedeemResult.Fail(RedeemResult.Reason.FULL)
            else RedeemResult.Fail(RedeemResult.Reason.OFFLINE)
        }
        if (result is RedeemResult.Ok) {
            Prefs.codeProUntil = maxOf(Prefs.codeProUntil, result.until)
            Prefs.codeUsed = code
            Billing.refreshPro()
        }
        return result
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
                "createdAt" to System.currentTimeMillis(), "lastDevice" to "",
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

    suspend fun redemptions(code: String): List<Pair<String, Long>> =
        col().document(code).collection("redemptions").get().await().documents.map {
            (it.getString("model") ?: it.id.take(6)) to (it.getLong("at") ?: 0)
        }

    fun randomCode(): String {
        val abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = java.security.SecureRandom()
        return (1..3).joinToString("-") { (1..4).map { abc[r.nextInt(abc.length)] }.joinToString("") }
    }

}
