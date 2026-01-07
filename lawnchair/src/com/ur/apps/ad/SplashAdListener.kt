package com.ur.apps.ad


interface SplashAdListener {

    fun onAdLoaded(adLoader: BaseAdLoader)

    fun onAdLoadTimeout(adLoader: BaseAdLoader)

    fun onNoAdError(adLoader: BaseAdLoader)

    fun onAdShow(adLoader: BaseAdLoader)

    fun onAdClick(adLoader: BaseAdLoader)

    fun onAdDismiss(adLoader: BaseAdLoader)

}