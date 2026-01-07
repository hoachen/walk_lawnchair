package com.ur.apps.ad.topon

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.anythink.banner.api.ATBannerListener
import com.anythink.banner.api.ATBannerView
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.ATNetworkConfirmInfo
import com.anythink.core.api.ATSDK
import com.anythink.core.api.ATShowConfig
import com.anythink.core.api.AdError
import com.anythink.interstitial.api.ATInterstitial
import com.anythink.interstitial.api.ATInterstitialListener
import com.anythink.nativead.api.ATNative
import com.anythink.nativead.api.ATNativeAdView
import com.anythink.nativead.api.ATNativeEventListener
import com.anythink.nativead.api.ATNativeNetworkListener
import com.anythink.nativead.api.ATNativePrepareInfo
import com.anythink.nativead.api.NativeAd
import com.anythink.rewardvideo.api.ATRewardVideoAd
import com.anythink.rewardvideo.api.ATRewardVideoListener
import com.anythink.splashad.api.ATSplashAd
import com.anythink.splashad.api.ATSplashAdExtraInfo
import com.anythink.splashad.api.ATSplashExListener
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.sg.CryptoUtils
import com.sg.UserManager
import com.ur.apps.ad.AdShowScene
import com.ur.apps.ad.BaseAdLoader
import com.ur.apps.ad.RewardAdRecord
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.analysis.tenjin.TenjinManager
import com.ur.apps.analysis.utils.DeviceInfoCollector
import com.ur.apps.utils.URLog
import org.json.JSONObject


/**
 * https://newdocs.toponad.com/docs/Android
 */
class TopOnAdLoader : BaseAdLoader() {

    companion object {
        val TAG = "TopOnAdLoader"
        val TOPON_APPKEY = "a31e4b08d1534d7dd2390086a6a1268d"
        val TOPON_APPID = "h67caa79cef420"
        val TOPON_REWARD_PLACEMENT_ID = "n67caa8777d739"
        val TOPON_INTERSTITIAL_PLACEMENT_ID = "n67caa878419ea"
        val TOPON_SPLASHAD_PLACEMENT_ID = "n67caa87a1e848"
        val TOPON_BANNER_PLACEMENT_ID = "n67caa8791354c"
        val TOPON_NATIVE_PLACEMENT_ID = "n67caa87b04683"
    }


    // 激励广告
    private var atRewardedAd: ATRewardVideoAd? = null

    private var lastRewardVideoAdRecord: RewardAdRecord? = null

    // 插屏广告
    private var atInterstitialAd: ATInterstitial? = null

    private var lastInterstitialAdRecord: RewardAdRecord? = null

    // 开屏广告
    private var atSplashAd: ATSplashAd? = null

    // 原生广告
    private var atNative: ATNative? = null

    private var mNativeAd: NativeAd? = null

    private var context: Context? = null


    override fun initialize(application: Application) {
        context = application.applicationContext
        //初始化SDK
        ATSDK.init(application.applicationContext, TOPON_APPID, TOPON_APPKEY)
//        if (BuildConfig.DEBUG) {
//            ATSDK.integrationChecking(application.applicationContext)
//        }
//        ATSDK.setNetworkLogDebug(true)
    }

    private fun createCustomATShowConfig(atid: String): ATShowConfig {
        val deviceInfoCollector = DeviceInfoCollector.instance
        val customDataJSON = JSONObject().apply {
            put("bundle", deviceInfoCollector.getAppInfo().packageName)
            put("vc", deviceInfoCollector.getAppInfo().versionCode)
            put("atid", atid)
            put("ts", System.currentTimeMillis())
            put("gaid", deviceInfoCollector.getAdvertisingId())
            put("androidid", deviceInfoCollector.getDeviceIdentity().androidId)
        }
        val customData = CryptoUtils.encrypt(customDataJSON.toString())
        URLog.info(TAG, "setCustomAdConfig : before encryption\n $customDataJSON \n after encryption $customData")
        val adShowConfig = ATShowConfig.Builder().showCustomExt(customData).build()
        return adShowConfig
    }


