package com.sg.response

data class LoginResponse(
    val accessToken: String,   // 登录令牌
    val udCoin: Int,           // 用户金币
    val udAmount: Double,      // 用户金币对应的钱
    val countryCode: String,   // 国家简称（iso-3166）
    val userNo: String,        // 用户码
    val currencyCode: String   // 国家币种（iso-4217）
)
