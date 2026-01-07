package com.ur.apps.analysis.td

import android.content.Context
import android.util.Log
import cn.thinkingdata.analytics.TDAnalytics
import com.sg.UserManager
import com.ur.apps.analysis.utils.AdSPUtil
import com.ur.apps.analysis.utils.DeviceInfoCollector
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import kotlin.text.get


object TDAnalyticsManager {

    private const val APP_ID = "d36896d73e64481dac7212244034a5d0"
    private const val SERVER_URL = "https://ss.playwinner.org"
    private const val APP_CODE = "and@WalkWinCash"

    private const val EVENT_LOGIN = "login"
    private const val EVENT_LEVEL_UP = "levelup"
    private const val EVENT_CASH = "Cash"
    private const val EVENT_COIN = "Coin"
    private const val EVENT_AD_REVENUE = "Ad_Revenue"
    private const val EVENT_BLOCK = "Block"
    private const val EVENT_BUTTON_SHOW = "button_show"
    private const val EVENT_BUTTON_CLICK = "button_click"
    private const val EVENT_AD_SHOW = "Ad_Show"
    private const val EVENT_AD_CLICK = "Ad_Click"
    private const val EVENT_AD_NETWORK = "Adnetwork"

    fun initialize(context : Context) {
        TDAnalytics.init(context.applicationContext, APP_ID, SERVER_URL)
        val androidId : String = DeviceInfoCollector.instance.getDeviceIdentity().androidId
        TDAnalytics.setDistinctId("$APP_CODE@$androidId")
        TDAnalytics.login("$APP_CODE@$androidId")
        setSuperProperties()
        //开启自动采集事件
        TDAnalytics.enableAutoTrack(
            TDAnalytics.TDAutoTrackEventType.APP_START or TDAnalytics.TDAutoTrackEventType.APP_END
                    or TDAnalytics.TDAutoTrackEventType.APP_INSTALL
            or TDAnalytics.TDAutoTrackEventType.APP_CRASH
        )
        TDAnalytics.userSet(JSONObject().apply {
            put("appcode", APP_CODE)
        })
        setUserInfo()
    }

    private fun setUserInfo() {
        val adNetworkInfo = AdSPUtil.get().getAdNetworkInfo()
        adNetworkInfo?.let {
            try {
//                TDAnalytics.userSet(JSONObject().apply {
//                    put("country_code", DeviceInfoCollector.instance.getMobileCountryCode())
//                })
                val userNetworkJSON = JSONObject(adNetworkInfo)

                UserManager.instance.setUserAdNetwork(userNetworkJSON.getString("ad_network"))
                TDAnalytics.userSet(userNetworkJSON)
//                Log.i("TDAnalytics" ,"userSet $userNetworkJSON")
            } catch (e: Exception) {
                Log.e("TDAnalytics" ,"${e.message}")
            }
        }
    }


    private fun setSuperProperties() {
        //设置公共事件属性以后，每个事件都会带有公共事件属性
        val superProperties = JSONObject().apply {
            put("channel", "ta") //字符串
            put("age", 1) //数字
            put("isSuccess", true) //布尔
            put("birthday", Date()) //时间
//            put("object",JSONObject().apply { //对象
//                put("key", "value")
//            })
//            put("object_arr",JSONArray().apply { //对象组
//                put(JSONObject().apply {
//                    put("key", "value")
//                })
//            })
//            put("arr",JSONArray().apply { //数组
//                put("value")
//            })
            put("appcode", APP_CODE)
        }
        //设置公共事件属性
        TDAnalytics.setSuperProperties(superProperties);
    }

