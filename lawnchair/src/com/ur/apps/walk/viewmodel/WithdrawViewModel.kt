package com.ur.apps.walk.viewmodel

import android.app.Application
import com.ur.apps.utils.URLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sg.response.WithdrawalInfoResponse
import com.android.launcher3.R
import com.ur.apps.walk.model.RegionOption
import com.ur.apps.walk.model.RegionUi
import com.ur.apps.walk.model.WithdrawOption
import com.ur.apps.walk.repository.WithdrawRepository
import com.ur.apps.walk.utils.RegionHelper
import com.ur.apps.walk.withdraw.WithDrawData
import kotlinx.coroutines.launch

/**
 * 提现页面ViewModel
 */
private const val TAG = "WithdrawViewModel"

class WithdrawViewModel(application: Application) : AndroidViewModel(application) {
    private var mWithdrawalInfoResponse: WithdrawalInfoResponse = WithdrawalInfoResponse()
    private val repository = WithdrawRepository(application)

    // 当前用户余额（美元）
    private val _balance = MutableLiveData(Pair("", 0.0))
    val balance: LiveData<Pair<String, Double>> = _balance

    // 当前用户金币数量
    private val _coins = MutableLiveData(0L)
    val coins: LiveData<Long> = _coins

    // 提现选项列表
    private val _withdrawOptions = MutableLiveData<List<WithdrawOption>>(emptyList())
    val withdrawOptions: LiveData<List<WithdrawOption>> = _withdrawOptions

    // 选中的提现选项
    private val _selectedOption = MutableLiveData<WithdrawOption?>()
    val selectedOption: LiveData<WithdrawOption?> = _selectedOption

    // 当前区域代码
    private val _currentRegion = MutableLiveData<RegionUi>()
    val currentRegion: LiveData<RegionUi> = _currentRegion

    // 当前区域代码
    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String> = _toast

    private val _inputType = MutableLiveData<String>()
    val inputType: LiveData<String> = _inputType

    // 初始化
    init {
        // 获取当前区域
//        _currentRegion.value = RegionHelper.getRegion(application)
        // 加载提现选项
        loadWithdrawOptions()
    }

    /**
     * 加载提现选项数据
     */
    private fun loadWithdrawOptions() {
        viewModelScope.launch {
            val withdrawalInfoResponse = repository.loadWithDrawInfo(getApplication())
            mWithdrawalInfoResponse = withdrawalInfoResponse
            val options = handleWithdrawalInfoResponseIntoWithDrawList();
            handleUserData()
            _withdrawOptions.postValue(options)
        }
    }

    private fun handleUserData() {
        if (mWithdrawalInfoResponse.isInvalid()) {
            URLog.e(TAG, "error that with update user withdraw data fetch error.")
        } else {
            _coins.value = mWithdrawalInfoResponse.udCoin
            _balance.value =
                Pair(mWithdrawalInfoResponse.currencyCode, mWithdrawalInfoResponse.udAmount)

            _currentRegion.value = RegionUi(
                mWithdrawalInfoResponse.countryCode,
                mWithdrawalInfoResponse.nationalFlagUrl
            )
        }
    }

    private fun handleWithdrawalInfoResponseIntoWithDrawList(): List<WithdrawOption> {
        val options: MutableList<WithdrawOption> = mutableListOf()
        if (mWithdrawalInfoResponse.isInvalid()) {
            URLog.e(TAG, "error that with draw info list fetch error.")
        } else {
            mWithdrawalInfoResponse.paymentProviders.forEach {
                it.items.forEach {
                    options.add(it.let {
                        WithdrawOption(
                            id = it.id, type = it.currencyCode,
                            amount = it.amount ?: 0.0,
                            coinsRequired = it.coin ?: 0,
                            remainCount = it.remainTimes,
                        )
                    })
                }
            }
        }
        return options
    }

    /**
     * 选择提现选项
     */
    fun selectOption(option: WithdrawOption) {
        val currentOptions = _withdrawOptions.value?.toMutableList() ?: mutableListOf()
        currentOptions.forEach { it.isSelected = it.id == option.id }
        _withdrawOptions.value = currentOptions
        _selectedOption.value = option
    }

    /**
     * 更新区域设置
     */
    fun updateRegion(region: RegionOption) {
        viewModelScope.launch {
            if (RegionHelper.setRegion(getApplication(), region.code)) {
                _currentRegion.value = RegionUi(region.code, region.nationFlag)
                loadWithdrawOptions()
            } else {
                _toast.value =
                    getApplication<Application>().getString(R.string.update_region_error_toast)
            }
        }
    }

    /**
     * 处理提现操作
     * @return 提现是否成功
     */
    fun handleWithdraw(): Boolean {
        val selectedOption = _selectedOption.value ?: return false
        val currentCoins = _coins.value ?: 0L

        // 检查金币是否足够
        if (currentCoins < selectedOption.coinsRequired) {
            return false
        }

        // 实际应用中，这里应该调用API进行提现操作
        // 模拟提现操作，扣除相应金币
        _coins.value = currentCoins - selectedOption.coinsRequired

        return true
    }

    fun buildWithDrawData(value: WithdrawOption): WithDrawData {
        if (mWithdrawalInfoResponse.isInvalid()) {
            return WithDrawData()
        } else {
            val item = mWithdrawalInfoResponse.paymentProviders.first()
            val data = WithDrawData()
            data.paymentId = item.id
            data.paymentProviderItemId = value.id

            data.formFields = item.formFields
            data.udCoin = mWithdrawalInfoResponse.udCoin
            data.udAmount = mWithdrawalInfoResponse.udAmount
            return data
        }
    }

}
