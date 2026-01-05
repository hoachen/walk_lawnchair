package com.sg

import com.ur.apps.utils.URLog
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class LoggingInterceptor : Interceptor {
    companion object {
        const val TAG = "okhttp"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 记录请求日志
        val requestLog = buildString {
            append("\n---------- Request ----------\n")
            append("method: ${request.method}\n")
            append("url: ${request.url}\n")
            append("headers: ${request.headers}\n")

            val requestBody = request.body
            if (requestBody != null) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                var charset = Charset.forName("UTF-8")
                val contentType = requestBody.contentType()
                if (contentType != null) {
                    charset = contentType.charset(charset)!!
                }
                append("body:\n${buffer.readString(charset)}\n")
            }
            append("-----------------------------\n")
        }
        URLog.info(TAG, requestLog)

        // 记录请求开始时间
        val startNs = System.nanoTime()

        // 执行请求
        val response = chain.proceed(request)

        // 计算耗时
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        val responseBody = response.body

        // 记录响应日志
        val responseLog = buildString {
            append("\n---------- Response ----------\n")
            append("code: ${response.code}\n")
            append("cost: ${tookMs}ms\n")
            append("url: ${response.request.url}\n")
            append("headers: ${response.headers}\n")


            if (responseBody != null) {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                var charset = Charset.forName("UTF-8")
                val contentType = responseBody.contentType()
                if (contentType != null) {
                    charset = contentType.charset(charset)!!
                }
                if (responseBody.contentLength() != 0L) {
                    append("body (${buffer.size} bytes):\n")
                    append("${buffer.clone().readString(charset)}\n")
                }
            }
            append("-----------------------------\n")
        }
        URLog.info(TAG, responseLog)
        return response.newBuilder()
            .body(responseBody)
            .build()
    }
}