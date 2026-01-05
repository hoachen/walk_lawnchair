package com.ur.apps.walk.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.core.content.edit
import com.ur.apps.walk.R

/**
 * 主题管理类，负责应用主题颜色和管理颜色配置
 */
class ThemeManager private constructor() {
    enum class BrandTheme(@StyleRes val themeRes: Int) {
        Theme1(R.style.Theme_WalkApp_Theme1),
        Theme2(R.style.Theme_WalkApp_Theme2),
    }

    companion object {
        private const val TAG = "ThemeManager"

        private const val PREFS_NAME = "app_theme_prefs"
        private const val KEY_THEME = "key_theme"

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager().also { instance = it }
            }
        }
    }


    private var currentTheme: BrandTheme? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, BrandTheme.Theme1.name)
        currentTheme = BrandTheme.valueOf(saved!!)
    }

    fun getCurrentTheme(): BrandTheme = currentTheme ?: BrandTheme.Theme1

    fun applyTheme(activity: Activity) {
        activity.setTheme(getCurrentTheme().themeRes)
    }

    fun changeTheme(activity: Activity, newTheme: BrandTheme) {
        if (newTheme == getCurrentTheme()) return

        currentTheme = newTheme
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_THEME, newTheme.name) }

        // 重新创建当前 Activity，重新走 onCreate & setContentView
        activity.recreate()
    }

    fun getAllThemes(): List<BrandTheme> = BrandTheme.entries.toList()

    @ColorInt
    fun getColorFromSpecificTheme(
        baseContext: Context,
        @StyleRes themeResId: Int,
        @AttrRes attrResId: Int,
        @ColorInt defaultColor: Int = 0
    ): Int {
        // 用一个 ContextThemeWrapper 套上指定的 theme
        val themedContext = ContextThemeWrapper(baseContext, themeResId)
        val typedValue = TypedValue()
        val resolved = themedContext.theme.resolveAttribute(attrResId, typedValue, true)
        return if (resolved) typedValue.data else defaultColor
    }
}

fun Context.getThemeColor(
    @AttrRes attrResId: Int,
    @ColorInt defaultColor: Int = Color.TRANSPARENT
): Int {
    val typedValue = TypedValue()
    val resolved = theme.resolveAttribute(attrResId, typedValue, true)
    return if (resolved) typedValue.data else defaultColor
}
