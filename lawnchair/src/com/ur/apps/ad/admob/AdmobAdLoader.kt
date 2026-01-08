package com.ur.apps.ad.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ur.apps.ad.AdShowScene
import com.ur.apps.ad.BaseAdLoader
import com.android.launcher3.R
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.analysis.tenjin.TenjinManager
import com.ur.apps.utils.URLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


open class AdmobAdLoader : BaseAdLoader() {

    companion object {

        const val TAG = "AdmobAdLoader"

        val ADMOB_NATIVE_UNIT_ID = "ca-app-pub-2830772598550207/3731936764"

        val ADMOB_NATIVE_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110"

        val ADMOB_REWARD_UNIT_ID = "ca-app-pub-9332346327319073/7617118213"

        val ADMOB_INTERSTITIAL_UNIT_ID = "ca-app-pub-9332346327319073/8650998280"

        val ADMOB_BANNER_UNIT_ID = "ca-app-pub-2830772598550207/6650384775"

        val ADMOB_BANNER_UNIT_TEST_ID =  "ca-app-pub-3940256099942544/9214589741"

        val ADMOB_OPENAD_UNIT_ID =  "ca-app-pub-2830772598550207/9227806024"

        var useDebugAdmobId = false

        const val EVENT_ADMOB_AD_SHOW = "admob_ad_impression"
        const val EVENT_ADMOB_AD_CLICK = "admob_ad_click"
    }

     open fun getNativeUnitId() : String {
        if (useDebugAdmobId) {
            URLog.i(loggerTag(), "user Debug Admob native unit id $ADMOB_NATIVE_UNIT_ID_TEST")
            return ADMOB_NATIVE_UNIT_ID_TEST
        }
        return ADMOB_NATIVE_UNIT_ID
    }



    open fun reportAdmobAdImpression(unitId : String, adFormat : String) {
        URLog.i(loggerTag(), "reportAdmobAdImpression $unitId $adFormat")
        TDAnalyticsManager.reportTrackEvent(
            EVENT_ADMOB_AD_SHOW,
            JSONObject().apply {
                put("unitId", unitId)
                put("adFormat", adFormat)
            }
        )
    }

    open fun reportAdmobAdClick(unitId : String, adFormat : String) {
        URLog.i(loggerTag(), "reportAdmobAdClick $unitId $adFormat")
        TDAnalyticsManager.reportTrackEvent(
            EVENT_ADMOB_AD_CLICK,
            JSONObject().apply {
                put("unitId", unitId)
                put("adFormat", adFormat)
            }
        )
    }


    open fun getBannerAdFormat() : String = "Locker_Banner"

    open fun getNativeAdFormat() : String = "Locker_Native"

    open fun getBannerUnitId() : String {
        if (useDebugAdmobId) {
            URLog.i(loggerTag(), "user Debug Admob banner unit id $ADMOB_BANNER_UNIT_TEST_ID")
            return ADMOB_BANNER_UNIT_TEST_ID
        }
        return ADMOB_BANNER_UNIT_ID
    }

    open fun loggerTag() : String = TAG

    private var interstitialAd : InterstitialAd? = null
    private var rewardedAd : RewardedAd? = null
    private var nativeAd : NativeAd? = null

    private var nativeAdShowed : Boolean = false

    private var bannerAd : AdView? = null

