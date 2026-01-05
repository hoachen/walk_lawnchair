package com.ur.apps.utils;


import com.sg.UserManager;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求头管理器
 * 负责管理和构建HTTP请求的头部信息
 */
public final class HeaderManager {
    public static Map<String, String> defaultHeaders = new HashMap<>();
    private static Map<String, String> dynamicHeaders = new HashMap<>();
    private static String authorizationToken = "OXD3m6iQWc3w2wQ+";

    /**
     * 初始化设备信息相关的请求头
     * @param deviceInfo 设备信息对象
     */
    public static void initializeDeviceHeaders(DeviceInfoManager deviceInfo) {
        String channelId = UserManager.Companion.getInstance().getUserAdNetwork();
        defaultHeaders.put("X-Device-ID", deviceInfo.getDeviceId());
        defaultHeaders.put("X-Device-GAID", deviceInfo.getAdvertisingId() != null ? deviceInfo.getAdvertisingId() : "");
        defaultHeaders.put("X-Device-PKG", deviceInfo.getPackageName());
        defaultHeaders.put("X-Device-CHANNEL", channelId);
        defaultHeaders.put("Authorization", authorizationToken);
        defaultHeaders.put("accept", "application/json");
        defaultHeaders.put("Content-Type", "application/json");
        
        URLog.debug("HeaderManager", "Headers initialized: X-Device-ID=" + deviceInfo.getDeviceId() +
                ", X-Device-GAID=" + deviceInfo.getAdvertisingId() + 
                ", X-Device-CHANNEL=" + channelId);
    }

    /**
     * 初始化动态请求头（包含更多设备信息）
     * @param deviceInfo 设备信息对象
     */
    private static void initializeDynamicHeaders(DeviceInfoManager deviceInfo) {
        dynamicHeaders.clear();
        String deviceId = deviceInfo != null ? deviceInfo.getDeviceId() : "";
        String packageName = deviceInfo != null ? deviceInfo.getPackageName() : "";
        String offerId = deviceInfo != null ? deviceInfo.getUserAgent() : "";
        String country = deviceInfo != null ? deviceInfo.getCountryCode() : "";
        String advertisingId = deviceInfo != null ? deviceInfo.getAdvertisingId() : "";
        String channelId = UserManager.Companion.getInstance().getUserAdNetwork();

        dynamicHeaders.put("X-Device-ID", deviceId);
        dynamicHeaders.put("X-Device-PKG", packageName);
        dynamicHeaders.put("X-Device-GAID", advertisingId);
        dynamicHeaders.put("X-Device-CHANNEL", channelId);
        dynamicHeaders.put("Authorization", authorizationToken);
        dynamicHeaders.put("accept", "application/json");
        dynamicHeaders.put("Content-Type", "application/json");
        dynamicHeaders.put("X-Offer-ID", offerId);
        dynamicHeaders.put("X-Country", country);
        dynamicHeaders.put("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        
        URLog.debug("HeaderManager", "Headers initialized: X-Device-ID=" + deviceId +
                ", X-Offer-ID=" + offerId + 
                ", X-Country=" + country + 
                ",Gaid" + advertisingId + 
                ",X-Device-CHANNEL=" + channelId);
    }

    /**
     * 获取动态请求头
     * @param deviceInfo 设备信息对象
     * @return 包含动态请求头的Map
     */
    public static Map<String, String> getDynamicHeaders(DeviceInfoManager deviceInfo) {
        dynamicHeaders.clear();
        String deviceId = deviceInfo != null ? deviceInfo.getDeviceId() : "";
        String packageName = deviceInfo != null ? deviceInfo.getPackageName() : "";
        String offerId = deviceInfo != null ? deviceInfo.getUserAgent() : "";
        String country = deviceInfo != null ? deviceInfo.getCountryCode() : "";
        String advertisingId = deviceInfo != null ? deviceInfo.getAdvertisingId() : "";
        String channelId = UserManager.Companion.getInstance().getUserAdNetwork();
        String carrierCode = deviceInfo != null ? deviceInfo.getCarrierCode() : "";
        String simOperator = deviceInfo != null ? deviceInfo.getSimOperator() : "";
        String simCountryIso = deviceInfo != null ? deviceInfo.getSimCountryIso() : "";
        String mccMnc = deviceInfo != null ? deviceInfo.getMccMnc() : "";
        String ip = deviceInfo != null ? deviceInfo.getIpAddress() : "";
        String simCount = deviceInfo != null ? String.valueOf(deviceInfo.getSimCount()) : "";
        dynamicHeaders.put("X-Device-ID", deviceId);
        dynamicHeaders.put("X-Device-PKG", packageName);
        dynamicHeaders.put("X-Device-GAID", advertisingId);
        dynamicHeaders.put("X-Device-CHANNEL", channelId);
        dynamicHeaders.put("Authorization", authorizationToken);
        dynamicHeaders.put("accept", "application/json");
        dynamicHeaders.put("Content-Type", "application/json");
        dynamicHeaders.put("X-Offer-ID", offerId);
        dynamicHeaders.put("X-Country", country);
        dynamicHeaders.put("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        dynamicHeaders.put("X-Carrier-Code", carrierCode);
        dynamicHeaders.put("X-Sim-Operator", simOperator);
        dynamicHeaders.put("X-Sim-Country", simCountryIso);
        dynamicHeaders.put("X-MCC-MNC", mccMnc);
        dynamicHeaders.put("X-Sim-Count", simCount);
        dynamicHeaders.put("X-Device-IP", ip);
        URLog.debug("HeaderManager", "Headers initialized: X-Device-ID=" + deviceId +
                ", X-Offer-ID=" + offerId + 
                ", X-Country=" + country + 
                ",Gaid" + advertisingId + 
                ",X-Device-CHANNEL=" + channelId);
        return new HashMap<>(dynamicHeaders);
    }

    /**
     * 更新时间戳
     */
    private static void updateTimestamp() {
        dynamicHeaders.put("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    }
}
