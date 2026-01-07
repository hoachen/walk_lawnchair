package com.ur.apps.walk.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.ur.apps.utils.URLog
import com.android.launcher3.R
import com.ur.apps.walk.utils.ThemeManager
import com.ur.apps.walk.utils.getThemeColor

/**
 * 颜色选择器适配器 - 优化版
 */
class ColorOptionAdapter(
    private val context: Context,
    private val themes: List<ThemeManager.BrandTheme>,
    private var selectedThemeName: String,
    private val onColorSelected: (ThemeManager.BrandTheme) -> Unit
) : RecyclerView.Adapter<ColorOptionAdapter.ColorViewHolder>() {

    companion object {
        private const val TAG = "ColorOptionAdapter"
    }

    // 创建ThemeManager实例用于渐变背景
    private val themeManager = ThemeManager.getInstance()

    init {
        URLog.d(TAG, "初始化: 主题数量=${themes.size}, 选中Name=$selectedThemeName")
        // 调试日志：输出所有可用主题
        themes.forEachIndexed { index, theme ->
            URLog.d(
                TAG,
                "主题[$index]: ID=${theme.name}, 颜色=${com.google.android.material.R.attr.colorPrimary}"
            )
        }
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val colorView: View = itemView.findViewById(R.id.color_square)
        private val checkMark: ImageView = itemView.findViewById(R.id.check_mark)

        fun bind(theme: ThemeManager.BrandTheme) {
            try {
                // 使用渐变背景而不是单一颜色
                val color = themeManager.getColorFromSpecificTheme(
                    baseContext = context,
                    themeResId = theme.themeRes,
                    attrResId = com.google.android.material.R.attr.colorPrimary
                )

                URLog.d(TAG, "为主题${theme.name}创建渐变背景")

                // 设置渐变背景到colorView
                colorView.setBackgroundColor(color)

                // 确保cardView的背景是透明的，以便显示渐变效果
                cardView.setCardBackgroundColor(Color.TRANSPARENT)

                // 设置选中状态
                val isSelected = theme.name == selectedThemeName
                URLog.d(
                    TAG,
                    "绑定主题 ${theme.name}, 是否选中: $isSelected (当前选中ID: $selectedThemeName)"
                )

                // 更新选中图标的显示状态
                checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE

                // 设置卡片阴影和边框
                if (isSelected) {
                    // 选中状态：增加阴影效果并使用内边框
                    cardView.cardElevation =
                        context.resources.getDimension(R.dimen.color_option_selected_elevation)
                    cardView.setContentPadding(4, 4, 4, 4)
                    // 在选中状态下，我们可以用内边距和更高的阴影来突出显示
                } else {
                    // 未选中状态：正常阴影，无边框
                    cardView.cardElevation =
                        context.resources.getDimension(R.dimen.color_option_normal_elevation)
                    cardView.setContentPadding(0, 0, 0, 0)
                }

                // 设置点击事件
                itemView.setOnClickListener {
                    URLog.d(TAG, "点击了颜色主题: ${theme.name}")
                    if (selectedThemeName != theme.name) {
                        // 记录旧的选中位置
                        val oldSelectedPosition =
                            themes.indexOfFirst { it.name == selectedThemeName }

                        // 更新选中ID
                        selectedThemeName = theme.name

                        // 更新UI
                        if (oldSelectedPosition >= 0) {
                            notifyItemChanged(oldSelectedPosition)
                        }
                        notifyItemChanged(bindingAdapterPosition)

                        // 回调
                        onColorSelected(theme)
                    }
                }
            } catch (e: Exception) {
                URLog.e(TAG, "绑定颜色选项时出错: ${e.message}", e)
                // 回退到使用单一颜色
                try {
                    val color = context.getThemeColor(
                        com.google.android.material.R.attr.colorPrimary
                    )
                    colorView.setBackgroundColor(color)
                } catch (e2: Exception) {
                    URLog.e(TAG, "回退到单一颜色也失败: ${e2.message}")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_option, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(themes[position])
    }

    override fun getItemCount(): Int = themes.size
}
