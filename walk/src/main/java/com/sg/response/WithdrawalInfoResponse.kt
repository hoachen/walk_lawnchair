package com.sg.response

/**
 * 提现信息响应
 */
data class WithdrawalInfoResponse(
    val udAmount: Double = 0.0,              // 用户总金额
    val udAvailableAmount: Double = 0.0,     // 可领取的钱
    val udPendingAmount: Double = 0.0,       // pending的钱
    val udCoin: Long = 0,                  // 用户总金币
    val countryCode: String = "",           // 国家代码
    val currencyCode: String = "",          // 货币代码
    val nationalFlagUrl: String = "",       // 国旗URL
    val paymentProviders: List<PaymentProvider> = listOf() // 支付提供商列表
) {
    fun isInvalid(): Boolean {
        return udAmount == 0.0 && udAvailableAmount == 0.0 && udPendingAmount == 0.0 && udCoin == 0L
                && paymentProviders.isEmpty()
    }
}