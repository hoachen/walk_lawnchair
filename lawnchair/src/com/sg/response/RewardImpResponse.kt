package com.sg.response

/**
 * 广告奖励响应
 */
data class RewardImpResponse(
    val udAmount: String,        // 用户总金额
    val rewardAmount: String,    // 本次奖励金额
    val udCoin: String,          // 用户总金币
    val rewardCoin: String,      // 本次奖励金币
    val currencyCode: String,    // 货币代码，例如"IDR"
    val doubleSwitch: String     // 翻倍开关，"t"表示可翻倍，"f"表示不可翻倍
) {
    /**
     * 判断是否可以进行翻倍奖励
     * @return 如果可以翻倍返回true，否则返回false
     */
    fun canDouble(): Boolean {
        return doubleSwitch == "t"
    }
} 