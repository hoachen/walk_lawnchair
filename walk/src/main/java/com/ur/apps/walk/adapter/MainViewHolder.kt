package com.ur.apps.walk.adapter

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ur.apps.ad.AdLoaderManager
import com.ur.apps.ad.topon.TopOnAdLoader
import com.ur.apps.analysis.td.TDAnalyticsManager
import com.ur.apps.utils.URLog
import com.ur.apps.walk.R
import com.ur.apps.walk.databinding.ItemAchievementsBinding
import com.ur.apps.walk.databinding.ItemAdBinding
import com.ur.apps.walk.databinding.ItemButtonsBinding
import com.ur.apps.walk.databinding.ItemHeaderBinding
import com.ur.apps.walk.databinding.ItemHeartWithStepsBinding
import com.ur.apps.walk.databinding.ItemStatsBinding
import com.ur.apps.walk.databinding.ItemStepOverviewCardBinding
import com.ur.apps.walk.databinding.ItemSummaryBinding
import com.ur.apps.walk.databinding.ItemTaskBinding
import com.ur.apps.walk.databinding.ItemTaskListHeaderBinding
import com.ur.apps.walk.databinding.ItemTimeDateBinding
import com.ur.apps.walk.databinding.ItemWeeklyTrendBinding
import com.ur.apps.walk.model.MainItem
import com.ur.apps.walk.model.TaskModel
import com.ur.apps.walk.utils.CoinAnimationUtils

/**
 * Header ViewHolder
 */
class HeaderViewHolder(private val binding: ItemHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.HeaderItem, onItemClickListener: MainItemClickListener) {
        // 设置应用名称和副标题
        binding.tvAppName.text = itemView.context.getString(R.string.home_title_brand)
        binding.tvAppSubtitle.text = itemView.context.getString(R.string.home_subtitle_slogan)

        // 设置按钮点击事件（现在按钮是FrameLayout）
        binding.btnProfile.setOnClickListener { onItemClickListener.onProfileClick() }
        binding.btnWithdraw.setOnClickListener { onItemClickListener.onCoinClick() }
    }
}

/**
 * HeartWithSteps ViewHolder
 */
class HeartWithStepsViewHolder(private val binding: ItemHeartWithStepsBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem, onItemClickListener: MainItemClickListener) {
        when (item) {
            is MainItem.HeartAnimationItem -> {
                // 对于心脏动画item，不需要设置数据，只需要管理动画
                updateStepCount(0) // 默认显示0步
                updateStats(0.0, 0, 0) // 默认显示0数据
            }

            is MainItem.StepCountItem -> {
                // 对于步数item，设置步数
                updateStepCount(item.stepCount)
                setupProgress(item.stepCount)
                // 根据步数计算统计数据
                val distance = calculateDistance(item.stepCount)
                val calories = calculateCalories(item.stepCount)
                val duration = calculateDuration(item.stepCount)
                updateStats(distance, calories, duration)
            }

            else -> {
                // 不应该发生
            }
        }

