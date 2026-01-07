package com.ur.apps.walk

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.Window
import com.ur.apps.absui.BaseUrFullScreenActivity
import com.ur.apps.utils.URLog
import com.ur.apps.walk.utils.LocaleHelper
import com.ur.apps.walk.utils.ThemeManager

/**
 * 基础Activity，用于处理语言设置和主题应用
 */
open class BaseActivity : BaseUrFullScreenActivity(), WalkApplication.ForegroundStateListener {
    private val TAG = "BaseActivity"

    override fun attachBaseContext(newBase: Context) {
        // 获取当前语言设置并应用
        val language = LocaleHelper.getLanguage(newBase)
        val context = LocaleHelper.applyLanguage(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupTheme()
        super.onCreate(savedInstanceState)

        // 测试获取当前Activity功能
        val app = application as WalkApplication
        val currentActivity = app.getCurrentActivity()
        URLog.d(TAG, "当前活动的Activity: ${currentActivity?.javaClass?.simpleName}")

        // 测试根据名称查找Activity
        val activityName = this.javaClass.simpleName
        val foundActivity = app.findActivityByName(activityName)
        URLog.d(TAG, "查找到的Activity (${activityName}): ${foundActivity != null}")

        // 检查并输出应用的前台/后台状态
        val isInForeground = app.isAppInForeground()
        URLog.d(TAG, "onCreate时应用是否在前台: $isInForeground")

        // 注册前台状态监听器
        app.addForegroundStateListener(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        SAdSDK.instance.dispatchTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()

        // 注销前台状态监听器
        val app = application as WalkApplication
        app.removeForegroundStateListener(this)
    }

    private fun setupTheme() {
        // 应用已保存的主题设置
        ThemeManager.getInstance().applyTheme(this)
        setupWindow(window)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 配置变化时重新应用语言设置
        val language = LocaleHelper.getLanguage(this)
        LocaleHelper.applyLanguage(this, language)
    }

    override fun onResume() {
        super.onResume()
        setupTheme()
    }

    override fun setupWindow(window: Window?) {
        super.setupWindow(window)
        window?.setBackgroundDrawableResource(R.drawable.app_window_background)
    }

    // 实现ForegroundStateListener接口的方法
    override fun onEnterForeground() {
        URLog.d(TAG, "${javaClass.simpleName} 收到应用进入前台的通知")
    }

    override fun onEnterBackground() {
        URLog.d(TAG, "${javaClass.simpleName} 收到应用进入后台的通知")
    }
} 