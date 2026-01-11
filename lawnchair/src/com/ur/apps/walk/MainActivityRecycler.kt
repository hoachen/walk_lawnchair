package com.ur.apps.walk

import android.animation.ValueAnimator
import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sg.UserManager
import com.sg.model.UserInfo
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.ad.AdShowScene
import com.ur.apps.ad.BaseAdLoader
import com.ur.apps.ad.NativeAdListener
import com.ur.apps.ad.topon.TopOnAdLoader
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.analysis.tenjin.TenjinManager
import com.ur.apps.utils.URLog
import com.ur.apps.walk.adapter.MainItemClickListener
import com.ur.apps.walk.adapter.MainRecyclerAdapter
import com.android.launcher3.databinding.ActivityMainRecyclerBinding
import com.ur.apps.walk.dialog.LuckyWheelDialog
import com.ur.apps.walk.model.MainItem
import com.ur.apps.walk.model.TaskModel
import com.ur.apps.walk.step.bean.ExerciseStats
import com.ur.apps.walk.step.callback.StepCountChangeCallBack
import com.ur.apps.walk.step.constants.StepConstants
import com.ur.apps.walk.step.manager.StepManager
import com.ur.apps.walk.utils.CoinAnimationUtils
import com.ur.apps.walk.utils.DialogUtils
import com.ur.apps.walk.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import com.android.launcher3.R
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.ur.apps.walk.constants.StatisticConstants
import com.ur.apps.walk.dialog.RateUsDialog
import com.ur.apps.walk.dialog.SetDefaultLauncherDialog
import com.ur.apps.walk.step.utils.SharedPreferencesUtils
import org.json.JSONObject


class MainActivityRecycler : BaseRewardActivity(), StepCountChangeCallBack,
    MainItemClickListener, NativeAdListener {

    private lateinit var binding: ActivityMainRecyclerBinding
    private lateinit var stepManager: StepManager
    private lateinit var viewModel: MainViewModel
    private lateinit var mainAdapter: MainRecyclerAdapter
    private lateinit var initialMainItems: List<MainItem>

    // 存储所有手指引导动画的视图，键为目标视图的hashCode
    private val handPointerMap = ConcurrentHashMap<Int, ImageView>()

    private val TAG = "MainActivityRecycler"
    private val FIRST_LAUNCH_PREF = "first_launch_pref"
    private val IS_FIRST_LAUNCH = "is_first_launch"

    // 每日步数目标
    private val dailyStepGoal = 1000
    private var currentStepCount = 0

    private var isComeBackShowAd = false

    // 新增浮动气球相关属性
    private lateinit var floatingBalloon: View
    private var balloonAnimator: ValueAnimator? = null
    private val random = Random()
    private var balloonSpeedX = 2f
    private var balloonSpeedY = 2f
    private var screenWidth = 0
    private var screenHeight = 0

    // 气球速度系数，值越小移动越慢
    private var balloonSpeedFactor = 0.5f

    // 任务常量
    private val TASK_ONE = 1000

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == StepConstants.STEP_PERMISSON_CPDE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 用户授予活动识别权限后，请求通知权限
                stepManager.restartStepDetector()
                checkAndRequestNotificationPermission()
            }
        } else if (requestCode == StepConstants.NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 用户授予通知权限，重新启动步数服务
                stepManager.restartStepDetector()
                // 使用重试逻辑重新启动服务
                stepManager.retryStartService()
                requestIgnoringBatteryOptimizations()
            }
        }
    }

    private fun requestIgnoringBatteryOptimizations() {
        if (!stepManager.isIgnoringBatteryOptimizations()) {
            stepManager.requestIgnoreBatteryOptimizations(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainRecyclerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.refreshUserInfo()

        // 先初始化StepManager
        initStepManager()

        // 然后初始化RecyclerView
        setupRecyclerView()

        isComeBackShowAd = false

        // 创建浮动气球
        createFloatingBalloon()

        // 检查是否是首次启动应用，并显示手指引导动画
        checkFirstLaunchAndShowGuide()
        statsReportDrawOverlays()
        AdLoaderManager.addNativeAdListener(this)
        AdLoaderManager.loadNativeAd(this)
        maybeShowReward(intent)
    }

    private fun statsReportDrawOverlays() {
        val isCanShow = Settings.canDrawOverlays(this)
        TDAnalyticsManager.reportTrackEvent(
            "overlays_permission_check",
            JSONObject().apply {
                put("hasOverlaysPermission", "$isCanShow")
            }
        )
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        maybeShowReward(intent)
    }

    private fun maybeShowReward(intent: Intent) {

        val taskId = intent.getIntExtra(ON_CLAIM_TID, -1)
        val goal = intent.getIntExtra(ON_CLAIM_GOAL, -1)
        URLog.i(TAG, "maybeShowReward taskId($taskId) goal($goal)")

        if (taskId != -1 && goal != -1) {
            Handler(Looper.getMainLooper()).postDelayed({
                URLog.i(TAG, "schedule handle task claim taskId($taskId) goal($goal)")
                handleTaskClaim(taskId, goal)
            }, 300)
        }
    }

    private fun initStepManager() {
        stepManager = StepManager.getInstance(this)
        stepManager.registerCallback(this)
        stepManager.restartStepDetector()
        // 首先请求活动识别权限
        if (!stepManager.checkActivityRecognitionPermission()) {
            stepManager.requestActivityRecognitionPermission(this)
        } else {
            // 如果已有活动识别权限，则请求通知权限
            checkAndRequestNotificationPermission()
        }
        stepManager.requestUpdateCallBack()
    }

    private fun checkAndRequestNotificationPermission() {
        if (!stepManager.checkNotificationPermission()) {
            URLog.i(TAG, " checkNotificationPermission, no Permission")
            stepManager.requestNotificationPermission(this)
        } else {
            requestIgnoringBatteryOptimizations()
        }
    }

    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerAdapter(this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivityRecycler)
            adapter = mainAdapter
            setHasFixedSize(true)
            itemAnimator = null  // 关闭所有动画
        }

        // 设置下拉刷新监听器
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 下拉刷新时调用updateAllItems方法
            refreshData()
        }

        // 设置下拉刷新颜色
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // 初始化数据
        initialMainItems = createInitialItems()
        mainAdapter.setItems(initialMainItems)
