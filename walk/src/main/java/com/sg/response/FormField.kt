package com.sg.response

data class FormField(
    val fieldKey: String = "",/*accountNo*/
    val label: String? = "",/*标签，显⽰给⽤⼾*/
    val type: String = "",/*EMAIL、NUMBER、PHONE_NUMBER、TEXT、SELECT*/
    val placeholder: String? = "",/*"请输⼊持卡⼈真实姓名", // 输⼊框提⽰⽂案*/
    val required: Int = 0,/*// 是否必填*/
    val defaultValue: String? = "",/*"defaultValue": "张三", // 默认值（例如，从⽤⼾实名 信息中带出）*/
    val options: List<SelectOptions>, /*// 当 type 是 'SELECT' 时，提供选项*/
) {
    companion object {
        const val EMAIL = "EMAIL"
        const val NUMBER = "NUMBER"
        const val PHONE_NUMBER = "PHONE_NUMBER"
        const val TEXT = "TEXT"
        const val SELECT = "SELECT"
    }
}

data class SelectOptions(
    val value: String? = "",/*"icbc", // 提交给后端的值*/
    val displayText: String? = "",/*"中国⼯商银⾏" // 显⽰给⽤⼾的⽂本*/
)