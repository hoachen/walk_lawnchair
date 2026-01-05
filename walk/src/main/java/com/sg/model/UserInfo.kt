package com.sg.model

import com.sg.response.LoginResponse
import com.sg.response.UserInfoResponse

data class UserInfo(
    val accessToken: String? = null,   // 登录令牌
    val udCoin: Int,           // 用户金币
    val udAmount: Double,      // 用户金币对应的钱
    val countryCode: String,   // 国家简称（iso-3166）
    val userNo: String,        // 用户码
    val currencyCode: String   // 国家币种（iso-4217）
)

fun LoginResponse.toUserInfo(): UserInfo {
    return UserInfo(
        accessToken = this.accessToken,
        udCoin = this.udCoin,
        udAmount = this.udAmount,
        countryCode = this.countryCode,
        userNo = this.userNo,
        currencyCode = this.currencyCode
        )
}

fun UserInfoResponse.toUserInfo(): UserInfo {
    return UserInfo(
        udCoin = this.udCoin,
        udAmount = this.udAmount,
        countryCode = this.countryCode,
        userNo = this.userNo,
        currencyCode = this.currencyCode
    )
}

