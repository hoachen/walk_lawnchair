package com.ur.apps.walk

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.ur.apps.absui.BaseUrFullScreenActivity
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.analysis.tenjin.TenjinManager
import com.ur.apps.splash.GoogleMobileAdsConsentManager
import com.ur.apps.utils.URLog
import com.android.launcher3.databinding.ActivitySplashBinding
import com.ur.apps.walk.utils.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : BaseUrFullScreenActivity() {

    private val TAG = "SplashActivity"

    private lateinit var binding: ActivitySplashBinding

    // 延迟跳转的时间（毫秒）
    private val SPLASH_AD_LOAD_TOIMEOUT: Long = 5000

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
//    private val gatherConsentFinished = AtomicBoolean(false)
    private var secondsRemaining: Long = 0L

    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 启动动画效果
        startAnimations()
        AdLoaderManager.loadInterstitialAd(this)
        AdLoaderManager.loadRewardVideoAd(this)

        // Create a timer so the SplashActivity will be displayed for a fixed amount of time.

//        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
//        googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
//            if (consentError != null) {
//                // Consent not obtained in current session.
//                URLog.w(TAG, String.format("%s: %s", consentError.errorCode, consentError.message))
//            }
//            gatherConsentFinished.set(true)
//
//            if (googleMobileAdsConsentManager.canRequestAds) {
//                initializeMobileAdsSdk()
//            }
//            if (secondsRemaining <= 0) {
//                URLog.i(TAG, "AppOpenAd secondsRemaining" )
//                navigateToMainActivity()
//            }
//        }
//        // This sample attempts to load ads using consent obtained in the previous session.
//        if (googleMobileAdsConsentManager.canRequestAds) {
//        }
        if ((application as WalkApplication).isAppOpenAdAvailable()) {
            showAdIfAvailable()
        } else {
            createTimer()
            initializeMobileAdsSdk()
        }
        // 提前初始化Tenjin
        TenjinManager.notifyAdDisplayed()
    }

    override fun setupWindow(window: Window?) {
        window?.setBackgroundDrawableResource(R.drawable.app_window_background)
    }

    private fun startAnimations() {
        // 创建一组动画
        val animatorSet = AnimatorSet()

        // Logo缩放和旋转动画
        val scaleX = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 0.2f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 0.2f, 1.0f)
        val rotation = ObjectAnimator.ofFloat(binding.ivLogo, View.ROTATION, 0f, 360f)

        // 应用名称和口号的渐变动画
        val appNameAlpha = ObjectAnimator.ofFloat(binding.tvAppName, View.ALPHA, 0f, 1f)
        val sloganAlpha = ObjectAnimator.ofFloat(binding.tvSlogan, View.ALPHA, 0f, 1f)

        // 设置动画时长和插值器
        scaleX.duration = 1000
        scaleY.duration = 1000
        rotation.duration = 1000
        appNameAlpha.duration = 800
        sloganAlpha.duration = 800

        rotation.interpolator = AnticipateOvershootInterpolator()
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()

        // 组合并启动动画
        animatorSet.playTogether(scaleX, scaleY, rotation)
        animatorSet.start()

        // 延迟启动文字动画
        mainHandler.postDelayed({
            appNameAlpha.start()
        }, 700)

        mainHandler.postDelayed({
            sloganAlpha.start()
        }, 1000)
    }


    /**
     * Create the countdown timer, which counts down to zero and show the app open ad.
     *
     * @param time the number of milliseconds that the timer counts down from
     */
    private fun createTimer() {
        val countDownTimer: CountDownTimer =
            object : CountDownTimer(SPLASH_AD_LOAD_TOIMEOUT, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    URLog.i(TAG, "AppOpenAd countDownTimer onTick $millisUntilFinished " )
                    secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
                }

                override fun onFinish() {
                    secondsRemaining = 0
                    showAdIfAvailable()
                }
            }
        URLog.i(TAG, "AppOpenAd start countDownTimer" )
        countDownTimer.start()
    }

    private fun showAdIfAvailable() {
        AdLoaderManager.loadNativeAd(this)
        (application as WalkApplication).showAdIfAvailable(
            this@SplashActivity,
            object : WalkApplication.OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    // Check if the consent form is currently on screen before moving to the main
                    // activity.
                    URLog.i(TAG, "AppOpenAd onShowAdComplete" )
                    navigateToMainActivity()

                }
            }
        )
    }

    private fun initializeMobileAdsSdk() {
//        if (isMobileAdsInitializeCalled.getAndSet(true)) {
//            return
//        }
        // Set your test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
//                .setTestDeviceIds(listOf(MyApplication.TEST_DEVICE_HASHED_ID))
                .build()
        )
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@SplashActivity) {}
            runOnUiThread {
                // Load an ad on the main thread.
                (application as WalkApplication).loadAd(this@SplashActivity)
            }
        }
    }

    private fun navigateToMainActivity() {
        // 跳转到权限请求页面，而不是直接跳转到主页面
        val intent = Intent(this, PermissionRequestActivity::class.java)
        startActivity(intent)
        // 添加过渡动画
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        // 关闭启动页，避免返回到此页面
        finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 配置变化时重新应用语言设置
        val language = LocaleHelper.getLanguage(this)
        LocaleHelper.applyLanguage(this, language)
    }

    override fun attachBaseContext(newBase: Context) {
        // 获取当前语言设置并应用
        val language = LocaleHelper.getLanguage(newBase)
        val context = LocaleHelper.applyLanguage(newBase, language)
        super.attachBaseContext(context)
    }
}