        // 设置金币按钮点击事件
        binding.btnCoinExchange.setOnClickListener {
            onItemClickListener.onCoinClick()
        }
    }

    private fun updateStepCount(stepCount: Int) {
        binding.tvStepCount.text = stepCount.toString()

        // 更新自定义进度条
        binding.taskProgressBar.setCurrentSteps(stepCount)
    }

    private fun setupProgress(stepCount: Int) {
        // 自定义进度条会自动绘制刻度和进度，所以这里不需要额外设置
        // 只需要更新步数，进度条会自动重绘
        updateStepCount(stepCount)
    }

    private fun calculateDistance(stepCount: Int): Double {
        // 假设平均步长0.7米，转换为公里
        return stepCount * 0.7 / 1000.0
    }

    private fun calculateCalories(stepCount: Int): Int {
        // 假设每100步消耗5卡路里
        return stepCount * 5 / 100
    }

    private fun calculateDuration(stepCount: Int): Int {
        // 假设每分钟走100步
        return stepCount / 100
    }

    private fun updateStats(distance: Double, calories: Int, duration: Int) {
        // 更新距离数据
        binding.distanceCard.tvTitle.text = itemView.context.getString(R.string.label_distance)
        binding.distanceCard.tvValue.text = String.format("%.2f", distance)
        binding.distanceCard.tvUnit.text =
            itemView.context.getString(R.string.main_distance_unit)
        binding.distanceCard.iconView.setImageResource(R.drawable.ic_distance)

        // 更新卡路里数据
        binding.caloriesCard.tvTitle.text = itemView.context.getString(R.string.label_calories)
        binding.caloriesCard.tvValue.text = calories.toString()
        binding.caloriesCard.tvUnit.text =
            itemView.context.getString(R.string.main_calories_unit)
        binding.caloriesCard.iconView.setImageResource(R.drawable.ic_calories)

        // 更新时间数据
        binding.timeCard.tvTitle.text = itemView.context.getString(R.string.label_time)
        binding.timeCard.tvValue.text = duration.toString()
        binding.timeCard.tvUnit.text = itemView.context.getString(R.string.main_duration_unit)
        binding.timeCard.iconView.setImageResource(R.drawable.ic_time)
    }

    fun startAnimations() {
        // 新的布局没有心脏动画，所以不需要启动动画
        // 可以在这里添加其他动画效果，比如进度条动画
    }

    fun stopAnimations() {
        // 新的布局没有心脏动画，所以不需要停止动画
    }
}

/**
 * Stats ViewHolder
 */
class StatsViewHolder(private val binding: ItemStatsBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.StatsItem) {
        binding.tvDistanceValue.text = String.format("%.2f", item.stats.distance)
        binding.tvCaloriesValue.text = item.stats.calories.toInt().toString()
        binding.tvTimeValue.text = item.stats.duration.toString()
    }
}

/**
 * Task ViewHolder
 */
class TaskViewHolder(private val binding: ItemTaskBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var taskAdapter: TaskItemAdapter

    fun bind(item: MainItem.TaskItem, onItemClickListener: MainItemClickListener) {
        // 设置任务标题和进度（根据HTML设计）
        binding.tvTaskTitle.text = itemView.context.getString(R.string.task_earn_coins_title)

        // 加载任务列表并计算实际进度
        val tasks = TaskModel.updateTaskCompletion(itemView.context, item.currentDistance)
        val completedTasks = TaskModel.getCompletedCount(tasks)
        val totalTasks = TaskModel.getTotalCount()
        binding.tvTaskProgress.text = itemView.context.getString(
            R.string.task_list_progress_format,
            completedTasks,
            totalTasks
        )

        // 初始化横向任务列表
        if (!::taskAdapter.isInitialized) {
            taskAdapter = TaskItemAdapter(onItemClickListener, item.currentDistance)
            binding.rvTaskList.apply {
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = taskAdapter
            }
        } else {
            // 更新当前步数
            taskAdapter.updateCurrentDistance(item.currentDistance)
        }

        // 加载并显示任务列表
        taskAdapter.submitList(tasks)
    }
}

/**
 * Buttons ViewHolder
 */
class ButtonsViewHolder(private val binding: ItemButtonsBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.ButtonsItem, onItemClickListener: MainItemClickListener) {
        // 设置按钮可见性
        binding.displayInspiration.visibility =
            if (item.showInspirationButton) View.VISIBLE else View.GONE

        // 设置点击事件
        binding.displayInspiration.setOnClickListener { onItemClickListener.onInspirationClick() }

        // 如果激励按钮可见，启动动画
        if (item.showInspirationButton) {
            CoinAnimationUtils.restartAnimation(binding.displayInspiration)
        }
    }

    fun checkAndStartAnimations() {
        if (binding.displayInspiration.visibility == View.VISIBLE) {
            CoinAnimationUtils.restartAnimation(binding.displayInspiration)
        }
    }

    fun stopAnimations() {
        CoinAnimationUtils.stopAnimation()
    }
}

/**
 * BannerAd ViewHolder
 */
