package com.sg.repository

import android.content.Context
import com.sg.ApiClient
import com.sg.request.RewardImpRequest
import com.sg.response.RewardImpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RewardRepository(private val context: Context) {

    /**
     * 请求激励信息
     */
    suspend fun requestReward(request : RewardImpRequest): RewardImpResponse? = withContext(Dispatchers.IO) {
        try {
            val rewardService = ApiClient.getRewardService(context)
            val response = rewardService.submitAdImpression(request).execute().body()
            response?.data
        } catch (e: Exception) {
            e.printStackTrace()
             null
        }
    }


    companion object {
        const val TAG = "RewardRepository"
    }

}