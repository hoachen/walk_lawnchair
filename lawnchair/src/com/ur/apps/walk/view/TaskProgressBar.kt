package com.ur.apps.walk.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.android.launcher3.R
import com.ur.apps.walk.model.TaskModel

/**
 * 自定义任务进度条，根据TaskModel中的任务数据绘制进度和刻度
 */
class TaskProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 颜色属性
    private var progressColor: Int = Color.parseColor("#4CAF50") // 绿色，表示已完成进度
    private var backgroundColor: Int = Color.parseColor("#E0E0E0") // 灰色，表示背景
    private var scaleTextColor: Int = Color.parseColor("#000000") // 黑色，刻度文本颜色
    private var completedMarkerColor: Int = Color.parseColor("#FF9800") // 橙色，已完成标记颜色

    // 尺寸属性
    private var progressHeight: Float = 20f
    private var textPadding: Float = 15f // 增加文本间距
    private var cornerRadius: Float = 10f
    private var scaleTextSize: Float = 32f // 刻度文本大小

    // 画笔
    private lateinit var progressPaint: Paint
    private lateinit var backgroundPaint: Paint
    private lateinit var scaleTextPaint: Paint

    // 数据
    private var currentSteps: Int = 0
    private var tasks = TaskModel.DEFAULT_TASKS
    private var maxStepGoal: Int = tasks.maxByOrNull { it.stepGoal }?.stepGoal ?: 10000

    // 当前组信息（用于内部逻辑）
    private var currentGroupMin: Int = 0
    private var currentGroupMax: Int = 0
    private var currentGroupIndex: Int = 0
    private var totalGroups: Int = 0

    init {
        initAttributes(attrs, defStyleAttr)
        initPaints()
    }

    /**
     * 初始化自定义属性
     */
    private fun initAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) return

        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.TaskProgressBar,
            defStyleAttr,
            0
        )

        try {
            // 读取颜色属性
            progressColor = typedArray.getColor(
                R.styleable.TaskProgressBar_progressColor,
                Color.parseColor("#4CAF50")
            )
            backgroundColor = typedArray.getColor(
                R.styleable.TaskProgressBar_backgroundColor,
                Color.parseColor("#E0E0E0")
            )
            scaleTextColor = typedArray.getColor(
                R.styleable.TaskProgressBar_scaleTextColor,
                Color.parseColor("#000000")
            )
            completedMarkerColor = typedArray.getColor(
                R.styleable.TaskProgressBar_completedMarkerColor,
                Color.parseColor("#FF9800")
            )

            // 读取尺寸属性
            progressHeight = typedArray.getDimension(
                R.styleable.TaskProgressBar_progressHeight,
                20f
            )
            textPadding = typedArray.getDimension(
                R.styleable.TaskProgressBar_textPadding,
                15f
            )
            cornerRadius = typedArray.getDimension(
                R.styleable.TaskProgressBar_cornerRadius,
                10f
            )
            scaleTextSize = typedArray.getDimension(
                R.styleable.TaskProgressBar_scaleTextSize,
                32f
            )
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * 初始化画笔
     */
    private fun initPaints() {
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = progressColor
        }

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = backgroundColor
        }

        scaleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = scaleTextColor
            textSize = scaleTextSize
            isAntiAlias = true
        }
    }

    fun setCurrentSteps(steps: Int) {
        currentSteps = steps
        updateCurrentGroupInfo()
        invalidate()
    }

    fun setTasks(newTasks: List<TaskModel>) {
        tasks = newTasks
        maxStepGoal = tasks.maxByOrNull { it.stepGoal }?.stepGoal ?: 10000
        updateCurrentGroupInfo()
        invalidate()
    }

    /**
     * 更新当前组信息
     */
    private fun updateCurrentGroupInfo() {
        val groups = createGroups()
        totalGroups = groups.size

        // 确定当前步数所在的组
        currentGroupIndex = -1
        for ((index, group) in groups.withIndex()) {
            val groupMin = group.first().stepGoal
            val groupMax = group.last().stepGoal

            if (currentSteps >= groupMin && currentSteps <= groupMax) {
                currentGroupIndex = index
                currentGroupMin = groupMin
                currentGroupMax = groupMax
                break
            }
        }

        // 如果当前步数超过所有组，显示最后一组
        if (currentGroupIndex == -1 && currentSteps > tasks.last().stepGoal) {
            currentGroupIndex = groups.size - 1
            val lastGroup = groups.last()
            currentGroupMin = lastGroup.first().stepGoal
            currentGroupMax = lastGroup.last().stepGoal
        }
        // 如果当前步数小于所有组，显示第一组
        else if (currentGroupIndex == -1 && currentSteps < tasks.first().stepGoal) {
            currentGroupIndex = 0
            val firstGroup = groups.first()
            currentGroupMin = firstGroup.first().stepGoal
            currentGroupMax = firstGroup.last().stepGoal
        }
        // 如果当前步数在两个组之间（比如3000-4000），显示下一组
        else if (currentGroupIndex == -1) {
            // 找到第一个组最小值大于当前步数的组
            for ((index, group) in groups.withIndex()) {
                val groupMin = group.first().stepGoal
                if (currentSteps < groupMin) {
                    currentGroupIndex = index
                    currentGroupMin = group.first().stepGoal
                    currentGroupMax = group.last().stepGoal
                    break
                }
            }
            // 如果没找到，显示最后一组
            if (currentGroupIndex == -1) {
                currentGroupIndex = groups.size - 1
                val lastGroup = groups.last()
                currentGroupMin = lastGroup.first().stepGoal
                currentGroupMax = lastGroup.last().stepGoal
            }
        }
    }

    /**
     * 创建任务分组
     */
    private fun createGroups(): List<List<TaskModel>> {
        val groups = mutableListOf<List<TaskModel>>()
        var currentGroup = mutableListOf<TaskModel>()

        for (task in tasks) {
            currentGroup.add(task)
            if (currentGroup.size == 5) {
                groups.add(currentGroup.toList())
                currentGroup.clear()
            }
        }

        // 如果最后还有剩余任务，不拆分，并入前一组（如果前一组存在）
        if (currentGroup.isNotEmpty()) {
            if (groups.isNotEmpty()) {
                // 并入最后一组
                groups[groups.size - 1] = groups.last() + currentGroup
            } else {
                // 如果还没有任何组，创建一个新组
                groups.add(currentGroup)
            }
        }

        return groups
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // 计算进度条位置
        val progressTop = height / 2 - progressHeight / 2
        val progressBottom = progressTop + progressHeight

        // 绘制背景进度条（带圆角）
        val backgroundRect = RectF(0f, progressTop, width, progressBottom)
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

        // 计算当前进度（基于刻度之间的线性插值）
        val progress = if (currentGroupIndex >= 0) {
            // 获取当前组
            val groups = createGroups()
            if (currentGroupIndex < groups.size) {
                val currentGroupTasks = groups[currentGroupIndex]

                if (currentGroupTasks.size <= 1) {
                    // 如果只有一个任务，使用简单计算
                    val groupMin = currentGroupTasks.first().stepGoal.toFloat()
                    val groupMax = currentGroupTasks.last().stepGoal.toFloat()
                    if (groupMax > groupMin) {
                        ((currentSteps.toFloat() - groupMin) / (groupMax - groupMin)).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                } else {
                    // 找到当前步数所在的刻度区间
                    var leftIndex = -1
                    var rightIndex = -1

                    for (i in 0 until currentGroupTasks.size - 1) {
                        val leftStep = currentGroupTasks[i].stepGoal
                        val rightStep = currentGroupTasks[i + 1].stepGoal

                        if (currentSteps >= leftStep && currentSteps <= rightStep) {
                            leftIndex = i
                            rightIndex = i + 1
                            break
                        }
                    }

                    if (leftIndex >= 0 && rightIndex >= 0) {
                        // 在刻度区间内：线性插值
                        val leftStep = currentGroupTasks[leftIndex].stepGoal.toFloat()
                        val rightStep = currentGroupTasks[rightIndex].stepGoal.toFloat()
                        val leftPosition = leftIndex.toFloat() / (currentGroupTasks.size - 1).toFloat()
                        val rightPosition = rightIndex.toFloat() / (currentGroupTasks.size - 1).toFloat()

                        if (rightStep > leftStep) {
                            val ratio = (currentSteps.toFloat() - leftStep) / (rightStep - leftStep)
                            leftPosition + ratio * (rightPosition - leftPosition)
                        } else {
                            leftPosition
                        }
                    } else if (currentSteps < currentGroupTasks.first().stepGoal) {
                        // 小于第一个刻度
                        0f
                    } else if (currentSteps > currentGroupTasks.last().stepGoal) {
                        // 大于最后一个刻度
                        1f
                    } else {
                        // 其他情况：使用组范围计算
                        val groupMin = currentGroupTasks.first().stepGoal.toFloat()
                        val groupMax = currentGroupTasks.last().stepGoal.toFloat()
                        if (groupMax > groupMin) {
                            ((currentSteps.toFloat() - groupMin) / (groupMax - groupMin)).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    }
                }
            } else {
                0f
            }
        } else if (maxStepGoal > 0) {
            // 如果没有当前组，使用总范围计算
            currentSteps.toFloat() / maxStepGoal.toFloat()
        } else {
            0f
        }
        val progressWidth = width * progress.coerceIn(0f, 1f)

        // 绘制已完成进度（带圆角）
        if (progressWidth > 0) {
            val progressRect = RectF(0f, progressTop, progressWidth, progressBottom)
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
        }

        // 绘制刻度和文本
        drawScales(canvas, width, progressTop, progressBottom)
    }

    private fun drawScales(canvas: Canvas, width: Float, progressTop: Float, progressBottom: Float) {
        if (tasks.isEmpty() || currentGroupIndex < 0) return

        // 获取当前组
        val groups = createGroups()
        if (currentGroupIndex >= groups.size) return

        val currentGroupTasks = groups[currentGroupIndex]

        // 绘制当前组的所有刻度（在整个进度条宽度上均匀分布）
        for ((index, task) in currentGroupTasks.withIndex()) {
            // 在整个进度条宽度上均匀分布刻度
            val positionInGroup = if (currentGroupTasks.size > 1) {
                index.toFloat() / (currentGroupTasks.size - 1).toFloat()
            } else {
                0.5f // 如果只有一个任务，放在中间
            }

            // 直接使用均匀分布的位置
            val x = width * positionInGroup

            // 绘制刻度文本（不绘制刻度线）
            val text = task.stepGoal.toString() // 直接显示数字，不转换k/万
            val textWidth = scaleTextPaint.measureText(text)

            // 为左边和右边的文本添加偏移适配，防止被遮挡
            var textX = x - textWidth / 2

            // 左边文本偏移：确保不超出左边界
            if (positionInGroup == 0f) {
                textX = 0f // 左对齐
            }
            // 右边文本偏移：确保不超出右边界
            else if (positionInGroup == 1f) {
                textX = width - textWidth // 右对齐
            }
            // 中间文本：保持居中
            else {
                textX = x - textWidth / 2
            }

            // 修正文本Y坐标：使用基线位置而不是文本顶部
            val textY = progressBottom + textPadding + scaleTextSize * 0.3f

            canvas.drawText(text, textX, textY, scaleTextPaint)

            // 如果当前步数超过该任务，绘制已完成标记
            if (currentSteps >= task.stepGoal) {
                val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = completedMarkerColor // 使用配置的颜色
                }
                val markerRadius = 8f

                // 为左边和右边的圆点添加偏移适配，防止被遮挡
                var markerX = x

                // 左边圆点偏移：确保不超出左边界
                if (positionInGroup == 0f) {
                    markerX = markerRadius + 2f // 左偏移：半径+2像素
                }
                // 右边圆点偏移：确保不超出右边界
                else if (positionInGroup == 1f) {
                    markerX = width - markerRadius - 2f // 右偏移：宽度-半径-2像素
                }
                // 中间圆点：保持原位置
                else {
                    markerX = x
                }

                canvas.drawCircle(markerX, progressTop - markerRadius - 5f, markerRadius, markerPaint)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 计算所需高度：进度条高度 + 文本间距 + 文本高度 + 额外边距
        val textHeight = scaleTextSize
        val desiredHeight = (progressHeight + textPadding + textHeight + 60f).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, height)
    }
}
