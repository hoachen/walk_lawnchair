package com.ur.apps.walk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.sg.UserManager
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.ad.admob.AdmobAdLoader
import com.ur.apps.ad.admob.BubbleAdmobAdLoader
import com.ur.apps.analysis.shuzhi.SZSdkImpl
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.analysis.tenjin.TenjinManager
import com.ur.apps.analysis.utils.DeviceInfoCollector
import com.ur.apps.bubble.BubbleManager
import com.ur.apps.lock.LockAdManager
import com.ur.apps.utils.URLog
import com.ur.apps.splash.GoogleMobileAdsConsentManager
import com.ur.apps.walk.model.LeaderboardRepository
import com.ur.apps.walk.step.bean.ExerciseStats
import com.ur.apps.walk.step.callback.StepCountChangeCallBack
import com.ur.apps.walk.step.manager.StepManager
import com.ur.apps.walk.utils.LocaleHelper
import com.ur.apps.walk.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Date


class WalkApplication : Application(), StepCountChangeCallBack, DefaultLifecycleObserver {
    private val TAG = "WalkApplication"
    private lateinit var stepManager: StepManager

    // 使用弱引用持有当前活动的Activity，避免内存泄漏
    private var currentActivity: WeakReference<Activity>? = null

    private lateinit var appOpenAdManager: AppOpenAdManager

    // 使用应用级别的协程作用域
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    // 懒加载LeaderboardRepository
    private val leaderboardRepository by lazy { LeaderboardRepository(this) }

    // 用于保存活跃的Activity列表
    private val activeActivities = mutableListOf<WeakReference<Activity>>()

    // 应用前台状态
    private var isAppInForeground = false

    // 前台状态监听器列表
    private val foregroundStateListeners = mutableListOf<ForegroundStateListener>()

    companion object {
        private lateinit var instance: WalkApplication
        private val enableAct = listOf(MainActivityRecycler::class.java)

        fun getContext(): Context {
            return instance
        }
    }

    /**
     * 应用前台状态变化监听接口
     */
    interface ForegroundStateListener {
        /**
         * 当应用进入前台时调用
         */
        fun onEnterForeground()

        /**
         * 当应用进入后台时调用
         */
        fun onEnterBackground()
    }

    /**
     * 添加前台状态监听器
     * @param listener 要添加的监听器
     */
    fun addForegroundStateListener(listener: ForegroundStateListener) {
        foregroundStateListeners.add(listener)
    }

    /**
     * 移除前台状态监听器
     * @param listener 要移除的监听器
     */
    fun removeForegroundStateListener(listener: ForegroundStateListener) {
        foregroundStateListeners.remove(listener)
    }

    /**
     * 检查应用当前是否在前台
     * @return 应用是否在前台
     */
    fun isAppInForeground(): Boolean {
        return isAppInForeground
    }

    /**
     * 获取当前活动的Activity
     * @return 当前活动的Activity，如果没有则返回null
     */
    fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }

    /**
     * 根据类名查找正在运行的Activity实例
     * @param className 目标Activity的类名（例如："MainActivity"）
     * @return 找到的Activity实例，如果没找到则返回null
     */
    fun findActivityByName(className: String): Activity? {
        // 首先检查当前活动的Activity
        currentActivity?.get()?.let {
            if (it.javaClass.simpleName == className) {
                return it
            }
        }

        // 在所有活跃的Activity中查找
        for (ref in activeActivities) {
            ref.get()?.let {
                if (it.javaClass.simpleName == className) {
                    return it
                }
            }
        }

        return null
    }

    override fun attachBaseContext(base: Context) {
        // 获取当前语言设置并应用
        val language = LocaleHelper.getLanguage(base)
        super.attachBaseContext(LocaleHelper.applyLanguage(base, language))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 配置变化时重新应用语言设置
        val language = LocaleHelper.getLanguage(this)
        LocaleHelper.applyLanguage(this, language)
    }

    override fun onCreate() {
        super<Application>.onCreate()
        URLog.d(TAG, "Application onCreate 开始")
        instance = this
        ThemeManager.getInstance().init(this)

//        CrashReport.initCrashReport(applicationContext, "1d16093eb7", false);

        // 注册Activity生命周期回调，用于跟踪当前活动的Activity
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        registerActivityLifecycleCallbacks(BubbleManager(BubbleAdmobAdLoader()))
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        // 初始化主题设置
        initializeTheme()
        DeviceInfoCollector.instance.initContext(this)
        UserManager.instance.initContext(this)
        TenjinManager.initTenJinSDK(this)
        AdLoaderManager.initialize(this)
        TDAnalyticsManager.initialize(this)
        LockAdManager.instance.initContext(this)
        // 初始化排行榜数据库
        URLog.d(TAG, "开始初始化排行榜数据库")
        initializeDatabase()
        URLog.d(TAG, "数据库初始化任务已提交")

        URLog.d(TAG, "Application onCreate 完成")
        // 配置Glide
        Glide.init(this, GlideBuilder().apply {
            setDefaultRequestOptions(
                RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565) // 使用RGB_565格式，减少内存占用
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存所有版本的图片
                    .skipMemoryCache(false) // 使用内存缓存
            )
        })

        // 延迟初始化 StepManager，但不立即启动服务
        // 服务将在用户与应用交互后启动
        stepManager = StepManager.getInstance(this)
        stepManager.registerCallback(this)