    override fun initialize(application: Application) {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(application, ::logAdapterStatus)
        }
    }


    open fun enableEventImpressionToTenjin() : Boolean  = false

    private fun logAdapterStatus(initializationStatus: InitializationStatus) {
        for ((adapterClass, status) in initializationStatus.adapterStatusMap) {
            Log.i(
                loggerTag(),
                "Adapter: $adapterClass, Status: ${status.description}, Latency: ${status.latency}ms",
            )
        }
    }

    override fun loadRewardVideoAd(context: Context) {
        if (isRewardVideoAdReady()) {
            URLog.info(loggerTag(), "loadRewardVideoAd isRewardVideoAdReady = true")
            return
        }
        RewardedAd.load(
            context,
            ADMOB_REWARD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(loggerTag(), "Ad was loaded.")
                    rewardedAd = ad
                    rewardedAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                                Log.d(TAG, "Ad was dismissed.")
                                // Don't forget to set the ad reference to null so you
                                // don't show the ad a second time.
                                rewardedAd = null
                                internalRewardAdListeners.forEach {
                                    it.onRewardedAdPlayEnd(this@AdmobAdLoader)
                                    it.onRewardedAdClosed(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when fullscreen content failed to show.
                                Log.d(loggerTag(), "Ad failed to show.")
                                // Don't forget to set the ad reference to null so you
                                // don't show the ad a second time.
                                rewardedAd = null
                                internalRewardAdListeners.forEach {
                                    it.onRewardedVideoAdPlayFailed(this@AdmobAdLoader, adError.message)
                                }
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                Log.d(loggerTag(), "Ad showed fullscreen content.")
                                internalRewardAdListeners.forEach {
                                    it.onRewardedAdPlayStart(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d(loggerTag(), "Ad recorded an impression.")
                                internalRewardAdListeners.forEach {
                                    it.onRewardedAdPlayStart(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdClicked() {
                                // Called when an ad is clicked.
                                Log.d(loggerTag(), "Ad was clicked.")
                                internalRewardAdListeners.forEach {
                                    it.onRewardedAdPlayClicked(this@AdmobAdLoader)
                                }
                            }
                        }

                    internalRewardAdListeners.forEach {
                        it.onRewardedAdLoaded(this@AdmobAdLoader)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    rewardedAd = null
                    internalRewardAdListeners.forEach {
                        it.onRewardedAdFailed(this@AdmobAdLoader, adError.message)
                    }
                }
            },
        )
    }

    override fun isRewardVideoAdReady(): Boolean {
        return rewardedAd != null
    }

    override fun showRewardVideoAd(
        activity: Activity,
        adShowScene: AdShowScene
    ) {
        rewardedAd?.show(
            activity,
            OnUserEarnedRewardListener { rewardItem ->
                Log.d(loggerTag(), "User earned the reward.")
                // Handle the reward.
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
            },
        )
    }

    override fun loadInterstitialAd(context: Context) {
        InterstitialAd.load(
            context,
            ADMOB_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(loggerTag(), "Ad was loaded.")
                    interstitialAd = ad
                    interstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                                URLog.debug(TAG, "Ad was dismissed.")
                                interstitialAd = null
                                internalInterstitialAdListeners.forEach {
                                    it.onInterstitialAdClose(this@AdmobAdLoader)
                                    it.onInterstitialAdVideoEnd(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when fullscreen content failed to show.
                                URLog.debug(loggerTag(), "Ad failed to show.")
                                // Don't forget to set the ad reference to null so you
                                // don't show the ad a second time.
                                interstitialAd = null
                                internalInterstitialAdListeners.forEach {
                                    it.onInterstitialAdVideoError(this@AdmobAdLoader, adError.message)
                                }
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                URLog.debug(loggerTag(),  "Ad showed fullscreen content.")

                                internalInterstitialAdListeners.forEach {
                                    it.onInterstitialAdVideoStart(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                URLog.debug(loggerTag(), "Ad recorded an impression.")
                                internalInterstitialAdListeners.forEach {
                                    it.onInterstitialAdShow(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdClicked() {
                                // Called when ad is clicked.
                                URLog.debug(loggerTag(),  "Ad was clicked.")
                                internalInterstitialAdListeners.forEach {
                                    it.onInterstitialAdClicked(this@AdmobAdLoader)
                                }
                            }
                        }
                    internalInterstitialAdListeners.forEach {
                        it.onInterstitialAdLoaded(this@AdmobAdLoader)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(loggerTag(), adError.message)
                    interstitialAd = null
                    internalInterstitialAdListeners.forEach {
                        it.onInterstitialAdLoadFail(this@AdmobAdLoader, adError.message)
                    }
                }
            },
        )
    }

    override fun isInterstitialAdReady(): Boolean {
        return interstitialAd != null
    }

    override fun showInterstitialAd(
        activity: Activity,
        adShowScene: AdShowScene
    ) {
        interstitialAd?.show(activity)
    }


    override fun loadSplashAd(context: Context, fetchAdTimeout: Int) {
        // ignore
    }

    override fun isSplashAdReady(): Boolean {
        // ignore
        return false
    }

    override fun showSplashAd(activity: Activity, container: ViewGroup) {
        // ignore
    }

    override fun isBannerAdReady(): Boolean {
        return bannerAd != null
    }

    override fun loadBannerAd(context: Context, container: ViewGroup?) {
        bannerAd = AdView(context)
        bannerAd?.adUnitId = getBannerUnitId()
        bannerAd?.setAdSize(AdSize.SMART_BANNER)
        // Request an anchored adaptive banner with a width of 360.
        container?.let {
            bannerAd?.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
            (bannerAd?.parent as? ViewGroup)?.removeView(bannerAd)
            it.removeAllViews()
            it.addView(bannerAd)
        }
        val adRequest = AdRequest.Builder().build()
        bannerAd?.setOnPaidEventListener {
            val adValue = it
            Log.i(loggerTag(), "on Admob BannerAd paid $it")
            val adSourceName = bannerAd?.responseInfo?.loadedAdapterResponseInfo?.adSourceName
            val adSourceId = bannerAd?.responseInfo?.loadedAdapterResponseInfo?.adSourceId
            val revenue: Double = it.valueMicros * 1.0/ 1_000_000 // 价值，以微单位表示 (例如 5000 代表 0.005 USD)
            TDAnalyticsManager.reportAdRevenue(
                it.currencyCode,
                revenue,
                adSourceName ?: "",
                getBannerUnitId(),
                "admob",
                getBannerAdFormat(),
                adSourceId ?: ""
            )
            try {
                if (enableEventImpressionToTenjin()) {
                    TenjinManager.getTenjinInstance()?.eventAdImpressionAdMob(adValue, bannerAd)
                }
            } catch (_ : Exception) {
            }
        }
        bannerAd?.adListener = object : AdListener() {
                override fun onAdClicked() {
                    URLog.i(loggerTag(), "admob onBannerAdClicked")
                    internalBannerAdListeners.forEach {
                        it.onBannerAdClicked(this@AdmobAdLoader)
                    }
                    reportAdmobAdClick(getBannerUnitId(), getBannerAdFormat())
                }

                override fun onAdClosed() {
                    URLog.i(loggerTag(), "admob onBannerAdOpened")
                    internalBannerAdListeners.forEach {
                        it.onBannerAdOpened(this@AdmobAdLoader)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    URLog.i(loggerTag(), "admob onBannerAdLoadFail")
                    internalBannerAdListeners.forEach {
                        it.onBannerAdLoadFail(this@AdmobAdLoader, adError.message)
                    }
                    bannerAd = null
                }

                override fun onAdImpression() {
                    URLog.i(loggerTag(), "admob onBannerAdImpression")
                    internalBannerAdListeners.forEach {
                        it.onBannerAdShow(this@AdmobAdLoader)
                    }
                    reportAdmobAdImpression(getBannerUnitId(), getBannerAdFormat())
                }

                override fun onAdLoaded() {
                    URLog.i(loggerTag(), "admob onBannerAdLoaded")
                    internalBannerAdListeners.forEach {
                        it.onBannerAdLoaded(this@AdmobAdLoader)
                    }
                }

                override fun onAdOpened() {
                    URLog.i(loggerTag(), "admob onBannerAdOpened")
                    internalBannerAdListeners.forEach {
                        it.onBannerAdOpened(this@AdmobAdLoader)
                    }
                }
            }
        bannerAd?.loadAd(adRequest)
    }

    override fun showBannerAd(context: Context, container: ViewGroup) {
        if (bannerAd != null) {
            // Request an anchored adaptive banner with a width of 360.
            bannerAd?.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
            (bannerAd?.parent as? ViewGroup)?.removeView(bannerAd)
            container.removeAllViews()
            container.addView(bannerAd)
        } else {
            loadBannerAd(context, container)
        }
    }


    override fun isNativeAdReady(): Boolean {
        return nativeAd != null && !nativeAdShowed
    }

    override fun loadNativeAd(context: Context) {
        // It is recommended to call AdLoader.Builder on a background thread.
        if (isNativeAdReady() && !nativeAdShowed) {
            Log.i(loggerTag(), "onAdmob native isNativeAdReady")
            return
        }
        nativeAdShowed = false
        CoroutineScope(Dispatchers.IO).launch {
            val adLoader =
                AdLoader.Builder(context, getNativeUnitId())
                    .forNativeAd { ad ->
                        // The native ad loaded successfully. You can show the ad.
                        nativeAdShowed = false
                        nativeAd = ad
                        nativeAd?.setOnPaidEventListener {
                            Log.i(TAG, "on Admob native paid $it")
                            val adValue = it
                            val adSourceName = nativeAd?.responseInfo?.loadedAdapterResponseInfo?.adSourceName
                            val adSourceId = nativeAd?.responseInfo?.loadedAdapterResponseInfo?.adSourceId
                            val revenue: Double = it.valueMicros * 1.0/ 1_000_000 // 价值，以微单位表示 (例如 5000 代表 0.005 USD)
                            TDAnalyticsManager.reportAdRevenue(
                                it.currencyCode,
                                revenue, //
                                adSourceName ?: "",
                                getNativeUnitId(),
                                "admob",
                                getNativeAdFormat(),
                                adSourceId ?: ""
                            )
                            try {
                                if (enableEventImpressionToTenjin()) {
                                    TenjinManager.getTenjinInstance()
                                        ?.eventAdImpressionAdMob(adValue, nativeAd)
                                }
                            } catch (_ : Exception) {
                            }
                        }
                    }
                    .withAdListener(
                        object : AdListener() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                // The native ad load failed. Check the adError message for failure reasons.
                                URLog.i(loggerTag(), "admob onAdFailedToLoad ${adError.message}")
                                internalNativeAdListeners.forEach {
                                    it.onNativeAdLoadFail(this@AdmobAdLoader, adError.message)
                                }
                            }

                            override fun onAdClicked() {
                                super.onAdClicked()
                                URLog.i(loggerTag(), "admob native onAdClicked ")
                                internalNativeAdListeners.forEach {
                                    it.onNativeAdClicked(this@AdmobAdLoader)
                                }
                                reportAdmobAdClick(getNativeUnitId(), getNativeAdFormat())
                            }

                            override fun onAdImpression() {
                                super.onAdImpression()
                                nativeAdShowed = true
                                URLog.i(loggerTag(), "admob native onAdImpression ")
                                internalNativeAdListeners.forEach {
                                    it.onNativeAdShow(this@AdmobAdLoader)
                                }
                                reportAdmobAdImpression(getNativeUnitId(), getNativeAdFormat())
                                URLog.i(loggerTag(), "admob native onAdImpression set nativeAd is empty")
                            }

                            override fun onAdClosed() {
                                super.onAdClosed()
                                URLog.i(loggerTag(), "admob native onAdClosed ")
                                internalNativeAdListeners.forEach {
                                    it.onNativeAdClose(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                URLog.i(loggerTag(), "admob native onAdLoaded ")
                                internalNativeAdListeners.forEach {
                                    it.onNativeAdLoaded(this@AdmobAdLoader)
                                }
                            }

                            override fun onAdOpened() {
                                super.onAdOpened()
                                internalNativeAdListeners.forEach {
                                    it.onNativeAdOpened(this@AdmobAdLoader)
                                }
                            }
                        }
                    )
                    // Use the NativeAdOptions.Builder class to specify individual options settings.
                    .withNativeAdOptions(NativeAdOptions.Builder().build())
                    .build()
            val adRequest = AdRequest.Builder().build()
            adLoader.loadAd(adRequest)
        }
    }

    override fun showNativeAd(
        context: Context,
        adContainer: ViewGroup,
        adViewWidth: Int
    ) {
        nativeAd?.let {
            URLog.i(loggerTag(), "show admob native Ad")
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                    as LayoutInflater
            val adView =  inflater.inflate(R.layout.admob_native_ad_layout,
                null) as NativeAdView
            val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
            headlineView?.text = it.headline


            val adBody = adView.findViewById<TextView>(R.id.ad_body)
            adBody?.text = it.body
            val starRatingView = adView.findViewById<RatingBar>(R.id.ad_stars)
            if (it.starRating == null) {
                starRatingView?.visibility = View.INVISIBLE
            } else {
                starRatingView?.visibility = View.VISIBLE
                starRatingView?.rating = it.starRating?.toFloat()!!
            }
            val adAppIcon = adView.findViewById<ImageView>(R.id.ad_app_icon)
            if (it.icon == null) {
                adAppIcon?.visibility = View.GONE
            } else {
                adAppIcon?.setImageDrawable(it.icon?.drawable)
                adAppIcon?.visibility = View.VISIBLE
            }
            adView.headlineView = headlineView
            val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
            adView.mediaView = mediaView

             val callToActionButton = adView.findViewById<TextView>(R.id.ad_call_to_action)
            callToActionButton?.text = it.callToAction
            adView.callToActionView = callToActionButton
            adView.setNativeAd(it)
            adContainer.removeAllViews()
            adContainer.addView(adView)
        } ?: {
            URLog.i(loggerTag(), "showNativeAd but Native Ad not ready")
        }
    }

}
