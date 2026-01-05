package com.sg

import android.content.Context
import com.sg.api.CommonService
import com.sg.api.RewardService
import com.sg.api.UserService
import com.sg.api.WithdrawalService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private var retrofit: Retrofit? = null

    private val baseUrl: String = "https://serve.ridgeflare.com"

    private fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(CommonInterceptor(context))
                .addInterceptor(LoggingInterceptor())
                .addInterceptor(ResponseInterceptor()) // 如有需要可添加日志拦截器等
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create()) // 需要添加依赖：com.squareup.retrofit2:converter-gson:2.9.0
                .build()
        }
        return retrofit!!
    }

    fun getUserService(context: Context) = getClient(context).create(UserService::class.java)

    fun getCommonService(context: Context) = getClient(context).create(CommonService::class.java)

    fun getRewardService(context: Context) = getClient(context).create(RewardService::class.java)

    fun getWithdrawalService(context: Context) = getClient(context).create(WithdrawalService::class.java)


}
