package com.ur.apps.walk.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ur.apps.utils.URLog
import com.ur.apps.walk.R
import com.ur.apps.walk.settings.model.SettingItem
import com.ur.apps.walk.utils.ThemeManager
import com.ur.apps.walk.utils.getThemeColor

/**
 * 设置项适配器 - 采用多类型视图模式
 */
class SettingsAdapter(
    private val context: Context,
    private val sections: List<SettingItem.Section>,
    private val listeners: Listeners
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 回调接口
    interface Listeners {
        fun onSwitchChanged(id: String, isChecked: Boolean)
        fun onValueItemClicked(id: String)
        fun onThemeSelected(theme: ThemeManager.BrandTheme)
        fun onDistanceUnitChanged(id: String, isKm: Boolean)
        fun onActionItemClicked(action: SettingItem.Item.Action)
    }

    // 视图类型
    companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_SWITCH = 1
        private const val VIEW_TYPE_VALUE = 2
        private const val VIEW_TYPE_COLOR_PICKER = 3
        private const val VIEW_TYPE_DISTANCE = 4
        private const val VIEW_TYPE_ACTION = 5
    }

    // 数据结构：将Section和Item展平为一个列表
    private val flattenedItems = mutableListOf<Any>()

    init {
        // 处理数据，将sections和items展平成一个列表
        flattenData()
    }

    private fun flattenData() {
        flattenedItems.clear()
        sections.forEach { section ->
            // 添加Section
            flattenedItems.add(section)

            // 如果Section展开，添加所有Item
            if (section.isExpanded) {
                flattenedItems.addAll(section.items)
            }
        }
        notifyDataSetChanged()
    }

    // 根据位置获取视图类型
    override fun getItemViewType(position: Int): Int {
        return when (val item = flattenedItems[position]) {
            is SettingItem.Section -> VIEW_TYPE_SECTION
            is SettingItem.Item.Switch -> VIEW_TYPE_SWITCH
            is SettingItem.Item.Value -> VIEW_TYPE_VALUE
            is SettingItem.Item.ColorPicker -> VIEW_TYPE_COLOR_PICKER
            is SettingItem.Item.Distance -> VIEW_TYPE_DISTANCE
            is SettingItem.Item.Action -> VIEW_TYPE_ACTION
            else -> throw IllegalArgumentException("未知的视图类型")
        }
    }

    // 创建ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_section_header, parent, false)
                SectionViewHolder(view)
            }

            VIEW_TYPE_SWITCH -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_switch, parent, false)
                SwitchViewHolder(view)
            }

            VIEW_TYPE_VALUE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_value, parent, false)
                ValueViewHolder(view)
            }

            VIEW_TYPE_COLOR_PICKER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_color_picker, parent, false)
                ColorPickerViewHolder(view)
            }

            VIEW_TYPE_DISTANCE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_distance, parent, false)
                DistanceViewHolder(view)
            }

            VIEW_TYPE_ACTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_action, parent, false)
                ActionViewHolder(view)
            }

            else -> throw IllegalArgumentException("未知的视图类型")
        }
    }

    // 绑定ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionViewHolder -> {
                val section = flattenedItems[position] as SettingItem.Section
                holder.bind(section)
            }

            is SwitchViewHolder -> {
                val item = flattenedItems[position] as SettingItem.Item.Switch
                holder.bind(item)
            }

            is ValueViewHolder -> {
                val item = flattenedItems[position] as SettingItem.Item.Value
                holder.bind(item)
            }

            is ColorPickerViewHolder -> {
                val item = flattenedItems[position] as SettingItem.Item.ColorPicker
                holder.bind(item)
            }

            is DistanceViewHolder -> {
                val item = flattenedItems[position] as SettingItem.Item.Distance
                holder.bind(item)
            }

            is ActionViewHolder -> {
                val item = flattenedItems[position] as SettingItem.Item.Action
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = flattenedItems.size

    // Section ViewHolder
    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tv_section_title)

        fun bind(section: SettingItem.Section) {
            titleView.text = section.title

            // 点击切换展开/收起状态
            itemView.setOnClickListener {
                section.isExpanded = !section.isExpanded
                // 重新处理数据
                flattenData()
            }
        }

    }

    // Switch ViewHolder
    inner class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tv_title)
        private val switch: SwitchMaterial = itemView.findViewById(R.id.switch_setting)

        fun bind(item: SettingItem.Item.Switch) {
            titleView.text = item.title
            switch.isChecked = item.isChecked

            switch.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                listeners.onSwitchChanged(item.id, isChecked)
            }
        }
    }

    // Value ViewHolder
    inner class ValueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tv_title)
        private val valueView: TextView = itemView.findViewById(R.id.tv_value)

        fun bind(item: SettingItem.Item.Value) {
            titleView.text = item.title
            valueView.text = item.value

            itemView.setOnClickListener {
                listeners.onValueItemClicked(item.id)
            }
        }
    }

    // ColorPicker ViewHolder
    inner class ColorPickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_colors)
        private val themeManager = ThemeManager.getInstance()

        fun bind(item: SettingItem.Item.ColorPicker) {
            val allThemes = themeManager.getAllThemes()
            val currentTheme = themeManager.getCurrentTheme()

            // 日志记录可用主题数量和当前主题
            URLog.d(
                "SettingsAdapter",
                "可用主题数量: ${allThemes.size}, 当前主题ID: ${currentTheme.name}"
            )

            // 详细记录每个主题的颜色信息
            allThemes.forEach { theme ->
                try {
                    val color = themeManager.getColorFromSpecificTheme(
                        baseContext = context,
                        themeResId = theme.themeRes,
                        attrResId = com.google.android.material.R.attr.colorPrimary
                    )
                    URLog.d(
                        "SettingsAdapter",
                        "主题[${theme.name}]颜色: 资源ID=${com.google.android.material.R.attr.colorPrimary}, 颜色值=0x${
                            Integer.toHexString(color)
                        }"
                    )
                } catch (e: Exception) {
                    URLog.e("SettingsAdapter", "获取主题[${theme.name}]颜色失败: ${e.message}")
                }
            }

            // 设置网格布局，每行4个颜色
            colorRecyclerView.layoutManager = GridLayoutManager(context, 4)

            // 创建并设置颜色选项适配器
            val colorAdapter = ColorOptionAdapter(
                context,
                allThemes,
                currentTheme.name
            ) { selectedTheme ->
                // 更新选中颜色
                item.selectedColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary)

                // 回调通知
                listeners.onThemeSelected(selectedTheme)

                // 日志记录主题切换
                URLog.d("SettingsAdapter", "已切换主题: ${selectedTheme.name}")
            }

            colorRecyclerView.adapter = colorAdapter

            // 确保RecyclerView是可见的
            colorRecyclerView.visibility = View.VISIBLE
        }
    }

    // Distance ViewHolder
    inner class DistanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tv_title)
        private val toggleGroup: MaterialButtonToggleGroup =
            itemView.findViewById(R.id.toggle_distance)

        fun bind(item: SettingItem.Item.Distance) {
            titleView.text = item.title

            // 设置当前选中的单位
            toggleGroup.check(if (item.isKm) R.id.btn_km else R.id.btn_miles)

            // 设置单位切换监听
            toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val isKm = checkedId == R.id.btn_km
                    item.isKm = isKm
                    listeners.onDistanceUnitChanged(item.id, isKm)
                }
            }
        }
    }

    // Action ViewHolder
    inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tv_title)

        fun bind(item: SettingItem.Item.Action) {
            titleView.text = item.title

            itemView.setOnClickListener {
                listeners.onActionItemClicked(item)
            }
        }
    }

    // 公开方法：刷新数据
    fun refreshData() {
        flattenData()
    }
} 