    inner class InternalRewardVideoListener : ATRewardVideoListener {

        override fun onRewardedVideoAdLoaded() {
            URLog.i(TAG, "onRewardedVideoAdLoaded")
            internalRewardAdListeners.forEach {
                it.onRewardedAdLoaded(this@TopOnAdLoader)
            }
        }

        override fun onRewardedVideoAdFailed(adError: AdError) {
            //注意：禁止在此回调中执行广告的加载方法进行重试，否则会引起很多无用请求且可能会导致应用卡顿
            //AdError，请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_test?id=aderror
            URLog.i(TAG, "onRewardedVideoAdFailed:" + adError.fullErrorInfo)
            internalRewardAdListeners.forEach {
                it.onRewardedAdFailed(this@TopOnAdLoader, adError.fullErrorInfo)
            }
        }

        override fun onRewardedVideoAdPlayStart(atAdInfo: ATAdInfo) {
            //ATAdInfo可区分广告平台以及获取广告平台的广告位ID等
            //请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_sdk_callback_access?id=callback_info
            //建议在此回调中调用load进行广告的加载，方便下一次广告的展示（不需要调用isAdReady()）
            URLog.i(TAG, "onRewardedVideoAdPlayStart:" + atAdInfo.format)
            lastRewardVideoAdRecord?.apply {
                pbTs = System.currentTimeMillis()
            }
            internalRewardAdListeners.forEach {
                it.onRewardedAdPlayStart(this@TopOnAdLoader)
            }
            atRewardedAd?.load()
            TenjinManager.getTenjinInstance()?.let {
                URLog.info(
                    "TopOnAd",
                    "onRewardedVideoAdPlayStart report tenjin eventAdImpressionTopOn"
                )
                it.eventAdImpressionTopOn(atAdInfo)
            }
            TDAnalyticsManager.reportAdShow(
                adType = "Reward",
                adPosition = "Task_Reward",
                adPositionType = "Reward",
                placementId = TOPON_REWARD_PLACEMENT_ID
            )
            atAdInfo.reportAdRevenue()
        }

        override fun onRewardedVideoAdPlayEnd(atAdInfo: ATAdInfo) {
            URLog.i(TAG, "onRewardedVideoAdPlayEnd:" + atAdInfo.format)
            lastRewardVideoAdRecord?.apply {
                peTs = System.currentTimeMillis()
            }
            internalRewardAdListeners.forEach {
                it.onRewardedAdPlayEnd(this@TopOnAdLoader)
            }
        }

        override fun onRewardedVideoAdPlayFailed(adError: AdError, atAdInfo: ATAdInfo) {
            //AdError，请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_test?id=aderror
            URLog.e(TAG, "onRewardedVideoAdPlayFailed:" + adError.fullErrorInfo)
            internalRewardAdListeners.forEach {
                it.onRewardedVideoAdPlayFailed(
                    this@TopOnAdLoader,
                    adError.fullErrorInfo
                )
            }
        }

        override fun onRewardedVideoAdClosed(atAdInfo: ATAdInfo) {
            URLog.i(TAG, "onRewardedVideoAdClosed:" + atAdInfo.format)
            internalRewardAdListeners.forEach {
                it.onRewardedAdClosed(this@TopOnAdLoader)
            }
        }

        override fun onReward(atAdInfo: ATAdInfo) {
            //建议在此回调中下发奖励，一般在onRewardedVideoAdClosed之前回调
            URLog.i(TAG, "onReward:" + atAdInfo.format)
            lastRewardVideoAdRecord?.apply {
                ecpm = atAdInfo.ecpm.toFloat()
                adNetwork = atAdInfo.adNetworkType
            }
            internalRewardAdListeners.forEach {
                it.onReward(this@TopOnAdLoader, lastRewardVideoAdRecord!!)
            }
        }

        override fun onRewardedVideoAdPlayClicked(atAdInfo: ATAdInfo) {
            URLog.i(TAG, "onRewardedVideoAdPlayClicked:" + atAdInfo.format)
            internalRewardAdListeners.forEach {
                it.onRewardedAdPlayClicked(this@TopOnAdLoader)
            }
            TDAnalyticsManager.reportAdClick(
                adType = "Reward",
                adPosition = "Task_Reward",
                adPositionType = "Reward",
                placementId = TOPON_REWARD_PLACEMENT_ID
            )
        }

    }


