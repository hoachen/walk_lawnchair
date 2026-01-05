package com.ur.apps.walk.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ur.apps.walk.model.ScreenshotOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 截图数据仓库类
 */
class ScreenshotRepository(private val context: Context) {

    /**
     * 从assets文件中读取截图选项数据
     */
    suspend fun getScreenshotOptions(): List<ScreenshotOption> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("screenshot_options.json").bufferedReader().use { it.readText() }
            val gson = Gson()
            val optionsType = object : TypeToken<Map<String, List<ScreenshotOption>>>() {}.type
            val optionsMap: Map<String, List<ScreenshotOption>> = gson.fromJson(jsonString, optionsType)
            optionsMap["screenshotOptions"] ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 获取当前选中的截图选项
     */
    suspend fun getSelectedOption(): ScreenshotOption? = withContext(Dispatchers.IO) {
        try {
            val options = getScreenshotOptions()
            options.find { it.isDefault } ?: options.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}