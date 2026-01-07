package com.ur.apps.walk.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.android.launcher3.databinding.ItemDailyStepBinding
import com.ur.apps.walk.model.MainItem

/**
 * 每日步数数据的Adapter
 */
class DailyStepAdapter : RecyclerView.Adapter<DailyStepAdapter.DailyStepViewHolder>() {

    private var dailyData: List<MainItem.WeeklyTrendItem.DailyStepData> = emptyList()
    private var maxSteps: Int = 1
    private var minSteps: Int = 0

    fun submitList(data: List<MainItem.WeeklyTrendItem.DailyStepData>) {
        dailyData = data
        if (data.isNotEmpty()) {
            maxSteps = data.maxOf { it.stepCount }
            minSteps = data.minOf { it.stepCount }
        } else {
            maxSteps = 1
            minSteps = 0
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyStepViewHolder {
        val binding = ItemDailyStepBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyStepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyStepViewHolder, position: Int) {
        holder.bind(dailyData[position], maxSteps, minSteps)
    }

    override fun getItemCount(): Int = dailyData.size

    inner class DailyStepViewHolder(private val binding: ItemDailyStepBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: MainItem.WeeklyTrendItem.DailyStepData, maxSteps: Int, minSteps: Int) {
            // 设置日期标签
            binding.tvDayLabel.text = data.dayLabel

            // 设置步数值
            binding.tvStepValue.text = formatStepCount(data.stepCount)

            // 计算柱状图高度
            updateBarHeight(data.stepCount, maxSteps, minSteps)
        }

        private fun updateBarHeight(stepCount: Int, maxSteps: Int, minSteps: Int) {
            // 计算柱状图高度比例
            val heightRatio = if (maxSteps > minSteps) {
                (stepCount - minSteps).toFloat() / (maxSteps - minSteps)
            } else {
                0.5f // 如果所有值都相同，显示50%高度
            }

            // 确保高度比例在合理范围内
            val adjustedRatio = heightRatio.coerceIn(0.1f, 1.0f)

            // 设置填充视图的高度
            binding.fillView.post {
                val barHeight = binding.barContainer.height
                if (barHeight > 0) {
                    val actualHeight = (barHeight * adjustedRatio).toInt()
                    val layoutParams = binding.fillView.layoutParams
                    layoutParams.height = actualHeight
                    binding.fillView.layoutParams = layoutParams
                    binding.fillView.requestLayout()
                }
            }
        }

        private fun formatStepCount(steps: Int): String {
            return if (steps >= 1000) {
                "${steps / 1000}.${(steps % 1000) / 100}k"
            } else {
                steps.toString()
            }
        }
    }
}
