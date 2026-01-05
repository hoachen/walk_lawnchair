package com.ur.apps.lock

import android.content.Context
import com.ur.apps.analysis.utils.DeviceInfoCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LockRepository(private val context: Context) {

    private val deviceInfoCollector = DeviceInfoCollector.instance

    /**
     * 用户登录
     */
    suspend fun getLockInfo(): LockAdConfig? = withContext(Dispatchers.IO) {
        val lockService = LockApiClient.getLockService(context)
        val response = lockService.getConfig(context.packageName,
            deviceInfoCollector.getAppInfo().versionName).execute().body()
        response
    }


    companion object {
        const val TAG = "LockRepository"
    }

}