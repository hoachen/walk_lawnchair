package com.ur.apps.analysis.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.net.NetworkInterface
import java.util.Enumeration


object DeviceUtils {
//
//    /**
//     * 返回的JSON 信息
//     * public boolean isOpenDebug;//是否开启调试模式
//     * public boolean IsRunEmu;//是否模拟器
//     * public String EmuCheckReason;//模拟器判断原因
//     * public boolean IsRunVirtual;//是否虚拟机
//     * public boolean IsVpnOpen;//是否开启vpn
//     * public boolean IsRooted;//手机是否Root
//     */
//
//    fun checkDeviceInfo(context : Context, onCheckCallBack : WithString) {
//        SecuritApi.CheckEnv(context, onCheckCallBack)
//    }
//
//    fun checkIsVpnUsed() : Boolean {
//        try {
//            val niList: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
//            niList.toList().forEach { it ->
//                if (!it.isUp() || it.interfaceAddresses.size == 0) {
//                    return@forEach
//                }
//                if ("tun0" == it.name || "tun1" == it.name || "ppp0" == it.name) {
//                    return true
//                }
//            }
//        } catch (e : Throwable) {
//            e.printStackTrace()
//        }
//        return false
//    }
//
//    @SuppressLint("HardwareIds")
//    fun getAndroidID(context: Context): String = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)
//

}