class BannerAdViewHolder(private val binding: ItemAdBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val TAG = "BannerAdViewHolder"

    fun bind(item: MainItem.BannerAdItem) {
        // 初始状态：显示占位符，隐藏广告
        // 占位符已经在布局中显示，所以不需要额外设置
        // 广告容器初始隐藏
        binding.adContainer.visibility = View.VISIBLE
        AdLoaderManager.showBannerAd(itemView.context, binding.adContainer)
    }

    fun loadAd() {
        URLog.i(TAG, "invalidate banner ")
    }
}

/**
 * NativeAd ViewHolder
 */
class NativeAdViewHolder(private val binding: ItemAdBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val TAG = "NativeAdViewHolder"

    fun bind(item: MainItem.NativeAdItem) {
        binding.adContainer.visibility = View.VISIBLE
        AdLoaderManager.showNativeAd(
            itemView.context,
            binding.adContainer, -1
        )
    }

    fun loadAd() {
        URLog.i(TAG, "invalidate native ad ")
        TDAnalyticsManager.reportButtonClick(
            adType = "Native",
            adPosition = "Home_Bottom",
            adPositionType = "Home",
            placementId = TopOnAdLoader.TOPON_REWARD_PLACEMENT_ID
        )
        // 加载原生广告到容器中
        // 注意：AdLoaderManager.loadNativeAd只接受一个参数（Context）
        // showNativeAd方法需要三个参数（Context, ViewGroup, Int）但还没有实现
        AdLoaderManager.loadNativeAd(itemView.context)
    }
}

/**
 * Summary ViewHolder
 */
class SummaryViewHolder(private val binding: ItemSummaryBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.SummaryItem) {
        // 完全按照material.html的实现

        // 1. 设置标题和日期
        binding.tvLabel.text = itemView.context.getString(R.string.summary_title)
        binding.tvDate.text = item.date // 假设date已经是"12月12日 · 周五"格式

        // 2. 设置步数 - material.html中是"5,230步"
        binding.tvStepCount.text = formatStepCount(item.stepCount)
        binding.tvStepUnit.text = itemView.context.getString(R.string.summary_step_unit)

        // 3. 计算目标完成百分比
        val percentage = if (item.dailyGoal > 0) {
            (item.stepCount.toFloat() / item.dailyGoal * 100).coerceAtMost(100f)
        } else {
            0f
        }
        val percentageInt = percentage.toInt()
        binding.tvPercentage.text = "$percentageInt%"

        // 4. 设置环形进度标签为"GOAL"（material.html中环形内显示"GOAL"）
        binding.tvRingLabel.text =
            itemView.context.getString(R.string.summary_ring_label_today_target)

        // 5. 设置距离和消耗 - material.html中是卡片形式，显示"3.8 km"和"245 kcal"
        // 注意：新布局中有专门的卡片视图
        binding.tvDistanceValue.text = String.format("%.1f", item.distance)
        binding.tvDistanceLabel.text = itemView.context.getString(R.string.label_distance)

        binding.tvCaloriesValue.text = item.calories.toString()
        binding.tvCaloriesLabel.text = itemView.context.getString(R.string.label_calories)

        // 6. 设置图标
        binding.distanceIcon.text = "📍"
        binding.caloriesIcon.text = "🔥"

        // 7. 设置状态标签 - material.html中是"进行中"（带绿色圆点）
        val statusText =
            if (percentage >= 100f) itemView.context.getString(R.string.task_item_progress_label_completed) else itemView.context.getString(
                R.string.step_overview_badge_in_progress
            )
        binding.statusPill.visibility = View.VISIBLE
        binding.tvStatus.text = statusText

        // 8. 根据完成百分比设置状态圆点颜色
        // 注意：这里我们假设dot_background可以设置颜色
        // 实际实现可能需要更复杂的逻辑
        val dotColor = if (percentage >= 100f) {
            android.graphics.Color.parseColor("#10B981") // 完成色
        } else {
            android.graphics.Color.parseColor("#00D68F") // 进行中色
        }
        // 这里需要根据实际实现来设置圆点颜色
        // binding.statusDot.setBackgroundColor(dotColor)

        // 9. 注意：新布局中没有circularProgressView，而是使用drawable实现的环形
        // 所以不需要设置circularProgressView.progress
    }

    private fun formatStepCount(steps: Int): String {
        // material.html中显示为"5,230"格式（带千位分隔符）
        return if (steps >= 1000) {
            String.format("%,d", steps)
        } else {
            steps.toString()
        }
    }
}

