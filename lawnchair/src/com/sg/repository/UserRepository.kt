package com.sg.repository

import android.content.Context
import com.sg.ApiClient
import com.sg.model.UserInfo
import com.sg.model.toUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val context: Context) {

    /**
     * 用户登录
     */
    suspend fun login(): UserInfo? = withContext(Dispatchers.IO) {
        try {
            val userService = ApiClient.getUserService(context)
            val response = userService.login().execute().body()
            response?.data?.toUserInfo()
        } catch (e: Exception) {
            e.printStackTrace()
             null
        }
    }


    /**
     * 刷新用户信息
     */
    suspend fun getUserInfo(): UserInfo? = withContext(Dispatchers.IO) {
        try {
            val userService = ApiClient.getUserService(context)
            val response = userService.getUserInfo().execute().body()
            response?.data?.toUserInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        const val TAG = "UserRepository"
    }

}