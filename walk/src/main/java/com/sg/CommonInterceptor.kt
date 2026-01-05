package com.sg

import android.content.Context
import android.os.Build
import android.util.Log
import com.ur.apps.analysis.shuzhi.SZSdkImpl
import com.ur.apps.analysis.tenjin.TenjinManager
import com.ur.apps.analysis.utils.DeviceInfoCollector
import com.ur.apps.utils.URLog
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.util.TimeZone

private const val TAG = "CommonInterceptor"

class CommonInterceptor(private val applicationContext: Context) : Interceptor {

    // 设备信息收集器 - 仅用于提供数据，不负责拼装JSON
    private val deviceInfoCollector = DeviceInfoCollector.instance

    // 文档版本号
    private val serviceVersion = "2.2.4"

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url
        val urlBuilder: HttpUrl.Builder = originalUrl.newBuilder()
            .addQueryParameter("svn", serviceVersion)
            .addQueryParameter("bundle", applicationContext.packageName)
        try {
            val infoJson = buildInfoJson()
            val encryptedInfo: String = CryptoUtils.encrypt(infoJson)
            Log.i("okhttp", "before encryption \n $infoJson \n after encryption\n $encryptedInfo")
            urlBuilder.addQueryParameter("info", encryptedInfo)
        } catch (e: Exception) {
            Log.e("CommonInterceptor", "error encrypt info json", e)
        }

        val encryptedBody: RequestBody? = if (original.body != null) {
            // 加密请求体
            val originalBody = bodyToString(original.body!!)
            URLog.d(TAG, "request body($originalBody)")
            val encryptBody = CryptoUtils.encrypt(originalBody)
            encryptBody
                .toRequestBody("application/json; charset=utf-8".toMediaType())
        } else {
            original.body
        }
        val requestBuilder: Request.Builder = original.newBuilder()
            .method(original.method, encryptedBody)
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        // 添加用户 token，如果存在
        val token: String = UserManager.instance.token // 使用优化后的单例，不再需要!!
        if (token.isNotEmpty()) {
            requestBuilder.header("access_token", token)
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }

    private fun bodyToString(requestBody: RequestBody): String {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }


    /**
     * 构建设备信息JSON
     * 所有参数在此函数中直接添加到JSON对象，便于直观查看
     */
    private fun buildInfoJson(): String {
        val json = JSONObject()

        // ============ 屏幕信息 ============
        // 获取屏幕高度、宽度和密度
        val screenInfo = deviceInfoCollector.getScreenInfo()
        json.put("h", screenInfo.height)         // 屏幕高度(像素)
        json.put("w", screenInfo.width)          // 屏幕宽度(像素)
        json.put("dpi", screenInfo.densityDpi)   // 屏幕密度

        // ============ 应用信息 ============
        // 获取应用包名、版本号和版本名称
        val appInfo = deviceInfoCollector.getAppInfo()
        json.put("bundle", appInfo.packageName)  // 应用包名
        json.put("vc", appInfo.versionCode)      // 应用版本号
        json.put("vn", appInfo.versionName)      // 应用版本名称
//        json.put("sdkvc", appInfo.versionCode)      // 不需要传
//        json.put("sdkvn", appInfo.versionName)      // 不需要传
        // ============ 语言和地区信息 ============
        // 获取设备语言和区域信息
        val localeInfo = deviceInfoCollector.getLocaleInfo()
        json.put("language", localeInfo.language) // 设备语言(如zh_cn)
        json.put("locale", localeInfo.country)    // 设备区域代码(如CN)

        // ============ 系统信息 ============
        json.put("os", "Android")                          // 操作系统类型
        json.put("osv", Build.VERSION.SDK_INT.toString())  // Android系统版本(API级别)

        // 客户端时间戳，单位毫秒
        val timestamp = System.currentTimeMillis()
        json.put("ts", timestamp)

        json.put("tz", TimeZone.getDefault().id)           // 时区信息

        // ============ 网络信息 ============
        // 获取网络连接类型
        val networkType = deviceInfoCollector.getNetworkType()
        json.put("contype", networkType)  // 网络连接类型(WIFI/MOBILE/ETHERNET/UNKNOWN)

        // ============ 设备标识信息 ============
        // 获取设备ID和品牌信息
        val deviceIdentity = deviceInfoCollector.getDeviceIdentity()
        json.put("androidid", deviceIdentity.androidId)  // 设备Android ID
        json.put("brand", deviceIdentity.brand)          // 设备品牌
        json.put("model", deviceIdentity.model)          // 设备型号

        // ============ 安全信息 ============
        // 检测设备是否Root和是否使用VPN
        val isRooted = if (TenjinManager.deviceInfo != null) {
            TenjinManager.deviceInfo?.isRooted == true
        } else {
            deviceInfoCollector.isDeviceRooted()
        }
        val isVpnConnected = if (TenjinManager.deviceInfo !== null) {
            TenjinManager.deviceInfo?.isVpnOpen == true
        } else {
            deviceInfoCollector.isVpnConnected()
        }
        val isRunEmu = if (TenjinManager.deviceInfo !== null) {
            TenjinManager.deviceInfo?.isRunEmu == true
        } else {
            deviceInfoCollector.isVpnConnected()
        }
        json.put("root", if (isRooted) "true" else "false")         // 设备是否Root(1=是，0=否)
        json.put("vpn", if (isVpnConnected) "true" else "false")    // 是否使用VPN连接(1=是，0=否)

        // ADB调试状态
        val isAdbEnabled = deviceInfoCollector.isAdbEnabled()
        json.put("adb", if (isAdbEnabled) "true" else "false")      // 是否开启ADB调试(1=是，0=否)

        // 辅助功能状态
        val isA11yEnabled = deviceInfoCollector.isAccessibilityServiceEnabled()
        json.put("a11y", if (isA11yEnabled) "true" else "false")    // 是否开启辅助功能(1=是，0=否)

        // 移动国家码
        val mcc = deviceInfoCollector.getMobileCountryCode()
        json.put("mcc", mcc) // 移动国家码

        // 验证签名 verify sign
        val verifySign = deviceInfoCollector.generateVerifySign(
            appInfo.packageName,
            serviceVersion,
            appInfo.versionName,
            timestamp
        )
        json.put("vs", verifySign)                           // 验证签名
//        json.put("referrer", "")
        // 服务器时间
        val serverTime = deviceInfoCollector.getServerTime()
        if (serverTime > 0) {
            json.put("st", serverTime)                       // 服务器时间
        }
        json.put("ifa", deviceInfoCollector.getAdvertisingId())
        json.put("referrer", deviceInfoCollector.getReferrer())
        json.put("ue", if (isRunEmu) "1" else "0")
        json.put("smdid", SZSdkImpl.getShuZiQueryId())
        return json.toString()
    }
}
