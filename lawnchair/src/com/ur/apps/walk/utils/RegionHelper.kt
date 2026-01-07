package com.ur.apps.walk.utils

import android.content.Context
import com.ur.apps.utils.URLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sg.ApiClient
import com.sg.BaseResponse
import com.sg.ResponseInterceptor
import com.sg.request.ChangeCountryRequest
import com.sg.response.ChangeCountryResponse
import com.android.launcher3.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 区域管理工具类，用于管理用户选择的区域
 */
private const val TAG = "RegionHelper"

class RegionHelper {
    companion object {
        const val BR="BR"
        const val US="US"
        const val ID="ID"
        private var TAG: String? = null
            get() {
                if (field == null) {
                    field = "RegionHelper"
                }
                return field
            }

        private var REGION_PREF: String? = null
            get() {
                if (field == null) {
                    field = "app_region_preference"
                }
                return field
            }

        private var REGION_KEY: String? = null
            get() {
                if (field == null) {
                    field = "selected_region"
                }
                return field
            }

        private var REGION_LIST: String? = null
            get() {
                if (field == null) {
                    field = "region_list"
                }
                return field
            }

        // 使用上下文初始化字符串常量
        private fun initConstants(context: Context) {
            TAG = context.getString(R.string.region_helper_tag)
            REGION_PREF = context.getString(R.string.region_helper_pref_name)
            REGION_KEY = context.getString(R.string.region_helper_region_key)
            REGION_LIST = context.getString(R.string.region_helper_region_list)
        }

        // 获取系统默认区域代码
        fun getSystemRegion(context: Context): String {
            // 初始化常量
            initConstants(context)
            // 这里可以根据实际需求获取系统区域，简单起见默认为"cn"
            return context.getString(R.string.region_helper_default_region)
        }

        // 获取当前应用使用的区域代码
        fun getRegion(context: Context): String {
            // 初始化常量
            initConstants(context)
            val prefs = context.getSharedPreferences(REGION_PREF, Context.MODE_PRIVATE)
            // 如果没有设置，则返回系统区域
            return prefs.getString(REGION_KEY, getSystemRegion(context)) ?: getSystemRegion(context)
        }

        // 保存区域设置
        suspend fun setRegion(
            context: Context,
            region: String,
        ): Boolean = withContext(Dispatchers.IO) {
            // 初始化常量
            initConstants(context)
            val response: BaseResponse<ChangeCountryResponse> =
                ApiClient.getUserService(context).changeCountry(ChangeCountryRequest(region))
            URLog.i(TAG, "update region result(${response})")
            if (response.code == ResponseInterceptor.SUCCESS) {
                val prefs = context.getSharedPreferences(REGION_PREF, Context.MODE_PRIVATE)
                prefs.edit().putString(REGION_KEY, region).apply()
                return@withContext true
            } else {
                return@withContext false
            }
        }

        // 获取可用的区域列表（与语言选项数量保持一致）
        fun getAvailableRegions(context: Context): List<RegionItem> {
            // 初始化常量
            initConstants(context)
            val prefs = context.getSharedPreferences(REGION_PREF, Context.MODE_PRIVATE)
            // 如果没有设置，则返回系统区域
            val regionJson = prefs.getString(REGION_LIST, "[]") ?: "[]"
            val type = object : TypeToken<List<RegionItem>>() {}.type
            val regionItemList = Gson().fromJson<List<RegionItem>>(regionJson, type)
            return regionItemList
        }

        // 获取可用的区域列表（与语言选项数量保持一致）
        fun saveAvailableRegions(context: Context, list: List<RegionItem>) {
            // 初始化常量
            initConstants(context)
            val prefs = context.getSharedPreferences(REGION_PREF, Context.MODE_PRIVATE)
            prefs.edit().putString(REGION_LIST, Gson().toJson(list)).apply()
        }

        // 检查是否为系统默认区域
        private fun isSystemRegion(context: Context, regionCode: String): Boolean {
            return getSystemRegion(context) == regionCode
        }

        // 根据区域代码获取区域名称
        fun getRegionName(context: Context, regionCode: String): String {
            val regions = getAvailableRegions(context)
            return regions.find { it.code == regionCode }?.name
                ?: context.getString(R.string.settings_region_default)
        }
    }

    // 区域项数据类
    data class RegionItem(
        val code: String,      // 区域代码
        val name: String,      // 区域名称
        val flagUrl: String,    // 国旗
        val paymentUrl: String,    // 提现方
        val isSystemDefault: Boolean  // 是否为系统默认区域
    )
}
