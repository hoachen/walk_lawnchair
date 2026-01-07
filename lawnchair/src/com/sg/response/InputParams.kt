package com.sg.response

/**
 * 输入参数集合
 */
data class FormFieldParams(
    val accountNo: InputField,        // 账号输入字段（必有）
    val fullName: FullNameParam?,     // 姓名输入字段（可选）
    val document: DocumentParam?      // 文档输入字段（可选）
) 