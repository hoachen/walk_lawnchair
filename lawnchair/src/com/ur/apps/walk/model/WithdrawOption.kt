package com.ur.apps.walk.model

/**
 * 提现选项数据模型
 */
data class WithdrawOption(
    val id: Long,
    val type: String,           // 货币类型（如 RP, $）
    val amount: Double,            // 金额
    val coinsRequired: Long,     // 所需金币数量
    val remainCount: Int,        // 每日限制次数
    var isSelected: Boolean = false // 是否被选中
) 