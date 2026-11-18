package com.luminacamera.camera

import com.google.android.gms.ads.MobileAds
import com.luminacamera.camera.helpers.AdMobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.fossify.commons.FossifyApp

class LuminaApp : FossifyApp() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                MobileAds.initialize(this@LuminaApp) {
                    AdMobManager.preloadInterstitial(this@LuminaApp)
                }
            } catch (_: Throwable) {
                // Keep camera startup resilient even if Play Services is unavailable
            }
        }
    }
}
