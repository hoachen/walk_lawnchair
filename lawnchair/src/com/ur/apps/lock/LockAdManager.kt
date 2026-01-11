package com.ur.apps.lock

import android.app.Activity
import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import com.sg.UserManager
import com.sg.model.UserInfo
import com.ur.apps.ad.AdShowScene
import com.ur.apps.ad.BannerAdListener
import com.ur.apps.ad.BaseAdLoader
import com.android.launcher3.BuildConfig
import com.ur.apps.ad.InterstitialAdListener
import com.ur.apps.ad.NativeAdListener
import com.ur.apps.ad.RewardAdListener
import com.ur.apps.ad.admob.AdmobAdLoader
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.analysis.utils.AdSPUtil
import com.ur.apps.utils.DateUtils
import com.ur.apps.analysis.utils.DeviceInfoCollector
import com.ur.apps.lock.LockAdConfig
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_BANNER
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_INTERS
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_NONE
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_REWORD
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_NATIVE
import com.ur.apps.lock.LockAdRecord
import com.ur.apps.lock.LockRepository
import com.ur.apps.lock.parserJSON
import com.ur.apps.lock.toJSON
import com.ur.apps.lock.toJson
import com.ur.apps.lock.toLockAdConfig
import com.ur.apps.utils.DeviceInfoManager
import com.ur.apps.utils.GeoRequestUtil
import com.ur.apps.utils.URLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class LockAdManager private constructor() : RewardAdListener, InterstitialAdListener,
    BannerAdListener, NativeAdListener, UserManager.Companion.UserInfoListener {

    private var appContext: Context? = null

    private var deviceInfoManager: DeviceInfoManager? = null

    private var lockRepo : LockRepository? = null

    private var lockAdConfig : LockAdConfig? = LockAdConfig()

    private var lockAdRecord : LockAdRecord? = null

    private var lockAdLoader : AdmobAdLoader = AdmobAdLoader()

    private var nextAdFormat : String = AD_FORMAT_NATIVE

    private var isAdLoading: AtomicBoolean = AtomicBoolean(false)

    private var isShowAdWhenReady: AtomicBoolean = AtomicBoolean(false)

    private var activityWeakRef: WeakReference<Activity>? = null

    private var adListener : LockerAdLoadListener? = null

    private var retryLoadCount = 0

    private var isDebugMode = BuildConfig.DEBUG

    fun initContext(application: Application) {
        this.appContext = application.applicationContext
        this.deviceInfoManager = DeviceInfoManager.getInstance(appContext)
        this.lockRepo = LockRepository(application.applicationContext)
        initAdLoader(application)
        UserManager.addListener(this)
        GeoRequestUtil().fetchGeoInformation(deviceInfoManager) {
            loadLocalAdConfig()
            fetchRemoteAdConfig()
        }
    }

    private fun initAdLoader(application: Application) {
        this.lockAdLoader.initialize(application)
        this.lockAdLoader.addRewardAdListener(this)
        this.lockAdLoader.addInterstitialAdListener(this)
        this.lockAdLoader.addBannerAdListener(this)
        this.lockAdLoader.addNativeAdListener(this)
    }

    private fun fetchRemoteAdConfig(retryCount: Int = 0) {
        GlobalScope.launch(Dispatchers.IO) {
            val startRequestTime = System.currentTimeMillis()
            try {
                deviceInfoManager?.buildRegisterRequestBody()
                val remoteAdConfig = lockRepo?.getLockInfo()
                remoteAdConfig?.let {
                    URLog.info(TAG, "update ad config from remote $remoteAdConfig")
                    lockAdConfig  = remoteAdConfig
                    AdSPUtil.get().putString(AdSPUtil.KEY_LOCK_AD_CONFIG, it.toJson())
                    URLog.setLoggingEnabled(it.itm == 1 || BuildConfig.DEBUG)
                }
                val costTime = System.currentTimeMillis() - startRequestTime
                statsRemoteAdConfig(true, "", costTime)
                initAdRecordInfo()
                GlobalScope.launch(Dispatchers.Main) {
                    preloadAd()
                }
            } catch (e: Exception) {
                URLog.error(TAG, "load ad config failed: ${e.message}")
                val costTime = System.currentTimeMillis() - startRequestTime
                statsRemoteAdConfig(false, e.toString(), costTime)
                // 重试逻辑
                if (retryCount < 3) {
                    val nextRetryCount = retryCount + 1
                    URLog.info(TAG, "retry load ad config (${nextRetryCount}/3)")

                    delay(5000) // 等待5秒
                    fetchRemoteAdConfig(nextRetryCount) // 递归调用
                } else {
                    URLog.error(TAG, "retry load ad config failed max retry times")
                }
            }
        }
    }

    private fun statsRemoteAdConfig(isSuccess: Boolean, error : String, costTimeTS : Long) {
        TDAnalyticsManager.reportTrackEvent(
            EVENT_AD_CONFIG,
            JSONObject().apply {
                put("is_success", isSuccess)
                put("error", error)
                put("cost", costTimeTS)
            }
        )
    }

    private fun loadLocalAdConfig() {
        try {
            val adConfig = AdSPUtil.get().getString(AdSPUtil.KEY_LOCK_AD_CONFIG, "")
            val localAdConfig  = adConfig?.toLockAdConfig()
            if (localAdConfig != null) {
                lockAdConfig = localAdConfig
                URLog.info(TAG, "load ad config from local $adConfig")
            }
        } catch (e: Exception) {
            URLog.error(TAG, "load local config failed $e.message")
        }
    }

    private fun initAdRecordInfo() {
        val saveData = AdSPUtil.get().getString(AdSPUtil.KEY_LOCK_AD_RECORD, "")
        URLog.info(TAG, "load ad record: $saveData")
        if (TextUtils.isEmpty(saveData)) {
            lockAdRecord = LockAdRecord()
        } else {
            try {
                val saveJSON = JSONObject(saveData)
                lockAdRecord = LockAdRecord().parserJSON(saveJSON)
            } catch (e : Exception) {
                lockAdRecord = LockAdRecord()
            }
        }
        // 判断
        lockAdRecord?.let {
            val lastShowDate = DateUtils.toDateOnly(it.saveDateTs)
            val currentDate = DateUtils.toDateOnly(System.currentTimeMillis())
            if (lastShowDate != currentDate) {
                URLog.info(TAG, "reset ad record: $saveData")
                lockAdRecord?.resetRecord()
            }
        }
    }

    private fun recordLockAdShowInfo(adFormat : String) {
        lockAdRecord?.let {
            it.recordAdShow(adFormat)
            val saveInfo = it.toJSON().toString()
            URLog.info(TAG, "save ad record: $saveInfo")
            AdSPUtil.get().putString(AdSPUtil.KEY_LOCK_AD_RECORD, saveInfo)

            TDAnalyticsManager.reportTrackEvent(
                EVENT_AD_SHOW,
                JSONObject().apply {
                    put("ad_format", adFormat)
                }
            )
        }
    }

    fun setDebugMode() {
        isDebugMode = true
    }

    fun checkLockAdEnable(): Pair<Boolean, String> {
        if (isDebugMode) {
            URLog.info(TAG, "checkLockAdEnable isDebugMode")
            return Pair(true, "isDebugMode")
        }
        if (lockAdConfig == null) {
            return Pair(false, "shield no ad config")
        }
        if (lockAdConfig?.kg == true) {
            val userChannel = UserManager.instance.getUserAdNetwork()
            val hideChannels = lockAdConfig?.hideChannel
            if (hideChannels != null && hideChannels.contains(userChannel)) {
                val reason = "shield hide channel $userChannel"
                URLog.info(TAG, reason)
                return Pair(false, reason)
            }
            val enableChannels = lockAdConfig?.channel
            if (enableChannels != null && enableChannels.isNotEmpty() && !enableChannels.contains(userChannel)) {
                val reason = "shield channel $userChannel"
                URLog.info(TAG, reason)
                return Pair(false, reason)
            }
            val countryCode = deviceInfoManager?.countryCode ?: DeviceInfoCollector.instance.getLocaleInfo().country
            val hideCountryList = lockAdConfig?.hideCountry
            if (hideCountryList != null && hideCountryList.isNotEmpty() && hideCountryList.contains(countryCode)) {
                val reason = "shield hide country $countryCode"
                URLog.info(TAG, reason)
                return Pair(false, reason)
            }
            val countryList = lockAdConfig?.country
            if (countryList != null && countryList.isNotEmpty() && !countryList.contains(countryCode)) {
                val reason = "shield not start country $countryCode"
                URLog.info(TAG, reason)
                return Pair(false, reason)
            }
            if (!UserManager.instance.isEnableAoa()) {
                val reason = "shield ip disable ip=${deviceInfoManager?.ipAddress}"
                URLog.info(TAG, reason)
                return Pair(false, reason)
            }
            if (lockAdRecord != null && lockAdRecord!!.totalShowCount() > lockAdConfig!!.limit) {
                val reason = "shield show limit ${lockAdConfig!!.limit}"
                URLog.info(TAG, reason)
                return Pair(false, reason)
            }
            if (lockAdConfig?.adAdFormats != null) {
                return Pair(true, "ok")
            }
        }
        val reason = "shield remote kg is close"
        URLog.info(TAG, reason)
        return Pair(false, reason)
    }


    fun isNeedPCtx(): Boolean {
//        if (BuildConfig.DEBUG) {
//            return true
//        }
        val ctr = lockAdConfig?.ctr ?: 8
        val random = (0 until 100).random()
        URLog.i(TAG, "Bubble ctr: $random $ctr")
        return random <= ctr
    }

    fun launcherAdIntervalTs() : Long {
        if (BuildConfig.DEBUG) {
            return 1* 60 * 1000
        }
        if (lockAdConfig == null) {
            return 5 * 60 * 1000L
        }
        return lockAdConfig?.launcherShowInterval!!
    }

    /**
     * 预加载锁屏广告
     *
     */
    private fun preloadAd() {
        // 页面进入时
        val checkAdEnableResult = checkLockAdEnable()
        if (!checkAdEnableResult.first) {
            URLog.info(TAG, "preload shiled with ${checkAdEnableResult.second}")
            return
        }
        if (isAdLoading.get()) {
            URLog.info(TAG, "is ad loading")
            return
        }
        isAdLoading.set(true)
        nextAdFormat = lockAdConfig?.adAdFormats?.getRandomAdType() ?: AD_FORMAT_NONE
        when(nextAdFormat) {
            AD_FORMAT_NATIVE -> {
                lockAdLoader.loadNativeAd(appContext!!)
                URLog.info(TAG, "load native ad")
            }
            AD_FORMAT_BANNER -> {
                lockAdLoader.loadBannerAd(appContext!!, null)
                URLog.info(TAG, "load banner ad")
            }
            AD_FORMAT_INTERS -> {
                lockAdLoader.loadInterstitialAd(appContext!!)
                URLog.info(TAG, "load inters ad")
            }
            AD_FORMAT_REWORD -> {
                lockAdLoader.loadRewardVideoAd(appContext!!)
                URLog.info(TAG, "load reward ad")
            }
            AD_FORMAT_NONE -> {
                isAdLoading.set(false)
                URLog.info(TAG, "ignore load ad")
            }
        }
        if (isAdLoading.get()) {
            TDAnalyticsManager.reportTrackEvent(
                EVENT_AD_PRELOAD,
                JSONObject().apply {
                    put("ad_format", "none")
                }
            )
        }
    }

    fun isAdReady() : Boolean {
        return lockAdLoader.isBannerAdReady() || lockAdLoader.isNativeAdReady()
    }

    fun showAd(activity: Activity, container: ViewGroup) {
        URLog.info(TAG, "try show lock ad")
        val checkAdEnableResult = checkLockAdEnable()
        if (!checkAdEnableResult.first) {
            URLog.info(TAG, "showAd shiled with ${checkAdEnableResult.second}")
            TDAnalyticsManager.reportTrackEvent(
                EVENT_AD_SHILED,
                JSONObject().apply {
                    put("reason", checkAdEnableResult.second)
                }
            )
            return
        }
        activityWeakRef = WeakReference<Activity>(activity)

        when(nextAdFormat) {
            AD_FORMAT_NONE,AD_FORMAT_NATIVE -> {
                if (lockAdLoader.isNativeAdReady()) {
                    URLog.info(TAG, "try to show native ad")
                    lockAdLoader.showNativeAd(activity, container, -1)
                } else {
                    URLog.info(TAG, "try to show banner ad")
                    lockAdLoader.showBannerAd(activity, container)
                }
            }
            AD_FORMAT_BANNER -> {
                isShowAdWhenReady.set(false)
                if (lockAdLoader.isBannerAdReady()) {
                    URLog.info(TAG, "try to show banner ad")
                    lockAdLoader.showBannerAd(activity, container)
                } else if (lockAdLoader.isNativeAdReady()) {
                    URLog.info(TAG, "try to load and show native ad")
                    lockAdLoader.showNativeAd(activity, container, -1)
                } else {
                    URLog.info(TAG, "try to load and show banner ad")
                    lockAdLoader.loadBannerAd(activity, container)
                }

            }
            AD_FORMAT_INTERS -> {
                if (lockAdLoader.isInterstitialAdReady()) {
                    URLog.info(TAG, "try to show inters ad")
                    lockAdLoader.showInterstitialAd(activity)
                } else {
                    isShowAdWhenReady.set(true)
                    URLog.info(TAG, "try to load inters ad")
                    lockAdLoader.loadInterstitialAd(activity)
                }
            }
            AD_FORMAT_REWORD -> {
                if (lockAdLoader.isRewardVideoAdReady()) {
                    URLog.info(TAG, "try to show reward ad")
                    lockAdLoader.showRewardVideoAd(activity, AdShowScene.LOCKER)
                } else {
                    isShowAdWhenReady.set(true)
                    URLog.info(TAG, "try to load reward ad")
                    lockAdLoader.loadRewardVideoAd(activity)
                }
            }
            AD_FORMAT_NONE -> {
                isShowAdWhenReady.set(false)
                URLog.info(TAG, "none, ignore  show ad")
            }
        }
    }

    private fun preloadNextAdInternal() {
        URLog.info(TAG, "preload next ad")
        if (retryLoadCount >= RETRY_LOAD_COUNT_MAX) {
            URLog.info(TAG, "preload next ad limit count $RETRY_LOAD_COUNT_MAX")
            return
        }
        if (isAdLoading.get()) {
            URLog.info(TAG, "has preload next ad")
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            isShowAdWhenReady.set(false)
            isAdLoading.set(false)
            nextAdFormat = AD_FORMAT_NONE
            preloadAd()
        }
    }

    /**
     * 当lock取消时 取消广告展示预加载下一个
     * 当请求失败预加载下一个
     */
    fun preloadNextAd() {
        retryLoadCount = 0
        preloadNextAdInternal()
    }

    override fun onRewardedAdLoaded(adLoader: BaseAdLoader) {
        super.onRewardedAdLoaded(adLoader)
        URLog.info(TAG, "onRewardedAdLoaded isShowAdWhenReady= ${isShowAdWhenReady.get()}")
        if (nextAdFormat == AD_FORMAT_REWORD && isShowAdWhenReady.get()) {
            activityWeakRef?.get()?.let {
                lockAdLoader.showRewardVideoAd(it, AdShowScene.LOCKER)
            }
            isAdLoading.set(false)
            URLog.info(TAG, "show rewardAd")
        }
        TDAnalyticsManager.reportTrackEvent(
            EVENT_AD_LOADED,
            JSONObject().apply {
                put("ad_format", AD_FORMAT_REWORD)
            }
        )
    }

    override fun onRewardedAdFailed(adLoader: BaseAdLoader, error: String) {
        super.onRewardedAdFailed(adLoader, error)
        URLog.info(TAG, "onRewardedAdFailed")
        if (nextAdFormat == AD_FORMAT_REWORD) {
            URLog.info(TAG, "on reward load Failed, preloadNext")
            retryLoadCount = retryLoadCount + 1
            preloadNextAdInternal()
        }
    }

    override fun onRewardedAdPlayStart(adLoader: BaseAdLoader) {
        super.onRewardedAdPlayStart(adLoader)
        URLog.info(TAG, "onRewardedAdPlayStart")
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        recordLockAdShowInfo(AD_FORMAT_REWORD)
    }

    override fun onInterstitialAdLoaded(adLoader: BaseAdLoader) {
        super.onInterstitialAdLoaded(adLoader)
        URLog.info(TAG, "onInterstitialAdLoaded isShowAdWhenReady= ${isShowAdWhenReady.get()}")
        if (nextAdFormat == AD_FORMAT_INTERS && isShowAdWhenReady.get()) {
            activityWeakRef?.get()?.let {
                lockAdLoader.showInterstitialAd(it)
                URLog.info(TAG, "showInterstitialAd")
            }
        }
        TDAnalyticsManager.reportTrackEvent(
            EVENT_AD_LOADED,
            JSONObject().apply {
                put("ad_format", AD_FORMAT_INTERS)
            }
        )
    }

    override fun onInterstitialAdLoadFail(adLoader: BaseAdLoader, error: String) {
        super.onInterstitialAdLoadFail(adLoader, error)
        if (nextAdFormat == AD_FORMAT_INTERS) {
            URLog.info(TAG, "load interstitial failed")
            retryLoadCount = retryLoadCount + 1
            preloadNextAdInternal()
        }
    }

    override fun onInterstitialAdShow(adLoader: BaseAdLoader) {
        super.onInterstitialAdShow(adLoader)
        URLog.info(TAG, "on interstitial show")
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        recordLockAdShowInfo(AD_FORMAT_INTERS)
    }

    override fun onNativeAdLoaded(adLoader: BaseAdLoader) {
        super.onNativeAdLoaded(adLoader)
        URLog.info(TAG, "onNativeAdLoaded")
        adListener?.onAdLoaded()
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        TDAnalyticsManager.reportTrackEvent(
            EVENT_AD_LOADED,
            JSONObject().apply {
                put("ad_format", AD_FORMAT_NATIVE)
            }
        )
    }

    override fun onNativeAdLoadFail(adLoader: BaseAdLoader, error: String) {
        super.onNativeAdLoadFail(adLoader, error)
        URLog.info(TAG, "onNativeAdLoadFail $error")
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        if (nextAdFormat == AD_FORMAT_NATIVE) {
            URLog.info(TAG, "load native failed preload Next")
            retryLoadCount = retryLoadCount + 1
            preloadNextAdInternal()
        }
    }

    override fun onNativeAdShow(adLoader: BaseAdLoader) {
        super.onNativeAdShow(adLoader)
        URLog.info(TAG, "onNativeAdShow")
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        recordLockAdShowInfo(AD_FORMAT_NATIVE)
    }

    override fun onNativeAdClicked(adLoader: BaseAdLoader) {
        super.onNativeAdClicked(adLoader)
        URLog.info(TAG, "onNativeAdClicked")
        adListener?.onAdClicked()
    }

    override fun onBannerAdLoaded(adLoader: BaseAdLoader) {
        super.onBannerAdLoaded(adLoader)
        URLog.info(TAG, "onBannerAdLoaded")
        adListener?.onAdLoaded()
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        recordLockAdShowInfo(AD_FORMAT_BANNER)
        TDAnalyticsManager.reportTrackEvent(
            EVENT_AD_LOADED,
            JSONObject().apply {
                put("ad_format", AD_FORMAT_BANNER)
            }
        )
    }


    override fun onBannerAdClicked(adLoader: BaseAdLoader) {
        super.onBannerAdClicked(adLoader)
        URLog.info(TAG, "onBannerAdClicked")
        adListener?.onAdClicked()
    }

    override fun onBannerAdShow(adLoader: BaseAdLoader) {
        super.onBannerAdShow(adLoader)
        URLog.info(TAG, "onBannerAdShow")
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        recordLockAdShowInfo(AD_FORMAT_BANNER)
    }

    override fun onBannerAdLoadFail(adLoader: BaseAdLoader, error: String) {
        super.onBannerAdLoadFail(adLoader, error)
        URLog.info(TAG, "onBannerAdLoadFail $error")
        isAdLoading.set(false)
        isShowAdWhenReady.set(false)
        if (nextAdFormat == AD_FORMAT_BANNER) {
            retryLoadCount = retryLoadCount + 1
            preloadNextAdInternal()
        }
    }

    override fun onUserInfoChanged(userInfo: UserInfo) {
        //
    }

    override fun onUserAdNetworkChanged(adNetwork: String) {
        URLog.info(TAG, "onUserAdNetworkChanged  adNetwork $adNetwork")
        if (!TextUtils.isEmpty(adNetwork) && adNetwork != "organic") {
            preloadNextAd()
        }
    }

    fun setLockerAdLoadListener(cb : LockerAdLoadListener) {
        adListener = cb
    }

    fun removeLockerAdLoadListener() {
        adListener = null
    }

    companion object {
        private const val TAG = "LockAdManager"

        const val EVENT_AD_SHOW ="lock_ad_show"
        const val EVENT_AD_CONFIG ="lock_ad_config_request"
        const val EVENT_AD_SHILED ="lock_ad_shiled"
        const val EVENT_AD_PRELOAD ="lock_ad_request"

        const val EVENT_AD_LOADED ="lock_ad_loaded"

        const val RETRY_LOAD_COUNT_MAX = 3

        // 使用标准的Kotlin懒加载单例模式
        val instance: LockAdManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            LockAdManager()
        }

        interface LockerAdLoadListener {
            fun onAdLoaded()

            fun onAdClicked()
        }
    }
}
