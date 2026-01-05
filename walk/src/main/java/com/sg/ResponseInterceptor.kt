package com.sg

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * 响应拦截器
 * 负责解密服务器返回的加密响应
 */
class ResponseInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "ResponseInterceptor"
        public const val SUCCESS = "2000"
        private const val ERROR_CODE_INVALID_JSON = "1001"
        private const val ERROR_CODE_DECRYPT_FAILED = "1002"
        private const val ERROR_CODE_ACCESS_TOKEN_INVALID = "4001"
        private const val ERROR_CODE_PRODUCT_BANNED = "4100"
        
        // 用于通知UI层的接口
        interface ResponseErrorListener {
            /**
             * Token失效时的回调
             */
            fun onTokenInvalid()
            
            /**
             * 产品被禁用时的回调
             */
            fun onProductBanned()
        }
        
        // 所有注册的监听器
        private val listeners = mutableListOf<ResponseErrorListener>()
        
        /**
         * 添加响应错误监听器
         * @param listener 要添加的监听器
         */
        fun addListener(listener: ResponseErrorListener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
        
        /**
         * 移除响应错误监听器
         * @param listener 要移除的监听器
         */
        fun removeListener(listener: ResponseErrorListener) {
            listeners.remove(listener)
        }
        
        /**
         * 清除所有监听器
         */
        fun clearListeners() {
            listeners.clear()
        }
        
        /**
         * 通知所有监听器Token已失效
         */
        private fun notifyTokenInvalid() {
            for (listener in listeners) {
                listener.onTokenInvalid()
            }
        }
        
        /**
         * 通知所有监听器产品已被禁用
         */
        private fun notifyProductBanned() {
            for (listener in listeners) {
                listener.onProductBanned()
            }
        }
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val responseBody = originalResponse.body
        
        // 如果响应体为空，直接返回原始响应
        if (responseBody == null) {
            return originalResponse
        }
        
        try {
            // 读取加密响应体
            val encryptedBody = responseBody.string()
            
            // 检查加密内容是否为空
            if (encryptedBody.isBlank()) {
                Log.w(TAG, "Response body is empty")
                return originalResponse
            }
            Log.i("okhttp", "返回的body:\n$encryptedBody")
            // 解密获得明文 JSON 字符串
            val decryptedBody = CryptoUtils.decrypt(encryptedBody)
            
            // 检查是否是特殊错误码
            try {
                val jsonObject = JSONObject(decryptedBody)
                val code = jsonObject.optString("code", "")
                
                // 处理特定错误码
                when (code) {
                    ERROR_CODE_ACCESS_TOKEN_INVALID -> {
                        Log.w(TAG, "Detected invalid token (4001)")
                        // 清除本地token
                        UserManager.instance.clearToken()
                        // 通知UI层
                        notifyTokenInvalid()
                    }
                    ERROR_CODE_PRODUCT_BANNED -> {
                        Log.w(TAG, "Detected product banned (4100)")
                        // 通知UI层
                        notifyProductBanned()
                    }
                }
            } catch (e: JSONException) {
                // JSON解析错误，继续使用原有的错误处理逻辑
                Log.e(TAG, "Error parsing JSON for error code detection", e)
            }
            
            // 验证解密后的内容是有效的JSON（可选）
            try {
                JSONObject(decryptedBody)
            } catch (e: JSONException) {
                Log.e(TAG, "Decrypted content is not valid JSON", e)
                // 使用ApiException处理无效JSON错误
                return createErrorResponse(
                    originalResponse,
                    ApiException(ERROR_CODE_INVALID_JSON, "Invalid response format")
                )
            }
            
            // 创建新的响应体并返回
            val mediaType = "application/json; charset=UTF-8".toMediaTypeOrNull()
            val newBody = decryptedBody.toResponseBody(mediaType)
            
            return originalResponse.newBuilder()
                .body(newBody)
                .build()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error processing response", e)
            // 使用ApiException处理解密失败错误
            return createErrorResponse(
                originalResponse,
                ApiException(ERROR_CODE_DECRYPT_FAILED, "Failed to decrypt response")
            )
        }
    }
    
    /**
     * 创建包含错误信息的响应
     * @param originalResponse 原始响应
     * @param exception API异常
     * @return 包含错误信息的响应
     */
    private fun createErrorResponse(originalResponse: Response, exception: ApiException): Response {
        val errorJson = JSONObject().apply {
            put("error", exception.message)
            put("code", exception.code)
        }.toString()
        
        return originalResponse.newBuilder()
            .body(errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
    }
}