    public fun reportTrackEvent(eventName: String, jsonObject: JSONObject) {
        try {
            TDAnalytics.track(eventName, jsonObject)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

//    fun reportUserInfo() {
//        val properties = JSONObject()
//        properties.put("username", "TA")
//        TDAnalytics.userSet(properties)
//    }

//    fun reportLogin(guideId : String) {
//        val jsonObject = JSONObject().apply {
//            put("guideid", guideId)
//        }
//        reportTrackEvent(EVENT_LOGIN, jsonObject)
//    }

//    fun reportLevelUp(levelUp : String) {
//        val jsonObject = JSONObject().apply {
//            put("levelup", levelUp)
//        }
//        reportTrackEvent(EVENT_LEVEL_UP, jsonObject)
//    }

    fun reportCoin(coins : Int) {
        val jsonObject = JSONObject().apply {
            put("coins_num", coins)
        }
        reportTrackEvent(EVENT_COIN, jsonObject)
    }

    fun reportCash(cashPosition: String,
                   cashRevenue: Double,
                   currencyCode: String,
                   cashtrue: String) {
        val jsonObject = JSONObject().apply {
            put("cashposition", cashPosition)
            put("cashrevenue_num", cashRevenue)
            put("currencycode", currencyCode)
            put("cashtrue", cashtrue)
        }
//        Log.i("AdReport", "reportCash $jsonObject")
        reportTrackEvent(EVENT_CASH, jsonObject)
    }


    fun reportAdRevenue(
        countryCode: String,
        revenue: Double,
        networkName: String,
        adUnitId: String,
        adplatform: String,
        adFormat: String,
        adSource: String,
    ) {
        val jsonObject = JSONObject().apply {
            put("countrycode", countryCode)
            put("revenue_num", revenue)
            put("networkname", networkName)
            put("adunitid", adUnitId)
            put("adplatform", adplatform)
            put("adformat", adFormat)
            put("adsource", adSource)
        }
        Log.i("TDReport", "reportAdReVenue $jsonObject")
        reportTrackEvent(EVENT_AD_REVENUE, jsonObject)
    }

    fun reportBlock(blockType : String) {
        val jsonObject = JSONObject().apply {
            put("blocktype", blockType)
        }
        reportTrackEvent(EVENT_BLOCK, jsonObject)
    }

    fun reportAdShow(adType : String,
                     adPosition : String,
                     adPositionType : String,
                     placementId : String) {
        val jsonObject = JSONObject().apply {
            put("ad_type", adType)
            put("ad_position", adPosition)
            put("ad_position_type", adPositionType)
            put("placement_id", placementId)
        }
        reportTrackEvent(EVENT_AD_SHOW, jsonObject)
    }

    fun reportAdClick(adType : String,
                     adPosition : String,
                     adPositionType : String,
                     placementId : String) {
        val jsonObject = JSONObject().apply {
            put("ad_type", adType)
            put("ad_position", adPosition)
            put("ad_position_type", adPositionType)
            put("placement_id", placementId)
        }
        reportTrackEvent(EVENT_AD_CLICK, jsonObject)
    }

    fun reportButtonShow(adType : String,
                      adPosition : String,
                      adPositionType : String,
                      placementId : String) {
        val jsonObject = JSONObject().apply {
            put("ad_type", adType)
            put("ad_position", adPosition)
            put("ad_position_type", adPositionType)
            put("placement_id", placementId)
        }
        reportTrackEvent(EVENT_BUTTON_SHOW, jsonObject)
    }


    fun reportButtonClick(adType : String,
                         adPosition : String,
                         adPositionType : String,
                         placementId : String) {
        val jsonObject = JSONObject().apply {
            put("ad_type", adType)
            put("ad_position", adPosition)
            put("ad_position_type", adPositionType)
            put("placement_id", placementId)
        }
        reportTrackEvent(EVENT_BUTTON_CLICK, jsonObject)
    }

    fun reportAdNetwork(adNetworkJSON : JSONObject) {
        TDAnalytics.userSet(adNetworkJSON)
        val jsonObject = JSONObject().apply {
            put("Adnetwork", adNetworkJSON)
        }
        reportTrackEvent(EVENT_AD_NETWORK, jsonObject)
    }

    fun reportAdNetworkUnknown() {
        val jsonObject = JSONObject().apply {
            put("Adnetwork", "unknown")
        }
        reportTrackEvent(EVENT_AD_NETWORK, jsonObject)
    }
}