package com.powercut.editor

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.powercut.editor.core.utils.AppSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PowerCutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        MobileAds.initialize(this) {}
    }
}
