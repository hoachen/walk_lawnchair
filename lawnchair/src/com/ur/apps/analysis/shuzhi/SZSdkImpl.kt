package com.ur.apps.analysis.shuzhi

import android.app.Application
import android.content.Context
import android.util.Log

import cn.shuzilm.core.Main
import com.sg.UserManager
import com.ur.apps.analysis.utils.AdSPUtil
import com.ur.apps.analysis.utils.DeviceInfoCollector
import org.json.JSONObject
import java.util.UUID

object SZSdkImpl {

    private const val API_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALPktvb/hIxmi5jmWGYWUjzntnOk4ceOMTFucjjXWug5VAA9XmzO8CRfifKY/uYRkl9j0ai5m6Zt6DCrSlZE1fcCAwEAAQ=="

    const val SCENE_START = "START"
    const val SCENE_REWARD = "REWARD"
    const val SCENE_WITHDRAWAL = "WITHDRAWAL"

    private var applicationContext: Context? = null

    private var lastSZQueryId: String? = ""

    fun init(ctx: Application) {
        applicationContext = ctx
        Main.init(ctx, API_KEY)
        asyncUpdateShuziId()
        lastSZQueryId = AdSPUtil.get().getShuziQueryId()
    }

//    private async{"ouid":"","androidid":"","atid":"","pkg":"","rid":"","scene":""}

    fun asyncUpdateShuziId(scene: String = SCENE_START, atid : String = "") {
        val deviceIdentity = DeviceInfoCollector.instance
       val message = JSONObject().apply {
           put("ouid", "")
           put("androidid", deviceIdentity.getDeviceIdentity().androidId)
           put("atid", atid)
           put("pkg", applicationContext?.packageName)
           put("rid", generateUniqueId())
           put("scene", scene)
        }.toString()
        val channel =  UserManager.instance.getUserAdNetwork()
        Log.d("SZSdkImpl", "Query Id channel=$channel, msg=$message")
        Main.getQueryID(applicationContext, channel, message, true) { id ->
            Log.d("SZSdkImpl", "Device ID: $id")
            lastSZQueryId = id
            AdSPUtil.get().setShuziQueryId(id)
        }
    }

    private fun generateUniqueId() : String {
        val uniqueString = UUID.randomUUID().toString().replace("-", "")
        return uniqueString
    }

    fun getShuZiQueryId() : String? = lastSZQueryId
}