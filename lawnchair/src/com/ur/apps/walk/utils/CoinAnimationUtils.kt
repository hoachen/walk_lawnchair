package com.ur.apps.walk.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.android.launcher3.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 金币按钮动画工具类
 * 用于创建吸引用户点击的金币按钮动画效果
 */
class CoinAnimationUtils {

    companion object {
        // 跟踪动画是否正在运行
        private val isAnimating = AtomicBoolean(false)
        private var currentAnimatorSet: AnimatorSet? = null

        /**
         * 显示金币按钮并执行淡入动画
         *
         * @param coinView 金币按钮视图
         */
        fun showWithFadeIn(coinView: View) {
            // 先将金币按钮设置为不可见，然后使用动画淡入
            coinView.alpha = 0f
            coinView.visibility = View.VISIBLE
            coinView.animate()
                .alpha(1f)
                .setDuration(500)
                .withEndAction {
                    // 淡入后启动吸引注意力动画
                    val coinAnimatorSet = createCoinAttentionAnimation(coinView)
                    coinAnimatorSet.start()
                }
                .start()
        }

        /**
         * 隐藏金币按钮并执行淡出动画
         *
         * @param coinView 金币按钮视图
         * @param onAnimationEnd 动画结束后的回调
         */
        fun hideWithFadeOut(coinView: View, onAnimationEnd: () -> Unit = {}) {
            // 停止当前运行的动画
            stopAnimation()

            // 执行淡出动画
            coinView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    coinView.visibility = View.GONE
                    onAnimationEnd.invoke()
                }
                .start()
        }

        /**
         * 在应用恢复前台时重启动画
         *
         * @param coinView 金币按钮视图
         */
        fun restartAnimation(coinView: View) {
            // 确保视图可见
            if (coinView.visibility != View.VISIBLE) {
                return
            }

            // 停止任何可能正在运行的动画
            stopAnimation()

            // 重置视图属性，确保从干净状态开始
            coinView.alpha = 1.0f
            coinView.scaleX = 1.0f
            coinView.scaleY = 1.0f
            coinView.translationY = 0f

            // 重新启动注意力动画
            val coinAnimatorSet = createCoinAttentionAnimation(coinView)
            coinAnimatorSet.start()
        }

        /**
         * 为金币按钮创建吸引注意力的组合动画
         * 包括缩放、旋转和亮度变化的组合效果
         *
         * @param coinView 要添加动画的金币视图
         * @return 配置好的AnimatorSet
         */
        fun createCoinAttentionAnimation(coinView: View): AnimatorSet {
            // 如果已经有动画在运行，先取消
            stopAnimation()

            // 创建一个动画集合
            val animatorSet = AnimatorSet()

            // 1. 创建跳动效果动画（轻微的上下移动）
            val bounceUp = ObjectAnimator.ofFloat(coinView, View.TRANSLATION_Y, 0f, -20f)
            bounceUp.duration = 500
            bounceUp.interpolator = AccelerateDecelerateInterpolator()

            val bounceDown = ObjectAnimator.ofFloat(coinView, View.TRANSLATION_Y, -20f, 0f)
            bounceDown.duration = 500
            bounceDown.interpolator = AccelerateDecelerateInterpolator()

            val bounceSequence = AnimatorSet()
            bounceSequence.playSequentially(bounceUp, bounceDown)

            // 2. 创建呼吸效果（缩放动画）
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f, 1.0f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f, 1.0f)
            val breatheAnimator = ObjectAnimator.ofPropertyValuesHolder(coinView, scaleX, scaleY)
            breatheAnimator.duration = 1500
            breatheAnimator.repeatCount = ValueAnimator.INFINITE
            breatheAnimator.repeatMode = ValueAnimator.RESTART
            breatheAnimator.interpolator = AccelerateDecelerateInterpolator()

            // 4. 组合动画
            animatorSet.play(breatheAnimator)
            animatorSet.play(bounceSequence).after(500).before(breatheAnimator)

            // 保存当前动画引用
            currentAnimatorSet = animatorSet
            isAnimating.set(true)

            return animatorSet
        }

        /**
         * 停止当前正在运行的动画
         */
        fun stopAnimation() {
            if (isAnimating.getAndSet(false)) {
                currentAnimatorSet?.cancel()
                currentAnimatorSet = null
            }
        }
    }
}
