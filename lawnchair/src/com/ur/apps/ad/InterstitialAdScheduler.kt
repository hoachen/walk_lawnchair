package com.ur.apps.ad

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.ur.apps.utils.URLog
import java.lang.ref.WeakReference

class InterstitialAdScheduler private constructor(private val application: Application) {

    // region 核心逻辑组件
    private val handler = Handler(Looper.getMainLooper())
    private var currentActivityRef: WeakReference<Activity>? = null
    private var checkRunnable: Runnable? = null
    private var enableScheduler : Boolean = true


    // 状态标记
    private var isIncentiveShowing = false   // 激励广告展示状态
    private var isInterstitialShowing = false // 插屏广告展示状态
    // endregion

    init {
        setupLifecycleListener()
        AdLoaderManager.addRewardAdListener(object : RewardAdListener {
            override fun onRewardedAdPlayStart(adLoader: BaseAdLoader) {
                handleRewardVideoAdShow()
            }

            override fun onRewardedAdClosed(adLoader: BaseAdLoader) {
                handleRewardVideoAdDismiss()
            }
        })
        AdLoaderManager.addInterstitialAdListener(object : InterstitialAdListener {
            override fun onInterstitialAdClose(adLoader: BaseAdLoader) {
                handleInterstitialAdDismissed()
            }
        })
    }


    private fun isBlackListActivity(activity: Activity) : Boolean {
        return BLACK_LIST.contains(activity.localClassName)
    }

    // region 生命周期管理
    private fun setupLifecycleListener() {

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (!isBlackListActivity(activity) && isTargetActivity(activity)) {
                    Log.i(TAG, "onActivityResumed $activity")
                    handleActivityResume(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (!isBlackListActivity(activity) && isTargetActivity(activity)) {
                    Log.i(TAG, "onActivityPaused $activity")
                    handleActivityPause(activity)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (!isBlackListActivity(activity) && isTargetActivity(activity)) {
                    Log.i(TAG, "onActivityStopped $activity")
                    handleActivityStop(activity)
                }
            }
        })
    }


    // region 广告状态控制
    private fun handleRewardVideoAdShow() {
        Log.i(TAG, "handleRewardVideoAdShow")
        isIncentiveShowing = true
        stopTimer()
    }

    private fun handleRewardVideoAdDismiss() {
        Log.i(TAG, "handleRewardVideoAdDismiss")
        isIncentiveShowing = false
        startTimer()
    }

    private fun handleInterstitialAdDismissed() {
        Log.i(TAG, "handleInterstitialAdDismissed")
        isInterstitialShowing = false
        startTimer()
    }

    // region 定时器控制
    private fun startTimer() {
        if (isIncentiveShowing || isInterstitialShowing) {
            Log.i(TAG, "startTimer but $isIncentiveShowing or $isInterstitialShowing")
            return
        }

        stopTimer()
        if (!enableScheduler) {
            Log.i(TAG, "startTimer but enableScheduler = $enableScheduler")
            return
        }
        checkRunnable = Runnable {
            Log.i(TAG, "run timer")
            currentActivityRef?.get()?.let { activity ->
                if (!activity.isFinishing && !isIncentiveShowing) {
                    showInterstitialAd(activity)
                }
            }
            checkRunnable?.let {
                handler.postDelayed(it, INTERVAL)
            }
        }
        Log.i(TAG, "startTimer")
        handler.postDelayed(checkRunnable!!, INTERVAL)
    }

    private fun stopTimer() {
        Log.i(TAG, "stopTimer")
        checkRunnable?.let {
            handler.removeCallbacks(it)
            checkRunnable = null
        }
    }
    // endregion

    // region 触摸事件处理
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(activity: Activity) {
        activity.window.decorView.findViewById<View>(android.R.id.content)?.setOnTouchListener { _, event ->
            Log.i(TAG, "setupTouchListener onUser Touch to reset")
            resetTimer()
            false
        }
    }

    private fun resetTimer() {
        stopTimer()
        startTimer()
    }
    // endregion

    // region Activity生命周期处理
    private fun handleActivityResume(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        URLog.info(TAG, "handleActivityResume ")
        setupTouchListener(activity)
        if (!isInterstitialShowing) {
            startTimer()
        }
    }

    private fun handleActivityPause(activity: Activity) {
        activity.window.decorView.setOnTouchListener(null)
        if (!isInterstitialShowing) {
            stopTimer()
        }
    }

    private fun handleActivityStop(activity: Activity) {
        if (activity == currentActivityRef?.get()) {
            currentActivityRef?.clear()
        }
    }
    // endregion

    // region 广告展示逻辑（需接入具体广告SDK）
    private fun showInterstitialAd(activity: Activity) {
        URLog.info(TAG, "showInterstitialAd ")
        isInterstitialShowing = true
        stopTimer()
        AdLoaderManager.showInterstitialAd(activity, AdShowScene.NO_REWARD)
    }

    private fun isTargetActivity(activity: Activity) : Boolean {
        return activity.packageName.contains(ACTIVITY_PKG_NAME) ||
            activity.packageName.contains(ACTIVITY_LAUNCHER3) ||
            activity.packageName.contains(ACTIVITY_APP_LAWNCHAIR)
    }


    // endregion

    companion object {
        private const val INTERVAL = 30_000L // 30秒间隔
        @Volatile private var instance: InterstitialAdScheduler? = null

        private const val ACTIVITY_PKG_NAME = "com.ur.apps.walk"

        private const val ACTIVITY_APP_LAWNCHAIR = "app.lawnchair"

        private const val ACTIVITY_LAUNCHER3 = "com.android.launcher3"

        private val BLACK_LIST = listOf<String>(
            "PermissionRequestActivity",
            "LockScreenActivity"
        )

        private const val  TAG  = "InterstitialAdScheduler"

        fun initialize(application: Application) {
            instance = InterstitialAdScheduler(application)
        }


        fun disable() {
            instance?.enableScheduler = false
            instance?.stopTimer()
        }

        fun enable() {
            instance?.enableScheduler = true
        }

        fun get(): InterstitialAdScheduler {
            return instance ?: throw IllegalStateException("AdScheduler not initialized!")
        }
    }
}
