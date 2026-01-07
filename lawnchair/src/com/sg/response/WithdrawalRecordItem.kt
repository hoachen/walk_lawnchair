package com.sg.response

/**
 * 单条提现记录
 */
data class WithdrawalRecordItem(
    val amount: Double,          // 现金数
    val coin: Long,              // 金币数
    val status: Int,             // 状态 0:待审核, 1:审核通过, 2:审核失败, 3:提现成功, 4:提现异常
    val createTime: String,      // 创建时间，格式：yyyy-MM-dd HH:mm
    val orderId: String,         // 订单ID
    val currencyCode: String,    // 货币码
    val commissionSwitch: Int,   // 手续费开关
    val commissionAmount: Double,// 手续费钱
    val commissionCoin: Long,    // 手续费金币
    val countryCode: String      // 订单国家
) {
    companion object {
        // 提现记录状态常量
        const val STATUS_PENDING_REVIEW = 0      // 待审核
        const val STATUS_REVIEW_PASSED = 1       // 审核通过
        const val STATUS_REVIEW_FAILED = 2       // 审核失败
        const val STATUS_WITHDRAWAL_SUCCESS = 3  // 提现成功
        const val STATUS_WITHDRAWAL_EXCEPTION = 4 // 提现异常
    }
    
    /**
     * 判断是否待审核
     */
    fun isPendingReview(): Boolean {
        return status == STATUS_PENDING_REVIEW
    }
    
    /**
     * 判断是否审核通过
     */
    fun isReviewPassed(): Boolean {
        return status == STATUS_REVIEW_PASSED
    }
    
    /**
     * 判断是否提现成功
     */
    fun isWithdrawalSuccess(): Boolean {
        return status == STATUS_WITHDRAWAL_SUCCESS
    }
    
    /**
     * 判断是否有异常
     */
    fun hasException(): Boolean {
        return status == STATUS_WITHDRAWAL_EXCEPTION || status == STATUS_REVIEW_FAILED
    }
    
    /**
     * 判断是否需要支付手续费
     */
    fun hasCommission(): Boolean {
        return commissionSwitch == 1
    }
} 