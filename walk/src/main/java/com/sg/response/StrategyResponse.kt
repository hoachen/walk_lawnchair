package com.sg.response

import com.google.gson.annotations.SerializedName

/**
 * 策略信息响应
 */

data class StrategyResponse(
    @SerializedName("earningSwitch")
    val earningSwitch: Int = 1,           // 网赚功能 0关，1开，2: 等待后再次请求
    
    @SerializedName("afSw")
    val afSw: Int = 1,        // 广告功能开关，0关，1开
    
    @SerializedName("dsaSw")
    val directShowAdSwitch: Int,      // 是否有直接观看广告的入口，0关，1开

    @SerializedName("interactiveAdNspc")
    val interactiveAdNspc: String?,
    val interactiveAdUrl: String?,    // 互动广告链接，需要替换宏:__GAID__
    val groupLink: String?,           // 群组链接
    val rrRatio: Double,              // 上报收入比率
    val appUpgrade: AppUpgradeInfo,   // 应用更新信息
    val contactUs: ContactUsInfo,     // 联系我们信息

    @SerializedName("adConf")
    val adConfig: AdConfig,           // 广告配置

    @SerializedName("aoa")     // 应用外广告开关
    val aoa : AOA? = null

) {
    companion object {
        // 网赚功能开关常量
        const val EARNING_SWITCH_OFF = 0       // 关闭
        const val EARNING_SWITCH_ON = 1        // 开启
        const val EARNING_SWITCH_WAIT = 2      // 稍后再试
        
        // 广告功能开关常量
        const val AD_FUNCTION_OFF = 0          // 关闭
        const val AD_FUNCTION_ON = 1           // 开启
        
        // 直接观看广告入口开关常量
        const val DIRECT_SHOW_AD_OFF = 0       // 关闭
        const val DIRECT_SHOW_AD_ON = 1        // 开启
    }
    
    /**
     * 判断网赚功能是否开启
     */
    fun isEarningEnabled(): Boolean {
        return earningSwitch == EARNING_SWITCH_ON
    }
    
    /**
     * 判断是否需要等待再次请求网赚功能
     */
    fun shouldWaitForEarning(): Boolean {
        return earningSwitch == EARNING_SWITCH_WAIT
    }
    
    /**
     * 判断广告功能是否开启
     */
    fun isAdFunctionEnabled(): Boolean {
        return afSw == AD_FUNCTION_ON
    }
    
    /**
     * 判断直接观看广告入口是否开启
     */
    fun isDirectShowAdEnabled(): Boolean {
        return directShowAdSwitch == DIRECT_SHOW_AD_ON
    }
    
    /**
     * 判断是否有互动广告链接
     */
    fun hasInteractiveAd(): Boolean {
        return !interactiveAdUrl.isNullOrEmpty()
    }
    
    /**
     * 判断是否有群组链接
     */
    fun hasGroupLink(): Boolean {
        return !groupLink.isNullOrEmpty()
    }
    
    /**
     * 获取替换了GAID的互动广告URL
     * @param gaid 谷歌广告ID
     * @return 替换了GAID的URL，如果没有互动广告URL则返回null
     */
    fun getInteractiveAdUrlWithGaid(gaid: String): String? {
        return interactiveAdUrl?.replace("__GAID__", gaid)
    }


    fun isEnableAoa(): Boolean {
        return aoa == null || aoa.sw == 1
    }
} 