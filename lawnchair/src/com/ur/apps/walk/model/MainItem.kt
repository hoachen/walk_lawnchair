package com.ur.apps.walk.model

import com.ur.apps.walk.step.bean.ExerciseStats

/**
 * MainActivity RecyclerView的数据项
 */
sealed class MainItem(val type: Int) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_HEART_ANIMATION = 1
        const val TYPE_STEP_COUNT = 2
        const val TYPE_STATS = 3
        const val TYPE_TASK = 4
        const val TYPE_BUTTONS = 5
        const val TYPE_BANNER_AD = 6
        const val TYPE_SUMMARY = 7
        const val TYPE_TASK_LIST_HEADER = 8
        const val TYPE_WEEKLY_TREND = 9
        const val TYPE_ACHIEVEMENTS = 10
        const val TYPE_NATIVE_AD = 11
        const val TYPE_TIME_DATE = 12
        const val TYPE_STEP_OVERVIEW_CARD = 13
        const val TYPE_LOCKER_AD_CARD = 14
    }

    data class HeaderItem(
        val userCoin: Int = 0
    ) : MainItem(TYPE_HEADER)

    object HeartAnimationItem : MainItem(TYPE_HEART_ANIMATION)

    data class StepCountItem(
        val stepCount: Int = 0
    ) : MainItem(TYPE_STEP_COUNT)

    data class StatsItem(
        val stats: ExerciseStats = ExerciseStats(0, 0.0, 0.0, 0)
    ) : MainItem(TYPE_STATS)

    data class TaskItem(
        val taskId: Int = 1,
        val targetDistance: Int = 1000,
        val currentDistance: Int = 0,
        val title: String = ""
    ) : MainItem(TYPE_TASK)

    data class ButtonsItem(
        val isEarningEnabled: Boolean = true,
        val showInspirationButton: Boolean = false
    ) : MainItem(TYPE_BUTTONS)

    object BannerAdItem : MainItem(TYPE_BANNER_AD)
    object NativeAdItem : MainItem(TYPE_NATIVE_AD)

    object LockerAdItem : MainItem(TYPE_LOCKER_AD_CARD)

    // 今日概览区域
    data class SummaryItem(
        val stepCount: Int = 0,
        val dailyGoal: Int = 8000,
        val distance: Double = 0.0,
        val calories: Int = 0,
        val date: String = ""
    ) : MainItem(TYPE_SUMMARY)

    // 今日步数任务区域标题
    data class TaskListHeaderItem(
        val title: String = "",
        val subtitle: String = "",
        val progressText: String = ""
    ) : MainItem(TYPE_TASK_LIST_HEADER)

    // 最近7天步数趋势
    data class WeeklyTrendItem(
        val title: String = "",
        val subtitle: String = "",
        val averageText: String = "",
        val dailyData: List<DailyStepData> = emptyList()
    ) : MainItem(TYPE_WEEKLY_TREND) {
        data class DailyStepData(
            val dayLabel: String,
            val stepCount: Int,
            val percentage: Float // 0-1之间的百分比
        )
    }

    // 今日成就区域
    data class AchievementsItem(
        val title: String = "",
        val subtitle: String = "",
        val achievements: List<Achievement> = emptyList()
    ) : MainItem(TYPE_ACHIEVEMENTS) {
        data class Achievement(
            val icon: String,
            val title: String,
            val description: String,
            val tag: String
        )
    }

    // 时间日期显示（用于解锁页面）
    data class TimeDateItem(
        val time: String = "12:00",
        val date: String = ""
    ) : MainItem(TYPE_TIME_DATE)

    // 步数概览卡片（用于锁屏页面）
    data class StepOverviewCardItem(
        val stepCount: Int = 5230,
        val dailyGoal: Int = 8000,
        val distance: Double = 3.8,
        val calories: Int = 245,
        val badgeText: String = "In progress",
        val title: String = "Today’s steps overview",
        val subtitle: String = "Today’s goal 8,000 steps",
        val stepLabel: String = "Steps walked today",
        val ringLabel: String = "Completion"
    ) : MainItem(TYPE_STEP_OVERVIEW_CARD)
}
