package com.ur.apps.utils

import android.graphics.RectF
import android.view.View
import kotlin.random.Random

object ViewUtils {

    /**
     * 计算 View 在屏幕内的可见区域，并返回该区域内的一个随机点
     */
    fun getVisibleClickPoint(view: View, screenW: Int, screenH: Int): Pair<Float, Float>? {
        // 1. 获取 View 在屏幕上的绝对坐标
        val location = IntArray(2)
        view.getLocationOnScreen(location)

        val viewLeft = location[0].toFloat()
        val viewTop = location[1].toFloat()
        val viewRight = viewLeft + view.width
        val viewBottom = viewTop + view.height

        // View 的全局矩形
        val viewRect = RectF(viewLeft, viewTop, viewRight, viewBottom)

        // 屏幕的矩形
        val screenRect = RectF(0f, 0f, screenW.toFloat(), screenH.toFloat())

        // 2. 计算交集 (Intersection)
        // resultRect 将包含 View 在屏幕上实际露出的那部分区域（比如那 1px）
        val resultRect = RectF()
        val intersects = resultRect.setIntersect(viewRect, screenRect)
        URLog.i("ViewUtils", "target view $viewRect" +
                "\nscreen $screenRect" +
                "\nwith screen intersects = $resultRect")

        if (!intersects || resultRect.isEmpty) {
            return null // View 完全在屏幕外
        }

        // 3. 在交集区域内随机取点
        // 为了安全起见，不要取极边缘的点，稍微向内收缩 0.1px (如果区域足够大)
        var safeLeft = resultRect.left
        var safeRight = resultRect.right
        var safeTop = resultRect.top
        var safeBottom = resultRect.bottom

        // 只有当宽度足够时才收缩，防止 1px 宽度收缩后变成负数
        if (resultRect.width() > 2) {
            safeLeft += 0.5f
            safeRight -= 0.5f
        }
        if (resultRect.height() > 2) {
            safeTop += 0.5f
            safeBottom -= 0.5f
        }

        val x = safeLeft + (safeRight - safeLeft) * Random.nextFloat()
        val y = safeTop + (safeBottom - safeTop) * Random.nextFloat()

        return Pair(x, y)
    }
}