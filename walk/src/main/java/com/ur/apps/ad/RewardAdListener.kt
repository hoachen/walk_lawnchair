package com.ur.apps.ad


interface RewardAdListener {

     fun onRewardedAdLoaded(adLoader: BaseAdLoader)  {

     }

    fun onRewardedAdFailed(adLoader: BaseAdLoader, error: String) {

    }

    fun onRewardedAdPlayStart(adLoader: BaseAdLoader) {

    }

    fun onRewardedAdPlayEnd(adLoader: BaseAdLoader) {

    }

     fun onRewardedVideoAdPlayFailed(adLoader: BaseAdLoader, error: String) {
    }

    fun onRewardedAdClosed(adLoader: BaseAdLoader) {

    }

    fun onReward(adLoader: BaseAdLoader, rewardAdRecord: RewardAdRecord) {
    }

    fun onRewardedAdPlayClicked(adLoader: BaseAdLoader) {
    }

}