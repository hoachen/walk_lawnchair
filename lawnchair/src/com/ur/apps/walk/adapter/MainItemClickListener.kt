package com.ur.apps.walk.adapter

/**
 * MainAdapter的点击监听器接口
 */
interface MainItemClickListener {
    fun onProfileClick()
    fun onCoinClick()
    fun onEarnCoinsClick()
    fun onInspirationClick()
    fun onTaskClick(taskId: Int, targetDistance: Int, currentDistance: Int)
    fun onCloseTaskClick()
    fun onTreasureClick()
    fun onTaskClaimClick(taskId: Int, stepGoal: Int) // 新增：任务领取点击回调
}
