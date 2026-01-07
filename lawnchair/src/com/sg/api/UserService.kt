package com.sg.api

import com.sg.BaseResponse
import com.sg.request.ChangeCountryRequest
import com.sg.response.ChangeCountryResponse
import com.sg.response.CountryListResponse
import com.sg.response.LoginResponse
import com.sg.response.UserInfoResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserService {

    @POST("/user/login")
    fun login(): Call<BaseResponse<LoginResponse>>

    /**
     * 获取用户信息
     * @return 用户信息响应
     */
    @GET("/user/info")
    fun getUserInfo(): Call<BaseResponse<UserInfoResponse>>

    /**
     * 获取可切换的国家列表
     * @return 国家列表响应
     */
    @GET("/user/changeCountryList")
    suspend fun getCountryList(): BaseResponse<CountryListResponse>

    /**
     * 切换用户当前国家
     * @param request 包含目标国家代码的请求
     * @return 切换结果响应
     */
    @POST("/user/changeCountry")
    suspend fun changeCountry(@Body request: ChangeCountryRequest): BaseResponse<ChangeCountryResponse>
}
