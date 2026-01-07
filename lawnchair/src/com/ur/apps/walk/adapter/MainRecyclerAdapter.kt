package com.ur.apps.walk.adapter


/**
 * MainRecyclerAdapter - 用于 MainActivityRecycler 的专用 Adapter
 * 继承自 BaseAdapter，可以添加 MainActivityRecycler 特有的逻辑
 */
class MainRecyclerAdapter(
    onItemClickListener: MainItemClickListener
) : BaseAdapter(onItemClickListener) {

    // 可以在这里添加 MainActivityRecycler 特有的方法
    // 例如：特定的数据更新逻辑、动画控制等
    
    /**
     * 更新所有项目的数据（MainActivityRecycler 特有的方法）
     */
    fun updateAllItemsWithStats(stats: com.ur.apps.walk.step.bean.ExerciseStats) {
        // 这里可以添加 MainActivityRecycler 特有的更新逻辑
        // 例如：同时更新多个 item 类型
    }
    
    /**
     * 获取特定类型的 item 位置
     */
    fun findItemPositionByType(itemType: Int): Int {
        for (i in 0 until itemCount) {
            if (getItemViewType(i) == itemType) {
                return i
            }
        }
        return -1
    }
    
    /**
     * 检查是否包含特定类型的 item
     */
    fun containsItemType(itemType: Int): Boolean {
        return items.any { it.type == itemType }
    }
}
