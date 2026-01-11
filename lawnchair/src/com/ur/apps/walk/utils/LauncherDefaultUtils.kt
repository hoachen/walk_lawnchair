package com.ur.apps.walk.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.walk.constants.StatisticConstants
import com.ur.apps.walk.dialog.SetDefaultLauncherDialog
import org.json.JSONObject
import java.util.ArrayList

object LauncherDefaultUtils {
    


    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo?.activityInfo?.packageName == context.packageName) {
            return true
        }

        // Double check using getHomeActivities which is sometimes more reliable
        val homeActivities = ArrayList<android.content.pm.ResolveInfo>()
        val defaultHome = context.packageManager.getHomeActivities(homeActivities)
        return defaultHome != null && defaultHome.packageName == context.packageName
    }
}
