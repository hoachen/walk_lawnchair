package com.ur.apps.analysis.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

class AdSPUtil private constructor(val context : Context) {

    private var FILE_NAME: String = "ad_sp"
    private var sp : SharedPreferences? = null

    init {
        sp = context.getSharedPreferences(
            FILE_NAME,
            Context.MODE_PRIVATE
        )
    }


    fun setAdDisplayed() {
        sp?.edit()?.putBoolean(KEY_AD_DISPLAYED, true)?.apply()
    }

    fun isCheckAdShowed(): Boolean {
        return sp?.getBoolean(KEY_AD_DISPLAYED, false)  == true
    }

    fun setAdNetworkReported() {
        sp?.edit()?.putBoolean(KEY_AD_NETWORK_REPORTED, true)?.apply()
    }

    fun isAdNetworkReported(): Boolean {
        return sp?.getBoolean(KEY_AD_NETWORK_REPORTED, false)  == true
    }

    fun saveAdNetworkInfo(info : String ) {
        sp?.edit()?.putString(KEY_USER_NETWORK_INFO, info)?.apply()
    }

    fun getAdNetworkInfo() : String? {
        return sp?.getString(KEY_USER_NETWORK_INFO, "")
    }

    fun setShuziQueryId(queryId: String) {
        sp?.edit()?.putString(KEY_SHUZI_QUERY_ID, queryId)?.apply()
    }

    fun getShuziQueryId(): String? {
        return sp?.getString(KEY_SHUZI_QUERY_ID, "")
    }

    @SuppressLint("UseKtx")
    fun putString(key : String, value : String) {
        sp?.edit()?.putString(key, value)?.apply()
    }

    fun getString(key : String, default : String) : String? {
        return sp?.getString(key, default)
    }


    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: AdSPUtil? = null

         const val KEY_AD_DISPLAYED = "ad_displayed"
         const val KEY_AD_NETWORK_REPORTED = "ad_network_reported"
         const val KEY_USER_NETWORK_INFO = "user_network_info"
         const val KEY_SHUZI_QUERY_ID = "shuzi_query_id"

         const val KEY_LOCK_AD_RECORD = "lock_ad_record"

         const val KEY_LOCK_AD_CONFIG = "lock_ad_config"


        fun initialize(context: Context) {
            instance ?: synchronized(this) {
                instance ?: AdSPUtil(context).also { instance = it }
            }
        }

        fun get(): AdSPUtil {
            return instance ?: throw IllegalStateException("AdSPUtil not initialized!")
        }
    }
}