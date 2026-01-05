package com.sg.response

/**
 * 提现选项
 */
data class WithdrawalItem(
    val id: Long,                  // 对应paymentProviderItemId
    val amount: Double?,           // 提现金额（固定金额提现时）
    val coin: Long?,               // 对应的金币（固定金额提现时）
    val currencyCode: String,      // 货币代码
    val remainTimes: Int,          // 剩余可提现次数
    val allTimes: Int,             // 总提现次数
    val commissionAmount: Double,  // 手续费金额
    val commissionCoin: Long,      // 手续费对应的金币
    val commissionSwitch: Int,     // 手续费开关
    val maxWithdrawalAmount: Double?, // 最大提现金额（自定义金额提现时）
    val minWithdrawalAmount: Double?, // 最小提现金额（自定义金额提现时）
    val type: Int                  // 提现类型：1-指定金额，2-自定义金额
) {
    companion object {
        // 提现类型常量
        const val TYPE_FIXED_AMOUNT = 1     // 固定金额提现
        const val TYPE_CUSTOM_AMOUNT = 2    // 自定义金额提现
    }
    
    /**
     * 判断是否为固定金额提现
     */
    fun isFixedAmount(): Boolean {
        return type == TYPE_FIXED_AMOUNT
    }
    
    /**
     * 判断是否为自定义金额提现
     */
    fun isCustomAmount(): Boolean {
        return type == TYPE_CUSTOM_AMOUNT
    }
    
    /**
     * 判断是否需要支付手续费
     */
    fun hasCommission(): Boolean {
        return commissionSwitch == 1
    }
} 