//        SAdSDK.instance.onCreate(this)
        SZSdkImpl.init(this)
    }


    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.get()?.let {
            // Show the ad (if available) when the app moves to foreground.
            if ((it !is LockScreenActivity) && (it !is PermissionRequestActivity) && isWalkActivity(it)) {
                URLog.d(TAG, "onApplication AppOpenAd showAdIfAvailable ")
                appOpenAdManager.showAdIfAvailable(it)
            }
        }
    }

    private fun isWalkActivity(activity: Activity) : Boolean {
        return activity.javaClass.name.startsWith("com.ur.apps")
    }


    // Activity生命周期回调
    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        // 用于跟踪是否有Activity处于已启动但未停止的状态
        private var startedActivities = 0

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            URLog.d(TAG, "Activity创建: ${activity.javaClass.simpleName}")
            // 添加到活跃Activity列表
            val weakActivity = WeakReference(activity)
            activeActivities.add(weakActivity)
        }

        override fun onActivityStarted(activity: Activity) {
            URLog.d(TAG, "Activity启动: ${activity.javaClass.simpleName}")
//            if (appOpenAdManager.isShowingAd) {
//                URLog.d(TAG, "onActivityStarted isShowning OpenAd")
//                return
//            }
            // 如果这是第一个Started的Activity，说明应用从后台进入前台
            if (startedActivities == 0 && !isAppInForeground) {
                URLog.d(TAG, "应用进入前台")
                isAppInForeground = true
                notifyForegroundStateChanged(true)
            }
            startedActivities++
        }

        override fun onActivityResumed(activity: Activity) {
            // 当Activity进入前台时，更新当前活动的Activity引用
            URLog.d(TAG, "Activity恢复（前台）: ${activity.javaClass.simpleName}")
            currentActivity = WeakReference(activity)

        }

        override fun onActivityPaused(activity: Activity) {
            URLog.d(TAG, "Activity暂停: ${activity.javaClass.simpleName}")
        }

        override fun onActivityStopped(activity: Activity) {
            URLog.d(TAG, "Activity停止: ${activity.javaClass.simpleName}")

            // 递减已启动的Activity计数
            startedActivities--

            // 如果没有已启动的Activity，说明应用进入后台
            if (startedActivities == 0 && isAppInForeground) {
                URLog.d(TAG, "应用进入后台")
                isAppInForeground = false
                notifyForegroundStateChanged(false)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // 不需要在这里做任何处理
        }

        override fun onActivityDestroyed(activity: Activity) {
            URLog.d(TAG, "Activity销毁: ${activity.javaClass.simpleName}")

            // 从活跃Activity列表中移除
            activeActivities.removeAll { weakRef ->
                val act = weakRef.get()
                act == null || act == activity
            }

            // 如果当前跟踪的Activity被销毁，且是我们正在跟踪的Activity，则清除引用
            if (currentActivity?.get() == activity) {
                currentActivity = null
            }
        }
    }

    private fun initializeDatabase() {
        URLog.d(TAG, "启动数据库初始化协程")
        // 使用阻塞方式初始化数据库，确保应用启动时数据库已准备好
        applicationScope.launch {
            try {
                URLog.d(TAG, "开始执行数据库初始化")
                // 清除旧数据库文件，确保使用新的数据库结构
                val dbFile = getDatabasePath("leaderboard_database")
                if (dbFile.exists()) {
                    URLog.d(TAG, "删除旧数据库文件")
                    dbFile.delete()
                }

                leaderboardRepository.initializeDataIfNeeded()
                URLog.d(TAG, "数据库初始化完成")
            } catch (e: Exception) {
                URLog.e(TAG, "数据库初始化出错", e)
                e.printStackTrace()
            }
        }
    }

    // 添加主题初始化方法
    private fun initializeTheme() {
        URLog.d(TAG, "初始化应用主题")
        // 预加载主题设置，确保在Activity创建前主题已准备好
        val themeManager = ThemeManager.getInstance()
        val currentTheme = themeManager.getCurrentTheme()

        // 记录当前主题信息
        URLog.d(TAG, "当前主题: ID=${currentTheme.name}")

        // 注意：这里不需要做其他操作，因为BaseActivity会在创建时应用主题
    }


    override fun onLowMemory() {
        super.onLowMemory()
        // 清理内存
        Glide.get(this).clearMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 根据内存级别清理
        Glide.get(this).trimMemory(level)
    }

    override fun onStepChange(stepCount: ExerciseStats?) {

    }

    override fun onStepReachPeriod(hundred_level: Int) {
        // 获取Application实例

    }

    /**
     * 通知所有监听器前台状态变化
     * @param isForeground 应用是否进入前台
     */
    private fun notifyForegroundStateChanged(isForeground: Boolean) {
        foregroundStateListeners.forEach {
            if (isForeground) {
                it.onEnterForeground()
            } else {
                it.onEnterBackground()
                // 应用退出后台时打开StepService
                URLog.i(TAG, "Bubble App onEnterBackground")
                startStepService()
            }
        }
    }

    /**
     * 在适当时机启动计步服务
     * 应该在用户与应用交互后调用，例如在主界面可见时
     */
    fun startStepService() {
        if (::stepManager.isInitialized) {
            // 启动计步服务
            applicationScope.launch {
                stepManager.startStepService()
                URLog.d(TAG, "计步服务启动请求已发送")
            }
        }
    }

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    /**
     * Load an app open ad.
     *
     * @param activity the activity that shows the app open ad
     */
    fun loadAd(activity: Activity) {
        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
            appOpenAdManager.loadAd(activity)
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }


    fun isAppOpenAdAvailable() : Boolean = appOpenAdManager.isAdAvailable()

    /** Inner class that loads and shows app open ads. */
    // [START manager_class]
    private inner class AppOpenAdManager {

        private var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager =
            GoogleMobileAdsConsentManager.getInstance(applicationContext)
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0

        // [END manager_class]

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                URLog.d(TAG, "AppOpenAd isLoadingAd || isAdAvailable()")
                return
            }
            isLoadingAd = true
            // [START load_ad]
            AppOpenAd.load(
                context,
                AdmobAdLoader.ADMOB_OPENAD_UNIT_ID,
                AdRequest.Builder().build(),

                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        URLog.d(TAG, "AppOpenAd ad loaded.")
                        appOpenAd = ad
                        appOpenAd?.setOnPaidEventListener {
                            val adValue = it
                            Log.i(TAG, "Admob AppOpenAd paid $it")
                            val adSourceName = appOpenAd?.responseInfo?.loadedAdapterResponseInfo?.adSourceName
                            val adSourceId = appOpenAd?.responseInfo?.loadedAdapterResponseInfo?.adSourceId
                            val revenue: Double = it.valueMicros * 1.0/ 1000_000 // 价值，以微单位表示 (例如 5000 代表 0.005 USD)
                            TDAnalyticsManager.reportAdRevenue(
                                it.currencyCode,
                                revenue,
                                adSourceName ?: "",
                                AdmobAdLoader.ADMOB_OPENAD_UNIT_ID,
                                "admob",
                                "AppOpenAd",
                                adSourceId ?: ""
                            )
                            try {
                                TenjinManager.getTenjinInstance()?.eventAdImpressionAdMob(adValue,
                                    appOpenAd)
                            } catch (_ : Exception) {
                            }
                        }
                        isLoadingAd = false
                        loadTime = Date().time
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        URLog.d(TAG, "AppOpenAd failed to load with error: " + loadAdError.message)
                        isLoadingAd = false
                    }
                },
            )
        }

        // [START ad_expiration]
        /** Check if ad was loaded more than n hours ago. */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        /** Check if ad exists and can be shown. */
        fun isAdAvailable(): Boolean {
            // For time interval details, see: https://support.google.com/admob/answer/9341964
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        // [END ad_expiration]

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                    }
                },
            )
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                URLog.d(TAG, "AppOpenAd The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback.
            if (!isAdAvailable()) {
                URLog.d(TAG, "AppOpenAd The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
//                if (googleMobileAdsConsentManager.canRequestAds) {
//                }
                return
            }

            URLog.d(TAG, "AppOpenAd Will show ad.")
            appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    /** Called when full screen content is dismissed. */
                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        appOpenAd = null
                        URLog.d(TAG, "AppOpenAd onAdDismissedFullScreenContent.")
                        isShowingAd = false
                        onShowAdCompleteListener.onShowAdComplete()
                        loadAd(activity)
//                        if (googleMobileAdsConsentManager.canRequestAds) {
//                        }
                    }

                    /** Called when fullscreen content failed to show. */
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        isShowingAd = false
                        URLog.d(TAG, "AppOpenAd onAdFailedToShowFullScreenContent: " + adError.message)
                        onShowAdCompleteListener.onShowAdComplete()
                        loadAd(activity)
                        if (googleMobileAdsConsentManager.canRequestAds) {
                        }
                    }

                    /** Called when fullscreen content is shown. */
                    override fun onAdShowedFullScreenContent() {
                        URLog.d(TAG, "AppOpenAd onAdShowedFullScreenContent.")
                    }
                }
            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }

}
