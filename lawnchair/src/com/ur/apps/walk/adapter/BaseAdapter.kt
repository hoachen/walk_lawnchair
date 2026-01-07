package com.ur.apps.walk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.databinding.ItemAchievementsBinding
import com.android.launcher3.databinding.ItemAdBinding
import com.android.launcher3.databinding.ItemButtonsBinding
import com.android.launcher3.databinding.ItemHeaderBinding
import com.android.launcher3.databinding.ItemHeartWithStepsBinding
import com.android.launcher3.databinding.ItemStatsBinding
import com.android.launcher3.databinding.ItemSummaryBinding
import com.android.launcher3.databinding.ItemTaskBinding
import com.android.launcher3.databinding.ItemTaskListHeaderBinding
import com.android.launcher3.databinding.ItemWeeklyTrendBinding
import com.ur.apps.walk.model.MainItem

/**
 * BaseAdapter - 所有具体Adapter的基类
 * 包含通用的Adapter逻辑和ViewHolder创建
 */
abstract class BaseAdapter(
    protected val onItemClickListener: MainItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    protected val items = mutableListOf<MainItem>()

    fun setItems(newItems: List<MainItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItem(item: MainItem) {
        val index = items.indexOf(item)
        updateItem(index, item)
    }

    fun updateItem(position: Int, item: MainItem) {
        if (position in 0 until items.size) {
            items[position] = item
            notifyItemChanged(position)
        }
    }

    fun updateStepCount(stepCount: Int) {
        items.forEachIndexed { index, item ->
            if (item is MainItem.StepCountItem) {
                items[index] = MainItem.StepCountItem(stepCount)
                notifyItemChanged(index)
            }
        }
    }

    fun updateStats(stats: MainItem.StatsItem) {
        items.forEachIndexed { index, item ->
            if (item is MainItem.StatsItem) {
                items[index] = stats
                notifyItemChanged(index)
            }
        }
    }

    fun updateButtons(buttonsItem: MainItem.ButtonsItem) {
        items.forEachIndexed { index, item ->
            if (item is MainItem.ButtonsItem) {
                items[index] = buttonsItem
                notifyItemChanged(index)
            }
        }
    }

    fun updateTask(taskItem: MainItem.TaskItem) {
        items.forEachIndexed { index, item ->
            if (item is MainItem.TaskItem) {
                items[index] = taskItem
                notifyItemChanged(index)
            }
        }
    }

    /**
     * 获取指定位置的item
     */
    fun getItemAt(position: Int): MainItem? {
        return if (position in 0 until items.size) {
            items[position]
        } else {
            null
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            MainItem.TYPE_HEADER -> {
                val binding = ItemHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }

            MainItem.TYPE_HEART_ANIMATION -> {
                val binding = ItemHeartWithStepsBinding.inflate(inflater, parent, false)
                HeartWithStepsViewHolder(binding)
            }

            MainItem.TYPE_STEP_COUNT -> {
                val binding = ItemHeartWithStepsBinding.inflate(inflater, parent, false)
                HeartWithStepsViewHolder(binding)
            }

            MainItem.TYPE_STATS -> {
                val binding = ItemStatsBinding.inflate(inflater, parent, false)
                StatsViewHolder(binding)
            }

            MainItem.TYPE_TASK -> {
                val binding = ItemTaskBinding.inflate(inflater, parent, false)
                TaskViewHolder(binding)
            }

            MainItem.TYPE_BUTTONS -> {
                val binding = ItemButtonsBinding.inflate(inflater, parent, false)
                ButtonsViewHolder(binding)
            }

            MainItem.TYPE_BANNER_AD -> {
                val binding = ItemAdBinding.inflate(inflater, parent, false)
                BannerAdViewHolder(binding)
            }

            MainItem.TYPE_NATIVE_AD -> {
                val binding = ItemAdBinding.inflate(inflater, parent, false)
                NativeAdViewHolder(binding)
            }

            MainItem.TYPE_SUMMARY -> {
                val binding = ItemSummaryBinding.inflate(inflater, parent, false)
                SummaryViewHolder(binding)
            }

            MainItem.TYPE_TASK_LIST_HEADER -> {
                val binding = ItemTaskListHeaderBinding.inflate(inflater, parent, false)
                TaskListHeaderViewHolder(binding)
            }

            MainItem.TYPE_WEEKLY_TREND -> {
                val binding = ItemWeeklyTrendBinding.inflate(inflater, parent, false)
                WeeklyTrendViewHolder(binding)
            }

            MainItem.TYPE_ACHIEVEMENTS -> {
                val binding = ItemAchievementsBinding.inflate(inflater, parent, false)
                AchievementsViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MainItem.HeaderItem -> (holder as HeaderViewHolder).bind(item, onItemClickListener)
            is MainItem.HeartAnimationItem -> (holder as HeartWithStepsViewHolder).bind(item, onItemClickListener)
            is MainItem.StepCountItem -> (holder as HeartWithStepsViewHolder).bind(item, onItemClickListener)
            is MainItem.StatsItem -> (holder as StatsViewHolder).bind(item)
            is MainItem.TaskItem -> (holder as TaskViewHolder).bind(item, onItemClickListener)
            is MainItem.ButtonsItem -> (holder as ButtonsViewHolder).bind(item, onItemClickListener)
            is MainItem.BannerAdItem -> (holder as BannerAdViewHolder).bind(item)
            is MainItem.NativeAdItem -> (holder as NativeAdViewHolder).bind(item)
            is MainItem.SummaryItem -> (holder as SummaryViewHolder).bind(item)
            is MainItem.TaskListHeaderItem -> (holder as TaskListHeaderViewHolder).bind(item)
            is MainItem.WeeklyTrendItem -> (holder as WeeklyTrendViewHolder).bind(item)
            is MainItem.AchievementsItem -> (holder as AchievementsViewHolder).bind(item)
            else -> {
                // 其他类型由子类处理
                // 这里可以抛出异常或者什么都不做，因为子类应该处理这些类型
                // 对于 LockScreenAdapter，TimeDateItem 和 StepOverviewCardItem 会在子类中处理
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        when (holder) {
            is HeartWithStepsViewHolder -> holder.startAnimations()
            is ButtonsViewHolder -> holder.checkAndStartAnimations()
            is BannerAdViewHolder -> holder.loadAd()
            is NativeAdViewHolder -> holder.loadAd()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is HeartWithStepsViewHolder -> holder.stopAnimations()
            is ButtonsViewHolder -> holder.stopAnimations()
        }
    }
}
