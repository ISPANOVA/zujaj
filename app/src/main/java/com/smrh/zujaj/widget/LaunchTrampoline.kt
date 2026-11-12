package com.smrh.zujaj.widget

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Opens an app from a widget on the lock screen: shows over the lock screen, asks the phone to unlock
 * (fingerprint / face / PIN), then starts the app. Lock screens often drop launches of other apps otherwise.
 */
class LaunchTrampoline : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target: Intent? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA, Intent::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA)
        if (target == null) { finish(); return }
        fun go() { runCatching { startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; finish() }
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null || !km.isKeyguardLocked || Build.VERSION.SDK_INT < 26) { go(); return }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() { go() }
            override fun onDismissCancelled() { finish() }
            override fun onDismissError() { go() }
        })
    }

    companion object { const val EXTRA = "target" }
}
