package com.ur.apps.walk.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView

/**
 * 心跳脉冲波纹动画工具类
 * 用于创建类似水波纹扩散的心跳动画效果
 */
class HeartPulseAnimator {

    companion object {
        // 默认动画持续时间
        private const val DEFAULT_DURATION = 3500L
        
        // 默认透明度变化范围 - 更温和的透明度变化
        private const val MAX_ALPHA = 0.8f
        private const val MIN_ALPHA = 0.0f
        
        // 默认缩放范围 - 更平滑的缩放效果
        private const val MIN_SCALE = 1.0f
        private const val MAX_SCALE = 1.25f
        
        /**
         * 为单个心跳圆圈创建脉冲动画
         * @param view 要添加动画的视图
         * @param delay 动画延迟开始的时间(毫秒)
         * @param duration 动画持续时间(毫秒)
         * @param isLastCircle 是否是最外层圆圈
         * @return 配置好的AnimatorSet
         */
        private fun createPulseAnimator(
            view: View,
            delay: Long,
            duration: Long = DEFAULT_DURATION,
            isLastCircle: Boolean = false
        ): AnimatorSet {
            // 创建具有平滑过渡的缩放动画
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, MIN_SCALE, MAX_SCALE)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, MIN_SCALE, MAX_SCALE)
            
            // 改进透明度动画，使用多个关键帧创建更平滑的过渡
            // 注意透明度最后不直接降到0，而是保持一个很小的值，减少突变感
            val alpha = PropertyValuesHolder.ofFloat(
                View.ALPHA, 
                0.05f,  // 开始时几乎不可见
                MAX_ALPHA,  // 迅速淡入到最大透明度
                MAX_ALPHA * 0.9f,  // 保持较高透明度
                MAX_ALPHA * 0.7f,  // 开始缓慢变淡
                MAX_ALPHA * 0.3f,  // 继续变淡
                0.05f   // 几乎不可见，而不是完全消失
            )
            
            // 组合缩放和透明度动画
            val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha)
            animator.duration = duration
            
            // 使用更复杂的路径插值器，使得开始和结束的过渡更平滑
            animator.interpolator = PathInterpolator(0.2f, 0.0f, 0.3f, 1f)
            
            // 无限循环动画
            animator.repeatCount = ValueAnimator.INFINITE
            // 使用RESTART模式，但我们通过上面的透明度控制来实现更平滑的过渡
            animator.repeatMode = ValueAnimator.RESTART
            
            // 特殊处理：如果是最外层圆圈，添加一个额外的延迟到下一组波纹
            // 这样会在一组波纹结束后有一个短暂停顿，然后新一组波纹开始，增加节奏感
            val actualDelay = if (isLastCircle) {
                delay + 350  // 给最后一个圆圈添加额外延迟
            } else {
                delay
            }
            
            // 创建AnimatorSet并设置延迟
            val animatorSet = AnimatorSet()
            animatorSet.play(animator).after(actualDelay)
            
            return animatorSet
        }
        
        /**
         * 为多个心跳圆圈创建连续的脉冲水波纹动画
         * @param circles 要添加动画的圆圈视图列表，应该按照从内到外的顺序排列
         * @param baseDelay 每个圆圈动画之间的基础延迟(毫秒)
         * @param duration 每个动画的持续时间(毫秒)
         */
        fun createHeartPulseAnimation(
            circles: List<ImageView>,
            baseDelay: Long = 220,  // 进一步减小延迟，使波纹更连续
            duration: Long = DEFAULT_DURATION
        ) {
            // 确保视图列表不为空
            if (circles.isEmpty()) return
            
            // 准备所有圆圈的初始状态
            circles.forEach { circle ->
                // 初始化时设置一个很小的透明度，防止初始闪现
                circle.alpha = 0.05f
                circle.scaleX = MIN_SCALE
                circle.scaleY = MIN_SCALE
            }
            
            // 计算更合理的重叠时间 - 让动画更有连续性
            val overlapFactor = 0.65f  // 新波纹开始时，前一个波纹完成65%
            val effectiveDelay = (baseDelay * overlapFactor).toLong()
            
            // 为每个圆圈创建动画，并设置递增的延迟
            val animators = mutableListOf<AnimatorSet>()
            circles.forEachIndexed { index, circle ->
                val delay = index * effectiveDelay
                
                // 标记最后一个圆圈，以便特殊处理
                val isLastCircle = index == circles.size - 1
                
                val animator = createPulseAnimator(
                    circle, 
                    delay, 
                    duration, 
                    isLastCircle
                )
                animators.add(animator)
                animator.start()
            }
        }
        
        /**
         * 为心形创建更温和、更慢的心跳动画
         * 幅度更小，节奏更缓和，给人一种舒适的感觉
         * @param heartView 心形视图
         * @param duration 动画持续时间(毫秒)
         */
        fun createGentleHeartbeatAnimation(heartView: ImageView, duration: Long = 3500) {
            // 确保视图可见且重置状态
            heartView.alpha = 1.0f
            heartView.scaleX = 1.0f
            heartView.scaleY = 1.0f
            
            // 使用更小的缩放幅度 - 从1.0到1.08，比原来的1.15小很多
            val scaleUp = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f)
            val scaleUpY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f)
            
//            val scaleDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f, 1.0f)
//            val scaleDownY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f, 1.0f)

            val animator = ObjectAnimator.ofPropertyValuesHolder(heartView, scaleUp, scaleUpY)
            animator.duration = duration

            // 使用更复杂的路径插值器，使得开始和结束的过渡更平滑
            animator.interpolator = PathInterpolator(0.2f, 0.0f, 0.3f, 1f)

            // 无限循环动画
            animator.repeatCount = ValueAnimator.INFINITE
            // 使用RESTART模式，但我们通过上面的透明度控制来实现更平滑的过渡
            animator.repeatMode = ValueAnimator.RESTART

            // 创建AnimatorSet并设置延迟
            val animatorSet = AnimatorSet()
            animatorSet.play(animator)

            animatorSet.start()
        }
        
        /**
         * 停止所有动画
         * @param views 要停止动画的视图列表
         */
        fun stopAllAnimations(views: List<View>) {
            views.forEach { view ->
                view.clearAnimation()
                view.animate().cancel()
                
                // 重置视图属性
                view.alpha = 1.0f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }
    }
} 