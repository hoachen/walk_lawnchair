package com.ur.apps.lock

import android.content.Context
import com.ur.apps.utils.DeviceInfoManager
import com.ur.apps.utils.HeaderManager
import com.ur.apps.utils.URLog
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException

private const val TAG = "HeaderInterceptor"

class LockAdInterceptor(private val applicationContext: Context) : Interceptor {

    // 设备信息收集器 - 仅用于提供数据，不负责拼装JSON
    private val deviceInfoManager = DeviceInfoManager.getInstance(applicationContext)

    // 文档版本号
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url
        val urlBuilder: HttpUrl.Builder = originalUrl.newBuilder()
        val requestBuilder: Request.Builder = original.newBuilder()
            .method(original.method, original.body)
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")


        val headers = HeaderManager.getDynamicHeaders(deviceInfoManager)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        URLog.i(TAG, "add headers:$headers")
        val request = requestBuilder.build()
        return chain.proceed(request)
    }


    private fun bodyToString(requestBody: RequestBody): String {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }
}
