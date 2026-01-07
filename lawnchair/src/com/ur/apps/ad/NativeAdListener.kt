package com.ur.apps.ad

interface NativeAdListener {

    fun onNativeAdLoaded(adLoader: BaseAdLoader) {
    }

    fun onNativeAdLoadFail(adLoader: BaseAdLoader, error: String) {
    }

    fun onNativeAdClicked(adLoader: BaseAdLoader) {
    }

    fun onNativeAdShow(adLoader: BaseAdLoader) {
    }

    fun onNativeAdOpened(adLoader: BaseAdLoader) {

    }

    fun onNativeAdClose(adLoader: BaseAdLoader) {

    }

}