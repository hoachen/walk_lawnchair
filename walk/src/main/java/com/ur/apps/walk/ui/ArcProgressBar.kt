package com.ur.apps.walk.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.ur.apps.walk.R

class ArcProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0
    private var max = 100
    private var startAngle = 135f       // 默认起始角度
    private var sweepAngle = 270f       // 默认弧度（非完整圆：270°）
    private var progressColor = Color.RED
    private var backgroundColor = Color.GRAY
    private var strokeWidth = 20f       // 默认进度条宽度

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    init {
        // 读取自定义属性
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ArcProgressBar)
            progress = typedArray.getInt(R.styleable.ArcProgressBar_progress, progress)
            max = typedArray.getInt(R.styleable.ArcProgressBar_max, max)
            startAngle = typedArray.getFloat(R.styleable.ArcProgressBar_startAngle, startAngle)
            sweepAngle = typedArray.getFloat(R.styleable.ArcProgressBar_sweepAngle, sweepAngle)
            progressColor =
                typedArray.getColor(R.styleable.ArcProgressBar_progressColor, progressColor)
            backgroundColor =
                typedArray.getColor(R.styleable.ArcProgressBar_backgroundColor, backgroundColor)
            strokeWidth =
                typedArray.getDimension(R.styleable.ArcProgressBar_strokeWidth, strokeWidth)
            typedArray.recycle()
        }
        backgroundPaint.color = backgroundColor
        backgroundPaint.strokeWidth = strokeWidth

        progressPaint.color = progressColor
        progressPaint.strokeWidth = strokeWidth
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()  // 重绘视图
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 计算绘制区域，考虑 stroke 宽度
        val halfStroke = strokeWidth / 2
        val rectF = RectF(
            halfStroke,
            halfStroke,
            width.toFloat() - halfStroke,
            height.toFloat() - halfStroke
        )

        // 绘制背景弧（固定弧度）
        canvas.drawArc(rectF, startAngle, sweepAngle, false, backgroundPaint)

        // 根据 progress 绘制进度弧（弧长按进度比例变化）
        val progressSweep = sweepAngle * progress / max
        canvas.drawArc(rectF, startAngle, progressSweep, false, progressPaint)
    }
}
