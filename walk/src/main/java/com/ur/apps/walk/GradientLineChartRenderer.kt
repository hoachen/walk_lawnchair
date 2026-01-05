package com.ur.apps.walk

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * 自定义LineChartRenderer实现线段渐变效果
 */
class GradientLineChartRenderer(
    chart: LineDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineChartRenderer(chart, animator, viewPortHandler) {

    // 彩虹渐变色数组
    private val gradientColors = intArrayOf(
        Color.parseColor("#4CAF50"), // 绿色
        Color.parseColor("#8BC34A"), // 浅绿色
        Color.parseColor("#CDDC39"), // 酸橙色
        Color.parseColor("#FFEB3B"), // 黄色
        Color.parseColor("#FFC107"), // 琥珀色
        Color.parseColor("#FF9800"), // 橙色
        Color.parseColor("#FF5722"), // 深橙色
        Color.parseColor("#F44336"), // 红色
        Color.parseColor("#E91E63"), // 粉色
        Color.parseColor("#9C27B0"), // 紫色
        Color.parseColor("#673AB7"), // 深紫色
        Color.parseColor("#3F51B5"), // 靛蓝色
        Color.parseColor("#2196F3"), // 蓝色
        Color.parseColor("#03A9F4")  // 浅蓝色
    )

    // 存储转换后的点坐标
    private val pointsBuffer = FloatArray(100)
    
    // 绘制贝塞尔曲线的Path
    private val cubicPath = Path()
    
    // 控制点
    private val cubicControlPoints = FloatArray(4)

    override fun drawDataSet(c: Canvas, dataSet: ILineDataSet) {
        if (dataSet.entryCount < 2) return
        
        // 根据模式选择绘制方法
        if (dataSet.mode == LineDataSet.Mode.CUBIC_BEZIER) {
            // 使用贝塞尔曲线绘制渐变曲线
            drawCubicBezierGradient(c, dataSet)
        } else {
            // 使用直线绘制渐变效果
            drawGradientLines(c, dataSet)
        }
        
        // 使用自定义方法绘制彩色圆点
        drawColorfulCircles(c, dataSet)
    }
    
    // 绘制带渐变效果的贝塞尔曲线
    private fun drawCubicBezierGradient(c: Canvas, dataSet: ILineDataSet) {
        val entryCount = dataSet.entryCount
        
        // 获取转换器
        val trans = mChart.getTransformer(dataSet.axisDependency)
        
        // 设置画笔样式
        mRenderPaint.style = Paint.Style.STROKE
        mRenderPaint.strokeWidth = dataSet.lineWidth
        mRenderPaint.pathEffect = dataSet.dashPathEffect
        
        // 确保缓冲区足够大
        val requiredSize = entryCount * 2
        val pointsBuffer = if (this.pointsBuffer.size < requiredSize) {
            FloatArray(requiredSize)
        } else {
            this.pointsBuffer
        }
        
        // 填充缓冲区
        for (i in 0 until entryCount) {
            val e = dataSet.getEntryForIndex(i)
            pointsBuffer[i * 2] = e.x
            pointsBuffer[i * 2 + 1] = e.y * mAnimator.phaseY
        }
        
        // 转换坐标
        trans.pointValuesToPixel(pointsBuffer)
        
        // 将曲线分段绘制，每段应用不同的渐变色
        for (i in 0 until entryCount - 1) {
            val startIndex = i * 2
            val endIndex = (i + 1) * 2
            
            val x1 = pointsBuffer[startIndex]
            val y1 = pointsBuffer[startIndex + 1]
            val x2 = pointsBuffer[endIndex]
            val y2 = pointsBuffer[endIndex + 1]
            
            // 检查点是否在视口内
            if (!mViewPortHandler.isInBoundsRight(x1)) break
            
            if (!mViewPortHandler.isInBoundsLeft(x2) || 
                !mViewPortHandler.isInBoundsTop(Math.max(y1, y2)) ||
                !mViewPortHandler.isInBoundsBottom(Math.min(y1, y2))) continue
            
            // 计算控制点
            calculateCubicControlPoints(pointsBuffer, i, entryCount)
            
            // 为每段曲线创建渐变色
            val startColor = gradientColors[i % gradientColors.size]
            val endColor = gradientColors[(i + 1) % gradientColors.size]
            
            // 清除路径
            cubicPath.reset()
            
            // 移动到起点
            cubicPath.moveTo(x1, y1)
            
            // 添加贝塞尔曲线
            cubicPath.cubicTo(
                cubicControlPoints[0], cubicControlPoints[1],
                cubicControlPoints[2], cubicControlPoints[3],
                x2, y2
            )
            
            // 创建渐变
            val shader = LinearGradient(
                x1, y1, x2, y2,
                startColor, endColor,
                Shader.TileMode.CLAMP
            )
            
            // 应用渐变
            mRenderPaint.shader = shader
            
            // 绘制曲线
            c.drawPath(cubicPath, mRenderPaint)
        }
        
        // 清除着色器
        mRenderPaint.shader = null
    }
    
    // 计算贝塞尔曲线的控制点
    private fun calculateCubicControlPoints(points: FloatArray, i: Int, count: Int) {
        // 起点和终点
        val x1 = points[i * 2]
        val y1 = points[i * 2 + 1]
        val x2 = points[(i + 1) * 2]
        val y2 = points[(i + 1) * 2 + 1]
        
        // 计算前一点和后一点（如果存在）
        val prevX = if (i > 0) points[(i - 1) * 2] else x1
        val prevY = if (i > 0) points[(i - 1) * 2 + 1] else y1
        val nextX = if (i < count - 2) points[(i + 2) * 2] else x2
        val nextY = if (i < count - 2) points[(i + 2) * 2 + 1] else y2
        
        // 计算控制点 - 使用张力为0.2的Catmull-Rom样条
        val tension = 0.2f
        
        // 第一个控制点
        cubicControlPoints[0] = x1 + (x2 - prevX) * tension
        cubicControlPoints[1] = y1 + (y2 - prevY) * tension
        
        // 第二个控制点
        cubicControlPoints[2] = x2 - (nextX - x1) * tension
        cubicControlPoints[3] = y2 - (nextY - y1) * tension
    }
    
    // 自定义方法绘制渐变线条(保留直线绘制功能)
    private fun drawGradientLines(c: Canvas, dataSet: ILineDataSet) {
        val entryCount = dataSet.entryCount
        
        // 获取转换器
        val trans = mChart.getTransformer(dataSet.axisDependency)
        
        // 设置画笔样式
        mRenderPaint.style = Paint.Style.STROKE
        mRenderPaint.strokeWidth = dataSet.lineWidth
        mRenderPaint.pathEffect = dataSet.dashPathEffect
        
        // 确保缓冲区足够大
        val requiredSize = entryCount * 2
        val pointsBuffer = if (this.pointsBuffer.size < requiredSize) {
            FloatArray(requiredSize)
        } else {
            this.pointsBuffer
        }
        
        // 填充缓冲区
        for (i in 0 until entryCount) {
            val e = dataSet.getEntryForIndex(i)
            pointsBuffer[i * 2] = e.x
            pointsBuffer[i * 2 + 1] = e.y * mAnimator.phaseY
        }
        
        // 转换坐标
        trans.pointValuesToPixel(pointsBuffer)
        
        // 绘制渐变线段
        for (i in 0 until entryCount - 1) {
            val startIndex = i * 2
            val endIndex = (i + 1) * 2
            
            val x1 = pointsBuffer[startIndex]
            val y1 = pointsBuffer[startIndex + 1]
            val x2 = pointsBuffer[endIndex]
            val y2 = pointsBuffer[endIndex + 1]
            
            // 检查点是否在视口内
            if (!mViewPortHandler.isInBoundsRight(x1)) break
            
            if (!mViewPortHandler.isInBoundsLeft(x2) || 
                !mViewPortHandler.isInBoundsTop(Math.max(y1, y2)) ||
                !mViewPortHandler.isInBoundsBottom(Math.min(y1, y2))) continue
            
            // 为每段线创建渐变色
            val startColor = gradientColors[i % gradientColors.size]
            val endColor = gradientColors[(i + 1) % gradientColors.size]
            
            // 创建渐变
            val shader = LinearGradient(
                x1, y1, x2, y2,
                startColor, endColor,
                Shader.TileMode.CLAMP
            )
            
            mRenderPaint.shader = shader
            
            // 绘制线段
            c.drawLine(x1, y1, x2, y2, mRenderPaint)
        }
        
        // 清除着色器
        mRenderPaint.shader = null
    }
    
    // 自定义方法绘制彩色圆点
    private fun drawColorfulCircles(c: Canvas, dataSet: ILineDataSet) {
        if (!dataSet.isDrawCirclesEnabled || dataSet.entryCount == 0) {
            return
        }
        
        val trans = mChart.getTransformer(dataSet.axisDependency)
        
        // 准备画笔
        mRenderPaint.style = Paint.Style.FILL
        
        val circleRadius = dataSet.circleRadius
        val circleHoleRadius = dataSet.circleHoleRadius
        val drawCircleHole = dataSet.isDrawCircleHoleEnabled
        
        // 确保缓冲区足够大
        val requiredSize = dataSet.entryCount * 2
        val pointsBuffer = if (this.pointsBuffer.size < requiredSize) {
            FloatArray(requiredSize)
        } else {
            this.pointsBuffer
        }
        
        // 填充缓冲区
        for (i in 0 until dataSet.entryCount) {
            val e = dataSet.getEntryForIndex(i)
            pointsBuffer[i * 2] = e.x
            pointsBuffer[i * 2 + 1] = e.y * mAnimator.phaseY
        }
        
        // 转换坐标
        trans.pointValuesToPixel(pointsBuffer)
        
        // 绘制彩色圆点
        for (i in 0 until dataSet.entryCount) {
            val x = pointsBuffer[i * 2]
            val y = pointsBuffer[i * 2 + 1]
            
            // 检查点是否在视口内
            if (!mViewPortHandler.isInBoundsRight(x)) break
            
            if (!mViewPortHandler.isInBoundsLeft(x) || 
                !mViewPortHandler.isInBoundsY(y)) continue
            
            // 使用渐变色数组中的颜色
            mRenderPaint.color = gradientColors[i % gradientColors.size]
            
            // 绘制圆点
            c.drawCircle(x, y, circleRadius, mRenderPaint)
            
            // 绘制圆点空心
            if (drawCircleHole) {
                mRenderPaint.color = Color.WHITE
                c.drawCircle(x, y, circleHoleRadius, mRenderPaint)
            }
        }
    }
    
    // 重写高亮绘制方法，确保点击功能正常工作
    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
        val lineData = mChart.lineData

        for (high in indices) {
            val dataSetIndex = high.dataSetIndex
            val dataSet = lineData.getDataSetByIndex(dataSetIndex)

            if (dataSet == null || !dataSet.isHighlightEnabled) {
                continue
            }

            val entry = dataSet.getEntryForXValue(high.x, high.y)
            if (!isInBoundsX(entry, dataSet)) {
                continue
            }

            val pix = mChart.getTransformer(dataSet.axisDependency)
                .getPixelForValues(entry.x, entry.y * mAnimator.phaseY)

            high.setDraw(pix.x.toFloat(), pix.y.toFloat())

            // 绘制高亮线
            drawHighlightLines(c, pix.x.toFloat(), pix.y.toFloat(), dataSet)
            
            // 绘制高亮指示器
            // 放大高亮点的圆圈
            val originalCircleRadius = dataSet.circleRadius
            val highlightCircleRadius = originalCircleRadius * 1.5f
            
            // 绘制高亮圆点
            mRenderPaint.style = Paint.Style.FILL
            mRenderPaint.color = dataSet.highLightColor
            c.drawCircle(pix.x.toFloat(), pix.y.toFloat(), highlightCircleRadius, mRenderPaint)
            
            // 绘制内部空心
            if (dataSet.isDrawCircleHoleEnabled) {
                mRenderPaint.color = Color.WHITE
                c.drawCircle(pix.x.toFloat(), pix.y.toFloat(), dataSet.circleHoleRadius * 1.5f, mRenderPaint)
            }
        }
    }
    
    // 验证x值是否在数据集的边界内
    private fun isInBoundsX(entry: com.github.mikephil.charting.data.Entry, dataSet: ILineDataSet): Boolean {
        return entry.x <= dataSet.xMax && entry.x >= dataSet.xMin
    }
} 