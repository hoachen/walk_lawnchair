package com.ur.apps.walk

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.ur.apps.utils.URLog
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.sg.UserManager
import com.sg.model.UserInfo
import com.sg.response.RewardImpResponse
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.ad.AdShowScene
import com.ur.apps.ad.BaseAdLoader
import com.ur.apps.ad.InterstitialAdListener
import com.ur.apps.ad.RewardAdListener
import com.ur.apps.ad.RewardAdRecord
import com.ur.apps.ad.topon.TopOnAdLoader
import com.ur.apps.analysis.shuzhi.SZSdkImpl
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.utils.NetworkUtils
import com.ur.apps.utils.NetworkUtils.NETWORK_TYPE_4G
import com.ur.apps.utils.NetworkUtils.NETWORK_TYPE_5G
import com.ur.apps.utils.NetworkUtils.NETWORK_TYPE_NONE
import com.ur.apps.utils.NetworkUtils.NETWORK_TYPE_WIFI
import com.ur.apps.walk.viewmodel.RewardViewModel
import com.ur.apps.walk.widget.RewardCoinDialog

open class BaseRewardActivity : BaseActivity(), RewardAdListener, InterstitialAdListener {

    private lateinit var viewModel: RewardViewModel
    private var rewardCoinDialog: RewardCoinDialog? = null


    companion object {
        const val TAG = "RewardActivity"
    }

