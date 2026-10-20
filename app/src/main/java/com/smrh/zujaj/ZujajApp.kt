package com.smrh.zujaj

import android.app.Application
import kotlinx.coroutines.launch
import com.smrh.zujaj.billing.Billing
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.widget.Scheduler

class ZujajApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Flags.init(this)
        Billing.init(this)
        com.smrh.zujaj.ads.Ads.init(this)
        com.smrh.zujaj.ads.AdUnlock.init(this)
        runCatching { Scheduler.ensure(this) }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.smrh.zujaj.remote.Codes.revalidate(this@ZujajApp)
            com.smrh.zujaj.remote.Codes.watch(this@ZujajApp)
        }
    }
}
