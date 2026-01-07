package com.sg.response

/**
 * 用户信息查询接口响应
 */
data class UserInfoResponse(
    val udCoin: Int,           // 用户金币
    val udAmount: Double,      // 用户金币对应的钱
    val userNo: String,        // 用户码
    val countryCode: String,   // 国家简称（iso-3166）
    val currencyCode: String   // 国家币种（iso-4217）
) 