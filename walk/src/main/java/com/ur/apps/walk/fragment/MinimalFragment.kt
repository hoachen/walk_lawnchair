package com.ur.apps.walk.fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.lock.LockAdManager
import com.ur.apps.utils.URLog
import com.ur.apps.walk.LockScreenActivity
import com.ur.apps.walk.adapter.LockScreenAdapter
import com.ur.apps.walk.adapter.MainItemClickListener
import com.ur.apps.walk.constants.StatisticConstants
import com.ur.apps.walk.databinding.FragmentMinimalBinding
import com.ur.apps.walk.model.MainItem
import com.ur.apps.walk.model.TaskModel
import com.ur.apps.walk.step.bean.ExerciseStats
import com.ur.apps.walk.step.callback.StepCountChangeCallBack
import com.ur.apps.walk.step.manager.StepManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MinimalFragment : Fragment(), StepCountChangeCallBack, MainItemClickListener {

    companion object {
        const val TAG = "MinimalFragment"

        fun newInstance(): MinimalFragment {
            return MinimalFragment()
        }
    }

    // 任务常量
    private val TASK_ONE = 1000

    private lateinit var uiBinding: FragmentMinimalBinding
    private lateinit var mainAdapter: LockScreenAdapter

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var stepManager: StepManager

    // 广播接收器 - 监听屏幕开关事件
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    // 屏幕打开时的处理逻辑
                    URLog.d(TAG, "screen on")
                    onScreenOn()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    URLog.d(TAG, "screen off")
                    // 屏幕关闭时的处理逻辑
                    onScreenOff()
                }
            }
        }
    }

    // 更新时间任务
    private val updateTimeTask = object : Runnable {
        override fun run() {
            updateTimeAndDate()
            handler.postDelayed(this, 1000) // 每秒更新一次
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册屏幕状态广播接收器
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        activity?.registerReceiver(screenStateReceiver, intentFilter)
        LockAdManager.instance.setLockerAdLoadListener(object :
            LockAdManager.Companion.LockerAdLoadListener {
            override fun onAdLoaded() {
                URLog.i(TAG, "on LockAdLoaded update ad item")
                try {
                    mainAdapter.updateItem(MainItem.LockerAdItem)
                } catch (e: Exception) {
                }
            }

            override fun onAdClicked() {
                URLog.i(TAG, "on LockAdClick dissmiss")
                try {
                    (activity as LockScreenActivity).setMomentoClick(true,"adClicked")
                } catch (e: Exception) {

                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        URLog.i(TAG, "onCreateView")
        uiBinding = FragmentMinimalBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        URLog.i(TAG, "onViewCreated")

        // 初始化步数管理器
        initStepManager()

        // 初始化RecyclerView
        setupRecyclerView()


        setupLockerScroller()

        // 开始更新时间
        handler.post(updateTimeTask)
    }

    override fun onResume() {
        super.onResume()
        URLog.i(TAG, "onResume")
        TDAnalyticsManager.reportTrackEvent(
            "minimal_resume",
            JSONObject()
        )

        // 注册步数回调
        stepManager.registerCallback(this)
        stepManager.requestUpdateCallBack()

        // 更新步数显示
        updateStepCount()

        // 更新RecyclerView数据
        updateRecyclerViewItems()

        resetSeekBar(0)
    }

    override fun onPause() {
        super.onPause()
        URLog.i(TAG, "onPause")
        TDAnalyticsManager.reportTrackEvent(
            "minimal_pause",
            JSONObject()
        )
        // 移除更新任务
        handler.removeCallbacks(updateTimeTask)

        // 取消注册步数回调
        stepManager.unregisterCallback(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        URLog.i(TAG, "onDestroyView")
        // 移除更新任务并清理绑定
        handler.removeCallbacks(updateTimeTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        URLog.i(TAG, "onDestroy")
        // 确保移除所有回调
        handler.removeCallbacksAndMessages(null)
        // 取消注册屏幕状态广播接收器
        try {
            activity?.unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // 接收器可能未注册，忽略异常
            URLog.w(TAG, "屏幕状态广播接收器未注册或已取消注册")
        }
        LockAdManager.instance.removeLockerAdLoadListener()
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateTimeAndDate() {
        val currentTime = System.currentTimeMillis()

        // 更新时间格式
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat(
            getString(com.ur.apps.walk.R.string.lock_date_format_full),
            Locale.getDefault()
        )

        val currentTimeStr = timeFormat.format(Date(currentTime))
        val currentDateStr = dateFormat.format(Date(currentTime))

        // 更新RecyclerView中的时间日期item
        updateTimeDateInRecyclerView(currentTimeStr, currentDateStr)
    }

    /**
     * 更新RecyclerView中的时间日期item
     */
    private fun updateTimeDateInRecyclerView(time: String, date: String) {
        // 使用LockScreenAdapter的专用方法更新时间日期
        mainAdapter.updateTimeDate(time, date)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // 处理 activity result
        URLog.i(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
    }

    private fun initStepManager() {
        stepManager = StepManager.getInstance(requireContext())
    }

    /**
     * 更新步数显示
     */
    private fun updateStepCount() {
        val stats = stepManager.getTodayStats()
        URLog.i(TAG, "updateStepCount: $stats")

        // 更新RecyclerView中的任务数据
        updateRecyclerViewItems()
    }

    override fun onStepChange(stepCount: ExerciseStats?) {
        // 当步数变化时更新UI
        activity?.let {
            updateStepCount()
        }
    }

    override fun onStepReachPeriod(hundred_level: Int) {
        // 当达到特定步数周期时的处理
        URLog.i(TAG, "onStepReachPeriod: $hundred_level")
    }

    private fun setupLockerScroller() {
        uiBinding.seekbarUnlock.progress = 0
        uiBinding.tvSlideHint.alpha = 1.0f
        uiBinding.seekbarUnlock.isEnabled = false
        uiBinding.seekbarUnlock.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 可选：随着滑动，让文字透明度降低，产生渐隐效果
                val alpha = 1.0f - (progress / 100f)
                uiBinding.tvSlideHint.alpha = if (alpha < 0.2f) 0f else alpha
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始拖动
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0

                // 阈值判断：如果滑动超过 85%，则认为解锁成功
                if (progress > 85) {
                    // 防止用户没拉满，视觉上直接拉满
                    seekBar?.progress = 100
                    performUnlock()
                } else {
                    // 未达到阈值，弹回起点 (添加动画效果)
                    resetSeekBar()
                }
            }
        })
    }


    private fun resetSeekBar(duration: Long = 300) {
        uiBinding.seekbarUnlock.let {
            // 使用属性动画让滑块平滑弹回 0
            val animator = ValueAnimator.ofInt(it.progress, 0)
            animator.duration = duration // 动画时长 300ms
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                it.progress = animatedValue
                // 同时恢复文字透明度
                uiBinding.tvSlideHint.alpha = 1.0f - (animatedValue / 100f)
            }
            animator.start()
        }
    }

    private fun performUnlock() {
        // 执行解锁逻辑
        // 实际场景中这里会跳转页面或finish()
//        requireActivity().moveTaskToBack(true)
        activity?.let {
            if (it is LockScreenActivity) {
                it.setMomentoClick(true, "unlock")
            }
        }
    }

    /**
     * 屏幕打开时的回调方法
     */
    private fun onScreenOn() {
        URLog.i(TAG, "onScreenOn: 处理屏幕打开事件")
        // 屏幕打开时的处理逻辑
        // 例如：恢复时间更新、重新开始动画等
        handler.post(updateTimeTask)

//        activity?.let {
//            if (!it.isFinishing) {
//                // 可以在这里添加其他屏幕关闭时需要执行的操作
//                TDAnalyticsManager.reportTrackEvent(
//                    StatisticConstants.AD_SHOW_SCREEN_ON,
//                    JSONObject().put(StatisticConstants.FRAGMENT, "MinimalFragment")
//                )
//                LockAdManager.instance.showAd(it, uiBinding.adContainer)
//            }
//        }
        // 可以在这里添加其他屏幕打开时需要执行的操作
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.SCREEN_ON,
            JSONObject().put(StatisticConstants.FRAGMENT, "MinimalFragment")
        )
    }

    /**
     * 屏幕关闭时的回调方法
     */
    private fun onScreenOff() {
        URLog.i(TAG, "onScreenOff: 处理屏幕关闭事件")
        // 屏幕关闭时的处理逻辑
        // 例如：暂停时间更新、停止动画等
        handler.removeCallbacks(updateTimeTask)

        // 可以在这里添加其他屏幕关闭时需要执行的操作
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.PRELOAD_SCREEN_OFF,
            JSONObject().put(StatisticConstants.FRAGMENT, "MinimalFragment")
        )
        LockAdManager.instance.preloadNextAd()

        // 可以在这里添加其他屏幕关闭时需要执行的操作
        TDAnalyticsManager.reportTrackEvent(
            StatisticConstants.SCREEN_OFF,
            JSONObject().put(StatisticConstants.FRAGMENT, "MinimalFragment")
        )
    }

    /**
     * 初始化RecyclerView
     */
    private fun setupRecyclerView() {
        mainAdapter = LockScreenAdapter(requireActivity(), this)
        uiBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mainAdapter
            setHasFixedSize(true)
            itemAnimator = null  // 关闭所有动画
        }

        // 初始化数据
        updateRecyclerViewItems()
    }

    // MainItemClickListener 接口实现
    override fun onProfileClick() {
        // 解锁页面不需要个人资料点击
    }

    override fun onCoinClick() {
        // 解锁页面不需要金币点击
    }

    override fun onEarnCoinsClick() {
        // 解锁页面不需要赚金币点击
    }

    override fun onInspirationClick() {
        // 解锁页面不需要激励点击
    }

    override fun onTaskClick(taskId: Int, targetDistance: Int, currentDistance: Int) {
        // 解锁页面任务点击处理
        handleTaskAction(taskId, targetDistance, currentDistance)
    }

    override fun onCloseTaskClick() {
        // 解锁页面不需要关闭任务
    }

    override fun onTreasureClick() {
        // 解锁页面不需要宝箱点击
    }

    override fun onTaskClaimClick(taskId: Int, stepGoal: Int) {
        // 解锁页面任务领取点击
        handleTaskClaim(taskId, stepGoal)
    }

    /**
     * 处理任务操作（模拟奖励领取逻辑）
     */
    private fun handleTaskAction(taskId: Int, targetDistance: Int, currentDistance: Int) {
        if (currentDistance >= targetDistance) {
            // 在Fragment中显示激励广告
            // 注意：Fragment中可能需要通过Activity来显示广告
            // 这里不显示提示消息，与MainActivityRecycler保持一致
        } else {
            // 任务未完成，提示用户继续步行
            val remaining = targetDistance - currentDistance
            // 使用资源字符串，支持多语言
            showToast(getString(com.ur.apps.walk.R.string.task_remaining_steps, remaining))
        }
    }

    /**
     * 处理任务领取
     */
    private fun handleTaskClaim(taskId: Int, stepGoal: Int) {
        val stats = stepManager.getTodayStats()
        val currentSteps = stats.steps

        if (currentSteps >= stepGoal) {
            activity?.let {
                if (it is LockScreenActivity && !it.isFinishing) {
                    it.navigateToMainActivityShowSomething(taskId, stepGoal)
                }
            }
        } else {
            // 任务未完成
            val remaining = stepGoal - currentSteps
            showToast(getString(com.ur.apps.walk.R.string.task_remaining_steps, remaining))
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    /**
     * 更新RecyclerView数据
     */
    private fun updateRecyclerViewItems() {
        val stats = stepManager.getTodayStats()
        val currentSteps = stats.steps

        val items = mutableListOf<MainItem>()

        // 1. 添加时间日期显示（作为第一个item）
        val currentTime = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat(
            getString(com.ur.apps.walk.R.string.lock_date_format_full),
            Locale.getDefault()
        )
        val currentTimeStr = timeFormat.format(Date(currentTime))
        val currentDateStr = dateFormat.format(Date(currentTime))

        items.add(
            MainItem.TimeDateItem(
                time = currentTimeStr,
                date = currentDateStr
            )
        )

        // 2. 添加步数概览卡片（作为第二个item）
        // 计算距离（假设平均步长0.7米，转换为公里）
        val distance = currentSteps * 0.7 / 1000.0
        // 计算消耗的卡路里（假设每100步消耗5卡路里）
        val calories = currentSteps * 5 / 100
        // 每日目标步数 - 使用任务列表的最大值
        val maxTaskStep = TaskModel.DEFAULT_TASKS.maxByOrNull { it.stepGoal }?.stepGoal ?: 8000
        val dailyGoal = maxTaskStep
        // 计算剩余步数
        val remainingSteps = dailyGoal - currentSteps
        // 根据完成情况设置徽章文本
        val badgeText =
            if (currentSteps >= dailyGoal) getString(com.ur.apps.walk.R.string.task_item_progress_label_completed) else getString(
                com.ur.apps.walk.R.string.step_overview_badge_in_progress
            )

        items.add(
            MainItem.StepOverviewCardItem(
                stepCount = currentSteps,
                dailyGoal = dailyGoal,
                distance = distance,
                calories = calories,
                badgeText = badgeText,
                title = getString(com.ur.apps.walk.R.string.step_overview_title),
                subtitle = getString(
                    com.ur.apps.walk.R.string.step_overview_subtitle_format,
                    String.format("%,d", dailyGoal)
                ),
                stepLabel = getString(com.ur.apps.walk.R.string.step_overview_step_label),
                ringLabel = getString(com.ur.apps.walk.R.string.step_overview_ring_label_completion)
            )
        )
        items.add(MainItem.LockerAdItem)

        // 3. 任务区域（包含标题和任务列表）
        items.add(
            MainItem.TaskItem(
                taskId = 1,
                targetDistance = TASK_ONE,
                currentDistance = currentSteps,
                title = getString(com.ur.apps.walk.R.string.earn_coins)
            )
        )

        mainAdapter.setItems(items)
    }
}
