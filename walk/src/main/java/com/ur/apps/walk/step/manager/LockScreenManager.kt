package com.ur.apps.walk.step.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.ur.apps.utils.URLog
import androidx.core.content.edit
import com.ur.apps.walk.LockScreenActivity
import com.ur.apps.walk.step.constants.StepConstants

/**
 * 锁屏管理策略层
 * 负责管理锁屏逻辑和状态
 */
class LockScreenManager(context: Context) {
    private val mContext: Context = context.applicationContext
    private val mPrefs: SharedPreferences =
        mContext.getSharedPreferences(StepConstants.PREF_NAME, Context.MODE_PRIVATE)

    var isLockScreenEnabled: Boolean
        /**
         * 检查是否启用了锁屏功能
         */
        get() = mPrefs.getBoolean(
            PREF_LOCK_SCREEN_ENABLED,
            true
        ) // 默认启用
        /**
         * 设置锁屏功能启用状态
         */
        set(enabled) {
            mPrefs.edit {
                putBoolean(PREF_LOCK_SCREEN_ENABLED, enabled)
            }
            URLog.d(
                TAG,
                "锁屏功能 " + (if (enabled) "启用" else "禁用")
            )
        }

    /**
     * 检查是否需要显示锁屏界面
     * 条件：屏幕亮起 + 锁屏功能启用 + 锁屏界面未显示
     */
    fun shouldShowLockScreen(): Boolean {
        if (!this.isLockScreenEnabled) {
            URLog.d(TAG, "锁屏功能未启用，不显示锁屏界面")
            return false
        }

        URLog.d(TAG, "满足显示锁屏界面的条件")
        return true
    }

    /**
     * 显示锁屏界面
     */
    fun showLockScreen() {
        if (!shouldShowLockScreen()) {
            return
        }

        try {
            val intent = Intent(mContext, LockScreenActivity::class.java)
            intent.addFlags(
                (Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )

            mContext.startActivity(intent)

            URLog.d(TAG, "启动锁屏界面成功")
        } catch (e: Exception) {
            URLog.e(TAG, "启动锁屏界面失败: " + e.message)
        }
    }

    /**
     * 处理屏幕亮起事件
     */
    fun onScreenOn() {
        URLog.d(TAG, "屏幕亮起，检查是否需要显示锁屏界面")
    }

    /**
     * 处理屏幕关闭事件
     */
    fun onScreenOff() {
        URLog.d(TAG, "屏幕关闭")
        if (shouldShowLockScreen()) {
            showLockScreen()
        }
    }

    /**
     * 处理用户解锁事件（用户输入密码/图案解锁）
     */
    fun onUserPresent() {
        URLog.d(TAG, "用户解锁")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        URLog.d(TAG, "清理锁屏管理器资源")
        // 这里可以添加任何需要清理的资源
        // 例如：取消注册监听器、释放引用等
    }

    companion object {
        private const val TAG = "LockScreenManager"

        // 锁屏配置的 SharedPreferences 键
        private const val PREF_LOCK_SCREEN_ENABLED = "lock_screen_enabled"
    }
}
