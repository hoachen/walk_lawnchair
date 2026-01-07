package com.ur.apps.lock

import android.content.Context
import com.sg.LoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object LockApiClient {

    private var retrofit: Retrofit? = null

    private val baseUrl: String = "https://lock.ur-tech.xyz/"

    private fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(LockAdInterceptor(context))
                .addInterceptor(LoggingInterceptor())
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create()) // 需要添加依赖：com.squareup.retrofit2:converter-gson:2.9.0
                .build()
        }
        return retrofit!!
    }

    fun getLockService(context: Context) = getClient(context).create(LockService::class.java)


}
