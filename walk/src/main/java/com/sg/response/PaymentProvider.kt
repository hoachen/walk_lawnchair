package com.sg.response

/**
 * 支付提供商
 */
data class PaymentProvider(
    val id: Long,                   // 对应paymentProviderId
    val name: String,               // 支付提供商名称
    val logoUrl: String,            // Logo URL
    val accountNoType: String,      // 账号类型（已废弃，使用inputParams）
    val formFields: List<FormField>,
    val items: List<WithdrawalItem> // 提现选项列表
) 