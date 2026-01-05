package com.ur.apps.walk.repository

import android.content.Context
import com.ur.apps.utils.URLog
import com.sg.ApiClient
import com.sg.BaseResponse
import com.sg.response.CountryListResponse
import com.ur.apps.walk.model.RegionOption
import com.ur.apps.walk.utils.RegionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 区域数据仓库类
 */
private const val TAG = "RegionRepository"

class RegionRepository(private val context: Context) {

    /**
     * 获取可用的区域选项列表
     */
    suspend fun getRegionOptions(): List<RegionOption> = withContext(Dispatchers.IO) {
        try {
            // 这里模拟从assets中读取数据，实际项目中可以替换为真实的API调用
            // 使用RegionHelper获取可用区域
            val response: BaseResponse<CountryListResponse> =
                ApiClient.getUserService(context).getCountryList()

            val countryListResponse = response.data ?: run {
                URLog.e(
                    TAG,
                    "get nothing from server when fetch with draw info response.st({$response.st}) response.msg({$response.msg}) response.uid({${response.uid}}) response.code({${response.code}})"
                )
                CountryListResponse(listOf())
            }

            if (countryListResponse.detail.isEmpty()) {
                URLog.e(TAG, "error that nothing country can change")
            } else {
                RegionHelper.saveAvailableRegions(
                    context,
                    countryListResponse.detail.mapIndexed { index, countryInfo ->
                        RegionHelper.RegionItem(
                            code = countryInfo.countryCode,
                            name = countryInfo.countryName,
                            flagUrl = countryInfo.nationalFlagUrl,
                            paymentUrl = countryInfo.paymentProvidersLogoUrl.firstOrNull() ?: "",
                            isSystemDefault = index == 0
                        )
                    })
            }

            val regions = RegionHelper.getAvailableRegions(context)

            // 转换为RegionOption列表
            return@withContext regions.mapIndexed { index, regionItem ->
                RegionOption(
                    id = index + 1,
                    code = regionItem.code,
                    name = regionItem.name,
                    paymentType = regionItem.paymentUrl, // 默认支付类型，实际应用中可能需要根据区域不同而变化
                    nationFlag = regionItem.flagUrl,
                    isSelected = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取当前选中的区域
     */
    fun getCurrentRegion(context: Context): String {
        return RegionHelper.getRegion(context)
    }

}