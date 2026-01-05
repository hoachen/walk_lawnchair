package com.sg.request

/**
 * 提现信息（请求和响应通用）
 */
data class WithdrawalInfo(
    val withdrawalType: Int,              // 提现类型，1-服务端指定金额，2-自定义金额
    val email:String,
    val accountNo: String,                // 用户账户，从2.0.1版本开始，所有的账号都传这个字段
    val fullName: String? = null,         // 用户全名，是否传根据下发的提现信息判断
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val documentType: String? = null,     // 用户证件类型，是否传根据下发的提现信息判断
    val documentId: String? = null,       // 用户证件号码，是否传根据下发的提现信息判断
    val customWithdrawalAmount: Double? = null, // 自定义金额，仅当withdrawalType=2时有效
    val paymentProviderId: Long,          // 支付服务商id
    val paymentProviderItemId: Long       // 支付服务商下配置项的id
) {
    companion object {
        // 提现类型常量
        const val TYPE_FIXED_AMOUNT = 1     // 服务端指定金额
        const val TYPE_CUSTOM_AMOUNT = 2    // 自定义金额
    }

    /**
     * 判断是否为自定义金额提现
     * @return 如果是自定义金额提现返回true，否则返回false
     */
    fun isCustomAmount(): Boolean {
        return withdrawalType == TYPE_CUSTOM_AMOUNT
    }
} 