package com.ur.apps.walk

import android.app.ComponentCaller
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardDismissCallback
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import androidx.viewpager.widget.ViewPager
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.utils.URLog
import com.ur.apps.walk.adapter.LockScreenPagerAdapter
import com.ur.apps.walk.constants.StatisticConstants
import com.ur.apps.walk.fragment.LockScreenEmptyFragmentLeft
import com.ur.apps.walk.fragment.MinimalFragment
import org.json.JSONObject


/**
 * 锁屏界面 Activity
 * 显示在亮屏后的自定义锁屏界面
 * 使用 ViewPager 实现滑动解锁
 */
private const val TAG = "LockScreenActivity"

class LockScreenActivity : BaseActivity() {
    companion object {
        const val KEYGUARD_DISMISS_CHECK_MESSAGE = 1001
    }

    private lateinit var fvpMain: ViewPager
    private lateinit var adapter: LockScreenPagerAdapter

    private var mIsMomentoClick = false
    private lateinit var mKeyguardManager: KeyguardManager
    private var mIsPagerChanged = false

    private val mKeyguardHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg == null || msg.what != 456) {
                return
            }
            val intValue = if (msg.obj == null) 0 else (msg.obj as Int)
            if (!this@LockScreenActivity.isKeyguardLock()) {
                // 如果锁屏中，就进行一次广告转发
            } else if (intValue < 10) {
                sendMessageDelayed(obtainMessage(456, intValue + 1), 1000L)
            }
        }
    }

    private fun isKeyguardLock(): Boolean {
        return mKeyguardManager.isKeyguardLocked
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        URLog.i(TAG, "onCreate")
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.LOCK_ACT,
            JSONObject().put(StatisticConstants.TYPE, "create")
        )
        setContentView(R.layout.activity_lock_screen)

        // 设置窗口标志
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        mKeyguardManager = getSystemService(KeyguardManager::class.java)

        initViews()
        initPager()
        initLockScreen()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        URLog.i(TAG, "onNewIntent")
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.LOCK_ACT,
            JSONObject().put(StatisticConstants.TYPE, "newIntent")
        )
        super.onNewIntent(intent, caller)
    }

    private fun initViews() {
        fvpMain = findViewById(R.id.fvp_main)
    }

    private fun initPager() {
        adapter = LockScreenPagerAdapter(supportFragmentManager)

        // 添加片段：左侧空片段和主片段
        adapter.addFragment(LockScreenEmptyFragmentLeft.newInstance())
        adapter.addFragment(MinimalFragment.newInstance())

        fvpMain.adapter = adapter
        fvpMain.currentItem = 1 // 默认显示主片段
        fvpMain.offscreenPageLimit = 1

        // 设置页面变化监听器
        fvpMain.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position != 0) {
                    return
                }
                // 处理滑动过程中的背景变化等
            }

            override fun onPageSelected(position: Int) {
                mIsPagerChanged = true
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    try {
                        if (mIsPagerChanged) {
                            if (mIsMomentoClick) {
                                requestCheckKeyguardDismiss()
                            }
                            // 如果滑动到左侧空页面，则解锁
                            if (fvpMain.currentItem == 0) {
                                mKeyguardManager.requestDismissKeyguard(
                                    this@LockScreenActivity,
                                    null
                                )
                                moveTaskToBack()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.LOCK_ACT,
            JSONObject().put(StatisticConstants.TYPE, "resume")
        )
        // 确保显示主片段
        fvpMain.currentItem = 1
        mKeyguardManager = getSystemService(KeyguardManager::class.java)
    }

    // 禁止返回键
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            true // 拦截返回键
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        URLog.i(TAG, "onDestroy")
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.LOCK_ACT,
            JSONObject().put(StatisticConstants.TYPE, "destroy")
        )
        this.mKeyguardHandler.removeCallbacksAndMessages(null)
    }

    fun moveTaskToBack() {
        moveTaskToBack(true)
    }

    private fun initLockScreen() {
        try {
            val cls = Class.forName("android.os.ServiceManager")
            val invoke = cls.getMethod("getService", String::class.java).invoke(cls, "window")
            val cls2 = Class.forName("android.view.IWindowManager")
            val method = cls2.getMethod(
                "requestSystemKeyEvent",
                Integer.TYPE,
                ComponentName::class.java,
                java.lang.Boolean.TYPE
            )
            val classes = cls2.getClasses()
            if (classes.size <= 0) {
                return
            }
            val cls3: Class<*> = classes[0]!!
            if (cls3.getName() == "android.view.IWindowManager\$Stub") {
                method.invoke(
                    cls2.cast(
                        cls3.getMethod("asInterface", IBinder::class.java).invoke(cls3, invoke)
                    ), 3, getComponentName(), false
                )
            }
        } catch (unused: Exception) {
        }
    }

    /**
     * 点广告的行为，需要调用到这里，进行解锁确认
     */
    fun requestCheckKeyguardDismiss() {
        if (!isKeyguardLock()) {
            this.mIsMomentoClick = false
        } else {
            TDAnalyticsManager.reportTrackEvent(
                StatisticConstants.KEYGUARD_ACTION,
                JSONObject().put(StatisticConstants.TYPE, "dismiss")
            )
            this.mKeyguardManager.requestDismissKeyguard(this, object : KeyguardDismissCallback() {
                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    URLog.d(TAG, "onDismissCancelled")
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.KEYGUARD_ACTION,
                        JSONObject().put(StatisticConstants.TYPE, "cancel")
                    )
                }

                override fun onDismissError() {
                    super.onDismissError()
                    moveTaskToBack()
                    URLog.d(TAG, "onDismissError")
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.KEYGUARD_ACTION,
                        JSONObject().put(StatisticConstants.TYPE, "error")
                    )
                }

                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    TDAnalyticsManager.reportTrackEvent(
                        StatisticConstants.KEYGUARD_ACTION,
                        JSONObject().put(StatisticConstants.TYPE, "success")
                    )
                    this@LockScreenActivity.mKeyguardHandler.removeCallbacksAndMessages(null)
                    this@LockScreenActivity.mKeyguardHandler.sendMessageDelayed(
                        this@LockScreenActivity.mKeyguardHandler.obtainMessage(
                            KEYGUARD_DISMISS_CHECK_MESSAGE,
                            0
                        ), 1000L
                    )
                }
            })
        }
    }

    fun navigateToMainActivityShowSomething(taskId: Int, stepGoal: Int) {
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.NAVI_MAIN_SHOW,
            JSONObject().put(StatisticConstants.TASK_ID, taskId)
        )
        fvpMain.setCurrentItem(0)
        requestCheckKeyguardDismiss()
        // 跳转到权限请求页面，而不是直接跳转到主页面
        val intent = Intent(this, PermissionRequestActivity::class.java)
        intent.putExtra(MainActivityRecycler.ON_CLAIM_TID, taskId)
        intent.putExtra(MainActivityRecycler.ON_CLAIM_GOAL, stepGoal)
        startActivity(intent)
        // 添加过渡动画
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        moveTaskToBack()
    }

    /**
     * 设置广告点击状态
     */
    fun setMomentoClick(isMomento: Boolean, source: String) {
        this.mIsMomentoClick = isMomento
        if (isMomento) {
            TDAnalyticsManager.reportTrackEvent(
                StatisticConstants.LOCK_ACT_AD_CLICK,
                JSONObject().put(StatisticConstants.SOURCE, source)
            )
            requestCheckKeyguardDismiss()
        }
    }

    override fun setupWindow(window: Window?) {
        super.setupWindow(window)
        window?.setBackgroundDrawableResource(R.drawable.lock_screen_background)
    }
}