    /**
     * 注册广告监听器，获得激励广告的回调
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[RewardViewModel::class.java]
        viewModel.init()
        AdLoaderManager.addRewardAdListener(this)
        AdLoaderManager.addInterstitialAdListener(this)
        observeRewardInfoChanged()
        observeUserInfoChanged()
        observeUserWithdrawEnableChanged()
        observeUserAdNetworkChanged()
    }

    /**
     * 监听网络请求激励金币结果，弹窗提醒
     */
    private fun observeRewardInfoChanged() {
        viewModel.rewardInfo.observe(this) { result ->
            result?.first?.let {
                showRewardCoinDialog(it, result.second.scene == AdShowScene.REDEEM)
                try {
                    TDAnalyticsManager.reportCoin(it.rewardCoin.toInt())
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * 监听用户账号信息是否发生变化
     */
    private fun observeUserInfoChanged() {
        viewModel.userInfo.observe(this) {
            it?.let { handleUserInfoChanged(it) }
        }
    }

    open fun handleUserInfoChanged(userInfo: UserInfo) {
        URLog.i(TAG, "handleUserInfoChanged  current coins is ${userInfo.udCoin}")
    }


    /**
     * 监听用户是否具备提现条件
     */
    private fun observeUserWithdrawEnableChanged() {
        viewModel.userWithDrawEnable.observe(this) {
            it?.let { handleUserWithdrawEnableChanged(it) }
        }
    }

    // 子类覆写处理
    open fun handleUserWithdrawEnableChanged(enable: Boolean) {
        URLog.i(TAG, "handleUserWithdrawEnableChanged enable= $enable")
    }

    private fun observeUserAdNetworkChanged() {
        viewModel.userAdNetwork.observe(this) {
            tryInitSAdSdk(it)
        }
    }


    open fun tryInitSAdSdk(channel : String = "") {
        val userChannel = UserManager.instance.getUserAdNetwork()
        if (!TextUtils.isEmpty(userChannel)) {
//            SAdSDK.instance.start(this, userChannel)
        }
    }

    fun showRewardAd(adShowScene: AdShowScene = AdShowScene.COMMON) : Result<Boolean> {
        if (AdLoaderManager.isRewardVideoAdReady()) {
            TDAnalyticsManager.reportButtonClick(
                adType = "Reward",
                adPosition = "任务_激励",
                adPositionType = "激励",
                placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
            )
            URLog.i(TAG, "show reward video ad ")
            AdLoaderManager.showRewardVideoAd(this, adShowScene)
            return Result.success(true)
        } else if (AdLoaderManager.isInterstitialAdReady()) {
            TDAnalyticsManager.reportButtonClick(
                adType = "Reward",
                adPosition = "任务_激励",
                adPositionType = "激励",
                placementId = TopOnAdLoader.TOPON_INTERSTITIAL_PLACEMENT_ID
            )
            URLog.i(TAG, "show reward interstitial ad ")
            AdLoaderManager.showInterstitialAd(this, adShowScene)
            return Result.success(true)
        } else {
            URLog.i(TAG, "show ad loading toast")
            showAdLoadingToast()
        }
        return Result.failure(Throwable("reward ad loading"))
    }

    private fun showAdLoadingToast() {
        val networkUtils = NetworkUtils.getInstance(this)
        val networkType = networkUtils.getNetworkType()
        val message = when (networkType) {
            NETWORK_TYPE_NONE -> getString(R.string.network_error)
            NETWORK_TYPE_WIFI -> getString(R.string.ad_loading)
            NETWORK_TYPE_5G -> getString(R.string.ad_loading)
            NETWORK_TYPE_4G -> getString(R.string.ad_loading)
            else -> getString(R.string.poor_network)
        }
        // 使用自定义的Toast避免重复显示
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showBannerAd(bannerAdContainer: ViewGroup) {
        TDAnalyticsManager.reportButtonClick(
            adType = "Banner",
            adPosition = "首页_底部",
            adPositionType = "首页",
            placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
        )
        AdLoaderManager.loadBannerAd(this, bannerAdContainer)
    }


    open fun showBannerInCoinDialog() = true


    private fun showRewardCoinDialog(reward: RewardImpResponse, isRedeem: Boolean = false) {
        rewardCoinDialog?.dismiss()
        rewardCoinDialog = null
        rewardCoinDialog =
            RewardCoinDialog(this, reward, showBannerInCoinDialog(), isRedeem).apply {
                setReWatchRewardAdListener {
                    TDAnalyticsManager.reportButtonClick(
                        adType = "Reward",
                        adPosition = "任务_激励_翻倍",
                        adPositionType = "激励_翻倍",
                        placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
                    )
                    showRewardAd(AdShowScene.DOUBLE)
                }
                setGetRedeemListener {
                    val intent = Intent(this@BaseRewardActivity, WithdrawActivity::class.java)
                    startActivity(intent)
                }
                setOnTaskDoneClaimAdListener {
                    AdLoaderManager.showInterstitialAd(this@BaseRewardActivity, AdShowScene.NO_REWARD)
                }
            }
        this.rewardCoinDialog?.show()
    }


    /**
     * 获得激励广告的回调，尝试请求服务端获取激励
     */
    override fun onReward(adLoader: BaseAdLoader, rewardAdRecord: RewardAdRecord) {
        super.onReward(adLoader, rewardAdRecord)
        URLog.i(TAG, "onReward rewardAdRecord= $rewardAdRecord")
        SZSdkImpl.asyncUpdateShuziId(scene = SZSdkImpl.SCENE_REWARD,
            atid = rewardAdRecord.adTraceId)
        viewModel.requestReward(rewardAdRecord)
    }

    override fun onInterstitialReward(adLoader: BaseAdLoader, rewardAdRecord: RewardAdRecord) {
        super.onInterstitialReward(adLoader, rewardAdRecord)
        URLog.i(TAG, "onInterstitialReward rewardAdRecord= $rewardAdRecord")
        SZSdkImpl.asyncUpdateShuziId(scene = SZSdkImpl.SCENE_REWARD,
            atid = rewardAdRecord.adTraceId)
        viewModel.requestReward(rewardAdRecord)
    }

    /**
     * 注销激励广告监听器，避免泄露
     */
    override fun onDestroy() {
        super.onDestroy()
        AdLoaderManager.removeRewardAdListener(this)
    }
}