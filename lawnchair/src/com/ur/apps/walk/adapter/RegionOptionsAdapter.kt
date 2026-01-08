package com.ur.apps.walk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.android.launcher3.R
import com.ur.apps.walk.model.RegionOption

/**
 * 区域选项RecyclerView适配器
 */
class RegionOptionsAdapter(
    private val onOptionSelected: (RegionOption) -> Unit
) : ListAdapter<RegionOption, RegionOptionsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_region_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
    }

    /**
     * 更新选中状态并刷新列表
     */
    fun updateSelection(selectedOption: RegionOption) {
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
        private val cardConstraint: View = itemView.findViewById(R.id.card_constraint)!!
        private val regionScreen: View = itemView.findViewById(R.id.region_screen)!!
        private val imgFlag: ImageView = itemView.findViewById(R.id.img_flag)!!
        private val tvRegionName: TextView = itemView.findViewById(R.id.tv_region_name)!!
        private val imgPaymentType: ImageView = itemView.findViewById(R.id.payment_type)!!
        private val regionSelectedDisplay: ImageView =
            itemView.findViewById(R.id.region_selected_display)!!

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

        fun bind(option: RegionOption) {

            Glide.with(itemView.context)
                .load(option.nationFlag)
                .centerInside()
                .into(imgFlag)

            // 设置区域名称
            tvRegionName.text = option.name

            Glide.with(itemView.context)
                .load(option.paymentType)
                .centerInside()
                .into(imgPaymentType)

            itemView.isSelected = option.isSelected

            // 设置选中状态
            if (option.isSelected) {
                regionSelectedDisplay.setImageResource(R.drawable.region_pick_up)
                cardConstraint.setBackgroundResource(R.drawable.region_item_card_stroke_selected)
                regionScreen.setBackgroundResource(R.drawable.region_item_card_screen_selected)
            } else {
                regionSelectedDisplay.setImageResource(R.drawable.region_unpick_up)
                cardConstraint.setBackgroundResource(R.drawable.region_item_card_stroke_normal)
                regionScreen.setBackgroundResource(R.drawable.region_item_card_screen_normal)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RegionOption>() {
            override fun areItemsTheSame(
                oldItem: RegionOption,
                newItem: RegionOption
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: RegionOption,
                newItem: RegionOption
            ): Boolean {
                return oldItem == newItem && oldItem.isSelected == newItem.isSelected
            }
        }
    }
}
