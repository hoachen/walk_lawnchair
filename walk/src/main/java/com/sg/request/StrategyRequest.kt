package com.sg.request

/**
 * 策略信息请求
 */
data class StrategyRequest(
    val scene: String  // 请求接口的时机，取值：START、REWARD
) {
    companion object {
        // 场景常量
        const val SCENE_START = "START"  // 启动时
        const val SCENE_REWARD = "REWARD" // 奖励时
    }
} 