/**
 * TaskListHeader ViewHolder
 */
class TaskListHeaderViewHolder(private val binding: ItemTaskListHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.TaskListHeaderItem) {
        binding.tvTitle.text = item.title
        binding.tvSubtitle.text = item.subtitle
        binding.tvProgress.text = item.progressText
    }
}

/**
 * WeeklyTrend ViewHolder
 */
class WeeklyTrendViewHolder(private val binding: ItemWeeklyTrendBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var dailyStepAdapter: DailyStepAdapter

    fun bind(item: MainItem.WeeklyTrendItem) {
        binding.tvTitle.text = item.title
        binding.tvSubtitle.text = item.subtitle
        binding.tvAverage.text = item.averageText

        // 初始化DailyStepAdapter
        if (!::dailyStepAdapter.isInitialized) {
            dailyStepAdapter = DailyStepAdapter()
            binding.rvDailySteps.apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                    context,
                    androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = dailyStepAdapter
            }
        }

        // 设置柱状图数据
        dailyStepAdapter.submitList(item.dailyData)
    }
}

/**
 * Achievements ViewHolder
 */
class AchievementsViewHolder(private val binding: ItemAchievementsBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.AchievementsItem) {
        binding.tvTitle.text = item.title
        binding.tvSubtitle.text = item.subtitle

        // 更新成就列表
        updateAchievements(item.achievements)
    }

    private fun updateAchievements(achievements: List<MainItem.AchievementsItem.Achievement>) {
        // 获取所有成就视图
        val achievementViews = listOf(
            Triple(
                binding.achievement1Icon,
                binding.achievement1Title,
                binding.achievement1Desc to binding.achievement1Tag
            ),
            Triple(
                binding.achievement2Icon,
                binding.achievement2Title,
                binding.achievement2Desc to binding.achievement2Tag
            ),
            Triple(
                binding.achievement3Icon,
                binding.achievement3Title,
                binding.achievement3Desc to binding.achievement3Tag
            )
        )

        // 更新每个成就
        achievements.forEachIndexed { index, achievement ->
            if (index < achievementViews.size) {
                val (iconView, titleView, descAndTag) = achievementViews[index]
                val (descView, tagView) = descAndTag

                // 设置图标（这里使用文本作为图标，实际应用中可能需要使用图片资源）
                iconView.text = achievement.icon
                titleView.text = achievement.title
                descView.text = achievement.description
                tagView.text = achievement.tag
            }
        }
    }
}

/**
 * TimeDate ViewHolder
 */
class TimeDateViewHolder(private val binding: ItemTimeDateBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.TimeDateItem) {
        binding.tvTime.text = item.time
        binding.tvDate.text = item.date
    }
}

/**
 * StepOverviewCard ViewHolder - 锁屏界面步数概览卡片
 * 基于 HTML 设计实现玻璃拟态效果
 */
class StepOverviewCardViewHolder(private val binding: ItemStepOverviewCardBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MainItem.StepOverviewCardItem) {
        // 设置标题
        binding.tvTitle.text = item.title

        // 设置徽章文本
        binding.tvBadgeText.text = item.badgeText

        // 格式化步数显示（添加千位分隔符）
        binding.tvStepCount.text = formatStepCount(item.stepCount)

        // 设置步数标签（显示目标步数）
        binding.tvStepLabel.text = itemView.context.getString(
            R.string.step_overview_subtitle_format,
            formatStepCount(item.dailyGoal)
        )

        // 计算完成百分比
        val percentage = if (item.dailyGoal > 0) {
            (item.stepCount.toFloat() / item.dailyGoal * 100).coerceAtMost(100f)
        } else {
            0f
        }
        val percentageInt = percentage.toInt()
        binding.tvPercentage.text = "$percentageInt%"
    }

    private fun formatStepCount(steps: Int): String {
        return if (steps >= 1000) {
            String.format("%,d", steps)
        } else {
            steps.toString()
        }
    }
}
