package com.ur.apps.walk.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.android.launcher3.R

/**
 * 自定义环形进度View，完全匹配material.html中的环形进度设计
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH
        color = ContextCompat.getColor(context, R.color.md_theme_outline)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH
        color = ContextCompat.getColor(context, R.color.md_theme_primary)
    }

    private val rectF = RectF()

    var progress: Int = 65
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = STROKE_WIDTH / 2
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制背景环（360度）
        canvas.drawArc(rectF, 0f, 360f, false, backgroundPaint)

        // 绘制进度环（根据进度计算角度）
        val sweepAngle = (progress * 360f) / 100f
        canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
    }

    companion object {
        private const val STROKE_WIDTH = 16f // 8dp * 2（因为stroke在两侧绘制）
    }
}