    override fun loadRewardVideoAd(context: Context) {
        if (isRewardVideoAdReady()) {
            URLog.info(TAG, "loadRewardVideoAd ad has Loaded")
            return
        }
        URLog.info(TAG, "start loadRewardVideoAd $TOPON_REWARD_PLACEMENT_ID")
        if (atRewardedAd == null) {
            atRewardedAd = ATRewardVideoAd(context.applicationContext, TOPON_REWARD_PLACEMENT_ID)
            atRewardedAd?.setAdListener(InternalRewardVideoListener())
        }
        atRewardedAd?.load()
    }

    override fun isRewardVideoAdReady(): Boolean {
        return atRewardedAd?.isAdReady == true
    }

    override fun showRewardVideoAd(activity: Activity, adShowScene: AdShowScene) {
        if (atRewardedAd?.isAdReady == true) {
            lastRewardVideoAdRecord = RewardAdRecord(
                scene = adShowScene,
                auid = TOPON_REWARD_PLACEMENT_ID,
                adFormat = RewardAdRecord.FORMAT_REWARDED_VIDEO,
                adPlatform = RewardAdRecord.PLATFORM_TOPON
            )
            val atShowConfig = createCustomATShowConfig(lastRewardVideoAdRecord!!.adTraceId)
            URLog.info(TAG, "show rewardAd, customConfig $lastRewardVideoAdRecord")
            TenjinManager.notifyAdDisplayed()
            atRewardedAd?.show(activity, atShowConfig)
        } else {
            loadRewardVideoAd(activity)
        }
    }

    inner class InternalInterstitialListener : ATInterstitialListener {
        override fun onInterstitialAdLoaded() {
            URLog.info(TAG, "onInterstitialAdLoaded")
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdLoaded(this@TopOnAdLoader)
            }
        }

