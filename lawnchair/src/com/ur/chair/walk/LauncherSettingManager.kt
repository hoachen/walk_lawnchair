package com.ur.chair.walk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings

object LauncherSettingManager {

    fun openLauncherChooser(context: Context) {
        val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val manageDefaults = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val appSettings = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(homeSettings)
        } catch (t: Throwable) {
            try {
                context.startActivity(manageDefaults)
            } catch (t2: Throwable) {
                context.startActivity(appSettings)
            }
        }
    }

    fun openLauncherChooserAndFinish(activity: Activity) {
        openLauncherChooser(activity)
        activity.finish()
    }
}
