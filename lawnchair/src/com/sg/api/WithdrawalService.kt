package com.sg.api

import com.sg.BaseResponse
import com.sg.request.WithdrawalOrderQueryRequest
import com.sg.request.WithdrawalRecordRequest
import com.sg.response.WithdrawalInfoResponse
import com.sg.response.WithdrawalOrderQueryResponse
import com.sg.response.WithdrawalOrderResponse
import com.sg.response.WithdrawalRecordResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 提现相关的API服务
 */
interface WithdrawalService {
    /**
     * 获取提现信息
     * @return 提现信息响应
     */
    @GET("/withdrawal/info")
    suspend fun getWithdrawalInfo(): BaseResponse<WithdrawalInfoResponse>

    /**
     * 提交提现请求
     * @param withdrawalInfo 提现信息
     * @return 提现结果
     */
    @POST("/withdrawal/obtain")
    suspend fun withdraw(@Body body: Map<String, @JvmSuppressWildcards Any>): BaseResponse<WithdrawalOrderResponse>

    /**
     * 查询提现订单
     * @param request 订单查询请求
     * @return 订单信息响应
     */
    @POST("/withdrawal/order/query")
    fun queryOrder(@Body request: WithdrawalOrderQueryRequest): Call<BaseResponse<WithdrawalOrderQueryResponse>>

    /**
     * 获取提现记录
     * @param request 提现记录查询请求
     * @return 提现记录响应
     */
    @POST("/withdrawal/record")
    fun getRecords(@Body request: WithdrawalRecordRequest): Call<BaseResponse<WithdrawalRecordResponse>>
} 