        override fun onInterstitialAdLoadFail(adError: AdError) {
            //注意：禁止在此回调中执行广告的加载方法进行重试，否则会引起很多无用请求且可能会导致应用卡顿
            //AdError，请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_test?id=aderror
            Log.e(TAG, "onInterstitialAdLoadFail:" + adError.fullErrorInfo)
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdLoadFail(
                    this@TopOnAdLoader,
                    adError.fullErrorInfo
                )
            }
        }

        override fun onInterstitialAdClicked(atAdInfo: ATAdInfo) {
            URLog.info(TAG, "onInterstitialAdClicked")
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdClicked(this@TopOnAdLoader)
            }
            TDAnalyticsManager.reportAdClick(
                adType = "Interstitial",
                adPosition = "Interstitial Ad",
                adPositionType = "Interstitial",
                placementId = TOPON_INTERSTITIAL_PLACEMENT_ID
            )
        }

        override fun onInterstitialAdShow(adInfo: ATAdInfo) {
            //建议在此回调中调用load进行广告的加载，方便下一次广告的展示（不需要调用isAdReady()）
            atInterstitialAd?.load()
            lastInterstitialAdRecord?.apply {
                pbTs = System.currentTimeMillis()
            }
            TenjinManager.getTenjinInstance()?.let {
                URLog.info(TAG, "onInterstitialAdShow  report tenjin eventAdImpressionTopOn")
                it.eventAdImpressionTopOn(adInfo)
            }
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdShow(this@TopOnAdLoader)
            }
            TDAnalyticsManager.reportAdShow(
                adType = "Interstitial",
                adPosition = "Interstitial Ad",
                adPositionType = "Interstitial",
                placementId = TOPON_INTERSTITIAL_PLACEMENT_ID
            )
            adInfo.reportAdRevenue()
        }

        override fun onInterstitialAdClose(atAdInfo: ATAdInfo) {
            URLog.info(TAG, "onInterstitialAdClose")
            lastInterstitialAdRecord?.apply {
                ecpm = atAdInfo.ecpm.toFloat()
                adNetwork = atAdInfo.adNetworkType
            }
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdClose(this@TopOnAdLoader)
            }
            if (lastInterstitialAdRecord?.scene != AdShowScene.NO_REWARD) {
                onInterstitialReward(atAdInfo)
            } else {
                URLog.info(TAG, "ad show scene no_reward, ignore onInterstitialReward")
            }
        }


        fun onInterstitialReward(atAdInfo: ATAdInfo) {
            //建议在此回调中下发奖励，一般在onRewardedVideoAdClosed之前回调
            URLog.info(TAG, "onInterstitialReward:" + atAdInfo.format)
            lastInterstitialAdRecord?.apply {
                ecpm = atAdInfo.ecpm.toFloat()
                adNetwork = atAdInfo.adNetworkType
            }
            internalInterstitialAdListeners.forEach {
                it.onInterstitialReward(this@TopOnAdLoader, lastInterstitialAdRecord!!)
            }
        }

        override fun onInterstitialAdVideoStart(atAdInfo: ATAdInfo) {
            //ATAdInfo可区分广告平台以及获取广告平台的广告位ID等
            //请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_sdk_callback_access?id=callback_info
            URLog.info(TAG, "onInterstitialAdVideoStart")
            lastInterstitialAdRecord?.apply {
                pbTs = System.currentTimeMillis()
            }
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdVideoStart(this@TopOnAdLoader)
            }
        }

        override fun onInterstitialAdVideoEnd(atAdInfo: ATAdInfo) {
            URLog.info(TAG, "onInterstitialAdVideoEnd")
            lastInterstitialAdRecord?.apply {
                peTs = System.currentTimeMillis()
            }
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdVideoEnd(this@TopOnAdLoader)
            }
        }

        override fun onInterstitialAdVideoError(adError: AdError) {
            //AdError，请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_test?id=aderror
            Log.e(TAG, "onInterstitialAdVideoError:" + adError.fullErrorInfo)
            internalInterstitialAdListeners.forEach {
                it.onInterstitialAdVideoError(this@TopOnAdLoader, adError.fullErrorInfo)
            }
        }
    }

    override fun loadInterstitialAd(context: Context) {
        if (atInterstitialAd == null) {
            atInterstitialAd = ATInterstitial(
                context.applicationContext,
                TOPON_INTERSTITIAL_PLACEMENT_ID
            ).apply {
                setAdListener(InternalInterstitialListener())
            }
        }
        atInterstitialAd?.load()
    }

    override fun isInterstitialAdReady(): Boolean {
        return atInterstitialAd?.isAdReady == true
    }

    override fun showInterstitialAd(activity: Activity, adShowScene: AdShowScene) {
        if (isInterstitialAdReady()) {
            URLog.i(TAG, "show interstitial $adShowScene")
            lastInterstitialAdRecord = RewardAdRecord(
                scene = adShowScene,
                auid = TOPON_INTERSTITIAL_PLACEMENT_ID,
                adFormat = RewardAdRecord.FORMAT_INTERSTITIAL,
                adPlatform = RewardAdRecord.PLATFORM_TOPON
            )
            TenjinManager.notifyAdDisplayed()
            atInterstitialAd?.show(
                activity, createCustomATShowConfig(
                    lastInterstitialAdRecord!!.adTraceId
                )
            )
        } else {
            loadInterstitialAd(activity.applicationContext)
        }
    }


    inner class InternalSplashListener : ATSplashExListener {
        override fun onAdLoaded(isTimeout: Boolean) {
            URLog.info(TAG, "onSplashAdLoaded isTimeout = $isTimeout")
            if (isTimeout) {
                internalSplashAdListeners.forEach {
                    it.onAdLoadTimeout(this@TopOnAdLoader)
                }
            } else {
                internalSplashAdListeners.forEach {
                    it.onAdLoaded(this@TopOnAdLoader)
                }
            }
        }

        override fun onAdLoadTimeout() {
            URLog.info(TAG, "onSplashAdLoadTimeout")
            internalSplashAdListeners.forEach {
                it.onAdLoadTimeout(this@TopOnAdLoader)
            }
        }

        override fun onNoAdError(error: AdError) {
            URLog.info(TAG, "onSplashNoAdError ${error.fullErrorInfo}")
            internalSplashAdListeners.forEach {
                it.onNoAdError(this@TopOnAdLoader)
            }
        }

        override fun onAdShow(adInfo: ATAdInfo) {
            URLog.info(TAG, "onSplashAdShow ${adInfo.format}")
            internalSplashAdListeners.forEach {
                it.onAdShow(this@TopOnAdLoader)
            }
            lastInterstitialAdRecord?.apply {
                pbTs = System.currentTimeMillis()
            }
            TenjinManager.getTenjinInstance()?.let {
                URLog.info("TopOnAd", "onSplash AdShow  report tenjin eventAdImpressionTopOn")
                it.eventAdImpressionTopOn(adInfo)
            }
            TDAnalyticsManager.reportAdShow(
                adType = "Splash",
                adPosition = "Splash Ad",
                adPositionType = "Splash",
                placementId = TOPON_SPLASHAD_PLACEMENT_ID
            )
            adInfo.reportAdRevenue()
        }

        override fun onAdClick(adInfo: ATAdInfo) {
            URLog.info(TAG, "onSplashAdClick ${adInfo.format}")
            internalSplashAdListeners.forEach {
                it.onAdClick(this@TopOnAdLoader)
            }
            TDAnalyticsManager.reportAdClick(
                adType = "Splash",
                adPosition = "Splash Ad",
                adPositionType = "Splash",
                placementId = TOPON_SPLASHAD_PLACEMENT_ID
            )
        }

        override fun onAdDismiss(atAdInfo: ATAdInfo, p1: ATSplashAdExtraInfo) {
            URLog.info(TAG, "onSplashAdDismiss ${atAdInfo.format}")
            lastInterstitialAdRecord?.apply {
                peTs = System.currentTimeMillis()
            }
            internalSplashAdListeners.forEach {
                it.onAdDismiss(this@TopOnAdLoader)
            }
        }

        override fun onDeeplinkCallback(adInfo: ATAdInfo, p1: Boolean) {
            URLog.info(TAG, "onDeeplinkCallback ${adInfo.format}")
        }

        override fun onDownloadConfirm(
            context: Context,
            adInfo: ATAdInfo,
            p2: ATNetworkConfirmInfo?
        ) {
            URLog.info(TAG, "onDownloadConfirm ${adInfo.format}")
        }
    }

    override fun loadSplashAd(context: Context, fetchAdTimeout: Int) {
        if (atSplashAd == null) {
            atSplashAd = ATSplashAd(
                context, TOPON_SPLASHAD_PLACEMENT_ID,
                InternalSplashListener(), fetchAdTimeout
            )
        }
        URLog.info(TAG, "start load SplashAd")
        atSplashAd?.loadAd()
    }

    override fun isSplashAdReady(): Boolean {
        return atSplashAd?.isAdReady == true
    }

    override fun showSplashAd(activity: Activity, container: ViewGroup) {
        if (isSplashAdReady()) {
            atSplashAd?.show(activity, container);
        }
    }


    override fun loadBannerAd(context: Context, container: ViewGroup?) {
        val bannerView = ATBannerView(context)
        bannerView.setPlacementId(TOPON_BANNER_PLACEMENT_ID)

//        val width: Int = ViewGroup.LayoutParams.WRAP_CONTENT //定一个宽度值，比如屏幕宽度
//        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        //如果出现Banner有时高、有时低的情况，请使用此代码
        val ratio = 320 / 80f //必须跟TopOn后台配置的Banner广告源宽高比例一致，假设尺寸为320x80
        val width = context.resources.displayMetrics.widthPixels;//定一个宽度值，比如屏幕宽度
        val height = (width / ratio)
        bannerView.layoutParams = FrameLayout.LayoutParams(width, height.toInt())
        bannerView.setBannerAdListener(object : ATBannerListener {
            override fun onBannerLoaded() {

            }

            override fun onBannerFailed(adError: AdError) {
                //注意：禁止在此回调中执行广告的加载方法进行重试，否则会引起很多无用请求且可能会导致应用卡顿
                //AdError，请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_test?id=aderror
                Log.e(TAG, "onBannerFailed:" + adError.fullErrorInfo)
            }

            override fun onBannerClicked(atAdInfo: ATAdInfo) {
                TDAnalyticsManager.reportAdClick(
                    adType = "Banner",
                    adPosition = "Redeem_Coin_Reward",
                    adPositionType = "Coin",
                    placementId = TOPON_BANNER_PLACEMENT_ID
                )
            }

            override fun onBannerShow(adInfo: ATAdInfo) {
                //ATAdInfo可区分广告平台以及获取广告平台的广告位ID等
                //请参考 https://docs.toponad.com/#/zh-cn/android/android_doc/android_sdk_callback_access?id=callback_info
                TenjinManager.getTenjinInstance()?.let {
                    URLog.info("TopOnAd", "onBannerShow  report tenjin eventAdImpressionTopOn")
                    it.eventAdImpressionTopOn(adInfo)
                }
                TDAnalyticsManager.reportAdShow(
                    adType = "Banner",
                    adPosition = "Redeem_Coin_Reward",
                    adPositionType = "Coin",
                    placementId = TOPON_BANNER_PLACEMENT_ID
                )
                adInfo.reportAdRevenue()
            }

            override fun onBannerClose(atAdInfo: ATAdInfo) {
                if (bannerView.parent != null) {
                    (bannerView.parent as ViewGroup).removeView(bannerView)
                }
            }

            override fun onBannerAutoRefreshed(atAdInfo: ATAdInfo) {

            }

            override fun onBannerAutoRefreshFail(adError: AdError) {
                //AdError：https://docs.toponad.com/#/zh-cn/android/android_doc/android_test?id=aderror
                Log.e(TAG, "onBannerAutoRefreshFail:" + adError.fullErrorInfo)
            }
        })
        container?.addView(bannerView)
        bannerView.loadAd()
    }

    override fun showBannerAd(context: Context, container: ViewGroup) {
    }

    override fun isNativeAdReady() : Boolean {
        return false
    }

    override fun isBannerAdReady() : Boolean {
        return false
    }

    override fun loadNativeAd(context: Context) {
        if (atNative == null) {
            atNative = ATNative(
                context, TOPON_NATIVE_PLACEMENT_ID,
                object : ATNativeNetworkListener {
                    override fun onNativeAdLoaded() {
                        URLog.info(TAG, "onNativeAdLoaded")
                    }

                    override fun onNativeAdLoadFail(adError: AdError?) {
                        URLog.info(TAG, "onNativeAdLoadFail:" + adError?.fullErrorInfo)
                    }
                })
        }
        atNative?.makeAdRequest()
    }

    override fun showNativeAd(context: Context, adContainer: ViewGroup, adViewWidth: Int) {
        if (atNative == null) {
            return;
        }

        if (atNative?.checkAdStatus()?.isReady == false) {
            return;
        }
        //渲染广告必须创建的容器
        val atNativeAdView: ATNativeAdView = ATNativeAdView(context)
        // todo check selfRenderView 
//        val mSelfRenderView: View //开发者自定义布局的容器
//
//        if (mSelfRenderView == null) {
//            mSelfRenderView = mATNativeAdView.findViewById(R.id.native_selfrender_view); //可在xml布局定义
//        }
        //开发者可以在调用getNativeAd后直接使用ATNative#makeAdRequest发起预加载下一次的广告
        loadNativeAd(context)
        if (atNative?.nativeAd != null) {
            mNativeAd?.destory()
            mNativeAd = atNative!!.nativeAd
            val atNativePrepareInfo = ATNativePrepareInfo()

            if (mNativeAd?.isNativeExpress == false) {
                //todo 自渲染
//                bindSelfRenderView(this, mNativeAd?.adMaterial, mSelfRenderView, atNativePrepareInfo)
//                mNativeAd?.renderAdContainer(atNativeAdView, mSelfRenderView);
            } else {
                //模板渲染
                mNativeAd?.renderAdContainer(atNativeAdView, null);
            }
            mNativeAd?.setNativeEventListener(object : ATNativeEventListener {
                override fun onAdImpressed(atNativeAdView: ATNativeAdView?, atAdInfo: ATAdInfo?) {
                    URLog.info(TAG, "native ad onAdImpressed:\n" + atAdInfo.toString())
                    TenjinManager.getTenjinInstance()?.let {
                        URLog.info("TopOnAd", "onNative AdImpressed eventAdImpressionTopOn")
                        it.eventAdImpressionTopOn(atAdInfo)
                    }
                    TDAnalyticsManager.reportAdShow(
                        adType = "Native",
                        adPosition = "Redeem_Coin_Reward",
                        adPositionType = "Coin",
                        placementId = TOPON_NATIVE_PLACEMENT_ID
                    )
                }

                override fun onAdClicked(atNativeAdView: ATNativeAdView?, atAdInfo: ATAdInfo?) {
                    URLog.info(TAG, "native ad onAdClicked:\n" + atAdInfo.toString())
                    TDAnalyticsManager.reportAdClick(
                        adType = "Native",
                        adPosition = "Redeem_Coin_Reward",
                        adPositionType = "Coin",
                        placementId = TOPON_NATIVE_PLACEMENT_ID
                    )
                }

                override fun onAdVideoStart(atNativeAdView: ATNativeAdView?) {
                    URLog.info(TAG, "native ad onAdVideoStart")
                }

                override fun onAdVideoEnd(atNativeAdView: ATNativeAdView?) {
                    URLog.info(TAG, "native ad onAdVideoEnd")
                }

                override fun onAdVideoProgress(atNativeAdView: ATNativeAdView?, progress: Int) {
                    URLog.info(TAG, "native ad onAdVideoProgress:$progress")
                }

            })
            mNativeAd?.prepare(atNativeAdView, atNativePrepareInfo)

        }
    }
}

fun ATAdInfo.reportAdRevenue() {
    val adInfo = this
    // report 到数数
    TDAnalyticsManager.reportAdRevenue(
        UserManager.instance.getUserInfo()?.countryCode ?: this.country,
        this.ecpm / 1000, // TopOn给的是ecpm ,也就是一千次广告展示的价格，收入= ecpm /100
        this.networkName,
        this.placementId,
        "topon",
        this.format,
        this.adsourceId
    )
    // report 到FireBase
    Firebase.analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION) {
        param(FirebaseAnalytics.Param.AD_PLATFORM, "topon")
        param(FirebaseAnalytics.Param.AD_UNIT_NAME, adInfo.placementId)
        param(FirebaseAnalytics.Param.AD_FORMAT, adInfo.format)
        param(FirebaseAnalytics.Param.AD_SOURCE, adInfo.networkName)
        param(
            FirebaseAnalytics.Param.VALUE,
            adInfo.ecpm / 1000
        ) //  // TopOn给的是ecpm ,也就是一千次广告展示的价格，收入= ecpm /100
        param(FirebaseAnalytics.Param.CURRENCY, "USD") // All Topin revenue is sent in USD
    }
}