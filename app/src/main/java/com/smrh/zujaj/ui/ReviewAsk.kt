package com.smrh.zujaj.ui

import android.app.Activity
import com.smrh.zujaj.data.Prefs
import java.time.LocalDate

/**
 * Google's in-app rating sheet, shown at a good moment: after someone has added 3 widgets and has had the
 * app for 5 days. Google Play's rules forbid asking "do you like it?" first and only sending happy people to
 * the rating (review gating), so the sheet is shown as is, to everyone who qualifies. Google decides itself
 * whether it actually appears (it limits how often), and we ask again at most every 4 months.
 */
object ReviewAsk {
    fun noteWidgetAdded() { Prefs.setString("added_n", ((Prefs.getString("added_n").toIntOrNull() ?: 0) + 1).toString()) }

    fun due(): Boolean {
        if (com.smrh.zujaj.security.Guard.isTest) return false
        val today = LocalDate.now().toEpochDay()
        val first = Prefs.getString("first_day").toLongOrNull() ?: today.also { Prefs.setString("first_day", it.toString()) }
        val last = Prefs.getString("rev_last").toLongOrNull() ?: 0L
        return (Prefs.getString("added_n").toIntOrNull() ?: 0) >= 3 && today - first >= 5 && today - last >= 120
    }

    fun maybeAsk(act: Activity) {
        if (!due()) return
        Prefs.setString("rev_last", LocalDate.now().toEpochDay().toString())
        runCatching {
            val m = com.google.android.play.core.review.ReviewManagerFactory.create(act)
            m.requestReviewFlow().addOnCompleteListener { t -> if (t.isSuccessful && !act.isFinishing) m.launchReviewFlow(act, t.result) }
        }
    }
}
