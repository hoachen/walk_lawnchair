package com.ur.apps.walk.repository

import android.content.Context
import com.sg.ApiClient
import com.sg.BaseResponse
import com.sg.ResponseInterceptor
import com.sg.request.ChangeCountryRequest
import com.sg.response.ChangeCountryResponse
import com.sg.response.FormField
import com.sg.response.WithdrawalInfoResponse
import com.sg.response.WithdrawalOrderResponse
import com.ur.apps.utils.URLog
import com.ur.apps.walk.model.RegionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private const val TAG = "WithdrawRepository"

/**
 * 提现数据仓库类
 */
class WithdrawRepository(private val context: Context) {

    /**
     * 从assets文件中读取提现选项数据
     */
    suspend fun loadWithDrawInfo(context: Context): WithdrawalInfoResponse =
        withContext(Dispatchers.IO) {
            try {
                val response: BaseResponse<WithdrawalInfoResponse> =
                    ApiClient.getWithdrawalService(context).getWithdrawalInfo()
                response.data ?: run {
                    URLog.e(
                        TAG,
                        "get nothing from server when fetch with draw info response.st({$response.st}) response.msg({$response.msg}) response.uid({${response.uid}}) response.code({${response.code}})"
                    )
                    WithdrawalInfoResponse()
                }

            } catch (e: Exception) {
                URLog.e(TAG, "get with draw info failed: ", e)
                WithdrawalInfoResponse()
            }
        }

    suspend fun updateRegion(region: RegionOption): Boolean = withContext(Dispatchers.IO) {
        val response: BaseResponse<ChangeCountryResponse> =
            ApiClient.getUserService(context).changeCountry(ChangeCountryRequest(region.code))
        region.code == (response.data?.countryCode ?: "")
    }

    suspend fun performRealWithDraw(
        withdrawalType: Int,
        paymentProviderId: Long,
        paymentProviderItemId: Long,
        formFieldMap: MutableMap<String, Pair<FormField, String>>,
    ): WithdrawalOrderResponse? =
        withContext(Dispatchers.IO) {
            try {

                val finalMap = mutableMapOf<String, Any>()
                finalMap.put("withdrawalType", withdrawalType)
                finalMap.put("paymentProviderId", paymentProviderId)
                finalMap.put("paymentProviderItemId", paymentProviderItemId)
                formFieldMap.forEach {
                    finalMap.put(it.key, it.value.second)
                }

                URLog.i(TAG, "before with draw perform ($finalMap)")
                val response: BaseResponse<WithdrawalOrderResponse> =
                    ApiClient.getWithdrawalService(context).withdraw(
                        finalMap
                    )
                URLog.i(
                    TAG,
                    "with draw result(${response.data})"
                )

                if (response.code == ResponseInterceptor.SUCCESS) {
                    response.data ?: run {
                        URLog.e(
                            TAG,
                            "get nothing from server when fetch with draw info response.st({$response.st}) " +
                                    "response.msg({$response.msg}) response.uid({${response.uid}}) response.code({${response.code}})"
                        )
                    }
                    return@withContext response.data
                } else {
                    return@withContext null
                }

            } catch (e: Exception) {
                URLog.e(TAG, "get with draw info failed: ", e)
                return@withContext null
            }
        }


} 