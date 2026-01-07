package com.ur.apps.ad


interface InterstitialAdListener {
   fun onInterstitialReward(adLoader: BaseAdLoader, rewardAdRecord: RewardAdRecord) {
   }

    fun onInterstitialAdLoaded(adLoader: BaseAdLoader) {
    }

    fun onInterstitialAdLoadFail(adLoader: BaseAdLoader, error: String) {
    }

    fun onInterstitialAdClicked(adLoader: BaseAdLoader) {
    }

    fun onInterstitialAdShow(adLoader: BaseAdLoader) {
    }

    fun onInterstitialAdClose(adLoader: BaseAdLoader) {
    }

    fun onInterstitialAdVideoStart(adLoader: BaseAdLoader) {
    }

    fun onInterstitialAdVideoEnd(adLoader: BaseAdLoader) {
    }

    fun onInterstitialAdVideoError(adLoader: BaseAdLoader, error: String) {
    }



}