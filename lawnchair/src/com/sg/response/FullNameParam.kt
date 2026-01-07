package com.sg.response

/**
 * 姓名参数定义
 */
data class FullNameParam(
    val require: String,     // 是否必填，"t"表示必填
    val displayText: String  // 文本框提示内容
) {
    /**
     * 判断是否必须填写姓名
     * @return 如果必须填写返回true，否则返回false
     */
    fun isRequired(): Boolean {
        return require == "t"
    }
} 