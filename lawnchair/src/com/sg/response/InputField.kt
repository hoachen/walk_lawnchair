package com.sg.response

/**
 * 输入字段定义
 */
data class InputField(
    val inputType: String,      // 输入类型: EMAIL、NUMBER、PHONE_NUMBER、TEXT
    val displayText: String     // 文本框提示内容
) {
    companion object {
        // 输入类型常量
        const val TYPE_EMAIL = "EMAIL"
        const val TYPE_NUMBER = "NUMBER"
        const val TYPE_PHONE_NUMBER = "PHONE_NUMBER"
        const val TYPE_TEXT = "TEXT"
    }
} 