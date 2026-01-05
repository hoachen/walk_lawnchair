package com.sg.response

/**
 * 国家信息
 */
data class CountryInfo(
    val countryCode: String,               // 国家代码（iso-3166）
    val countryName: String,               // 国家名称
    val nationalFlagUrl: String,           // 国旗图片URL
    val paymentProvidersLogoUrl: List<String>  // 支付提供商Logo列表
) 