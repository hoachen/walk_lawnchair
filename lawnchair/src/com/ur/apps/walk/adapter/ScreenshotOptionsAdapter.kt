package com.ur.apps.walk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.ur.apps.walk.model.ScreenshotOption
import com.ur.apps.walk.utils.getThemeColor

/**
 * 截图选项RecyclerView适配器
 */
class ScreenshotOptionsAdapter(
    private val onOptionSelected: (ScreenshotOption) -> Unit
) : ListAdapter<ScreenshotOption, ScreenshotOptionsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
    }

    /**
     * 更新选中状态并刷新列表
     */
    fun updateSelection(selectedOption: ScreenshotOption) {
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
        private val cardView: CardView = itemView.findViewById(R.id.card_screenshot_option)!!
        private val tvOptionName: TextView = itemView.findViewById(R.id.tv_option_name)!!
        private val tvRegion: TextView = itemView.findViewById(R.id.tv_region)!!
        private val radioButton: RadioButton = itemView.findViewById(R.id.radio_select)!!

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val selectedOption = getItem(position)
                    // 调用选中回调
                    onOptionSelected(selectedOption)
                }
            }
        }

        fun bind(option: ScreenshotOption) {
            // 设置选项名称
            tvOptionName.text = option.name

            // 设置区域信息
            tvRegion.text = option.region

            // 设置选中状态
            radioButton.isChecked = option.isSelected

            // 设置选中状态的视觉效果
            if (option.isSelected) {
                cardView.setCardBackgroundColor(
                    cardView.context.getThemeColor(
                        com.google.android.material.R.attr.colorSecondary
                    )
                )
            } else {
                cardView.setCardBackgroundColor(itemView.context.getColor(android.R.color.white))
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ScreenshotOption>() {
            override fun areItemsTheSame(
                oldItem: ScreenshotOption,
                newItem: ScreenshotOption
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ScreenshotOption,
                newItem: ScreenshotOption
            ): Boolean {
                return oldItem == newItem && oldItem.isSelected == newItem.isSelected
            }
        }
    }
}
