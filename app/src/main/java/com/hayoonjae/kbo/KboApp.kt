package com.hayoonjae.kbo

import android.app.Application
import com.hayoonjae.kbo.notify.Notifier

class KboApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
    }
}
