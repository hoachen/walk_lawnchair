package com.ur.apps.walk.model

/**
 * 截图选项数据模型
 */
data class ScreenshotOption(
    val id: Int,
    val code: String,        // 选项代码
    val name: String,        // 选项名称
    val region: String,      // 区域/货币
    val isDefault: Boolean,  // 是否为默认选项
    var isSelected: Boolean = false // 是否被选中
)