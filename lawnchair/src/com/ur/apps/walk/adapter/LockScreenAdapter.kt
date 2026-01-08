package com.ur.apps.walk.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ur.apps.utils.URLog
import com.android.launcher3.databinding.ItemLockerAdBinding
import com.android.launcher3.databinding.ItemStepOverviewCardBinding
import com.android.launcher3.databinding.ItemTimeDateBinding
import com.ur.apps.walk.model.MainItem
import com.android.launcher3.R


/**
 * LockScreenAdapter - 用于 LockScreenActivity（通过 MinimalFragment）的专用 Adapter
 * 继承自 BaseAdapter，可以添加锁屏界面特有的逻辑
 */
class LockScreenAdapter(
    private val  activity: Activity,
    onItemClickListener: MainItemClickListener
) : BaseAdapter(onItemClickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            MainItem.TYPE_TIME_DATE -> {
                val binding = ItemTimeDateBinding.inflate(inflater, parent, false)
                TimeDateViewHolder(binding)
            }

            MainItem.TYPE_STEP_OVERVIEW_CARD -> {
                val binding = ItemStepOverviewCardBinding.inflate(inflater, parent, false)
                StepOverviewCardViewHolder(binding)
            }

            MainItem.TYPE_LOCKER_AD_CARD -> {
                val binding = ItemLockerAdBinding.inflate(inflater, parent, false)
                LockScreenAdViewHolder(binding)
            }

            else -> super.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MainItem.TimeDateItem -> (holder as TimeDateViewHolder).bind(item)
            is MainItem.StepOverviewCardItem -> (holder as StepOverviewCardViewHolder).bind(item)
            is MainItem.LockerAdItem -> (holder as LockScreenAdViewHolder).bind(activity = activity,item)
            else -> super.onBindViewHolder(holder, position)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is LockScreenAdViewHolder) {
            URLog.info(" unbind LockScreenAdView")
            holder.unbind()
        }
    }


    // 可以在这里添加锁屏界面特有的方法
    // 例如：简化数据更新、特定的交互逻辑等

    /**
     * 更新锁屏界面的时间日期显示
     */
    fun updateTimeDate(time: String, date: String) {
        // 查找 TimeDateItem 的位置并更新
        for (i in 0 until itemCount) {
            if (getItemViewType(i) == MainItem.TYPE_TIME_DATE) {
                updateItem(i, MainItem.TimeDateItem(time, date))
                break
            }
        }
    }

    /**
     * 更新锁屏界面的步数概览
     */
    fun updateStepOverview(
        stepCount: Int,
        dailyGoal: Int,
        distance: Double,
        calories: Int,
        badgeText: String
    ) {
        // 查找 StepOverviewCardItem 的位置并更新
        for (i in 0 until itemCount) {
            if (getItemViewType(i) == MainItem.TYPE_STEP_OVERVIEW_CARD) {
                val currentItem = getItemAt(i)
                if (currentItem is MainItem.StepOverviewCardItem) {
                    updateItem(i, currentItem.copy(
                        stepCount = stepCount,
                        dailyGoal = dailyGoal,
                        distance = distance,
                        calories = calories,
                        badgeText = badgeText
                    ))
                }
                break
            }
        }
    }

    /**
     * 设置锁屏界面的简化数据（只显示时间和步数概览）
     */
    fun setLockScreenItems(
        time: String,
        date: String,
        stepCount: Int,
        dailyGoal: Int,
        distance: Double,
        calories: Int
    ) {
        val items = mutableListOf<MainItem>()

        // 1. 时间日期显示
        items.add(MainItem.TimeDateItem(time, date))

        // 2. 步数概览卡片
        val badgeText = if (stepCount >= dailyGoal) com.ur.apps.walk.WalkApplication.getContext().getString(R.string.task_item_progress_label_completed) else com.ur.apps.walk.WalkApplication.getContext().getString(R.string.step_overview_badge_in_progress)
        items.add(
            MainItem.StepOverviewCardItem(
                stepCount = stepCount,
                dailyGoal = dailyGoal,
                distance = distance,
                calories = calories,
                badgeText = badgeText,
                title = com.ur.apps.walk.WalkApplication.getContext().getString(R.string.step_overview_title),
                subtitle = com.ur.apps.walk.WalkApplication.getContext().getString(R.string.step_overview_subtitle_format, String.format("%,d", dailyGoal)),
                stepLabel = com.ur.apps.walk.WalkApplication.getContext().getString(R.string.step_overview_step_label),
                ringLabel = com.ur.apps.walk.WalkApplication.getContext().getString(R.string.step_overview_ring_label_completion)
            )
        )

        setItems(items)
    }
}
