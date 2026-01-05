package com.ur.apps.ad

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.ur.apps.ad.admob.HomeAdmobAdLoader
import com.ur.apps.ad.topon.TopOnAdLoader
import com.ur.apps.walk.BuildConfig

object AdLoaderManager : BaseAdLoader() {

    private const val TAG = "AdLoaderManager"
    @SuppressLint("StaticFieldLeak")
    private val topOnAdLoader: TopOnAdLoader = TopOnAdLoader()
    private val homeAdmobAdLoader: HomeAdmobAdLoader = HomeAdmobAdLoader()

    override fun initialize(application: Application) {
        topOnAdLoader.initialize(application)
//        applovinAdLoader.initialize(context)
        InterstitialAdScheduler.initialize(application)
        // 如果不想一直弹插屏广告，调此方法
        if (BuildConfig.DEBUG) {
            InterstitialAdScheduler.disable()
        }
    }

    override fun loadRewardVideoAd(context: Context) {
        topOnAdLoader.loadRewardVideoAd(context)
    }

    override fun isRewardVideoAdReady(): Boolean {
        return topOnAdLoader.isRewardVideoAdReady()
    }

    override fun showRewardVideoAd(activity: Activity, adShowScene: AdShowScene) {
        topOnAdLoader.showRewardVideoAd(activity, adShowScene)
    }

    override fun addRewardAdListener(rewardAdListener: RewardAdListener) {
        Log.i(TAG, "resetRewardAdListener $rewardAdListener")
        topOnAdLoader.addRewardAdListener(rewardAdListener)
    }

    override fun removeRewardAdListener(rewardAdListener: RewardAdListener) {
        Log.i(TAG, "resetRewardAdListener $rewardAdListener")
        topOnAdLoader.removeRewardAdListener(rewardAdListener)
    }


    override fun loadInterstitialAd(context: Context) {
        topOnAdLoader.loadInterstitialAd(context)
    }

    override fun isInterstitialAdReady(): Boolean {
        return topOnAdLoader.isInterstitialAdReady()
    }

    override fun showInterstitialAd(activity: Activity, adShowScene: AdShowScene) {
        topOnAdLoader.showInterstitialAd(activity, adShowScene)
    }

    override fun loadSplashAd(
        context: Context,
        fetchAdTimeout: Int
    ) {
        topOnAdLoader.loadSplashAd(context, fetchAdTimeout)
    }

    override fun isSplashAdReady(): Boolean {
        return topOnAdLoader.isSplashAdReady()
    }

    override fun addSplashAdListener(splashAdListener: SplashAdListener) {
        topOnAdLoader.addSplashAdListener(splashAdListener)
    }

    override fun removeSplashAdListener(splashAdListener: SplashAdListener) {
        topOnAdLoader.removeSplashAdListener(splashAdListener)
    }


    override fun addInterstitialAdListener(interstitialAdListener: InterstitialAdListener) {
        topOnAdLoader.addInterstitialAdListener(interstitialAdListener)
    }

    override fun removeInterstitialAdListener(interstitialAdListener: InterstitialAdListener) {
        topOnAdLoader.removeInterstitialAdListener(interstitialAdListener)
    }

    override fun addNativeAdListener(nativeAdListener: NativeAdListener) {
        homeAdmobAdLoader.addNativeAdListener(nativeAdListener)
    }

    override fun removeNativeAdListener(nativeAdListener: NativeAdListener) {
        homeAdmobAdLoader.removeNativeAdListener(nativeAdListener)
    }

    override fun showSplashAd(activity: Activity, container: ViewGroup) {
        topOnAdLoader.showSplashAd(activity, container)
    }

    override fun isBannerAdReady(): Boolean {
        return homeAdmobAdLoader.isBannerAdReady()
    }

    override fun loadBannerAd(context: Context, container: ViewGroup?) {
        homeAdmobAdLoader.loadBannerAd(context, container)
    }

    override fun showBannerAd(context: Context, container: ViewGroup) {
        homeAdmobAdLoader.showBannerAd(context, container)
    }

    override fun isNativeAdReady(): Boolean {
        return homeAdmobAdLoader.isNativeAdReady()
    }

    override fun loadNativeAd(context: Context) {
        homeAdmobAdLoader.loadNativeAd(context)
    }

    override fun showNativeAd(context: Context, adContainer: ViewGroup, adViewWidth: Int) {
        homeAdmobAdLoader.showNativeAd(context, adContainer, adViewWidth)
    }


}