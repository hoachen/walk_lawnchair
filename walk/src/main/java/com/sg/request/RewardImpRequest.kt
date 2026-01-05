package com.sg.request

/**
 * 广告奖励请求
 */
data class RewardImpRequest(
    val scene: String,      // 枚举类型：COMMON、DOUBLE（如果是领奖后的翻倍入口请求奖励，一定传这个）
    val ecpm: Float,        // 广告ecpm 浮点型
    val atid: String,       // ad trace id生成广告唯一串
    val auid: String,       // max广告id：adUnitId
    val pbTs: Long,         // 视频播放开始时间戳，单位毫秒
    val peTs: Long,         // 视频播放结束时间戳，单位毫秒
    val pDur: Long,         // 视频播放时长，单位毫秒
    val adPlatform: String, // MAX（max广告） IRONSOURCE
    val adFormat: String,   // REWARDED_VIDEO(激励视频)、INTERSTITIAL（插屏）
    val adNetwork: String,  // 本次广告展示的广告来源
    val userSrc: String = "",    // 用户来源
    val lastC: String = ""       // 最后一次点击坐标，示例"113,7664:-32,72734"
)