package com.sg.response

/**
 * 文档参数定义
 */
data class DocumentParam(
    val require: String,                // 是否必填，"t"表示必填
    val documentType: List<DocumentType>, // 文档类型选项列表
    val documentId: InputField          // 文档ID输入字段定义
) {
    /**
     * 判断是否必须填写文档信息
     * @return 如果必须填写返回true，否则返回false
     */
    fun isRequired(): Boolean {
        return require == "t"
    }
} 