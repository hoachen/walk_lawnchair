package com.ur.apps.walk.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.icu.util.ULocale
import android.os.Build
import android.os.LocaleList
import com.ur.apps.walk.MainActivityRecycler
import com.android.launcher3.R
import java.util.Locale

/**
 * 语言管理工具类，用于更改应用程序的语言
 */
class LocaleHelper {
    companion object {
        private const val LANGUAGE_PREF = "app_language_preference"
        private const val LANGUAGE_KEY = "selected_language"

        // 获取系统默认语言代码
        fun getSystemLanguage(): String {
            return Locale.getDefault().language
        }

        // 获取当前应用使用的语言代码
        fun getLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(LANGUAGE_PREF, Context.MODE_PRIVATE)
            // 如果没有设置，则返回系统语言
            return prefs.getString(LANGUAGE_KEY, getSystemLanguage()) ?: getSystemLanguage()
        }

        // 保存语言设置
        fun setLanguage(context: Context, language: String) {
            val prefs = context.getSharedPreferences(LANGUAGE_PREF, Context.MODE_PRIVATE)
            prefs.edit().putString(LANGUAGE_KEY, language).apply()
        }

        // 应用选定的语言
        fun applyLanguage(context: Context, language: String): ContextWrapper {
            val locale = when (language) {
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "pt" -> Locale("pt", "BR")
                "in" -> Locale("id", "ID")
                else -> Locale.US
            }

            return updateResources(context, locale)
        }

        // 更新资源和配置以应用新的语言
        private fun updateResources(context: Context, locale: Locale): ContextWrapper {
            var newContext = context
            val resources = context.resources
            val configuration = Configuration(resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
            } else {
                configuration.locale = locale
                Locale.setDefault(locale)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                newContext = context.createConfigurationContext(configuration)
            } else {
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }

            return ContextWrapper(newContext)
        }

        // 重启应用到首页
        fun restartApp(context: Context) {
            val intent = Intent(context, MainActivityRecycler::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        // 获取可用的语言列表
        fun getAvailableLanguages(context: Context): List<LanguageItem> {
            return listOf(
                LanguageItem("en", context.getString(R.string.settings_language_english), isSystemDefault("en")),
                LanguageItem("zh", context.getString(R.string.settings_language_simplified_chinese), isSystemDefault("zh")),
                LanguageItem("pt", context.getString(R.string.settings_language_portuguese), isSystemDefault("pt")),
                LanguageItem("in", context.getString(R.string.settings_language_indonesian), isSystemDefault("in"))
            )
        }

        // 检查是否为系统默认语言
        private fun isSystemDefault(languageCode: String): Boolean {
            return getSystemLanguage() == languageCode
        }

        // 根据语言代码获取语言名称
        fun getLanguageName(context: Context, languageCode: String): String {
            val languages = getAvailableLanguages(context)
            return languages.find { it.code == languageCode }?.name ?: context.getString(R.string.settings_language_english)
        }
    }

    // 语言项数据类
    data class LanguageItem(
        val code: String,      // 语言代码
        val name: String,      // 语言名称
        val isSystemDefault: Boolean  // 是否为系统默认语言
    )
}
