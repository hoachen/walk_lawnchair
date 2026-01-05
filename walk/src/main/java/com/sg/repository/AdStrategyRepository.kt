package com.sg.repository

import android.content.Context
import com.sg.ApiClient
import com.sg.request.StrategyRequest
import com.sg.response.ProductReferralResponse
import com.sg.response.StrategyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdStrategyRepository(private val context: Context) {

    /**
     * 请求策略配置
     */
    suspend fun requestStrategy(strategyRequest: StrategyRequest): StrategyResponse? = withContext(Dispatchers.IO) {
        try {
            val commonService = ApiClient.getCommonService(context)
            val response = commonService.getStrategy(strategyRequest).execute().body()
            response?.data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getProductReferrals(): ProductReferralResponse? = withContext(Dispatchers.IO) {
        try {
            val commonService = ApiClient.getCommonService(context)
            val response = commonService.getProductReferrals().execute().body()
            response?.data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        const val TAG = "AdStrategyRepository"
    }
}