package com.ur.apps.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import kotlin.Suppress;

/**
 * 网络工具类
 * 功能：网络状态检测、网络类型判断、连接性测试、网络变化监听
 */
public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    // 网络连接性测试配置
    private static final int CONNECT_TIMEOUT_MS = 3000; // 连接超时时间
    private static final int READ_TIMEOUT_MS = 3000;    // 读取超时时间
    private static final String[] TEST_HOSTS = {
            "8.8.8.8",          // Google DNS
            "114.114.114.114",  // 114 DNS
            "1.1.1.1",          // Cloudflare DNS
            "www.baidu.com"
    };
    private static final int TEST_PORT = 53; // DNS端口
    private static final int TEST_PORT_HTTP = 80; // HTTP端口

    // 网络类型常量
    public static final int NETWORK_TYPE_NONE = -1;
    public static final int NETWORK_TYPE_WIFI = 1;
    public static final int NETWORK_TYPE_2G = 2;
    public static final int NETWORK_TYPE_3G = 3;
    public static final int NETWORK_TYPE_4G = 4;
    public static final int NETWORK_TYPE_5G = 5;
    public static final int NETWORK_TYPE_UNKNOWN = 0;

    // 网络强度等级
    public static final int NETWORK_STRENGTH_NONE = 0;
    public static final int NETWORK_STRENGTH_POOR = 1;
    public static final int NETWORK_STRENGTH_MODERATE = 2;
    public static final int NETWORK_STRENGTH_GOOD = 3;
    public static final int NETWORK_STRENGTH_EXCELLENT = 4;

    // 单例模式
    private static volatile NetworkUtils instance;
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final TelephonyManager telephonyManager;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // 网络监听回调
    private NetworkCallbackImpl networkCallback;

    // 网络状态监听器接口
    public interface NetworkStateListener {
        void onNetworkConnected(boolean isConnected, int networkType);
        void onNetworkStrengthChanged(int strength);
        void onNetworkLost();
    }

    /**
     * 私有构造方法
     */
    @SuppressLint("ServiceCast")
    private NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager)
                this.context.getSystemService(CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager)
                this.context.getApplicationContext().getSystemService(WIFI_SERVICE);
        this.telephonyManager = (TelephonyManager)
                this.context.getSystemService(Context.TELEPHONY_SERVICE);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 获取单例实例
     */
    public static NetworkUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkUtils.class) {
                if (instance == null) {
                    instance = new NetworkUtils(context);
                }
            }
        }
        return instance;
    }

    /**
     * 检查是否有网络连接权限
     */
    @RequiresPermission(ACCESS_NETWORK_STATE)
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            @Suppress(names = "deprecation")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    /**
     * 检查是否是WiFi连接
     */
    @RequiresPermission(ACCESS_NETWORK_STATE)
    public boolean isWifiConnected() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            @Suppress(names = "deprecation")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.isConnected() &&
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }

    /**
     * 检查是否是移动数据连接
     */
    @RequiresPermission(ACCESS_NETWORK_STATE)
    public boolean isMobileDataConnected() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            @Suppress(names = "deprecation")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.isConnected() &&
                    networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
    }

    /**
     * 获取当前网络类型
     * @return 网络类型常量
     */
    @RequiresPermission(allOf = {ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE})
    public int getNetworkType() {
        try {
            if (!isNetworkAvailable()) {
                return NETWORK_TYPE_NONE;
            }

            if (isWifiConnected()) {
                return NETWORK_TYPE_WIFI;
            }

            if (isMobileDataConnected()) {
                return getMobileNetworkType();
            }
        } catch (Exception e) {
            return NETWORK_TYPE_UNKNOWN;
        }

        return NETWORK_TYPE_UNKNOWN;
    }

    /**
     * 获取移动网络类型（2G/3G/4G/5G）
     */
    @SuppressLint("MissingPermission")
    private int getMobileNetworkType() {
        try {
            if (telephonyManager == null) {
                return NETWORK_TYPE_UNKNOWN;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                int dataNetworkType = telephonyManager.getDataNetworkType();
                return convertToNetworkType(dataNetworkType);
            } else {
                @Suppress(names = "deprecation")
                int networkType = telephonyManager.getNetworkType();
                return convertToNetworkType(networkType);
            }
        } catch (Exception e) {
            return NETWORK_TYPE_UNKNOWN;
        }
    }

    /**
     * 转换TelephonyManager网络类型到自定义类型
     */
    private int convertToNetworkType(int telephonyNetworkType) {
        switch (telephonyNetworkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_TYPE_2G;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_TYPE_3G;

            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return NETWORK_TYPE_4G;

            case TelephonyManager.NETWORK_TYPE_NR: // 5G
                return NETWORK_TYPE_5G;

            default:
                return NETWORK_TYPE_UNKNOWN;
        }
    }

    /**
     * 获取网络信号强度（WiFi或移动网络）
     */
    @RequiresPermission(allOf = {ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE})
    public int getNetworkStrength() {
        if (!isNetworkAvailable()) {
            return NETWORK_STRENGTH_NONE;
        }

        if (isWifiConnected()) {
            return getWifiStrength();
        } else if (isMobileDataConnected()) {
            return getMobileSignalStrength();
        }

        return NETWORK_STRENGTH_POOR;
    }

    /**
     * 获取WiFi信号强度
     */
    @SuppressLint("MissingPermission")
    private int getWifiStrength() {
        if (wifiManager == null) {
            return NETWORK_STRENGTH_NONE;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return NETWORK_STRENGTH_NONE;
        }

        int rssi = wifiInfo.getRssi();
        int level = WifiManager.calculateSignalLevel(rssi, 5);

        // 将5级信号转换为4级
        switch (level) {
            case 0: return NETWORK_STRENGTH_NONE;
            case 1: return NETWORK_STRENGTH_POOR;
            case 2: return NETWORK_STRENGTH_MODERATE;
            case 3: return NETWORK_STRENGTH_GOOD;
            case 4: return NETWORK_STRENGTH_EXCELLENT;
            default: return NETWORK_STRENGTH_MODERATE;
        }
    }

    /**
     * 获取移动网络信号强度（简化版本）
     * 注意：实际应用中需要更复杂的逻辑来获取准确的信号强度
     */
    @SuppressLint("MissingPermission")
    private int getMobileSignalStrength() {
        // 这里返回一个模拟值，实际项目中需要实现获取信号强度的逻辑
        // 可以使用TelephonyManager.getSignalStrength()等API

        return NETWORK_STRENGTH_MODERATE; // 默认中等信号
    }

    /**
     * 获取网络连接速度（估算）
     */
    public String getNetworkSpeedDescription() {
        int type = getNetworkType();
        int strength = getNetworkStrength();

        switch (type) {
            case NETWORK_TYPE_WIFI:
                switch (strength) {
                    case NETWORK_STRENGTH_EXCELLENT:
                        return "Excellent WiFi";
                    case NETWORK_STRENGTH_GOOD:
                        return "Good WiFi";
                    case NETWORK_STRENGTH_MODERATE:
                        return "Moderate WiFi";
                    case NETWORK_STRENGTH_POOR:
                        return "Poor WiFi";
                    default:
                        return "WiFi connected";
                }

            case NETWORK_TYPE_5G:
                return "5G network";

            case NETWORK_TYPE_4G:
                switch (strength) {
                    case NETWORK_STRENGTH_EXCELLENT:
                    case NETWORK_STRENGTH_GOOD:
                        return "4G high-speed network";
                    default:
                        return "4G network";
                }

            case NETWORK_TYPE_3G:
                return "3G network";

            case NETWORK_TYPE_2G:
                return "2G network";

            default:
                return "Unknown network";
        }
    }

    /**
     * 测试网络连接性（实际连接测试）
     */
    public void testNetworkConnectivity(ConnectivityTestCallback callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onTestResult(false, "Network unavailable");
            }
            return;
        }

        executorService.execute(() -> {
            boolean success = false;
            String message = "Connection test failed";
            long startTime = System.currentTimeMillis();

            // 尝试连接多个测试服务器
            for (String host : TEST_HOSTS) {
                if (testConnection(host, TEST_PORT)) {
                    success = true;
                    message = String.format("Connected - %s (%.0fms)",
                            host, System.currentTimeMillis() - startTime);
                    break;
                }

                // 如果DNS端口失败，尝试HTTP端口
                if (testConnection(host, TEST_PORT_HTTP)) {
                    success = true;
                    message = String.format("Connected - %s:80 (%.0fms)",
                            host, System.currentTimeMillis() - startTime);
                    break;
                }
            }

            final boolean finalSuccess = success;
            final String finalMessage = message;

            if (callback != null) {
                mainHandler.post(() -> callback.onTestResult(finalSuccess, finalMessage));
            }
        });
    }

    /**
     * 测试单个主机连接
     */
    private boolean testConnection(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            // 简单发送测试数据
            socket.getOutputStream().write(0x00);
            return socket.isConnected() && !socket.isClosed();
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 注册网络状态监听
     */
    @RequiresPermission(ACCESS_NETWORK_STATE)
    public void registerNetworkCallback(NetworkStateListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build();

            networkCallback = new NetworkCallbackImpl(listener);
            connectivityManager.registerNetworkCallback(request, networkCallback);
        } else {
            // 对于低版本，可以使用定时轮询或广播监听
            // 这里简化为不实现
        }
    }

    /**
     * 注销网络状态监听
     */
    public void unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    /**
     * 检查是否是VPN连接
     */
    @RequiresPermission(ACCESS_NETWORK_STATE)
    public boolean isVPNConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        } else {
            @Suppress(names = "deprecation")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.isConnected() &&
                    networkInfo.getType() == ConnectivityManager.TYPE_VPN;
        }
    }

    /**
     * 获取网络运营商名称
     */
    @SuppressLint("MissingPermission")
    public String getNetworkOperatorName() {
        if (telephonyManager == null) {
            return "Unknown";
        }

        String operatorName = telephonyManager.getNetworkOperatorName();
        if (operatorName == null || operatorName.trim().isEmpty()) {
            return "Unknown operator";
        }

        return operatorName;
    }

    /**
     * 判断是否是漫游状态
     */
    @SuppressLint("MissingPermission")
    public boolean isNetworkRoaming() {
        return telephonyManager != null && telephonyManager.isNetworkRoaming();
    }

    /**
     * 异步测试网络连接性（返回Future）
     */
    public Future<Boolean> testNetworkConnectivityAsync() {
        return executorService.submit(() -> {
            if (!isNetworkAvailable()) {
                return false;
            }

            for (String host : TEST_HOSTS) {
                if (testConnection(host, TEST_PORT)) {
                    return true;
                }
            }

            return false;
        });
    }

    /**
     * 网络连接测试回调接口
     */
    public interface ConnectivityTestCallback {
        void onTestResult(boolean success, String message);
    }

    /**
     * 网络回调实现类（API 21+）
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        private final NetworkStateListener listener;

        NetworkCallbackImpl(NetworkStateListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            if (listener != null) {
                // 这里需要实际获取网络类型
                listener.onNetworkConnected(true, NETWORK_TYPE_UNKNOWN);
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            if (listener != null) {
                listener.onNetworkLost();
            }
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                                          @NonNull NetworkCapabilities networkCapabilities) {
            if (listener != null) {
                // 可以在这里更新网络强度等信息
                listener.onNetworkStrengthChanged(NETWORK_STRENGTH_MODERATE);
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        unregisterNetworkCallback();
        executorService.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 快速检查网络状态（简化方法）
     */
    public static boolean isConnected(Context context) {
        return getInstance(context).isNetworkAvailable();
    }
}
