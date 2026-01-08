package com.ur.apps.walk.viewmodel

import android.app.Application
import com.ur.apps.utils.URLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sg.UserManager
import com.sg.model.UserInfo
import com.sg.repository.RewardRepository
import com.sg.request.StrategyRequest
import com.sg.response.RewardImpResponse
import com.ur.apps.ad.RewardAdRecord
import com.ur.apps.ad.toRewardImpRequest
import com.ur.apps.walk.repository.WithdrawRepository
import kotlinx.coroutines.launch

class RewardViewModel(application: Application) : AndroidViewModel(application), UserManager.Companion.UserInfoListener  {

    private val rewardRepo = RewardRepository(application)
    private val withdrawRepo = WithdrawRepository(application)


    // 当前用户余额（美元）
    private val _rewardInfo : MutableLiveData<Pair<RewardImpResponse?, RewardAdRecord>> = MutableLiveData(null)
    val rewardInfo: LiveData<Pair<RewardImpResponse?, RewardAdRecord>> = _rewardInfo

    private val _userInfo : MutableLiveData<UserInfo?> = MutableLiveData(null)
    val userInfo : LiveData<UserInfo?> = _userInfo

    private val _userAdNetwork : MutableLiveData<String> = MutableLiveData("")
    val userAdNetwork : LiveData<String> = _userAdNetwork

    private val _userWithDrawEnable : MutableLiveData<Boolean> = MutableLiveData(false)
    val userWithDrawEnable : LiveData<Boolean> = _userWithDrawEnable

    fun init() {
        UserManager.addListener(this)
    }

    fun requestReward(rewardAdRecord: RewardAdRecord) {
        viewModelScope.launch {
            val rewardImpResponse = rewardRepo.requestReward(rewardAdRecord.toRewardImpRequest())
            URLog.i(TAG, "request reward $rewardImpResponse")
            _rewardInfo.postValue(Pair(rewardImpResponse, rewardAdRecord))
            UserManager.instance.refreshUserInfo(StrategyRequest.SCENE_REWARD)
        }
    }

    private fun checkUserWithDrawEnable(currentCoins: Int) {
        viewModelScope.launch {
            val response = withdrawRepo.loadWithDrawInfo(getApplication())
//            URLog.i(TAG, "loadWithDrawInfo response $response  userCurrentCoins $currentCoins")
            response.paymentProviders.firstOrNull()?.items?.first {
                it.remainTimes > 0
            }?.let { item ->
                // 最小可提现的单元
                URLog.i(TAG, "Minimum withdrawal level $item  userCurrentCoins $currentCoins")
                item.coin?.let {
                    _userWithDrawEnable.postValue(it < currentCoins)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        UserManager.removeListener(this)
    }


    companion object {
        const val TAG = "RewardViewModel"
    }

    override fun onUserInfoChanged(userInfo: UserInfo) {
        URLog.i(TAG, "onUserInfoChanged $userInfo")
        _userInfo.postValue(userInfo)
        checkUserWithDrawEnable(userInfo.udCoin)
    }

    override fun onUserAdNetworkChanged(adNetwork: String) {
        URLog.i(TAG, "onUserAdNetworkChanged $adNetwork")
        _userAdNetwork.postValue(adNetwork)
    }
}
