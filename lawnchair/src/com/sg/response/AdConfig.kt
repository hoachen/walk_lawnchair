package com.sg.response

import com.google.gson.annotations.SerializedName

/**
 * 广告配置信息
 */
data class AdConfig(
    @SerializedName("MAX")
    val MAX: Map<String, String>? = null,  // 旧版MAX配置，新版本废弃

    @SerializedName("rvLoadWaitTime")
    val rvLoadWaitTime: Int,              // 激励视频load的等待时长，单位：秒

    @SerializedName("interLoadWaitTime")
    val interLoadWaitTime: Int,           // 插屏load的等待时长，单位：秒

    @SerializedName("accInterval")
    val accInterval: Int,                 // 清理配置，单位：次

    @SerializedName("bidFactor")
    val bidFactor: Map<String, Double>? = null, // 比价系数

    @SerializedName("maxAppKey")
    val maxAppKey: String? = null,        // MAX应用Key

    @SerializedName("ironSourceAppKey")
    val ironSourceAppKey: String? = null, // IronSource应用Key

    @SerializedName("topOnAppKey")
    val topOnAppKey: String? = null,      // TopOn应用Key

    @SerializedName("topOnAppId")
    val topOnAppId: String? = null,       // TopOn应用ID

    @SerializedName("bigoAppId")
    val bigoAppId: String? = null,        // Bigo应用ID
//    val kwaiAppId: String? = null,        // Kwai应用ID

    @SerializedName("kwaiToken")
    val kwaiToken: String? = null,

    @SerializedName("IRONSOURCE")
    val IRONSOURCE: List<String>? = null, // IronSource支持的广告类型
    
    @SerializedName("MAX_ID")
    val maxId: Map<String, List<String>>? = null, // MAX广告单元ID
    
    @SerializedName("TOPON_ID")
    val topOnId: Map<String, List<String>>? = null, // TopOn广告单元ID
    
    @SerializedName("BIGO_ID")
    val bigoId: Map<String, List<String>>? = null, // Bigo广告单元ID
    
    @SerializedName("KWAI_ID")
    val kwaiId: Map<String, List<String>>? = null, // Kwai广告单元ID

    @SerializedName("blockAll")
    val blockAll: Map<String, Map<String, String>>? = null, // 完全屏蔽的广告

    @SerializedName("blockReward")
    val blockReward: Map<String, List<String>>? = null, // 屏蔽奖励的广告

    @SerializedName("blockAdNetwork")
    val blockAdNetwork: Map<String, List<String>>? = null, // 屏蔽的广告网络

    @SerializedName("blockAdSource")
    val blockAdSource: Map<String, List<String>>? = null, // 屏蔽的广告来源

    @SerializedName("stopSubAdSrc")
    val stopSubAdSrc: List<String>? = null ,// 停止的子广告来源，该字段废弃
) {
    companion object {
        // 广告类型常量
        const val AD_TYPE_BANNER = "BANNER"
        const val AD_TYPE_REWARDED_VIDEO = "REWARDED_VIDEO"
        const val AD_TYPE_INTERSTITIAL = "INTERSTITIAL"
        const val AD_TYPE_SPLASH = "SPLASH"
        
        // 广告平台常量
        const val PLATFORM_MAX = "MAX"
        const val PLATFORM_IRONSOURCE = "IRONSOURCE"
        const val PLATFORM_TOPON = "TOPON"
        const val PLATFORM_BIGO = "BIGO"
        const val PLATFORM_KWAI = "KWAI"
    }
    
    /**
     * 获取指定平台和广告类型的广告单元ID列表
     */
    fun getAdUnitIds(platform: String, adType: String): List<String> {
        return when (platform) {
            PLATFORM_MAX -> maxId?.get(adType) ?: emptyList()
            PLATFORM_TOPON -> topOnId?.get(adType) ?: emptyList()
            PLATFORM_BIGO -> bigoId?.get(adType) ?: emptyList()
            PLATFORM_KWAI -> kwaiId?.get(adType) ?: emptyList()
            else -> emptyList()
        }
    }
    
    /**
     * 判断广告单元ID是否被完全屏蔽
     */
    fun isAdUnitBlocked(platform: String, adUnitId: String): Boolean {
        val blockedIds = blockAll?.get(platform) ?: emptyMap()
        return blockedIds.contains(adUnitId)
    }
    
    /**
     * 判断广告单元ID是否屏蔽奖励
     */
    fun isRewardBlocked(platform: String, adUnitId: String): Boolean {
        val blockedIds = blockReward?.get(platform) ?: emptyList()
        return blockedIds.contains(adUnitId)
    }
    
    /**
     * 判断广告网络是否被屏蔽
     */
    fun isAdNetworkBlocked(platform: String, network: String): Boolean {
        val blockedNetworks = blockAdNetwork?.get(platform) ?: emptyList()
        return blockedNetworks.contains(network)
    }
} 