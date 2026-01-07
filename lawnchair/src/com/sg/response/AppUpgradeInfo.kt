package com.sg.response

/**
 * 应用更新信息
 */
data class AppUpgradeInfo(
    val level: String,  // 更新级别: MANDATORY(强制更新)、RECOMMEND（推荐更新）、NOT(不需要更新)
    val tips: String    // 给用户的提示文案
) {
    companion object {
        // 更新级别常量
        const val LEVEL_MANDATORY = "MANDATORY"    // 强制更新
        const val LEVEL_RECOMMEND = "RECOMMEND"    // 推荐更新
        const val LEVEL_NOT = "NOT"                // 不需要更新
    }
    
    /**
     * 判断是否需要强制更新
     */
    fun isMandatoryUpgrade(): Boolean {
        return level == LEVEL_MANDATORY
    }
    
    /**
     * 判断是否建议更新
     */
    fun isRecommendUpgrade(): Boolean {
        return level == LEVEL_RECOMMEND
    }
    
    /**
     * 判断是否需要更新
     */
    fun needsUpgrade(): Boolean {
        return level == LEVEL_MANDATORY || level == LEVEL_RECOMMEND
    }
} 