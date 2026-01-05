package com.sg.response

import com.google.gson.annotations.SerializedName

/**
 * 提现订单响应类
 * 包含提现订单信息、金币扣减情况和金额详情
 */
data class WithdrawalOrderResponse(
    /**
     * 提现订单id
     */
    @SerializedName("orderId")
    val orderId: String? = null,
    
    /**
     * 提现扣减金币
     */
    @SerializedName("reduceCoin")
    val reduceCoin: Int = 0,
    
    /**
     * 提现扣减金币对应的钱
     */
    @SerializedName("reduceAmount")
    val reduceAmount: Float = 0f,
    
    /**
     * UD金额
     */
    @SerializedName("udAmount")
    val udAmount: Float = 0f,
    
    /**
     * 可领取的钱
     */
    @SerializedName("udAvailableAmount")
    val udAvailableAmount: Float = 0f,
    
    /**
     * pending的钱
     */
    @SerializedName("udPendingAmount")
    val udPendingAmount: Float = 0f,
    
    /**
     * UD金币
     */
    @SerializedName("udCoin")
    val udCoin: Int = 0,
    
    /**
     * 订单状态
     */
    @SerializedName("status")
    val status: Int = 0,
    
    /**
     * 货币代码
     */
    @SerializedName("currencyCode")
    val currencyCode: String? = null,
    
    /**
     * 手续费的钱
     */
    @SerializedName("commissionAmount")
    val commissionAmount: Float = 0f,
    
    /**
     * 手续费对应的金币
     */
    @SerializedName("commissionCoin")
    val commissionCoin: Int = 0
) 