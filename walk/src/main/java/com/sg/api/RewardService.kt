package com.sg.api

import com.sg.BaseResponse
import com.sg.request.RewardImpRequest
import com.sg.response.RewardImpResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 奖励相关的API服务
 */
interface RewardService {
    /**
     * 提交广告展示获取奖励
     * @param request 广告展示信息请求
     * @return 奖励信息响应
     */
    @POST("/reward/imp")
    fun submitAdImpression(@Body request: RewardImpRequest): Call<BaseResponse<RewardImpResponse>>
} 