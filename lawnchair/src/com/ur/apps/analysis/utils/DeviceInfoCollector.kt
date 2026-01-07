package com.ur.apps.analysis.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicReference


/**
 * 设备信息收集器
 * 负责收集各种设备信息并以数据对象形式返回
 * 不直接操作JSON对象，只提供原始数据
 */
class DeviceInfoCollector private constructor() {

    private var context: Context? = null

    // 缓存的广告ID
    private val cachedAdvertisingId = AtomicReference<String>()
    // 缓存的安装来源信息
    private val cachedReferrer = AtomicReference<String>()
    // 服务器时间
    private var serverTime: Long = 0

    fun initContext(context: Context) {
        this.context = context.applicationContext
        // 异步加载广告ID
        loadAdvertisingId()
        // 尝试加载安装来源信息
        loadReferrer()
    }
    
    /**
     * 异步加载广告ID
     */
    private fun loadAdvertisingId() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val advertisingId = AdvertisingIdClient.getGoogleAdId(context)
                Log.i(TAG , "fetch advertisingId$advertisingId")
                cachedAdvertisingId.set(advertisingId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading advertising ID", e)
                cachedAdvertisingId.set("")
            }
        }
    }
    
    /**
     * 加载安装来源信息
     */
    private fun loadReferrer() {
        try {
            val installerPackageName = context!!.packageManager.getInstallerPackageName(context!!.packageName)
            cachedReferrer.set(installerPackageName ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installer package name", e)
            cachedReferrer.set("")
        }
    }




    /**
     * 获取屏幕相关信息
     */
    fun getScreenInfo(): ScreenInfo {
        try {
            fun getScreenSizeViaWindowManager(context: Context): Pair<Int, Int> {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API 30+ 使用新方式获取
                    val bounds = windowManager.currentWindowMetrics.bounds
                    Pair(bounds.width(), bounds.height())
                } else {
                    // 旧版本兼容方案
                    val display = windowManager.defaultDisplay
                    val metrics = DisplayMetrics()
                    display.getRealMetrics(metrics)
                    Pair(metrics.widthPixels, metrics.heightPixels)
                }
            }



            val windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
//                context.display?.getRealMetrics(displayMetrics)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
            return ScreenInfo(
                height = displayMetrics.heightPixels,
                width = displayMetrics.widthPixels,
                densityDpi = displayMetrics.densityDpi
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting screen info", e)
            return ScreenInfo(0, 0, 0)
        }
    }

    /**
     * 获取应用相关信息
     */
    fun getAppInfo(): AppInfo {
        try {
            val packageName = context!!.packageName
            
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context!!.packageManager.getPackageInfo(
                    packageName, 
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context!!.packageManager.getPackageInfo(packageName, 0)
            }
            
            return AppInfo(
                packageName = packageName,
                versionCode = packageInfo.versionCode.toString(),
                versionName = packageInfo.versionName ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting app info", e)
            return AppInfo(
                packageName = context!!.packageName,
                versionCode = "",
                versionName = ""
            )
        }
    }

    /**
     * 获取语言和地区信息
     */
    fun getLocaleInfo(): LocaleInfo {
        try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context!!.resources.configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                context!!.resources.configuration.locale
            }
            
            return LocaleInfo(
                language = locale.language + "_" + locale.country.lowercase(),
                country = locale.country
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting locale info", e)
            return LocaleInfo(
                language = "en_us",
                country = "US"
            )
        }
    }

    /**
     * 获取网络连接类型
     */
    fun getNetworkType(): String {
        try {
            val connectivityManager =
                context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var connectionType = "UNKNOWN"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                
                if (capabilities != null) {
                    connectionType = when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                        else -> "UNKNOWN"
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isConnected) {
                    connectionType = when (networkInfo.type) {
                        ConnectivityManager.TYPE_WIFI -> "WIFI"
                        ConnectivityManager.TYPE_MOBILE -> "MOBILE"
                        ConnectivityManager.TYPE_ETHERNET -> "ETHERNET"
                        else -> "UNKNOWN"
                    }
                }
            }
            
            return connectionType
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting network info", e)
            return "UNKNOWN"
        }
    }

    /**
     * 获取设备标识和品牌相关信息
     */
    fun getDeviceIdentity(): DeviceIdentity {
        try {
            // 获取Android ID
            val androidId = Settings.Secure.getString(
                context!!.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
            
            return DeviceIdentity(
                androidId = androidId,
                brand = Build.BRAND,
                model = Build.MODEL
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting device identity info", e)
            return DeviceIdentity(
                androidId = "",
                brand = "unknown",
                model = "unknown"
            )
        }
    }

    /**
     * 检测设备是否已root
     */
    fun isDeviceRooted(): Boolean {
        try {
            val buildTags = Build.TAGS
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            
            if (buildTags != null && buildTags.contains("test-keys")) {
                return true
            }
            
            for (path in paths) {
                if (java.io.File(path).exists()) {
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root status", e)
            return false
        }
    }

    /**
     * 检测设备是否使用VPN连接
     */
    fun isVpnConnected(): Boolean {
        try {
            val connectivityManager =
                context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } else {
                @Suppress("DEPRECATION")
                val networks = connectivityManager.allNetworks
                for (network in networks) {
                    @Suppress("DEPRECATION")
                    val networkInfo = connectivityManager.getNetworkInfo(network)
                    if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_VPN && networkInfo.isConnected) {
                        return true
                    }
                }
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN connection", e)
            return false
        }
    }
    
    /**
     * 检测是否开启了ADB调试
     * @return Boolean, true表示开启，false表示关闭
     */
    fun isAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context!!.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ADB status", e)
            false
        }
    }
    
    /**
     * 检测是否开启了辅助功能
     * @return Boolean, true表示开启了至少一个辅助功能服务，false表示没有
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context!!.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return !enabledServicesSetting.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility services", e)
            return false
        }
    }
    
    /**
     * 获取移动国家码(MCC)
     * @return String 移动国家码，如果无法获取则返回空字符串
     */
    fun getMobileCountryCode(): String {
        try {
            val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // 检查权限
            if (context!!.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return ""
            }
            
            val networkOperator = telephonyManager.networkOperator
            if (networkOperator.isNotEmpty() && networkOperator.length >= 3) {
                return networkOperator.substring(0, 3)
            }
            
            return ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MCC", e)
            return ""
        }
    }
    
    /**
     * 生成验证签名(VS)
     * @param bundle 应用包名
     * @param svn 服务版本号
     * @param vn 应用版本名称
     * @param ts 时间戳
     * @return String MD5签名
     */
    fun generateVerifySign(bundle: String, svn: String, vn: String, ts: Long): String {
        val salt = "ZjTinpENsk_WOhGZ"
        val input = "$bundle$svn$vn$ts$salt"
        return md5(input)
    }
    
    /**
     * 计算MD5
     */
    private fun md5(input: String): String {
        try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating MD5", e)
            return ""
        }
    }
    
    /**
     * 设置服务器时间
     * @param serverTime 服务器时间（毫秒）
     */
    fun setServerTime(serverTime: Long) {
        this.serverTime = serverTime
    }
    
    /**
     * 获取服务器时间
     * @return Long 服务器时间，如果未设置则返回0
     */
    fun getServerTime(): Long {
        return serverTime
    }
    
    /**
     * 获取SDK版本信息
     * @return SdkInfo SDK版本信息
     */
    fun getSdkInfo(): SdkInfo {
        // 这里返回自定义的SDK版本信息，实际项目中应该根据实际SDK版本设置
        return SdkInfo(
            versionCode = "1",
            versionName = "1.0.0"
        )
    }
    
    /**
     * 获取广告ID (Google Advertising ID)
     * @return String 广告ID，如果不可用则返回空字符串
     */
    fun getAdvertisingId(): String {
        return cachedAdvertisingId.get() ?: ""
    }
    
    /**
     * 获取安装来源信息
     * @return String 安装来源包名，例如"com.android.vending"表示Google Play
     */
    fun getReferrer(): String {
        return cachedReferrer.get() ?: ""
    }
    
    /**
     * 检测是否使用模拟器
     * @return Boolean true表示使用模拟器，false表示使用真机
     */
    fun isEmulator(): Boolean {
        return try {
            // 检查构建属性
            (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.BOARD.contains("android_x86")
                || Build.BOARD.contains("sdk"))
                // 检查特定设备属性
                || checkEmulatorFiles()
                // 检查x86架构
                || Build.SUPPORTED_ABIS[0].contains("x86")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking emulator status", e)
            false
        }
    }
    
    /**
     * 检查模拟器特定文件
     */
    private fun checkEmulatorFiles(): Boolean {
        val files = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/socket/genyd",
            "/dev/socket/baseband_genyd"
        )
        
        for (file in files) {
            if (File(file).exists()) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "DeviceInfoCollector"

        val instance: DeviceInfoCollector by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeviceInfoCollector()
        }
    }
    
    // 数据类定义
    
    /**
     * 屏幕信息数据类
     */
    data class ScreenInfo(
        val height: Int,       // 屏幕高度(像素)
        val width: Int,        // 屏幕宽度(像素)
        val densityDpi: Int    // 屏幕密度
    )
    
    /**
     * 应用信息数据类
     */
    data class AppInfo(
        val packageName: String,   // 应用包名
        val versionCode: String,   // 应用版本号
        val versionName: String    // 应用版本名称
    )
    
    /**
     * 语言和地区信息数据类
     */
    data class LocaleInfo(
        val language: String,  // 设备语言(如zh_cn)
        val country: String    // 设备区域代码(如CN)
    )
    
    /**
     * 设备标识信息数据类
     */
    data class DeviceIdentity(
        val androidId: String, // 设备Android ID
        val brand: String,     // 设备品牌
        val model: String      // 设备型号
    )
    
    /**
     * SDK版本信息数据类
     */
    data class SdkInfo(
        val versionCode: String,   // SDK版本号
        val versionName: String    // SDK版本名称
    )
} 