package com.ur.apps.ad

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.ViewGroup

abstract class BaseAdLoader {

    internal var internalRewardAdListeners : MutableSet<RewardAdListener> = mutableSetOf()

    internal var internalSplashAdListeners: MutableSet<SplashAdListener> = mutableSetOf()

    internal var internalInterstitialAdListeners : MutableSet<InterstitialAdListener> = mutableSetOf()

    internal var internalNativeAdListeners : MutableSet<NativeAdListener> = mutableSetOf()

    internal var internalBannerAdListeners : MutableSet<BannerAdListener> = mutableSetOf()


    abstract fun initialize(application: Application)

    abstract fun loadRewardVideoAd(context: Context)

    abstract fun isRewardVideoAdReady() : Boolean

    abstract fun showRewardVideoAd(activity: Activity, adShowScene: AdShowScene = AdShowScene.COMMON)

    open fun addRewardAdListener(rewardAdListener: RewardAdListener) {
        internalRewardAdListeners.add(rewardAdListener)
    }

    open fun removeRewardAdListener(rewardAdListener: RewardAdListener) {
        internalRewardAdListeners.remove(rewardAdListener)
    }


    abstract fun loadInterstitialAd(context: Context)

    abstract fun isInterstitialAdReady() : Boolean

    abstract fun showInterstitialAd(activity: Activity, adShowScene: AdShowScene = AdShowScene.COMMON)

    open fun addInterstitialAdListener(interstitialAdListener: InterstitialAdListener) {
        internalInterstitialAdListeners.add(interstitialAdListener)
    }

    open fun removeInterstitialAdListener(interstitialAdListener: InterstitialAdListener) {
        internalInterstitialAdListeners.remove(interstitialAdListener)
    }


    abstract fun loadSplashAd(context: Context,  fetchAdTimeout: Int = 5000)

    abstract fun isSplashAdReady() : Boolean


    open fun addSplashAdListener(splashAdListener: SplashAdListener) {
        internalSplashAdListeners.add(splashAdListener)
    }

    open fun removeSplashAdListener(splashAdListener: SplashAdListener) {
        internalSplashAdListeners.remove(splashAdListener)
    }

    abstract fun showSplashAd(activity: Activity, container: ViewGroup)


    open fun addBannerAdListener(bannerAdListener: BannerAdListener) {
        internalBannerAdListeners.add(bannerAdListener)
    }

    open fun removeBannerAdListener(bannerAdListener: BannerAdListener) {
        internalBannerAdListeners.remove(bannerAdListener)
    }

    abstract fun isBannerAdReady() : Boolean

    abstract fun loadBannerAd(context: Context, container: ViewGroup?)

    abstract fun showBannerAd(context: Context, container: ViewGroup)

    open fun addNativeAdListener(nativeAdListener: NativeAdListener) {
        internalNativeAdListeners.add(nativeAdListener)
    }

    open fun removeNativeAdListener(nativeAdListener: NativeAdListener) {
        internalNativeAdListeners.remove(nativeAdListener)
    }

    abstract fun isNativeAdReady() : Boolean

    abstract fun loadNativeAd(context: Context)
    abstract fun showNativeAd(context: Context, adContainer: ViewGroup, adViewWidth : Int)

}