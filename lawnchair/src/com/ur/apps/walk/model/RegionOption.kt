package com.ur.apps.walk.model

/**
 * 区域选项数据模型
 */
data class RegionOption(
    val id: Int,
    val code: String,        // 区域代码（如 id, us, cn）
    val name: String,        // 区域名称
    val paymentType: String, // 支付类型（如 DANA）
    val nationFlag: String, // 支付类型（如 DANA）
    var isSelected: Boolean = false // 是否被选中
)