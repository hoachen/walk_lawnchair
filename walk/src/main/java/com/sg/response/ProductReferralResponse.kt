package com.sg.response

/**
 * 推荐产品列表响应
 */
data class ProductReferralResponse(
    val products: List<ProductReferral>  // 推荐产品列表
) {
    /**
     * 判断产品列表是否为空
     */
    fun isEmpty(): Boolean {
        return products.isEmpty()
    }
    
    /**
     * 获取产品数量
     */
    fun getCount(): Int {
        return products.size
    }
} 