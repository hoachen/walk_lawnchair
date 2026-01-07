package com.ur.apps.ad

interface BannerAdListener {

    fun onBannerAdLoaded(adLoader: BaseAdLoader) {
    }

    fun onBannerAdLoadFail(adLoader: BaseAdLoader, error: String) {
    }

    fun onBannerAdClicked(adLoader: BaseAdLoader) {
    }

    fun onBannerAdShow(adLoader: BaseAdLoader) {
    }

    fun onBannerAdOpened(adLoader: BaseAdLoader) {
    }

}