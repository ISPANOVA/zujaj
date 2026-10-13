package com.smrh.zujaj

import android.app.Application
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
        runCatching { Scheduler.ensure(this) }
    }
}
