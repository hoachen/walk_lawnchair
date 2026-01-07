package com.ur.apps.walk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.ur.apps.walk.model.WithdrawOption
import com.ur.apps.walk.utils.getThemeColor
import java.text.NumberFormat
import java.util.Locale

/**
 * 提现选项RecyclerView适配器
 */
class WithdrawOptionsAdapter(
    private val onOptionSelected: (WithdrawOption) -> Unit
) : ListAdapter<WithdrawOption, WithdrawOptionsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_withdraw_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
    }

    /**
     * 更新选中状态并刷新列表
     */
    fun updateSelection(selectedOption: WithdrawOption) {
        val currentList = currentList.toMutableList()
        // 重置所有选项的选中状态
        currentList.forEach { it.isSelected = false }
        // 设置新的选中项
        val selectedIndex = currentList.indexOfFirst { it.id == selectedOption.id }
        if (selectedIndex != -1) {
            currentList[selectedIndex] = currentList[selectedIndex].copy(isSelected = true)
            submitList(currentList)
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_withdraw_option)
        private val tvRemainCount: TextView = itemView.findViewById(R.id.tv_remain_count)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvCoinRequired: TextView = itemView.findViewById(R.id.tv_coin_required)


        fun bind(option: WithdrawOption) {
            // 设置剩余次数限制
            tvRemainCount.text = "${option.remainCount}/D"

            // 设置金额
            tvAmount.text = "${option.type} ${option.amount}"

            // 设置所需金币
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            tvCoinRequired.text = formatter.format(option.coinsRequired)



            // 设置选中状态 - 增强视觉效果
            if (option.remainCount <= 0) {
                tvRemainCount.setBackgroundResource(
                    R.drawable.with_draw_item_option_disable
                )
                tvRemainCount.setTextColor(
                    itemView.context.getThemeColor(
                        com.google.android.material.R.attr.colorOnSurface
                    )
                )
                itemView.setOnClickListener(null)
            } else {
                itemView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val selectedOption = getItem(position)
                        // 调用选中回调
                        onOptionSelected(selectedOption)
                    }
                }
                if (option.isSelected) {
                    // 使用较高的elevation来突出显示
                    // 变更卡片背景色，模拟边框效果
                    tvRemainCount.setBackgroundResource(
                        R.drawable.with_draw_item_option_selected
                    )
                    tvRemainCount.setTextColor(
                        itemView.context.getThemeColor(
                            com.google.android.material.R.attr.colorOnPrimary
                        )
                    )
                    tvAmount.textSize = 26f
                } else {
                    tvRemainCount.setBackgroundResource(
                        R.drawable.with_draw_item_option_normal
                    )
                    tvRemainCount.setTextColor(
                        itemView.context.getThemeColor(
                            com.google.android.material.R.attr.colorOnSurface
                        )
                    )
                    tvAmount.textSize = 24f
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WithdrawOption>() {
            override fun areItemsTheSame(
                oldItem: WithdrawOption,
                newItem: WithdrawOption
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: WithdrawOption,
                newItem: WithdrawOption
            ): Boolean {
                return oldItem == newItem && oldItem.isSelected == newItem.isSelected
            }
        }
    }
}
