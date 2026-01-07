package com.ur.apps.walk.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ur.apps.walk.step.utils.SharedPreferencesUtils
import java.util.Calendar

/**
 * 任务数据模型
 */
data class TaskModel(
    val id: Int,
    val stepGoal: Int, // 目标步数
    val completed: Boolean = false, // 是否已完成
    val claimed: Boolean = false // 是否已领取奖励
) {
    companion object {
        // 预定义的任务列表：100、500、1000、2000、3000、4000、5000、6000、7000、8000、9000、10000
        val DEFAULT_TASKS = listOf(
            TaskModel(1, 100),
            TaskModel(2, 500),
            TaskModel(3, 1000),
            TaskModel(4, 2000),
            TaskModel(5, 3000),
            TaskModel(6, 4000),
            TaskModel(7, 5000),
            TaskModel(8, 6000),
            TaskModel(9, 7000),
            TaskModel(10, 8000),
            TaskModel(11, 9000),
            TaskModel(12, 10000)
        )
        
        private const val PREF_KEY_TASKS = "daily_tasks"
        private const val PREF_KEY_LAST_RESET_DATE = "last_task_reset_date"
        
        /**
         * 从 SharedPreferences 加载任务列表
         */
        fun loadTasks(context: Context): List<TaskModel> {
            val prefs = SharedPreferencesUtils(context)
            val gson = Gson()
            
            // 检查是否需要重置（每天凌晨重置）
            checkAndResetIfNeeded(context, prefs)
            
            val tasksJson = prefs.getParam(PREF_KEY_TASKS, "") as String
            return if (tasksJson.isNotEmpty()) {
                val type = object : TypeToken<List<TaskModel>>() {}.type
                gson.fromJson(tasksJson, type) ?: DEFAULT_TASKS
            } else {
                DEFAULT_TASKS
            }
        }
        
        /**
         * 保存任务列表到 SharedPreferences
         */
        fun saveTasks(context: Context, tasks: List<TaskModel>) {
            val prefs = SharedPreferencesUtils(context)
            val gson = Gson()
            val tasksJson = gson.toJson(tasks)
            prefs.setParam(PREF_KEY_TASKS, tasksJson)
        }
        
        /**
         * 更新任务状态
         */
        fun updateTaskCompletion(context: Context, currentSteps: Int): List<TaskModel> {
            val tasks = loadTasks(context).toMutableList()
            var updated = false
            
            for (i in tasks.indices) {
                val task = tasks[i]
                if (!task.completed && currentSteps >= task.stepGoal) {
                    tasks[i] = task.copy(completed = true)
                    updated = true
                }
            }
            
            if (updated) {
                saveTasks(context, tasks)
            }
            
            return tasks
        }
        
        /**
         * 标记任务为已领取
         */
        fun markTaskAsClaimed(context: Context, taskId: Int): List<TaskModel> {
            val tasks = loadTasks(context).toMutableList()
            val index = tasks.indexOfFirst { it.id == taskId }
            
            if (index != -1) {
                val task = tasks[index]
                if (task.completed && !task.claimed) {
                    tasks[index] = task.copy(claimed = true)
                    saveTasks(context, tasks)
                }
            }
            
            return tasks
        }
        
        /**
         * 检查并重置任务（每天凌晨重置）
         */
        private fun checkAndResetIfNeeded(context: Context, prefs: SharedPreferencesUtils) {
            val lastResetDate = prefs.getParam(PREF_KEY_LAST_RESET_DATE, 0L) as Long
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // 如果上次重置日期是0（从未重置过）或者不是今天，则重置
            if (lastResetDate == 0L || !isSameDay(lastResetDate, today)) {
                // 重置任务
                saveTasks(context, DEFAULT_TASKS)
                // 更新重置日期
                prefs.setParam(PREF_KEY_LAST_RESET_DATE, today)
            }
        }
        
        /**
         * 检查两个时间戳是否在同一天
         */
        private fun isSameDay(time1: Long, time2: Long): Boolean {
            val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
            val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                   cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
        }
        
        /**
         * 获取下一个未完成的任务
         */
        fun getNextUncompletedTask(tasks: List<TaskModel>): TaskModel? {
            return tasks.firstOrNull { !it.completed }
        }
        
        /**
         * 获取已完成但未领取的任务数量
         */
        fun getCompletedButUnclaimedCount(tasks: List<TaskModel>): Int {
            return tasks.count { it.completed && !it.claimed }
        }
        
        /**
         * 获取已完成的任务数量
         */
        fun getCompletedCount(tasks: List<TaskModel>): Int {
            return tasks.count { it.completed }
        }
        
        /**
         * 获取总任务数量
         */
        fun getTotalCount(): Int {
            return DEFAULT_TASKS.size
        }
    }
}
