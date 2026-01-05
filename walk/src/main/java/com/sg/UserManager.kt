package com.sg

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.sg.ResponseInterceptor.Companion.ResponseErrorListener
import com.sg.model.UserInfo
import com.sg.repository.AdStrategyRepository
import com.sg.repository.UserRepository
import com.sg.request.StrategyRequest
import com.sg.response.ContactUsInfo
import com.sg.response.StrategyResponse
import com.ur.apps.utils.URLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Token管理类
 * 使用Kotlin的单例模式管理用户token
 */
class UserManager private constructor() : ResponseErrorListener {

    var token: String = ""
        private set  // token只能在类内部修改
    
    private var appContext: Context? = null

    private var userRepo : UserRepository? = null

    private var adStrategyRepo : AdStrategyRepository? = null

    private var userInfo : UserInfo? = null

    private var strategy : StrategyResponse? = null

    private var isProductBanned = false
        private set  // token只能在类内部修改

    private var userAdNetwork : String  = ""

    // 初始化Context
    fun initContext(context: Context) {
        this.appContext = context.applicationContext
        this.userRepo = UserRepository(context)
        this.adStrategyRepo = AdStrategyRepository(context)
        // 尝试从SharedPreferences恢复token
        getSharedPreferences()?.let {
            token = it.getString(KEY_TOKEN, "") ?: ""
        }
        ResponseInterceptor.addListener(this)
    }
    
    // 获取SharedPreferences实例
    private fun getSharedPreferences(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun login() {
        Log.i(TAG, "login")
        GlobalScope.launch(Dispatchers.IO) {
            userInfo = userRepo?.login()
            userInfo?.let {
                it.accessToken?.let {
                    Log.i(TAG, "login success $userInfo")
                    saveToken(it)
                }
            }
            if (userInfo == null) {
                Log.e(TAG, "login fail")
            }
        }
    }

    private fun notifyUserInfoChanged(userInfo: UserInfo) {
        GlobalScope.launch(Dispatchers.Main) {
            listeners.forEach {
                it.onUserInfoChanged(userInfo)
            }
        }
    }

    private fun notifyUserAdNetworkChanged(adNetwork: String) {
        GlobalScope.launch(Dispatchers.Main) {
            listeners.forEach {
                it.onUserAdNetworkChanged(adNetwork)
            }
        }
    }


    fun refreshUserInfo(scene : String = StrategyRequest.SCENE_START) {
        if (TextUtils.isEmpty(token)) {
            Log.i(TAG, "user token is null to login ")
            login()
            return
        }
        Log.i(TAG, "start refreshUserInfo $scene")
        GlobalScope.launch(Dispatchers.IO) {
            val newUserInfo = userRepo?.getUserInfo()
            Log.i(TAG, "refreshUserInfo success $newUserInfo")
            newUserInfo?.let {
                userInfo = it
                notifyUserInfoChanged(it)
                userInfo?.accessToken?.let { saveToken(it) }
            }
        }
        requestStrategyRepo(scene)
    }

    fun getUserInfo(): UserInfo? {
        return userInfo
    }

    private fun requestStrategyRepo(scene : String = StrategyRequest.SCENE_START) {
        GlobalScope.launch(Dispatchers.IO) {
            Log.i(TAG, "start requestStrategy scene $scene")
            strategy = adStrategyRepo?.requestStrategy(StrategyRequest(scene))
            Log.i(TAG, "requestStrategy $strategy")
        }
    }


    override fun onTokenInvalid() {
        Log.i(TAG, "")
        login()
    }

    override fun onProductBanned() {
        isProductBanned = true
    }

    fun saveToken(token: String) {
        this.token = token
        // 保存到SharedPreferences
        getSharedPreferences()?.edit()?.apply {
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun clearToken() {
        this.token = ""
        // 从SharedPreferences中清除token
        getSharedPreferences()?.edit()?.apply {
            remove(KEY_TOKEN)
            apply()
        }
    }

    fun getUserAdNetwork() : String {
        return userAdNetwork
    }

    fun setUserAdNetwork(channel : String) {
        URLog.i(TAG, "setUserAdNetwork $channel")
        userAdNetwork = channel
        notifyUserAdNetworkChanged(channel)
    }


    fun getGroupUrl(): String? {
        return strategy?.groupLink
    }


    fun isEarningEnabled() : Boolean {
        return strategy == null || strategy?.isEarningEnabled() == true
    }

    fun getContactUsInfo(): ContactUsInfo? {
        return strategy?.contactUs
    }

    fun isEnableAdFunction(): Boolean {
        return strategy == null || strategy?.afSw == 1
    }

    fun isEnableAoa(): Boolean {
        if (strategy == null) {
            return true
        }
        return strategy?.isEnableAoa() == true
    }


    companion object {
        private const val PREF_NAME = "token_prefs"
        private const val KEY_TOKEN = "user_token"
        private const val TAG = "UserManager"
        
        // 使用标准的Kotlin懒加载单例模式
        val instance: UserManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UserManager()
        }


        // 用于通知UI层的接口
        public interface UserInfoListener {

            fun onUserInfoChanged(userInfo : UserInfo)

            fun onUserAdNetworkChanged(adNetwork: String);
        }

        // 所有注册的监听器
        private val listeners = mutableListOf<UserInfoListener>()

        /**
         * 添加响应错误监听器
         * @param listener 要添加的监听器
         */
        fun addListener(listener: UserInfoListener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        /**
         * 移除响应错误监听器
         * @param listener 要移除的监听器
         */
        fun removeListener(listener: UserInfoListener) {
            listeners.remove(listener)
        }

        /**
         * 清除所有监听器
         */
        fun clearListeners() {
            listeners.clear()
        }

    }

}
