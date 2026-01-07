package com.sg.api

import com.sg.BaseResponse
import com.sg.request.StrategyRequest
import com.sg.response.ProductReferralResponse
import com.sg.response.StrategyResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 通用服务接口
 */
interface CommonService {
    /**
     * 获取推荐产品列表
     * @return 推荐产品列表响应
     */
    @GET("/common/productReferral/get")
    fun getProductReferrals(): Call<BaseResponse<ProductReferralResponse>>
    
    /**
     * 获取策略信息
     * @param request 策略信息请求
     * @return 策略信息响应
     */
    @POST("/st")
    fun getStrategy(@Body request: StrategyRequest): Call<BaseResponse<StrategyResponse>>
} 