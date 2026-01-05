package com.ur.apps.walk.step.utils

import com.ur.apps.walk.WalkApplication
import com.ur.apps.walk.step.bean.StepData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * 运动数据生成工具类
 * 用于生成模拟的运动数据，包括步数、距离、卡路里和运动时长
 */
class ExerciseDataGenerator {
    companion object {
        private const val MIN_STEPS_PER_DAY = 3000 // 每天最少步数
        private const val MAX_STEPS_PER_DAY = 12000 // 每天最多步数
        private const val STEP_LENGTH = 0.7 // 每步距离（米）
        private const val CALORIES_PER_STEP = 0.04 // 每步消耗卡路里

        /**
         * 生成指定天数的运动数据
         * @param days 要生成的天数
         * @return 生成的运动数据列表
         */
        fun generateExerciseData(days: Int): List<StepData> {
            DbUtils.createDb(WalkApplication.getContext())
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val exerciseDataList = mutableListOf<StepData>()

            // 生成过去days天的数据
            for (i in days - 1 downTo 0) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val date = dateFormat.format(calendar.time)

                // 生成随机步数，周末增加活动量
                val isWeekend = calendar.get(Calendar.DAY_OF_WEEK) in arrayOf(Calendar.SATURDAY, Calendar.SUNDAY)
                val baseSteps = if (isWeekend) {
                    Random.nextInt(MIN_STEPS_PER_DAY + 2000, MAX_STEPS_PER_DAY + 3000)
                } else {
                    Random.nextInt(MIN_STEPS_PER_DAY, MAX_STEPS_PER_DAY)
                }

                // 创建运动数据
                val stepData = StepData().apply {
                    today = date
                    step = baseSteps.toString()

                }

                exerciseDataList.add(stepData)
            }

            // 将数据保存到数据库

            exerciseDataList.forEach { DbUtils.insert(it) }

            return exerciseDataList
        }
    }
}