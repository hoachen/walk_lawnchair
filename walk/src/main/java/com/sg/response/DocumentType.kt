package com.sg.response

/**
 * 文档类型选项
 */
data class DocumentType(
    val showName: String,      // 展示给用户的可选项
    val requestValue: String   // 提现时，该字段携带的参数
) 