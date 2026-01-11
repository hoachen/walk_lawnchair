package com.ur.apps.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.core.content.edit

private const val TAG = "CheckAndExitUtils"

object CheckAndExitUtils {
    private val SET_ME_LAUNCHER_EXIT_ONCE = "set_me_launcher_exit_once"
    private val IS_SET_ME_LAUNCHER_EXIT_ONCE = "is_set_me_launcher_exit_once"
    public fun checkAndExitIfNeed(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences(SET_ME_LAUNCHER_EXIT_ONCE, MODE_PRIVATE)
        val isSetMeLauncherExitOnce =
            sharedPreferences.getBoolean(IS_SET_ME_LAUNCHER_EXIT_ONCE, false)

        Log.i(
            TAG,
            "post delay kill me isSetMeLauncherExitOnce($isSetMeLauncherExitOnce)",
        )

        if (!isSetMeLauncherExitOnce) {
            sharedPreferences.edit(true) { putBoolean(IS_SET_ME_LAUNCHER_EXIT_ONCE, true) }
            Log.i(TAG, "post delay kill me ")
            // 更新首次启动标志
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    Log.i(TAG, "kill me ")
                    android.os.Process.killProcess(Process.myPid())
                },
                0,
            )
        }
    }
}
