package com.sg.response

/**
 * 联系我们信息
 */
data class ContactUsInfo(
    val btnText: String,  // button的文案
    val text: String,     // 点击button后，展示给用户的文案
    val tg: String?,      // 点击button后，展示给用户的tg账号，没有则不显示该联系方式
    val email: String?    // 点击button后，展示给用户的email，没有则不显示该联系方式
) {
    /**
     * 判断是否有Telegram联系方式
     */
    fun hasTelegram(): Boolean {
        return !tg.isNullOrEmpty()
    }
    
    /**
     * 判断是否有邮箱联系方式
     */
    fun hasEmail(): Boolean {
        return !email.isNullOrEmpty()
    }
} 