package com.sg.response

/**
 * 提现订单查询响应
 */
data class WithdrawalOrderQueryResponse(
    val orderId: String,          // 订单ID
    val status: Int,              // 订单状态
    val coin: Long,               // 金币数量
    val currencyCode: String,     // 货币代码
    val commissionSwitch: Int,    // 手续费开关
    val commissionAmount: Double, // 手续费金额
    val commissionCoin: Long,     // 手续费金币
    val countryCode: String,      // 国家代码
    val amount: Double            // 提现金额
) {
    companion object {
        // 订单状态常量（假设值，请根据实际业务调整）
        const val STATUS_PENDING = 0     // 处理中
        const val STATUS_SUCCESS = 1     // 成功
        const val STATUS_FAILED = 2      // 失败
        const val STATUS_CANCELLED = 3   // 已取消
    }
    
    /**
     * 判断订单是否处理中
     */
    fun isPending(): Boolean {
        return status == STATUS_PENDING
    }
    
    /**
     * 判断订单是否成功
     */
    fun isSuccess(): Boolean {
        return status == STATUS_SUCCESS
    }
    
    /**
     * 判断订单是否失败
     */
    fun isFailed(): Boolean {
        return status == STATUS_FAILED
    }
    
    /**
     * 判断是否需要支付手续费
     */
    fun hasCommission(): Boolean {
        return commissionSwitch == 1
    }
} 