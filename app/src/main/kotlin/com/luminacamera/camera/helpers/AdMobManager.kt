package com.luminacamera.camera.helpers

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.luminacamera.camera.BuildConfig

object AdMobManager {
    private const val INTERSTITIAL_COOLDOWN_MS = 120_000L

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    @Volatile
    private var isLoadingInterstitial = false

    @Volatile
    private var lastInterstitialShownAtMs = 0L

    fun loadAdaptiveBanner(activity: Activity, container: FrameLayout): AdView? {
        return try {
            val adSize = getAdaptiveBannerSize(activity, container)
            val adView = AdView(activity).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        container.visibility = android.view.View.VISIBLE
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        // Keep settings UI clean if no fill or offline
                    }
                }
            }
            container.removeAllViews()
            container.addView(
                adView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            adView.loadAd(AdRequest.Builder().build())
            adView
        } catch (_: Throwable) {
            null
        }
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        val appContext = context.applicationContext
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                appContext,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                    }
                }
            )
        } catch (_: Throwable) {
            interstitialAd = null
            isLoadingInterstitial = false
        }
    }

    fun maybeShowInterstitial(activity: Activity, onComplete: () -> Unit = {}) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastInterstitialShownAtMs
        val readyAd = interstitialAd

        if (readyAd == null || (lastInterstitialShownAtMs != 0L && elapsed < INTERSTITIAL_COOLDOWN_MS)) {
            if (readyAd == null) {
                preloadInterstitial(activity)
            }
            onComplete()
            return
        }

        readyAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAtMs = SystemClock.elapsedRealtime()
                interstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preloadInterstitial(activity)
                onComplete()
            }
        }

        try {
            readyAd.show(activity)
        } catch (_: Throwable) {
            interstitialAd = null
            onComplete()
        }
    }

    private fun getAdaptiveBannerSize(activity: Activity, container: FrameLayout): AdSize {
        val outMetrics = DisplayMetrics()
        val adWidthPixels = if (container.width > 0) {
            container.width.toFloat()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.width().toFloat()
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(outMetrics)
            outMetrics.widthPixels.toFloat()
        }

        val density = activity.resources.displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
}
