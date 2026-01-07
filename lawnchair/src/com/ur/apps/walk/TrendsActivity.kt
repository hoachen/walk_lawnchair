package com.ur.apps.walk

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.android.launcher3.BuildConfig
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import com.ur.apps.utils.URLog
import com.android.launcher3.R
import com.android.launcher3.databinding.ActivityTrendsBinding
import com.ur.apps.walk.step.bean.StepData
import com.ur.apps.walk.step.constants.StepConstants
import com.ur.apps.walk.step.manager.StepManager
import com.ur.apps.walk.step.utils.DbUtils
import com.ur.apps.walk.utils.getThemeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TAG = "TrendsActivity"

class TrendsActivity : BaseActivity() {

    private lateinit var binding: ActivityTrendsBinding
    private lateinit var stepManager: StepManager
    private lateinit var lineChart: LineChart
    private var currentPeriodLabel = ""

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
        const val TAB_DAILY = 0
        const val TAB_WEEKLY = 1
        const val TAB_MONTHLY = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stepManager = StepManager.getInstance(this)
        setupChart()
        setupToolbar()
        setupTabs()
        setupInitialTab()

        // 初始化默认标签
        currentPeriodLabel = getString(R.string.tab_daily)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        currentPeriodLabel = getString(R.string.tab_daily)
                        showDailyTrend()
                        binding.bestDayTitle.text = getString(R.string.best_day)
                    }

                    1 -> {
                        currentPeriodLabel = getString(R.string.tab_weekly)
                        showWeeklyTrend()
                        binding.bestDayTitle.text = getString(R.string.best_week)
                    }

                    2 -> {
                        currentPeriodLabel = getString(R.string.tab_monthly)
                        showMonthlyTrend()
                        binding.bestDayTitle.text = getString(R.string.best_month)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupChart() {
        // 创建LineChart实例
        lineChart = LineChart(this)

        // 设置LineChart的布局参数，确保充分利用可用空间
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(0, 0, 0, 10) // 恢复底部边距
        lineChart.layoutParams = layoutParams

        binding.chartContainer.addView(lineChart)

        // 设置图表样式
        lineChart.apply {
            description.isEnabled = false // 隐藏描述
            legend.isEnabled = false // 隐藏图例
            setDrawGridBackground(false) // 不绘制网格背景
            setDrawBorders(false) // 不绘制边框
            setTouchEnabled(true) // 允许触摸
            setScaleEnabled(false) // 禁止缩放
            setPinchZoom(false) // 禁止缩放
            // 移除自定义视口偏移
            setBackgroundColor(
                ContextCompat.getColor(
                    this@TrendsActivity,
                    android.R.color.transparent
                )
            ) // 设置背景色

            // 隐藏右侧Y轴
            axisRight.isEnabled = false

            // 隐藏左侧Y轴 - 完全禁用左侧Y轴
            axisLeft.isEnabled = false

            // 设置X轴样式
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM // 位置
                setDrawGridLines(false) // 不绘制网格线
                setDrawAxisLine(false) // 不绘制轴线
                textColor =
                    getThemeColor(
                        com.google.android.material.R.attr.colorOnBackground
                    ) // 文字颜色
                textSize = 12f // 文字大小
                granularity = 1f // 设置粒度
                setLabelCount(7, true) // 设置标签数量
                setCenterAxisLabels(false) // 确保标签不居中显示
                setAvoidFirstLastClipping(true) // 避免首尾标签被裁剪
                labelRotationAngle = 0f // 标签不旋转
                yOffset = 8f // 调小标签与轴的距离
            }

            // 设置适当的内边距
            setExtraOffsets(10f, 10f, 10f, 10f)

            // 设置空数据文本
            setNoDataText(getString(R.string.no_data_available))
            setNoDataTextColor(
                getThemeColor(
                    com.google.android.material.R.attr.colorOnBackground
                )
            )

            // 设置自定义渲染器，实现线段渐变效果
            renderer = GradientLineChartRenderer(this, animator, viewPortHandler)
        }
    }

    private fun setupInitialTab() {
        val initialTab = intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_DAILY)
        binding.tabLayout.getTabAt(initialTab)?.select()
    }

    private fun showDailyTrend() {
        try {
            // 获取过去7天的数据并按日聚合
            val allStepData = DbUtils.getQueryAll(StepData::class.java)
            var stepDataList = filterLastNDays(allStepData, 7)

            // 如果没有数据，生成测试数据
            if (stepDataList.isEmpty() && BuildConfig.DEBUG) {
                stepDataList = generateTestDailyData(7)
            }

            // 确保每天只有一个数据点 - 按日期聚合
            val aggregatedDailyData = aggregateDailyData(stepDataList)

            updateTrendView(aggregatedDailyData, getString(R.string.tab_daily))
        } catch (e: Exception) {
            e.printStackTrace()
            // 显示空视图
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.setTextColor(
                getThemeColor(
                    com.google.android.material.R.attr.colorOnBackground
                )
            )
            binding.emptyView.text = getString(R.string.no_data_available)
            binding.chartContainer.visibility = View.GONE
        }
    }

    private fun showWeeklyTrend() {
        try {
            // 获取数据并按周聚合
            val allStepData = DbUtils.getQueryAll(StepData::class.java)
            var weeklyData = aggregateWeeklyData(allStepData)

            // 如果没有数据，并且是DEBUG版本，才生成测试数据
            if (weeklyData.isEmpty() && BuildConfig.DEBUG) {
                weeklyData = generateTestWeeklyData(12)
            }

            URLog.i(TAG, "showWeeklyTrend: 周数据点数量: ${weeklyData.size}")
            updateTrendView(weeklyData, getString(R.string.tab_weekly))
        } catch (e: Exception) {
            e.printStackTrace()
            // 显示空视图
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.setTextColor(
                getThemeColor(
                    com.google.android.material.R.attr.colorOnBackground
                )
            )
            binding.emptyView.text = getString(R.string.no_data_available)
            binding.chartContainer.visibility = View.GONE
        }
    }

    private fun showMonthlyTrend() {
        try {
            // 获取数据并按月聚合
            val allStepData = DbUtils.getQueryAll(StepData::class.java)
            var monthlyData = aggregateMonthlyData(allStepData)

            // 如果没有数据，并且是DEBUG版本，才生成测试数据
            if (monthlyData.isEmpty() && BuildConfig.DEBUG) {
                monthlyData = generateTestMonthlyData(12)
            }

            URLog.i(TAG, "showMonthlyTrend: 月度数据点数量: ${monthlyData.size}")
            updateTrendView(monthlyData, getString(R.string.tab_monthly))
        } catch (e: Exception) {
            e.printStackTrace()
            // 显示空视图
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.setTextColor(
                getThemeColor(
                    com.google.android.material.R.attr.colorOnBackground
                )
            )
            binding.emptyView.text = getString(R.string.no_data_available)
            binding.chartContainer.visibility = View.GONE
        }
    }

    private fun filterLastNDays(stepDataList: List<StepData>, days: Int): List<StepData> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days + 1) // +1 是为了包含今天

        val sdf = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
        val startDate = sdf.format(calendar.time)

        return stepDataList.filter { it.today.compareTo(startDate) >= 0 }
            .sortedBy { it.today }
    }

    private fun updateTrendView(stepDataList: List<StepData>, periodLabel: String) {
        if (stepDataList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.setTextColor( getThemeColor(
                com.google.android.material.R.attr.colorOnBackground
            ) )
            binding.emptyView.text = getString(R.string.no_data_available)
            binding.chartContainer.visibility = View.GONE
            return
        }

        try {
            binding.emptyView.visibility = View.GONE
            binding.chartContainer.visibility = View.VISIBLE

            // 更新图表数据
            updateChart(stepDataList, periodLabel)

            // 更新当前统计信息
            val latestData = stepDataList.lastOrNull()
            latestData?.let {
                val steps = it.step.toIntOrNull() ?: 0
                val distance = calculateDistance(steps)
                val calories = calculateCalories(steps)

                binding.tvStepsValue.text = formatStepCount(steps)
                binding.tvDistanceValue.text = getString(R.string.format_distance, distance)
                binding.tvCaloriesValue.text = calories.toString()

                // 更新日期显示，根据不同的选项卡显示不同格式的日期
                updateDateDisplay(it.today, periodLabel)

                // 更新"最佳日"标记
                updateBestDayBadge(stepDataList)

                // 更新"最佳记录"区域
                updateBestDayInfo(stepDataList, periodLabel)
            }
        } catch (e: Exception) {
            // 处理异常，防止应用崩溃
            e.printStackTrace()
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.setTextColor( getThemeColor(
                com.google.android.material.R.attr.colorOnBackground
            ) )
            binding.emptyView.text = getString(R.string.no_data_available)
            binding.chartContainer.visibility = View.GONE
        }
    }

    private fun updateDateDisplay(dateStr: String, periodLabel: String) {
        val date = dateFromString(dateStr) ?: Date()
        val calendar = Calendar.getInstance().apply { time = date }

        when (periodLabel) {
            getString(R.string.tab_daily) -> {
                // 日视图显示格式: "Fri, Mar 7"
                val dateFormat = SimpleDateFormat(getString(R.string.date_format_daily), Locale.US)
                binding.tvDate.text = dateFormat.format(date)
            }

            getString(R.string.tab_weekly) -> {
                // 周视图显示格式: "Mar 2 - 8"
                val weekStart = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                }
                val weekEnd = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                }
                val monthDayFormat =
                    SimpleDateFormat(getString(R.string.date_format_month_day), Locale.US)
                binding.tvDate.text = getString(
                    R.string.date_format_weekly_range,
                    monthDayFormat.format(weekStart.time),
                    weekEnd.get(Calendar.DAY_OF_MONTH)
                )
            }

            getString(R.string.tab_monthly) -> {
                // 月视图显示格式: "Mar 2025"
                val dateFormat =
                    SimpleDateFormat(getString(R.string.date_format_monthly), Locale.US)
                binding.tvDate.text = dateFormat.format(date)
            }
        }
    }

    private fun updateBestDayInfo(stepDataList: List<StepData>, periodLabel: String) {
        val bestDay = stepDataList.maxByOrNull { it.step.toIntOrNull() ?: 0 }
        bestDay?.let {
            val steps = it.step.toIntOrNull() ?: 0
            binding.tvBestDaySteps.text = formatStepCount(steps)

            // 计算距离和卡路里
            val distance = calculateDistance(steps)
            val calories = calculateCalories(steps)

            binding.tvBestDayDistance.text = getString(R.string.format_distance, distance)
            binding.tvBestDayCalories.text = calories.toString()
        }
    }

    private fun formatStepCount(steps: Int): String {
        return when {
            steps >= 1000 -> getString(R.string.step_format_thousands, steps / 1000f)
            else -> steps.toString()
        }
    }

    private fun updateChart(stepDataList: List<StepData>, periodLabel: String) {
        try {
            // 确保有数据
            if (stepDataList.isEmpty()) {
                return
            }

            // 创建XAxis标签
            val xLabels = when (periodLabel) {
                getString(R.string.tab_daily) -> createDailyXLabels(stepDataList)
                getString(R.string.tab_weekly) -> createWeeklyXLabels(stepDataList)
                getString(R.string.tab_monthly) -> createMonthlyXLabels(stepDataList)
                else -> listOf()
            }

            // 转换数据为图表格式
            val entries = stepDataList.mapIndexed { index, data ->
                Entry(index.toFloat(), data.step.toFloatOrNull() ?: 0f)
            }

            // 创建数据集
            val dataSet = LineDataSet(entries, getString(R.string.dataset_steps)).apply {
                // 设置基本线条属性，自定义渲染器会忽略颜色
                color =  getThemeColor(
                    com.google.android.material.R.attr.colorPrimary
                )  // 基础颜色
                lineWidth = 3f // 线条宽度
                setDrawCircles(true) // 绘制圆点
                circleRadius = 5f // 圆点半径
                setDrawCircleHole(true) // 绘制圆点空心
                circleHoleRadius = 2.5f // 空心半径
                setDrawValues(false) // 不绘制数值
                mode = LineDataSet.Mode.CUBIC_BEZIER // 曲线模式

                // 禁用填充区域，让渐变线条效果更明显
                setDrawFilled(false)

                // 启用高亮效果
                setDrawHighlightIndicators(true)
                highLightColor =  getThemeColor(
                    com.google.android.material.R.attr.colorPrimary
                )
                highlightLineWidth = 1.5f
            }

            // 重置图表的配置
            lineChart.apply {
                // 设置适当的内边距
                setExtraOffsets(10f, 10f, 10f, 10f)

                // 重置X轴配置
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(xLabels)
                    setLabelCount(xLabels.size, true) // 确保显示所有标签
                    setCenterAxisLabels(false) // 确保标签不居中显示

                    // 根据不同视图设置适当的轴线范围
                    if (periodLabel == getString(R.string.tab_daily)) {
                        // 配置日视图的X轴
                        axisMinimum = -0.3f  // 留出左侧空间
                        axisMaximum = (entries.size - 0.7f)  // 留出右侧空间
                        granularity = 1f // 每个单位为1
                        labelCount = 7 // 固定为7个标签
                        yOffset = 8f // 适当的标签与轴的距离
                    } else if (periodLabel == getString(R.string.tab_weekly)) {
                        axisMinimum = -0.3f  // 留出左侧空间
                        axisMaximum = (entries.size - 0.7f)  // 留出右侧空间
                        granularity = 1f
                        labelCount = entries.size
                        yOffset = 8f // 适当的标签与轴的距离
                    } else if (periodLabel == getString(R.string.tab_monthly)) {
                        axisMinimum = -0.3f  // 留出左侧空间
                        axisMaximum = (entries.size - 0.7f)  // 留出右侧空间
                        granularity = 1f
                        labelCount = entries.size
                        yOffset = 8f // 适当的标签与轴的距离
                    }
                }

                // 确保启用触摸和高亮
                isHighlightPerDragEnabled = true
                isHighlightPerTapEnabled = true

                // 清除之前的数据
                clear()

                // 应用新数据到图表
                data = LineData(dataSet)

                // 禁用图表动画，确保立即显示
                animateX(0)

                // 高亮当前日/周/月 - 必须在设置数据之后进行高亮操作
                val latestIndex = stepDataList.size - 1
                if (latestIndex >= 0) {
                    highlightValue(latestIndex.toFloat(), 0, false)
                }

                // 刷新图表
                invalidate()
            }
        } catch (e: Exception) {
            // 处理异常
            e.printStackTrace()
        }
    }

    /**
     * 创建日视图的X轴标签
     * 确保按照周日到周六的顺序排列
     */
    private fun createDailyXLabels(stepDataList: List<StepData>): List<String> {
        // 固定标签顺序为："日、一、二、三、四、五、六"
        // 注意：这是为了保证标签顺序是固定的，即使数据点的日期顺序可能不同

        // 创建日期->星期映射
        val dateToWeekday = mutableMapOf<String, String>()
        val dayNames = listOf(
            getString(R.string.weekday_sunday),
            getString(R.string.weekday_monday),
            getString(R.string.weekday_tuesday),
            getString(R.string.weekday_wednesday),
            getString(R.string.weekday_thursday),
            getString(R.string.weekday_friday),
            getString(R.string.weekday_saturday)
        )
        val calendar = Calendar.getInstance()

        // 对每个数据点，记录其对应的星期几
        stepDataList.forEach { data ->
            val date = dateFromString(data.today) ?: Date()
            calendar.time = date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 转为0-6的索引
            dateToWeekday[data.today] = dayNames[dayOfWeek]
        }

        // 对于每个数据点，返回其对应的星期标签
        return stepDataList.map { data -> dateToWeekday[data.today] ?: "" }
    }

    private fun createWeeklyXLabels(stepDataList: List<StepData>): List<String> {
        // 对于周视图，显示更简洁的周标签
        return stepDataList.mapIndexed { index, data ->
            getString(R.string.weekly_label_format, index + 1)
        }
    }

    private fun createMonthlyXLabels(stepDataList: List<StepData>): List<String> {
        // 对于月视图，显示更简洁的月份标签
        return stepDataList.mapIndexed { index, data ->
            val date = dateFromString(data.today) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            getString(R.string.monthly_label_format, cal.get(Calendar.MONTH) + 1)
        }
    }

    /**
     * 将步数数据按周聚合
     */
    private fun aggregateWeeklyData(stepDataList: List<StepData>): List<StepData> {
        if (stepDataList.isEmpty()) return emptyList()

        // 按周分组并聚合数据
        val weeklyMap = HashMap<Int, StepData>()
        val calendar = Calendar.getInstance()

        for (data in stepDataList) {
            val dateStr = data.today ?: continue

            try {
                // 使用SimpleDateFormat解析日期字符串
                val sdf = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
                val date = sdf.parse(dateStr) ?: continue

                // 设置日历对象为当前日期
                calendar.time = date

                // 获取年份和周数
                val year = calendar.get(Calendar.YEAR)
                val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

                val key = year * 100 + weekOfYear  // 使用年份和周数的组合作为键

                if (weeklyMap.containsKey(key)) {
                    // 已存在该周的数据，累加步数
                    val existingData = weeklyMap[key]!!
                    val existingSteps = existingData.step?.toIntOrNull() ?: 0
                    val newSteps = data.step?.toIntOrNull() ?: 0
                    existingData.step = (existingSteps + newSteps).toString()
                } else {
                    // 创建该周的新数据
                    val weekData = StepData()
                    weekData.today = "${year}年第${weekOfYear}周"
                    weekData.step = data.step
                    weeklyMap[key] = weekData
                }
            } catch (e: Exception) {
                URLog.e(TAG, "Error parsing date: $dateStr", e)
                continue
            }
        }

        // 转换为列表并排序
        return weeklyMap.values.toList().sortedBy {
            val parts = it.today?.split("年第", "周") ?: listOf("0", "0")
            val year = parts[0].toIntOrNull() ?: 0
            val week = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            year * 100 + week
        }
    }

    /**
     * 将步数数据按月聚合
     */
    private fun aggregateMonthlyData(stepDataList: List<StepData>): List<StepData> {
        if (stepDataList.isEmpty()) return emptyList()

        // 按月分组并聚合数据
        val monthlyMap = HashMap<Int, StepData>()
        val calendar = Calendar.getInstance()

        for (data in stepDataList) {
            val dateStr = data.today ?: continue

            try {
                // 使用SimpleDateFormat解析日期字符串
                val sdf = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
                val date = sdf.parse(dateStr) ?: continue

                // 设置日历对象为当前日期
                calendar.time = date

                // 获取年份和月份
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1  // Calendar.MONTH从0开始，所以要+1

                val key = year * 100 + month  // 使用年份和月份的组合作为键

                if (monthlyMap.containsKey(key)) {
                    // 已存在该月的数据，累加步数
                    val existingData = monthlyMap[key]!!
                    val existingSteps = existingData.step?.toIntOrNull() ?: 0
                    val newSteps = data.step?.toIntOrNull() ?: 0
                    existingData.step = (existingSteps + newSteps).toString()
                } else {
                    // 创建该月的新数据
                    val monthData = StepData()
                    monthData.today = "${year}年${month}月"
                    monthData.step = data.step
                    monthlyMap[key] = monthData
                }
            } catch (e: Exception) {
                URLog.e(TAG, "Error parsing date: $dateStr", e)
                continue
            }
        }

        // 转换为列表并排序
        return monthlyMap.values.toList().sortedBy {
            val parts = it.today?.split("年", "月") ?: listOf("0", "0")
            val year = parts[0].toIntOrNull() ?: 0
            val month = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            year * 100 + month
        }
    }

    private fun dateFromString(dateStr: String): Date? {
        return try {
            SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateDistance(steps: Int): Double {
        // 简单假设每步0.7米
        return steps * 0.7 / 1000 // 转换为公里
    }

    private fun calculateCalories(steps: Int): Int {
        // 简单假设每1000步消耗40卡路里
        return (steps * 0.04).toInt()
    }

    private fun updateBestDayBadge(stepDataList: List<StepData>) {
        val bestDay = stepDataList.maxByOrNull { it.step.toIntOrNull() ?: 0 }
        val latestDay = stepDataList.lastOrNull()

        if (bestDay != null && latestDay != null && bestDay.today == latestDay.today) {
            binding.bestDayBadge.visibility = View.VISIBLE
        } else {
            binding.bestDayBadge.visibility = View.GONE
        }
    }

    /**
     * 将数据按天聚合，确保每天只有一个数据点
     */
    private fun aggregateDailyData(stepDataList: List<StepData>): List<StepData> {
        // 确保有一周完整的数据（7天）
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
        val result = mutableListOf<StepData>()

        // 创建一个包含最近7天日期的集合
        val dateMap = mutableMapOf<String, Int>()
        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(calendar.time)
            dateMap[dateStr] = 0
            calendar.add(Calendar.DAY_OF_YEAR, i) // 复位
        }

        // 将原始数据填入对应日期
        for (data in stepDataList) {
            val date = data.today
            if (dateMap.containsKey(date)) {
                dateMap[date] = data.step.toIntOrNull() ?: 0
            }
        }

        // 按日期排序并创建新的StepData列表
        dateMap.entries.sortedBy { it.key }.forEach { (date, steps) ->
            val stepData = StepData()
            stepData.today = date
            stepData.step = steps.toString()
            result.add(stepData)
        }

        return result
    }

    /**
     * 生成测试数据 - 修改为准确生成一周的数据
     */
    private fun generateTestDailyData(days: Int): List<StepData> {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
        val result = mutableListOf<StepData>()

        // 确保生成的数据是当前日期往前的days天
        for (i in days - 1 downTo 0) {
            // 计算日期
            calendar.time = Date() // 重置为当前日期
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = format.format(calendar.time)

            val stepData = StepData()
            stepData.today = date

            // 随机生成步数，范围2000-12000
            val steps = (2000 + Math.random() * 10000).toInt()
            stepData.step = steps.toString()

            result.add(stepData)
        }

        return result
    }

    // 直接生成月度测试数据，确保有12个月
    private fun generateTestMonthlyData(months: Int): List<StepData> {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
        val result = mutableListOf<StepData>()

        // 从当前月往前生成months个月的数据
        for (i in months - 1 downTo 0) {
            calendar.time = Date() // 重置为当前日期
            // 设置为每月的第一天
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            // 往前移动i个月
            calendar.add(Calendar.MONTH, -i)

            val date = format.format(calendar.time)

            val stepData = StepData()
            stepData.today = date

            // 月度数据步数应该更大，范围200000-1000000
            val steps = (200000 + Math.random() * 800000).toInt()
            stepData.step = steps.toString()

            result.add(stepData)
        }

        return result
    }

    // 直接生成周测试数据，确保有12周
    private fun generateTestWeeklyData(weeks: Int): List<StepData> {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat(StepConstants.DATE_FORMAT_FULL, Locale.getDefault())
        val result = mutableListOf<StepData>()

        // 从当前周往前生成weeks个周的数据
        for (i in weeks - 1 downTo 0) {
            calendar.time = Date() // 重置为当前日期
            // 设置为每周的周日
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            // 往前移动i周
            calendar.add(Calendar.WEEK_OF_YEAR, -i)

            val date = format.format(calendar.time)

            val stepData = StepData()
            stepData.today = date

            // 周数据步数范围30000-200000
            val steps = (30000 + Math.random() * 170000).toInt()
            stepData.step = steps.toString()

            result.add(stepData)
        }

        return result
    }
}
