package com.ur.apps.walk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sg.UserManager
import com.sg.response.FormField
import com.ur.apps.analysis.shuzhi.SZSdkImpl
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.utils.URLog
import com.ur.apps.walk.BuildConfig
import com.ur.apps.walk.repository.WithdrawRepository
import com.ur.apps.walk.utils.RegionHelper
import com.ur.apps.walk.withdraw.WithDrawData
import kotlinx.coroutines.launch

private const val TAG = "WithdrawAmountViewModel"

private const val DEBUG_DISPLAY_SUCCESS_DIALOG = false

class WithdrawAmountViewModel(application: Application) : AndroidViewModel(application) {

    private var mWithDrawData: WithDrawData = WithDrawData()
    private val repository = WithdrawRepository(application)


    // 当前用户余额（美元）
    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance

    // 当前用户金币数量
    private val _coins = MutableLiveData(0L)
    val coins: LiveData<Long> = _coins

    // 当前区域代码
    private val _currentRegion = MutableLiveData<String>()
    val currentRegion: LiveData<String> = _currentRegion

    // 当前区域代码
    private val _formFields = MutableLiveData<List<FormField>>()
    val formFields: LiveData<List<FormField>> = _formFields

    private val _withdrawSuccess = MutableLiveData<Boolean>()
    val withdrawSuccess: LiveData<Boolean> = _withdrawSuccess

    val formFieldMap = mutableMapOf<String, Pair<FormField, String>>()

    // 初始化
    init {
        // 获取当前区域
        _currentRegion.value = RegionHelper.getRegion(application)
        // 加载提现选项
//        loadWithdrawOptions()
    }

    /**
     * 加载提现选项数据
     */
    fun loadWithdrawData() {
        viewModelScope.launch {
            handleUserData()
            handleInputMethod()
        }
    }

    private fun handleInputMethod() {
        if (mWithDrawData.isInvalid()) {
            URLog.e(TAG, "error that with update user withdraw data fetch error.")
        } else {
            _formFields.value = mWithDrawData.formFields/*.forEach { formField ->
                when (formField.type) {
                    FormField.EMAIL -> {
                        _inputVisibleEmail.value = true
                        _inputVisiblePhoneNum.value = false
                        _inputVisibleText.value = false
                        _inputEmailDisplayText.value = formField.placeholder
                    }

                    FormField.NUMBER -> {}
                    FormField.PHONE_NUMBER -> {
                        _inputVisibleEmail.value = false
                        _inputVisiblePhoneNum.value = true
                        _inputVisibleText.value = false
                        _inputPhoneNumDisplayText.value =
                            formField.placeholder
                    }

                    FormField.TEXT -> {
                        _inputVisibleName.value = false
                        _inputVisibleEmail.value = false
                        _inputVisiblePhoneNum.value = false
                        _inputVisibleText.value = true
                        _inputTextDisplayText.value = formField.placeholder
                    }

                    FormField.SELECT -> {}
                }
            }*/
        }
    }

    private fun handleUserData() {
        if (mWithDrawData.isInvalid()) {
            URLog.e(TAG, "error that with update user withdraw data fetch error.")
        } else {
            _coins.value = mWithDrawData.udCoin
            _balance.value = mWithDrawData.udAmount
        }
    }

    /**
     * 处理提现操作
     * @return 提现是否成功
     */
    fun handleWithdraw(filterFormFieldMap: MutableMap<String, Pair<FormField, String>>) {

        if (BuildConfig.DEBUG && DEBUG_DISPLAY_SUCCESS_DIALOG) {
            _withdrawSuccess.value = true
        }

        // 记录信息用于调试
        URLog.d(TAG, "Withdraw request ($filterFormFieldMap)")

        viewModelScope.launch {
            SZSdkImpl.asyncUpdateShuziId(scene = SZSdkImpl.SCENE_WITHDRAWAL)
            val response = repository.performRealWithDraw(
                1,
                mWithDrawData.paymentId,
                mWithDrawData.paymentProviderItemId,
                filterFormFieldMap
            )
            val isSuccess = (response != null)
            TDAnalyticsManager.reportCash(
                cashPosition = "0",  // 提现的位置
                cashRevenue = _balance.value!!,  // 提现的收入
                currencyCode = (if (isSuccess) {
                    response?.currencyCode
                } else {
                    UserManager.instance.getUserInfo()?.currencyCode
                }).toString(), // 提现的国家编码
                cashtrue = isSuccess.toString() // 是否提现成功
            )
            if (isSuccess) {
                URLog.i(TAG, "with draw success,perform ui action")
                _withdrawSuccess.value = true
            }
        }
    }

    fun handleInputWithDrawData(stringExtra: String?) {
        if (stringExtra.isNullOrEmpty()) {
            return
        }
        val type = object : TypeToken<WithDrawData>() {}.type

        mWithDrawData = Gson().fromJson(stringExtra, type)
    }
}