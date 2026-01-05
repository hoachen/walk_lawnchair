package com.ur.apps.walk//package com.ur.apps.walk
//
//import android.animation.ValueAnimator
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import com.ur.apps.utils.URLog
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.animation.Animation
//import android.view.animation.LinearInterpolator
//import android.view.animation.TranslateAnimation
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import com.sg.UserManager
//import com.sg.model.UserInfo
//import com.ur.apps.ad.AdLoaderManager
//import com.ur.apps.ad.AdShowScene
//import com.ur.apps.ad.topon.TopOnAdLoader
//import com.ur.apps.analysis.td.TDAnalyticsManager
//import com.ur.apps.analysis.tenjin.TenjinManager
//import com.ur.apps.walk.databinding.ActivityMainBackBinding
//import com.ur.apps.walk.databinding.ActivityMainBinding
//import com.ur.apps.walk.dialog.LuckyWheelDialog
//import com.ur.apps.walk.step.bean.ExerciseStats
//import com.ur.apps.walk.step.callback.StepCountChangeCallBack
//import com.ur.apps.walk.step.constants.StepConstants
//import com.ur.apps.walk.step.manager.StepManager
//import com.ur.apps.walk.utils.CoinAnimationUtils
//import com.ur.apps.walk.utils.DialogUtils
//import com.ur.apps.walk.utils.HeartPulseAnimator
//import com.ur.apps.walk.viewmodel.MainViewModel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import java.util.Random
//import java.util.concurrent.ConcurrentHashMap
//
//class MainActivity_Back : BaseRewardActivity(), StepCountChangeCallBack {
//
//    private lateinit var binding: ActivityMainBackBinding
//    private lateinit var stepManager: StepManager
//    private lateinit var viewModel: MainViewModel
//
//    // 存储所有手指引导动画的视图，键为目标视图的hashCode
//    private val handPointerMap = ConcurrentHashMap<Int, ImageView>()
//
//    private val TAG = "MainActivity"
//    private val FIRST_LAUNCH_PREF = "first_launch_pref"
//    private val IS_FIRST_LAUNCH = "is_first_launch"
//
//    // 每日步数目标
//    private val dailyStepGoal = 1000
//    private var currentStepCount = 0
//
//    private var isComeBackShowAd = false
//
//    // 任务常量
//    private val TASK_ONE = 1000
//
//    // 心形圆圈视图列表
//    private lateinit var heartCircles: List<ImageView>
//
//    // 新增浮动气球相关属性
//    private lateinit var floatingBalloon: View
//    private var balloonAnimator: ValueAnimator? = null
//    private val random = Random()
//    private var balloonSpeedX = 2f
//    private var balloonSpeedY = 2f
//    private var screenWidth = 0
//    private var screenHeight = 0
//
//    // 气球速度系数，值越小移动越慢
//    private var balloonSpeedFactor = 0.5f
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == StepConstants.STEP_PERMISSON_CPDE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
//                // 用户授予活动识别权限后，请求通知权限
//                stepManager.restartStepDetector()
//                checkAndRequestNotificationPermission()
//            }
//        } else if (requestCode == StepConstants.NOTIFICATION_PERMISSION_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
//                // 用户授予通知权限，重新启动步数服务
//                stepManager.restartStepDetector()
//                // 使用重试逻辑重新启动服务
//                stepManager.retryStartService()
//                requestIgnoringBatteryOptimizations()
//            }
//        }
//    }
//
//    private fun requestIgnoringBatteryOptimizations() {
//        if (!stepManager.isIgnoringBatteryOptimizations()) {
//            stepManager.requestIgnoreBatteryOptimizations(this)
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBackBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
//        viewModel.refreshUserInfo()
//        initStepManager()
//        setupViews()
//        setupHeartCircleAnimations()
//        updateStepCount()
//        isComeBackShowAd = false
//        // 初始化ViewModel
//
//        // 创建浮动气球
//        createFloatingBalloon()
//
//        // 检查是否是首次启动应用，并显示手指引导动画
//        checkFirstLaunchAndShowGuide()
//    }
//
//    private fun initStepManager() {
//        //随机生成45天数据
////        if(BuildConfig.DEBUG){
////            ExerciseDataGenerator.generateExerciseData(45);
////        }
//
//        stepManager = StepManager.getInstance(this)
//        stepManager.registerCallback(this)
//        stepManager.restartStepDetector()
//        // 首先请求活动识别权限
//        if (!stepManager.checkActivityRecognitionPermission()) {
//            stepManager.requestActivityRecognitionPermission(this)
//        } else {
//            // 如果已有活动识别权限，则请求通知权限
//            checkAndRequestNotificationPermission()
//        }
//        stepManager.requestUpdateCallBack()
//    }
//
//    private fun checkAndRequestNotificationPermission() {
//        if (!stepManager.checkNotificationPermission()) {
//            URLog.i(TAG, " checkNotificationPermission, no Permission")
//            stepManager.requestNotificationPermission(this)
//        } else {
//            requestIgnoringBatteryOptimizations()
//        }
//    }
//
//    private fun setupViews() {
//        // 设置左上角个人资料按钮点击事件
//        binding.btnProfile.setOnClickListener {
//            val intent = Intent(this, ProfileActivity::class.java)
//            startActivity(intent)
//        }
//
//        // 设置底部"Earn Coins"按钮点击事件
//        binding.btnEarnCoins.setOnClickListener {
//            isComeBackShowAd = true
//            // 如果该视图有对应的手指引导动画，则移除它
//            removeHandPointerForTarget(binding.btnEarnCoins)
//
//            val intent = Intent(this, CoinTaskActivity::class.java)
//            startActivity(intent)
//        }
//
//        // 设置右上角金币按钮点击事件，打开提现页面
//        val tvCoins = binding.coinCircleButton.tvCoins
//        tvCoins?.setOnClickListener {
//            isComeBackShowAd = true
//
//            // 如果该视图有对应的手指引导动画，则移除它
//            removeHandPointerForTarget(tvCoins)
//
//            val intent = Intent(this@MainActivity_Back, WithdrawActivity::class.java)
//            startActivity(intent)
//        }
//
//        setupTaskLayout()
//
//        // 初始化心形圆圈视图列表 - 从内到外排序
//        heartCircles = listOf(
//            binding.mainHeartCircle1,
//            binding.mainHeartCircle2,
//            binding.mainHeartCircle3,
//            binding.mainHeartCircle4
//        )
//
//        // banner广告
//        showBannerAd(binding.bannerAdContainer)
//    }
//
//    private fun setupTaskLayout() {
//        // 设置任务容器关闭按钮点击事件
//        binding.btnCloseTaskContainer.setOnClickListener {
//            binding.layoutTaskFirstContainer.visibility = View.GONE
//        }
//        binding.layoutTaskFirst.apply {
//            // 根据当前行走距离判断任务是否完成
//            // 设置任务标题
//            root.findViewById<TextView>(R.id.tv_task_title).text = getString(
//                R.string.task_entry_get_extra_coin,
//            )
//
//            taskItem.setOnClickListener {
//                val todaySteps = stepManager.getTodaySteps()
//                handleTaskAction(1, TASK_ONE, todaySteps)
//            }
//        }
//    }
//
//    /**
//     * 处理任务操作（模拟奖励领取逻辑）
//     */
//    private fun handleTaskAction(taskId: Int, targetDistance: Int, currentDistance: Int) {
//        if (currentDistance >= targetDistance) {
//            showRewardAd()
//        } else {
//            // 任务未完成，提示用户继续步行
//            val remaining = targetDistance - currentDistance
//            // 使用资源字符串，支持多语言
//            showToast(getString(R.string.task_remaining_steps, remaining))
//        }
//    }
//
//    private fun showToast(message: String) {
//        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
//    }
//
//    /**
//     * 设置心形圆圈的水波纹动画
//     */
//    private fun setupHeartCircleAnimations() {
//        // 停止可能已存在的动画
//        HeartPulseAnimator.stopAllAnimations(heartCircles + listOf(binding.mainHeart))
//
//        // 为心形圆圈设置波纹动画 - 修改参数使动画更流畅
//        HeartPulseAnimator.createHeartPulseAnimation(
//            circles = heartCircles,
//            baseDelay = 300,    // 调整延迟时间，使连续波纹更丝滑
//            duration = 3500    // 增加持续时间，让动画更平滑
//        )
//
//        // 为中心的心形设置更温和的心跳动画
//        // 使用温和心跳动画方法，确保使用正确的方法名
//        HeartPulseAnimator.createGentleHeartbeatAnimation(
//            binding.mainHeart,  // ID为main_heart的视图
//            2500                // 更长的动画周期，使心跳更加缓慢
//        )
//    }
//
//    private fun updateStepCount() {
//
//        val stats = stepManager.getTodayStats()
//        URLog.i(TAG, "updateStepCount: " + stats)
//        // 更新步数
//        binding.tvSteps.text = stats.steps.toString()
//
//
//        // 更新距离
//        binding.tvDistanceValue.text = String.format("%.2f", stats.distance)
//
//        // 更新卡路里
//        binding.tvCaloriesValue.text = stats.calories.toInt().toString()
//
//        // 更新时间（分钟）
//        binding.tvTimeValue.text = stats.duration.toString()
//
//        // 更新进度条
//        updateProgressBar()
//    }
//
//    private fun updateProgressBar() {
//        // 设置进度条最大值，使用XML中设置的值
////        binding.progressBarSteps.max = dailyStepGoal
////        val newStepCount = stepManager.todaySteps
////        // 设置当前进度
////        binding.progressBarSteps.progress = newStepCount // for now test step count make it 1k
//
//        // 如果需要，可以在这里添加动画效果
////        ObjectAnimator.ofInt(binding.progressBarSteps, "progress", currentStepCount, newStepCount)
////            .setDuration(1000)
////            .start()
////        currentStepCount = newStepCount
//    }
//
//    override fun onStepChange(stepCount: ExerciseStats?) {
//        updateStepCount()
//    }
//
//    override fun onStepReachPeriod(hundred_level: Int) {
//        if (UserManager.instance.isEarningEnabled()) {
//            URLog.i(TAG, "onStepReachPeriod ($hundred_level)")
//            when (hundred_level) {
//                1, 3, 5 -> {
//                    URLog.i(
//                        TAG,
//                        "onStepReachPeriod ($hundred_level) createHandPointerGuide(binding.btnEarnCoins)"
//                    )
//                    createHandPointerGuide(binding.btnEarnCoins)
//                }
//
//                else -> {
//
//                }
//            }
//            CoinAnimationUtils.showWithFadeIn(binding.displayInspiration)
//            createHandPointerGuide(binding.displayInspiration)
//
//            binding.displayInspiration.setOnClickListener {
//                removeHandPointerForTarget(binding.displayInspiration)
//                // 使用CoinAnimationUtils隐藏金币入口
//                CoinAnimationUtils.hideWithFadeOut(binding.displayInspiration) {
//                    // 在动画结束后显示激励对话框
//                    TDAnalyticsManager.reportButtonShow(
//                        adType = "Reward",
//                        adPosition = "任务_激励",
//                        adPositionType = "激励",
//                        placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
//                    )
//                    DialogUtils.showInspirationDialog(this) {
//                        showRewardAd()
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        TenjinManager.onMainActivityStart()
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        // 在页面恢复时重新启动动画
//        setupHeartCircleAnimations()
//
//        // 重启气球动画
//        if (::floatingBalloon.isInitialized && balloonAnimator?.isRunning != true) {
//            startBalloonAnimation()
//        }
//
//        // 在用户与应用交互后启动步数服务
//        if (stepManager.checkNotificationPermission() && stepManager.checkActivityRecognitionPermission()) {
//            // 如果所有权限都已获取，使用更强的重试逻辑
//            if (stepManager.isIgnoringBatteryOptimizations()) {
//                // 所有权限都有了，强制重试启动服务
//                stepManager.retryStartService()
//            } else {
//                // 正常启动
//                (application as WalkApplication).startStepService()
//            }
//        } else {
//            // 正常启动
//            (application as WalkApplication).startStepService()
//        }
//
//        if (UserManager.instance.isEarningEnabled()) {
//            // 检查金币按钮是否可见，如果可见则重新开始动画
//            if (binding.displayInspiration.visibility == View.VISIBLE) {
//                // 重启金币按钮动画
//                CoinAnimationUtils.restartAnimation(binding.displayInspiration)
//            }
//            startRepeatingTask()
//            binding.coinCircleButton.parentCircleButton.visibility = View.VISIBLE
//            binding.btnEarnCoins.visibility = View.VISIBLE
//        } else {
//            binding.iconTreasure.visibility = View.INVISIBLE
//            binding.coinCircleButton.parentCircleButton.visibility = View.INVISIBLE
//            binding.btnEarnCoins.visibility = View.INVISIBLE
//            binding.displayInspiration.visibility = View.INVISIBLE
//        }
//        AdLoaderManager.loadRewardVideoAd(this)
//        AdLoaderManager.loadInterstitialAd(this)
//
//        /**
//         *
//         * 从别的页面回来弹广告
//         */
//        if (isComeBackShowAd) {
//            showRewardOrInterstitialAd()
//            isComeBackShowAd = false
//        }
//
//        tryInitSAdSdk()
//    }
//
//    private fun showRewardOrInterstitialAd() {
//        if (AdLoaderManager.isRewardVideoAdReady()) {
//            AdLoaderManager.showRewardVideoAd(this)
//        } else if (AdLoaderManager.isInterstitialAdReady()) {
//            AdLoaderManager.showInterstitialAd(this)
//        }
//    }
//
//    // 启动任务
//    private fun startRepeatingTask() {
//        // 检查金币按钮是否可见，如果可见则重新开始动画
//        if (binding.iconTreasure.visibility == View.VISIBLE) {
//            // 重启金币按钮动画
//            CoinAnimationUtils.restartAnimation(binding.iconTreasure)
//        } else {
//            URLog.d(TAG, "perform count down treasure task start")
//            lifecycleScope.launch {
//                // 执行任务
//                URLog.i(TAG, "count down treasure")
//
//                // 延迟 30 秒
//                delay(30000L) // 30秒
//                URLog.i(TAG, "count down latch perform animation")
//
//                if (UserManager.instance.isEarningEnabled()) {
//                    CoinAnimationUtils.showWithFadeIn(binding.iconTreasure)
//
//                    binding.iconTreasure.setOnClickListener {
//                        showRewardAd()
//                        binding.iconTreasure.visibility = View.GONE
//                        startRepeatingTask()
//                    }
//                }
//            }
//        }
//    }
//
//    override fun handleUserInfoChanged(userInfo: UserInfo) {
//        super.handleUserInfoChanged(userInfo)
//        // 显示金币的数量
//        binding.coinCircleButton.tvCoinsNums.text = userInfo.udCoin.toString()
//    }
//
//    override fun showBannerInCoinDialog(): Boolean {
//        return false
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // 在页面暂停时停止动画，节省资源
//        HeartPulseAnimator.stopAllAnimations(heartCircles + listOf(binding.mainHeart))
//        CoinAnimationUtils.stopAnimation()
//    }
//
//    override fun onStop() {
//        super.onStop()
//
//        // 停止所有手指引导动画
//        removeAllHandPointers()
//        // 停止气球动画
//        balloonAnimator?.cancel()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // 停止所有动画
//        HeartPulseAnimator.stopAllAnimations(heartCircles + listOf(binding.mainHeart))
//        CoinAnimationUtils.stopAnimation()
//        // 清理所有手指引导动画
//        removeAllHandPointers()
//        // 停止气球动画
//        balloonAnimator?.cancel()
//    }
//
//    /**
//     * 移除某个目标视图的手指引导动画
//     * @param targetView 目标视图
//     */
//    private fun removeHandPointerForTarget(targetView: View) {
//        val key = targetView.hashCode()
//        handPointerMap[key]?.let { handPointerView ->
//            handPointerView.clearAnimation()
//            (handPointerView.parent as? ViewGroup)?.removeView(handPointerView)
//            handPointerMap.remove(key)
//        }
//    }
//
//    /**
//     * 移除所有手指引导动画
//     */
//    private fun removeAllHandPointers() {
//        for (handPointer in handPointerMap.values) {
//            handPointer.clearAnimation()
//            (handPointer.parent as? ViewGroup)?.removeView(handPointer)
//        }
//        handPointerMap.clear()
//    }
//
//    /**
//     * 创建手指指引动画
//     * @param targetView 需要指引的目标视图
//     * @param offsetY 指引图标在Y轴上的偏移量（单位dp），正数表示向下偏移，负数表示向上偏移
//     * @param animDistance 动画移动的距离（单位dp）
//     */
//    private fun createHandPointerGuide(
//        targetView: View,
//        offsetY: Float = 200f,
//        animDistance: Float = -180f
//    ) {
//        // 检查是否已经为该目标视图创建了手指引导动画
//        val targetKey = targetView.hashCode()
//        if (handPointerMap.containsKey(targetKey)) {
//            // 已存在，不重复创建
//            return
//        }
//
//        // 创建一个ImageView作为手指指针
//        val handPointerImageView = ImageView(this)
//        handPointerImageView.setImageResource(R.drawable.ic_hand_pointer)
//        handPointerImageView.setColorFilter(
//            ContextCompat.getColor(
//                this,
//                R.color.colorOnSecondaryContainer
//            )
//        )
//
//        // 存储到Map中
//        handPointerMap[targetKey] = handPointerImageView
//
//        // 设置固定大小为30dp
//        val sizeDp = 30
//        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
//        handPointerImageView.layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
//
//        // 添加到当前布局中
//        val rootView = binding.root
//        if (rootView is androidx.constraintlayout.widget.ConstraintLayout) {
//            // 对于ConstraintLayout，使用ConstraintLayout.LayoutParams
//            val layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
//                sizePx, sizePx
//            )
//            rootView.addView(handPointerImageView, layoutParams)
//        } else {
//            // 对于其他布局，使用通用的ViewGroup.LayoutParams
//            val layoutParams = android.view.ViewGroup.LayoutParams(
//                sizePx, sizePx
//            )
//            rootView.addView(handPointerImageView, layoutParams)
//        }
//
//        // 延迟500ms后开始动画，确保视图已经布局完成
//        handPointerImageView.post {
//            // 获取目标视图的位置和尺寸
//            val targetLocation = IntArray(2)
//            targetView.getLocationOnScreen(targetLocation)
//            val targetWidth = targetView.width
//            val targetHeight = targetView.height
//
//            // 手指图标已设置固定大小
//            val handWidth = sizePx
//            val handHeight = sizePx
//
//            // 计算手指的位置，使其水平居中对准目标视图
//            // 目标视图中心点X = 左上角X + 宽度/2
//            // 手指位置X = 目标中心点X - 手指宽度/2
//            val targetCenterX = targetLocation[0] + targetWidth / 2
//            handPointerImageView.x = targetCenterX - handWidth / 2f
//
//            // 计算目标视图底部位置
//            val targetBottom = targetLocation[1] + targetHeight
//
//            // 设置初始Y位置，在目标下方指定距离处
//            // 增加偏移量到120dp，让手指更靠下，动画幅度更大
//            val safeInitialOffset = 120f
//            handPointerImageView.y = (targetBottom + safeInitialOffset).toFloat()
//
//            // 设置动画距离 - 上移但不侵入目标区域
//            // 距离设为100dp，确保较大幅度但不侵入目标区域
//            // 保留20dp安全距离
//            val safetyMargin = 20
//            val safeAnimDistance = -(safeInitialOffset - safetyMargin)
//
//            // 创建从下往上的动画
//            val animation = TranslateAnimation(
//                0f, 0f,  // X轴不移动
//                0f, safeAnimDistance // Y轴移动的距离，确保安全
//            )
//            animation.duration = 1500 // 动画持续1.5秒
//            animation.repeatCount = Animation.INFINITE  // 无限重复，直到用户点击
//            animation.repeatMode = Animation.REVERSE // 反向重复
//            animation.fillAfter = true // 动画结束后保持结束状态
//
//            URLog.d(
//                TAG,
//                "动画设置：目标视图=${targetView}, 底部Y=$targetBottom, 手指位置Y=${handPointerImageView.y}, 移动距离=$safeAnimDistance"
//            )
//
//            // 开始动画
//            handPointerImageView.startAnimation(animation)
//        }
//    }
//
//    /**
//     * 检查是否是首次启动应用，并显示手指引导动画
//     */
//    private fun checkFirstLaunchAndShowGuide() {
//        // 判断是否是首次启动应用
//        val sharedPreferences = getSharedPreferences(FIRST_LAUNCH_PREF, Context.MODE_PRIVATE)
//        val isFirstLaunch = sharedPreferences.getBoolean(IS_FIRST_LAUNCH, true)
//
//        if (isFirstLaunch /*|| BuildConfig.DEBUG*/) {
//            // 显示抽奖对话框
//            showLuckyPrizeWheelDialog()
//
//            // 更新首次启动标志
//            sharedPreferences.edit().putBoolean(IS_FIRST_LAUNCH, false).apply()
//        }
//    }
//
//    private fun showLuckyPrizeWheelDialog() {
//        val dialog = LuckyWheelDialog(this) {
//            Handler(Looper.getMainLooper()).postDelayed(
//                {
//                    showRewardAd(AdShowScene.REDEEM)
//                    it.dismiss()
//                },
//                1500
//            )
//        }
//        dialog.show()
//    }
//
//    /**
//     * 创建浮动气球
//     */
//    private fun createFloatingBalloon() {
//        // 使用LayoutInflater加载布局
//        val inflater = LayoutInflater.from(this)
//        floatingBalloon = inflater.inflate(R.layout.layout_floating_balloon, binding.root, false)
//
//        // 添加到布局中
//        val rootView = binding.root
//        val layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        // 设置初始位置
//        layoutParams.topToTop =
//            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
//        layoutParams.startToStart =
//            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
//        layoutParams.topMargin = 200
//        layoutParams.marginStart = 100
//
//        rootView.addView(floatingBalloon, layoutParams)
//
//        floatingBalloon.setOnClickListener {
//            showRewardOrInterstitialAd()
//        }
//
//        // 获取屏幕尺寸以检测边界
//        rootView.post {
//            screenWidth = rootView.width
//            screenHeight = rootView.height
//
//            // 开始气球动画，以默认速度
//            startBalloonAnimation()
//        }
//    }
//
//    override fun handleUserWithdrawEnableChanged(enable: Boolean) {
//        super.handleUserWithdrawEnableChanged(enable)
//        if (enable) {
//            // 创建手指指引动画，指向金币按钮
//            binding.coinCircleButton.tvCoins?.let { targetView ->
//                createHandPointerGuide(targetView)
//            }
//        }
//    }
//
//    /**
//     * 开始气球飘浮动画
//     * @param speedFactor 速度系数，值越小移动越慢，默认0.5f
//     */
//    private fun startBalloonAnimation() {
//        val speedFactor: Float = 0.4f
//        // 更新速度系数
//        balloonSpeedFactor = speedFactor
//
//        // 初始化随机速度
//        balloonSpeedX =
//            (0.5f + random.nextFloat()) * speedFactor * if (random.nextBoolean()) 1 else -1
//        balloonSpeedY =
//            (0.5f + random.nextFloat()) * speedFactor * if (random.nextBoolean()) 1 else -1
//
//        // 创建动画更新器
//        balloonAnimator = ValueAnimator.ofFloat(0f, 1f)
//        balloonAnimator?.apply {
//            duration = 16 // 每帧16毫秒，约60fps
//            repeatCount = ValueAnimator.INFINITE
//            interpolator = LinearInterpolator()
//
//            addUpdateListener { _ ->
//                // 计算新位置
//                updateBalloonPosition()
//            }
//
//            start()
//        }
//    }
//
//    /**
//     * 更新气球位置
//     */
//    private fun updateBalloonPosition() {
//        // 获取当前位置
//        val balloonParams =
//            floatingBalloon.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
//        val balloonSize = floatingBalloon.width
//
//        // 更新坐标
//        var x = floatingBalloon.x + balloonSpeedX
//        var y = floatingBalloon.y + balloonSpeedY
//
//        // 检查边界碰撞
//        if (x <= 0 || x + balloonSize >= screenWidth) {
//            balloonSpeedX *= -1 // 碰到左右边界，反向
//            // 加入一点随机性
//            balloonSpeedX += (random.nextFloat() * 0.5f - 0.25f)
//            x = if (x <= 0) 0f else (screenWidth - balloonSize).toFloat()
//        }
//
//        if (y <= 0 || y + balloonSize >= screenHeight) {
//            balloonSpeedY *= -1 // 碰到上下边界，反向
//            // 加入一点随机性
//            balloonSpeedY += (random.nextFloat() * 0.5f - 0.25f)
//            y = if (y <= 0) 0f else (screenHeight - balloonSize).toFloat()
//        }
//
//        // 施加位置
//        floatingBalloon.x = x
//        floatingBalloon.y = y
//    }
//}