//        lifecycleScope.launch(Dispatchers.IO) { // test code
//            repeat(Int.MAX_VALUE) {
//                delay(10)
//                if (it % 10 == 0) {
//                    withContext(Dispatchers.Main) {
//                        onStepChange(
//                            ExerciseStats(it, it * 1.0, 100.0, it * 10L)
//                        )
//                    }
//                }
//            }
//        }
    }

    override fun onNativeAdLoaded(adLoader: BaseAdLoader) {
        super.onNativeAdLoaded(adLoader)
        URLog.i(TAG, "onNativeAdLoaded update native ad")
        mainAdapter.updateItem(MainItem.NativeAdItem)
    }

    private fun createInitialItems(): List<MainItem> {
        val items = mutableListOf<MainItem>()
        val stats = stepManager.getTodayStats()
        val currentSteps = stats.steps

        // 获取任务列表中的最大步数值作为每日目标
        val maxTaskStep = TaskModel.DEFAULT_TASKS.maxByOrNull { it.stepGoal }?.stepGoal ?: 8000

        // 1. 顶部栏
        items.add(MainItem.HeaderItem())

        // 2. 今日概览区域
        items.add(
            MainItem.SummaryItem(
                stepCount = currentSteps,
                dailyGoal = maxTaskStep, // 使用任务列表中的最大步数值作为每日目标
                distance = stats.distance,
                calories = stats.calories.toInt(),
                date = getCurrentDate()
            )
        )

        // 3. 任务区域（包含标题和任务列表）
        items.add(
            MainItem.TaskItem(
                taskId = 1,
                targetDistance = TASK_ONE,
                currentDistance = currentSteps,
                title = getString(R.string.earn_coins)
            )
        )

        // 4. Banner广告位
        items.add(MainItem.BannerAdItem)

        // 5. 最近7天步数趋势
        val weeklyData = getWeeklyStepData()
        val averageSteps = calculateAverageSteps(weeklyData)
        items.add(
            MainItem.WeeklyTrendItem(
                title = getString(R.string.weekly_trend_title),
                subtitle = getString(R.string.weekly_trend_subtitle),
                averageText = getString(
                    R.string.weekly_average_format,
                    formatStepCount(averageSteps)
                ),
                dailyData = weeklyData
            )
        )

        // 6. 今日成就区域
        items.add(
            MainItem.AchievementsItem(
                title = getString(R.string.achievements_title),
                subtitle = getString(R.string.achievements_subtitle),
                achievements = getTodayAchievements()
            )
        )

        // 7. 原生广告位（在页面最底部）
        items.add(MainItem.NativeAdItem)

        return items
    }

    private fun updateAllItems(stats: ExerciseStats) {
        val currentSteps = stats.steps

        // 更新所有item
        for (i in 0 until mainAdapter.itemCount) {
            when (val item = mainAdapter.getItemAt(i)) {
                is MainItem.SummaryItem -> {
                    // 更新SummaryItem
                    mainAdapter.updateItem(
                        i, item.copy(
                            stepCount = currentSteps,
                            distance = stats.distance,
                            calories = stats.calories.toInt()
                        )
                    )
                }

                is MainItem.TaskItem -> {
                    // 更新TaskItem
                    mainAdapter.updateItem(
                        i, item.copy(
                            currentDistance = currentSteps
                        )
                    )
                }

                is MainItem.WeeklyTrendItem -> {
                    // 更新WeeklyTrendItem（如果需要）
                    // 这里可以更新周趋势数据
                    val weeklyData = getWeeklyStepData()
                    val averageSteps = calculateAverageSteps(weeklyData)

                    mainAdapter.updateItem(
                        i,
                        item.copy(
                            averageText = getString(
                                R.string.weekly_average_format,
                                formatStepCount(averageSteps)
                            ),
                            dailyData = weeklyData
                        )
                    )
                }

                is MainItem.AchievementsItem -> {
                    // 更新AchievementsItem（如果需要）
                    // 这里可以更新成就数据
                    mainAdapter.updateItem(
                        i,
                        item.copy(
                            achievements = getTodayAchievements()
                        )
                    )
                }

                else -> {
                    // 其他item类型不需要更新
                }
            }
        }
    }

    /**
     * 刷新数据 - 下拉刷新时调用
     */
    private fun refreshData() {
        // 获取最新的步数统计数据
        val stats = stepManager.getTodayStats()

        // 更新所有item的数据
        updateAllItems(stats)

        // 延迟1秒后停止刷新动画，模拟网络请求
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshLayout.isRefreshing = false
            showToast(getString(R.string.refresh_completed))
        }, 1000)
    }

    /**
     * 处理任务操作（模拟奖励领取逻辑）
     */
    private fun handleTaskAction(taskId: Int, targetDistance: Int, currentDistance: Int) {
        if (currentDistance >= targetDistance) {
            showRewardAd()
        } else {
            // 任务未完成，提示用户继续步行
            val remaining = targetDistance - currentDistance
            // 使用资源字符串，支持多语言
            showToast(getString(R.string.task_remaining_steps, remaining))
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onStepChange(stepCount: ExerciseStats?) {
        stepCount?.let {
            // 确保mainAdapter已经初始化
            if (::mainAdapter.isInitialized) {
                updateAllItems(stepCount)
            }
        }
    }

    override fun onStepReachPeriod(hundred_level: Int) {
        if (UserManager.instance.isEarningEnabled()) {
            URLog.i(TAG, "onStepReachPeriod ($hundred_level)")
            when (hundred_level) {
                1, 3, 5 -> {
                    URLog.i(
                        TAG,
                        "onStepReachPeriod ($hundred_level) createHandPointerGuide for Earn Coins button"
                    )
                    // 为Earn Coins按钮创建手指引导
                    getEarnCoinsButtonView()?.let { view ->
                        createHandPointerGuide(view)
                    }
                }

                else -> {
                    // 其他情况
                }
            }

            // 显示激励按钮
            mainAdapter.updateButtons(
                MainItem.ButtonsItem(
                    isEarningEnabled = true,
                    showInspirationButton = true
                )
            )

            // 为激励按钮创建手指引导
            Handler(Looper.getMainLooper()).postDelayed({
                getInspirationButtonView()?.let { view ->
                    createHandPointerGuide(view)
                }
            }, 500) // 延迟500ms，确保View已经创建
        }
    }

    override fun onStart() {
        super.onStart()
        TenjinManager.onMainActivityStart()
        checkDefaultLauncher()
    }

    override fun onResume() {
        super.onResume()

        // 重启气球动画
        if (::floatingBalloon.isInitialized && balloonAnimator?.isRunning != true) {
            startBalloonAnimation()
        }

        // 在用户与应用交互后启动步数服务
        if (stepManager.checkNotificationPermission() && stepManager.checkActivityRecognitionPermission()) {
            // 如果所有权限都已获取，使用更强的重试逻辑
            if (stepManager.isIgnoringBatteryOptimizations()) {
                // 所有权限都有了，强制重试启动服务
                stepManager.retryStartService()
            }
        }
        (application as WalkApplication).startStepService()

        if (UserManager.instance.isEarningEnabled()) {
            startRepeatingTask()
        }

        AdLoaderManager.loadRewardVideoAd(this)
        AdLoaderManager.loadInterstitialAd(this)

        /**
         *
         * 从别的页面回来弹广告
         */
        if (isComeBackShowAd) {
            showRewardOrInterstitialAd()
            isComeBackShowAd = false
        }

        tryInitSAdSdk()
    }

    private fun showRewardOrInterstitialAd() {
        if (AdLoaderManager.isRewardVideoAdReady()) {
            AdLoaderManager.showRewardVideoAd(this)
        } else if (AdLoaderManager.isInterstitialAdReady()) {
            AdLoaderManager.showInterstitialAd(this)
        }
    }

    // 启动任务
    private fun startRepeatingTask() {
        // 宝箱逻辑暂时保留，但需要适配RecyclerView
        lifecycleScope.launch {
            // 延迟 30 秒
            delay(30000L) // 30秒

            if (UserManager.instance.isEarningEnabled()) {
                // 这里需要更新宝箱图标的显示状态
                // 由于宝箱图标在StepCountItem中，我们需要特殊处理
            }
        }
    }

    override fun handleUserInfoChanged(userInfo: UserInfo) {
        super.handleUserInfoChanged(userInfo)
        // 显示金币的数量 - 需要在HeaderItem中更新
        // 这里需要更新HeaderItem中的金币数量
    }

    override fun showBannerInCoinDialog(): Boolean {
        return false
    }

    override fun onPause() {
        super.onPause()
        // 停止气球动画
        balloonAnimator?.cancel()
    }

    override fun onStop() {
        super.onStop()

        // 停止所有手指引导动画
        removeAllHandPointers()
        // 停止气球动画
        balloonAnimator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理所有手指引导动画
        removeAllHandPointers()
        // 停止气球动画
        balloonAnimator?.cancel()
        AdLoaderManager.removeNativeAdListener(this)
    }

    /**
     * 移除某个目标视图的手指引导动画
     * @param targetView 目标视图
     */
    private fun removeHandPointerForTarget(targetView: View) {
        val key = targetView.hashCode()
        handPointerMap[key]?.let { handPointerView ->
            handPointerView.clearAnimation()
            (handPointerView.parent as? ViewGroup)?.removeView(handPointerView)
            handPointerMap.remove(key)
        }
    }

    /**
     * 移除所有手指引导动画
     */
    private fun removeAllHandPointers() {
        for (handPointer in handPointerMap.values) {
            handPointer.clearAnimation()
            (handPointer.parent as? ViewGroup)?.removeView(handPointer)
        }
        handPointerMap.clear()
    }

    /**
     * 创建手指指引动画
     * 注意：由于现在是RecyclerView，这个方法需要适配新的结构
     */
    private fun createHandPointerGuide(
        targetView: View,
    ) {
        // 检查是否已经为该目标视图创建了手指引导动画
        val targetKey = targetView.hashCode()
        if (handPointerMap.containsKey(targetKey)) {
            // 已存在，不重复创建
            return
        }

        // 创建一个ImageView作为手指指针
        val handPointerImageView = ImageView(this)
        handPointerImageView.setImageResource(R.drawable.ic_hand_pointer)
        handPointerImageView.setColorFilter(
            getThemeColor(
                com.google.android.material.R.attr.colorOnSecondaryContainer
            )
        )

        // 存储到Map中
        handPointerMap[targetKey] = handPointerImageView

        // 设置固定大小为30dp
        val sizeDp = 30
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        handPointerImageView.layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)

        // 获取目标视图的父容器（ViewHolder的itemView）
        val parentContainer = targetView.parent as? ViewGroup
        if (parentContainer != null) {
            // 设置父容器的clipChildren和clipToPadding为false，允许子视图超出边界
            parentContainer.clipChildren = false
            parentContainer.clipToPadding = false

            // 添加到目标视图的父容器中
            parentContainer.addView(handPointerImageView)

            // 设置手指图标的Z轴顺序，使其显示在最上层
            handPointerImageView.bringToFront()
        } else {
            // 如果无法获取父容器，则添加到全局容器中
            binding.handPointerContainer.addView(handPointerImageView)
        }

        // 延迟500ms后开始动画，确保视图已经布局完成
        handPointerImageView.post {
            // 获取目标视图在父容器中的相对位置
            val targetLocation = IntArray(2)
            targetView.getLocationInWindow(targetLocation)

            // 获取手指图标父容器在窗口中的位置
            val parentLocation = IntArray(2)
            handPointerImageView.parent?.let { parent ->
                if (parent is View) {
                    parent.getLocationInWindow(parentLocation)
                }
            }

            val targetWidth = targetView.width
            val targetHeight = targetView.height

            // 手指图标已设置固定大小
            val handWidth = sizePx
            val handHeight = sizePx

            // 计算手指的位置，使其水平居中对准目标视图
            // 使用相对位置计算，确保手指图标在父容器中的正确位置
            val targetCenterX = targetLocation[0] - parentLocation[0] + targetWidth / 2
            handPointerImageView.x = targetCenterX - handWidth / 2f

            // 计算目标视图底部位置（相对于父容器）
            val targetBottom = targetLocation[1] - parentLocation[1] + targetHeight

            // 设置初始Y位置，在目标下方指定距离处
            val safeInitialOffset = 0f
            handPointerImageView.y = (targetBottom + safeInitialOffset)

            // 设置动画距离
            val safetyMargin = 20
            val safeAnimDistance = -(safeInitialOffset - safetyMargin)

            // 创建从下往上的动画
            val animation = TranslateAnimation(
                0f, 0f,  // X轴不移动
                0f, safeAnimDistance // Y轴移动的距离，确保安全
            )
            animation.duration = 1500 // 动画持续1.5秒
            animation.repeatCount = Animation.INFINITE  // 无限重复，直到用户点击
            animation.repeatMode = Animation.REVERSE // 反向重复
            animation.fillAfter = true // 动画结束后保持结束状态

            URLog.d(
                TAG,
                "动画设置：目标视图=${targetView}, 父容器=${handPointerImageView.parent}, " +
                        "目标位置=(${targetLocation[0]}, ${targetLocation[1]}), " +
                        "父位置=(${parentLocation[0]}, ${parentLocation[1]}), " +
                        "手指位置=(${handPointerImageView.x}, ${handPointerImageView.y}), " +
                        "移动距离=$safeAnimDistance"
            )

            // 开始动画
            handPointerImageView.startAnimation(animation)
        }
    }

    /**
     * 检查是否是首次启动应用，并显示手指引导动画
     */
    private fun checkFirstLaunchAndShowGuide() {
        // 判断是否是首次启动应用
        val sharedPreferences = getSharedPreferences(FIRST_LAUNCH_PREF, Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean(IS_FIRST_LAUNCH, true)

        if (isFirstLaunch /*|| BuildConfig.DEBUG*/) {
            // 显示抽奖对话框
            showLuckyPrizeWheelDialog()

            // 更新首次启动标志
            sharedPreferences.edit().putBoolean(IS_FIRST_LAUNCH, false).apply()
        }
    }

    private fun showLuckyPrizeWheelDialog() {
        val dialog = LuckyWheelDialog(this) {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    showRewardAd(AdShowScene.REDEEM)
                    it.dismiss()
                },
                1500
            )
        }
        dialog.show()
    }

    /**
     * 创建浮动气球
     */
    private fun createFloatingBalloon() {
        // 使用LayoutInflater加载布局
        val inflater = LayoutInflater.from(this)
        floatingBalloon = inflater.inflate(
            R.layout.layout_floating_balloon,
            binding.activityMainRecyclerParent,
            false
        )

        // 添加到布局中
        binding.activityMainRecyclerParent.addView(floatingBalloon)

        floatingBalloon.setOnClickListener {
            showRewardAd()
        }

        // 获取屏幕尺寸以检测边界
        binding.root.post {
            screenWidth = binding.root.width
            screenHeight = binding.root.height

            // 开始气球动画，以默认速度
            startBalloonAnimation()
        }
    }

    override fun handleUserWithdrawEnableChanged(enable: Boolean) {
        super.handleUserWithdrawEnableChanged(enable)
        if (enable) {
            // 创建手指指引动画，指向金币按钮
            getCoinButtonView()?.let { view ->
                createHandPointerGuide(view)
            }
        }
    }

    /**
     * 开始气球飘浮动画
     * @param speedFactor 速度系数，值越小移动越慢，默认0.5f
     */
    private fun startBalloonAnimation() {
        val speedFactor: Float = 0.4f
        // 更新速度系数
        balloonSpeedFactor = speedFactor

        // 初始化随机速度
        balloonSpeedX =
            (0.5f + random.nextFloat()) * speedFactor * if (random.nextBoolean()) 1 else -1
        balloonSpeedY =
            (0.5f + random.nextFloat()) * speedFactor * if (random.nextBoolean()) 1 else -1

        // 创建动画更新器
        balloonAnimator = ValueAnimator.ofFloat(0f, 1f)
        balloonAnimator?.apply {
            duration = 16 // 每帧16毫秒，约60fps
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener { _ ->
                // 计算新位置
                updateBalloonPosition()
            }

            start()
        }
    }

    /**
     * 更新气球位置
     */
    private fun updateBalloonPosition() {
        // 获取当前位置
        val balloonSize = floatingBalloon.width

        // 更新坐标
        var x = floatingBalloon.x + balloonSpeedX
        var y = floatingBalloon.y + balloonSpeedY

        // 检查边界碰撞
        if (x <= 0 || x + balloonSize >= screenWidth) {
            balloonSpeedX *= -1 // 碰到左右边界，反向
            // 加入一点随机性
            balloonSpeedX += (random.nextFloat() * 0.5f - 0.25f)
            x = if (x <= 0) 0f else (screenWidth - balloonSize).toFloat()
        }

        if (y <= 0 || y + balloonSize >= screenHeight) {
            balloonSpeedY *= -1 // 碰到上下边界，反向
            // 加入一点随机性
            balloonSpeedY += (random.nextFloat() * 0.5f - 0.25f)
            y = if (y <= 0) 0f else (screenHeight - balloonSize).toFloat()
        }

        // 施加位置
        floatingBalloon.x = x
        floatingBalloon.y = y
    }

    // MainItemClickListener 接口实现
    override fun onProfileClick() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    override fun onCoinClick() {
        isComeBackShowAd = true
        val intent = Intent(this@MainActivityRecycler, WithdrawActivity::class.java)
        startActivity(intent)
    }

    override fun onEarnCoinsClick() {
        isComeBackShowAd = true
        val intent = Intent(this, CoinTaskActivity::class.java)
        startActivity(intent)
    }

    override fun onInspirationClick() {
        // 移除手指引导动画
        getInspirationButtonView()?.let { view ->
            removeHandPointerForTarget(view)
        }

        // 使用CoinAnimationUtils隐藏金币入口
        getInspirationButtonView()?.let { view ->
            CoinAnimationUtils.hideWithFadeOut(view) {
                // 在动画结束后显示激励对话框
                TDAnalyticsManager.reportButtonShow(
                    adType = "Reward",
                    adPosition = "Task_Reward",
                    adPositionType = "Reward",
                    placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
                )
                DialogUtils.showInspirationDialog(this) {
                    showRewardAd()
                }
            }
        }
    }

    override fun onTaskClick(taskId: Int, targetDistance: Int, currentDistance: Int) {
        handleTaskAction(taskId, targetDistance, currentDistance)
    }

    override fun onCloseTaskClick() {
        // 隐藏任务item，这里需要更新数据
        // 暂时简单处理：更新任务item的可见性
        // 在实际应用中，可能需要从items列表中移除或隐藏
    }

    override fun onTreasureClick() {
        showRewardAd()
        // 隐藏宝箱图标，这里需要更新StepCountItem
        // 在实际应用中，需要更新对应的item
    }

    override fun onTaskClaimClick(taskId: Int, stepGoal: Int) {
        // 处理任务领取
        handleTaskClaim(taskId, stepGoal)
    }

    // 获取Earn Coins按钮的View
    private fun getEarnCoinsButtonView(): View? {
        // 找到ButtonsItem的位置
        val position = findItemPositionByType(MainItem.TYPE_BUTTONS)
        if (position != -1) {
            val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder is com.ur.apps.walk.adapter.ButtonsViewHolder) {
                return viewHolder.itemView.findViewById(R.id.btn_earn_coins)
            }
        }
        return null
    }

    // 获取激励按钮的View
    private fun getInspirationButtonView(): View? {
        // 找到ButtonsItem的位置
        val position = findItemPositionByType(MainItem.TYPE_BUTTONS)
        if (position != -1) {
            val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder is com.ur.apps.walk.adapter.ButtonsViewHolder) {
                return viewHolder.itemView.findViewById(R.id.display_inspiration)
            }
        }
        return null
    }

    // 获取金币按钮的View
    private fun getCoinButtonView(): View? {
        // 找到HeaderItem的位置
        val position = findItemPositionByType(MainItem.TYPE_HEADER)
        if (position != -1) {
            val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder is com.ur.apps.walk.adapter.HeaderViewHolder) {
                return viewHolder.itemView.findViewById(R.id.btn_withdraw)
            }
        }
        return null
    }

    // 根据item类型查找位置
    private fun findItemPositionByType(itemType: Int): Int {
        for (i in 0 until mainAdapter.itemCount) {
            if (mainAdapter.getItemViewType(i) == itemType) {
                return i
            }
        }
        return -1
    }

    /**
     * 处理任务领取
     */
    private fun handleTaskClaim(taskId: Int, stepGoal: Int) {
        URLog.i(TAG, "handleTaskClaim taskId($taskId) goal($stepGoal)")

        val stats = stepManager.getTodayStats()
        val currentSteps = stats.steps

        if (currentSteps >= stepGoal) {
            // 标记任务为已领取
            if (showRewardAd().isSuccess) {
                URLog.i(TAG, "maybeShowReward isSuccess taskId($taskId) goal($stepGoal)")
                TaskModel.markTaskAsClaimed(this, taskId)
                // 更新任务列表显示
                updateTaskItem()
            } else {
                URLog.i(TAG, "maybeShowReward is failed taskId($taskId) goal($stepGoal)")
            }
        } else {
            // 任务未完成
            val remaining = stepGoal - currentSteps
            showToast(getString(R.string.task_remaining_steps, remaining))
        }
    }

    /**
     * 更新任务item
     */
    private fun updateTaskItem() {
        val stats = stepManager.getTodayStats()
        // 找到TaskItem的位置并更新
        for (i in 0 until mainAdapter.itemCount) {
            if (mainAdapter.getItemViewType(i) == MainItem.TYPE_TASK) {
                mainAdapter.updateItem(
                    i, MainItem.TaskItem(
                        taskId = 1,
                        targetDistance = TASK_ONE,
                        currentDistance = stats.steps,
                        title = getString(R.string.earn_coins)
                    )
                )
                break
            }
        }
    }

    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat(
            getString(R.string.lock_date_format_full),
            java.util.Locale.getDefault()
        )
        return dateFormat.format(java.util.Date())
    }

    /**
     * 获取最近7天步数数据（真实数据 + 默认数据）
     */
    private fun getWeeklyStepData(): List<MainItem.WeeklyTrendItem.DailyStepData> {
        try {
            // 从数据库获取所有步数数据
            val allStepData =
                com.ur.apps.walk.step.utils.DbUtils.getQueryAll(com.ur.apps.walk.step.bean.StepData::class.java)

            // 过滤最近7天的数据
            val filteredData = filterLastNDays(allStepData, 7)

            // 如果没有数据，返回示例数据
            if (filteredData.isEmpty()) {
                return getSampleWeeklyData()
            }

            // 按天聚合数据，确保每天只有一个数据点
            val aggregatedData = aggregateDailyData(filteredData)

            // 转换为WeeklyTrendItem.DailyStepData格式
            return aggregatedData.mapIndexed { index, stepData ->
                // 获取星期几标签（一、二、三...日）
                val dayLabel = getDayOfWeekLabel(stepData.today)

                // 获取步数
                val stepCount = stepData.step?.toIntOrNull() ?: 0

                // 计算百分比（相对于本周最大步数）
                val maxSteps = aggregatedData.maxOfOrNull { it.step?.toIntOrNull() ?: 0 } ?: 1
                val percentage = if (maxSteps > 0) {
                    stepCount.toFloat() / maxSteps
                } else {
                    0f
                }

                MainItem.WeeklyTrendItem.DailyStepData(dayLabel, stepCount, percentage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果出现异常，返回示例数据
            return getSampleWeeklyData()
        }
    }

    /**
     * 获取示例周数据（用于UI展示）
     */
    private fun getSampleWeeklyData(): List<MainItem.WeeklyTrendItem.DailyStepData> {
        // 获取当前日期
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        // 示例数据：过去7天的步数
        val sampleData = listOf(
            5100, // 周一
            6200, // 周二
            4300, // 周三
            7000, // 周四
            6000, // 周五
            3200, // 周六
            7500  // 周日
        )

        // 获取星期几标签
        val dayLabels = listOf(
            getString(R.string.weekday_monday),
            getString(R.string.weekday_tuesday),
            getString(R.string.weekday_wednesday),
            getString(R.string.weekday_thursday),
            getString(R.string.weekday_friday),
            getString(R.string.weekday_saturday),
            getString(R.string.weekday_sunday)
        )

        // 计算最大步数用于百分比计算
        val maxSteps = sampleData.maxOrNull() ?: 1

        return sampleData.mapIndexed { index, stepCount ->
            val dayLabel = dayLabels[index]
            val percentage = if (maxSteps > 0) {
                stepCount.toFloat() / maxSteps
            } else {
                0f
            }

            MainItem.WeeklyTrendItem.DailyStepData(dayLabel, stepCount, percentage)
        }
    }

    /**
     * 过滤最近N天的数据
     */
    private fun filterLastNDays(
        stepDataList: List<com.ur.apps.walk.step.bean.StepData>,
        days: Int
    ): List<com.ur.apps.walk.step.bean.StepData> {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days + 1) // +1 是为了包含今天

        val sdf = java.text.SimpleDateFormat(
            com.ur.apps.walk.step.constants.StepConstants.DATE_FORMAT_FULL,
            java.util.Locale.getDefault()
        )
        val startDate = sdf.format(calendar.time)

        return stepDataList.filter {
            val today = it.today
            if (today == null) false else today.compareTo(startDate) >= 0
        }.sortedBy { it.today }
    }

    /**
     * 将数据按天聚合，确保每天只有一个数据点
     */
    private fun aggregateDailyData(stepDataList: List<com.ur.apps.walk.step.bean.StepData>): List<com.ur.apps.walk.step.bean.StepData> {
        // 确保有一周完整的数据（7天）
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat(
            com.ur.apps.walk.step.constants.StepConstants.DATE_FORMAT_FULL,
            java.util.Locale.getDefault()
        )
        val result = mutableListOf<com.ur.apps.walk.step.bean.StepData>()

        // 创建一个包含最近7天日期的集合
        val dateMap = mutableMapOf<String, Int>()
        for (i in 6 downTo 0) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(calendar.time)
            dateMap[dateStr] = 0
            calendar.add(java.util.Calendar.DAY_OF_YEAR, i) // 复位
        }

        // 将原始数据填入对应日期
        for (data in stepDataList) {
            val date = data.today
            if (date != null && dateMap.containsKey(date)) {
                dateMap[date] = data.step?.toIntOrNull() ?: 0
            }
        }

        // 按日期排序并创建新的StepData列表
        dateMap.entries.sortedBy { it.key }.forEach { (date, steps) ->
            val stepData = com.ur.apps.walk.step.bean.StepData()
            stepData.today = date
            stepData.step = steps.toString()
            result.add(stepData)
        }

        return result
    }

    /**
     * 根据日期获取星期几标签
     */
    private fun getDayOfWeekLabel(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""

        return try {
            val sdf = java.text.SimpleDateFormat(
                com.ur.apps.walk.step.constants.StepConstants.DATE_FORMAT_FULL,
                java.util.Locale.getDefault()
            )
            val date = sdf.parse(dateStr) ?: return ""

            val calendar = java.util.Calendar.getInstance()
            calendar.time = date

            // 获取星期几（1=周日，2=周一，...，7=周六）
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

            when (dayOfWeek) {
                java.util.Calendar.SUNDAY -> getString(R.string.weekday_sunday)
                java.util.Calendar.MONDAY -> getString(R.string.weekday_monday)
                java.util.Calendar.TUESDAY -> getString(R.string.weekday_tuesday)
                java.util.Calendar.WEDNESDAY -> getString(R.string.weekday_wednesday)
                java.util.Calendar.THURSDAY -> getString(R.string.weekday_thursday)
                java.util.Calendar.FRIDAY -> getString(R.string.weekday_friday)
                java.util.Calendar.SATURDAY -> getString(R.string.weekday_saturday)
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取今日成就数据（动态计算）
     */
    private fun getTodayAchievements(): List<MainItem.AchievementsItem.Achievement> {
        val stats = stepManager.getTodayStats()
        val currentSteps = stats.steps

        // 1. 获取任务完成情况
        val tasks = TaskModel.updateTaskCompletion(this, currentSteps)
        val completedTasks = TaskModel.getCompletedCount(tasks)
        val totalTasks = TaskModel.getTotalCount()

        // 2. 计算任务完成百分比（用于"超过百分之多少用户"）
        val taskCompletionPercentage = if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks * 100).toInt()
        } else {
            0
        }

        // 3. 获取任务列表中的最大步数值
        val maxTaskStep = TaskModel.DEFAULT_TASKS.maxByOrNull { it.stepGoal }?.stepGoal ?: 8000

        // 4. 计算今日行走距离（公里）
        val distanceKm = String.format("%.1f", stats.distance)

        // 5. 计算剩余步数
        val remainingSteps = maxTaskStep - currentSteps

        return listOf(
            // 第一个成就：今日超过用户的统计行为（根据任务完成百分比计算）
            MainItem.AchievementsItem.Achievement(
                icon = "🏅",
                title = getString(R.string.ach_over_users_title_format, taskCompletionPercentage),
                description = getString(
                    R.string.ach_over_users_desc_format,
                    completedTasks,
                    totalTasks
                ),
                tag = getString(R.string.ach_over_users_tag_format, taskCompletionPercentage / 5)
            ),
            // 第二个成就：今日行走距离成就
            MainItem.AchievementsItem.Achievement(
                icon = "🚶",
                title = getString(R.string.ach_today_distance_title_format, distanceKm),
                description = getString(
                    R.string.ach_today_distance_desc_format,
                    (stats.distance * 2.5).toInt()
                ),
                tag = getString(R.string.ach_today_distance_tag)
            ),
            // 第三个成就：按照最大任务值显示
            MainItem.AchievementsItem.Achievement(
                icon = "🎯",
                title = getString(R.string.ach_complete_tasks_title_format, completedTasks),
                description = getString(R.string.ach_complete_tasks_desc_format, remainingSteps),
                tag = getString(R.string.ach_complete_tasks_tag_format, maxTaskStep)
            )
        )
    }

    /**
     * 计算平均步数
     */
    private fun calculateAverageSteps(dailyData: List<MainItem.WeeklyTrendItem.DailyStepData>): Int {
        if (dailyData.isEmpty()) {
            return 0
        }

        val totalSteps = dailyData.sumOf { it.stepCount }
        return totalSteps / dailyData.size
    }

    /**
     * 格式化步数显示
     */
    private fun formatStepCount(steps: Int): String {
        return if (steps >= 1000) {
            "${steps / 1000}.${(steps % 1000) / 100}k"
        } else {
            steps.toString()
        }
    }

    private fun checkDefaultLauncher() {
        if (isFinishing || isDestroyed) return

        val isDefault = com.ur.apps.walk.utils.LauncherDefaultUtils.isDefaultLauncher(this)
        val fragmentManager = supportFragmentManager
        val existingDialog = fragmentManager.findFragmentByTag(SetDefaultLauncherDialog.TAG)
        val existingRateDialog = fragmentManager.findFragmentByTag(RateUsDialog.TAG)

        if (isDefault) {
            TDAnalyticsManager.reportTrackEvent(
                StatisticConstants.LAUNCHER_DEFAULT,
                JSONObject().put(StatisticConstants.TYPE, "true")
            )

            if (existingDialog != null) {
                (existingDialog as? androidx.fragment.app.DialogFragment)?.dismissAllowingStateLoss()
                Handler(Looper.getMainLooper()).postDelayed({
                    Process.killProcess(Process.myPid())
                }, 0)
            } else {
                // Already default, normal run.
                // Check if we should show Rate Us
                val prefs = SharedPreferencesUtils(this)
                val hasRated = prefs.getParam(RateUsDialog.PREF_KEY_HAS_RATED, false) as Boolean

                if (!hasRated && existingRateDialog == null) {
                    RateUsDialog.newInstance().show(fragmentManager, RateUsDialog.TAG)
                }
            }
        } else {
            if (existingDialog == null) {
                SetDefaultLauncherDialog.newInstance().show(fragmentManager, SetDefaultLauncherDialog.TAG)
            }
        }
    }

    private fun getThemeColor(attrResId: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }

    companion object {
        const val ON_CLAIM_TID: String = "aaa"
        const val ON_CLAIM_GOAL: String = "bbb"
    }
}
