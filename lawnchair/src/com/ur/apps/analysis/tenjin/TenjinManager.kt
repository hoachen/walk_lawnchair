package com.ur.apps.analysis.tenjin

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sg.UserManager
import com.tenjin.android.TenjinSDK
import com.tenjin.android.config.TenjinConsts
import com.ur.apps.analysis.model.DeviceInfo
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.analysis.utils.AdSPUtil
import com.ur.apps.utils.URLog
import org.json.JSONObject

object TenjinManager {
    private const val TAG = "tenjin"
    private const val API_KEY = "NFD9UUTNRBIX8OXFUMONZHYHFFWAMVT1"
    private var isInit = false

    @SuppressLint("StaticFieldLeak")
    private var tenjinSDK : TenjinSDK? = null
    private var isAdDisplayed : Boolean = false

    @SuppressLint("StaticFieldLeak")
    private var applicationContext: Context? = null

    private var retryCounter = 0
    private const val MAX_RECONNECT_COUNT  = 10
    private val mainHandler : Handler = Handler(Looper.getMainLooper())

    var deviceInfo : DeviceInfo?= null
        private set

     fun initTenJinSDK(context : Context) {
         applicationContext = context.applicationContext
         AdSPUtil.initialize(context)
         // 如果已经初始化就不需要初始化了
         Log.i(TAG, "initTenJinSDK  isInit = $isInit")
         if (isInit) {
             return
         }
         isAdDisplayed = AdSPUtil.get().isCheckAdShowed()
         // 只有第一次广告展示后才初始化
         Log.i(TAG, "is first ad showed $isAdDisplayed")
         if (!isAdDisplayed) {
             return
         }
         Log.i(TAG, "try to initTenJinSDK")
         initTenJinSDKInternal()
     }

    // 可能应用温启动
    fun onMainActivityStart() {
        if (!isAdDisplayed ||  isInit) {
            Log.i(TAG, "onMainActivityStart is first ad showed $isAdDisplayed isInit $isInit")
            return
        }
        tenjinSDK = TenjinSDK.getInstance(applicationContext, API_KEY)
        tenjinSDK?.setAppStore(TenjinSDK.AppStoreType.googleplay)
        tenjinSDK?.connect()
    }

    // 首次广告展示需要初始化TenJin
    fun notifyAdDisplayed() {
        if (isAdDisplayed) {
            return
        }
        isAdDisplayed = true
        AdSPUtil.get().setAdDisplayed()
        Log.i(TAG, "ad showed to call tenjin connect")
        initTenJinSDKInternal()
    }

    private fun initTenJinSDKInternal() {
        // Tenjin SDK Integration
        // Add Tenjin API Key from your Tenjin dashboard - https://www.tenjin.io/dashboard/docs.
        tenjinSDK = TenjinSDK.getInstance(applicationContext, API_KEY)
        // Set the appstore
        // If you distribute your app on Google Play store or Amazon store. Then set it to googleplay
        tenjinSDK?.setAppStore(TenjinSDK.AppStoreType.googleplay)
        // connect to start the TenjinSDK
        isInit = true
        Log.i(TAG, "call connect Report open")
        tenjinSDK?.connect()
        fetchAdNetworkInfo()
    }

    private fun fetchAdNetworkInfo() {
        if (AdSPUtil.get().isAdNetworkReported()) {
            Log.i(TAG, "has report AdNetwork info")
            return
        }
        connectToFetchAdNetwork()
    }

    private fun connectToFetchAdNetwork() {
        tenjinSDK?.getAttributionInfo { data ->
            Log.i(TAG, "fetchAdNetworkInfo getAttributionInfo $data")
            // 如果没有 ad_network和advertising_id 走到重试逻辑
            if (data.containsKey(TenjinConsts.ATTR_PARAM_ADVERTISING_ID) &&
                data.containsKey(TenjinConsts.ATTR_PARAM_AD_NETWORK)) {
                val advertisingId = data[TenjinConsts.ATTR_PARAM_ADVERTISING_ID]
                val adNetwork = data[TenjinConsts.ATTR_PARAM_AD_NETWORK]
                val tenjinCampaignId = data[TenjinConsts.ATTR_PARAM_CAMPAIGN_ID]
                val tenjinCampaignName = data[TenjinConsts.ATTR_PARAM_CAMPAIGN_NAME]
                val tenjinSiteId = data[TenjinConsts.ATTR_PARAM_SITE_ID]
                val tenjinCreativeName = data[TenjinConsts.ATTR_PARAM_CREATIVE_NAME]
                val tenjinRemoteCampaignId = data[TenjinConsts.ATTR_PARAM_REMOTE_CAMPAIGN_ID]
                if (advertisingId != null && adNetwork != null) {
                    retryCounter = 0
                    AdSPUtil.get().setAdNetworkReported()
                    val adNetworkJSON = JSONObject().apply {
                        put("advertising_id", advertisingId)
                        put("ad_network", adNetwork)
                        put("campaign_id", tenjinCampaignId)
                        put("campaign_name", tenjinCampaignName)
                        put("site_id", tenjinSiteId)
                        put("creative_name", tenjinCreativeName)
                        put("remote_campaign_id", tenjinRemoteCampaignId)
                    }
                    URLog.i(TAG, "fetch User adNetworkInfo = $adNetworkJSON")
                    UserManager.instance.setUserAdNetwork(adNetwork)
                    AdSPUtil.get().saveAdNetworkInfo(adNetworkJSON.toString())
                    TDAnalyticsManager.reportAdNetwork(adNetworkJSON)
                } else {
                    // 触发重试逻辑
                    retryConnect()
                }
            }
        }
    }

    private fun retryConnect() {
        // 前3次每隔10秒一次，后3次每隔30秒一次。任意一次成功取到，则终止，并将结果通过数数进行上报。
        // 如果6次重试仍取不到，那么结果为：unknown, 并将这个结果通过数数上报
        retryCounter += 1
        if (retryCounter > MAX_RECONNECT_COUNT) {
            Log.i(TAG, "has retry to max count")
            TDAnalyticsManager.reportAdNetworkUnknown()
            return
        }
        val delayTime = if (retryCounter < 4) {
            5_000L
        } else if (retryCounter < 8) {
            10_000L
        } else {
            30_000L
        }
        Log.i(TAG, "delay $delayTime to connect")
        mainHandler.postDelayed({
            Log.i(TAG, "reconnect to connectToFetchAdNetwork")
            tenjinSDK?.connect()
            connectToFetchAdNetwork()
        }, delayTime)
    }


    fun getTenjinInstance() : TenjinSDK? = tenjinSDK
}