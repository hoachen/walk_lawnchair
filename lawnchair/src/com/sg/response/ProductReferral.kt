package com.sg.response

/**
 * 推荐产品信息
 */
data class ProductReferral(
    val bundle: String,      // 产品包名
    val title: String,       // 产品标题
    val iconUrl: String,     // 产品图标URL
    val downloadUrl: String, // 产品下载URL
    val desc: String         // 产品描述
) 