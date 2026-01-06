package app.lawnchair.overlay

import android.R.styleable.RecyclerView
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.R
import com.android.systemui.plugins.shared.LauncherOverlayManager

/**
 * @author
 * ZhaoChengQuan.Created on:2026/1/5.
 * @describe
 */

private const val TAG = "CustomFeedOverlay"

class CustomFeedOverlay(private val launcher: LawnchairLauncher) : LauncherOverlayManager,
    LauncherOverlayManager.LauncherOverlay {
    init {
        Log.d(TAG, "customFeedOverlay created ")
    }

    private var callbacks: LauncherOverlayManager.LauncherOverlayCallbacks? = null

    private var overlayView: View? = null


    private var isAttached = false
    private var currentProgress = 0f
    private var animator: ValueAnimator? = null

    private val screenWidth: Int get() = launcher.resources.displayMetrics.widthPixels


    private fun createOverlayView(): View {
        Log.d(TAG, "createOverlayView called, screenWidth=$screenWidth")

        // Custom FrameLayout that handles back key
        val overlayContainer = object : FrameLayout(launcher) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (currentProgress > 0f) {
                        Log.d(TAG, "Back key pressed, closing overlay")
                        animateToProgress(0f)
                        return true
                    }
                }
                return super.dispatchKeyEvent(event)
            }
        }

        return overlayContainer.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            // Make focusable to receive key events
            isFocusable = true
            isFocusableInTouchMode = true


            // Content container
            val contentContainer = FrameLayout(launcher).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply {
                    // Add padding for status bar
                    topMargin = getStatusBarHeight() + 200
                }
            }

            // RecyclerView for feed items
            val recyclerView = RecyclerView(launcher).apply {
                id = View.generateViewId()
                layoutManager = LinearLayoutManager(launcher)
                adapter = CustomFeedAdapter(launcher, getFeedItems())
                clipToPadding = false
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.custom_feed_padding),
                    resources.getDimensionPixelSize(R.dimen.custom_feed_padding),
                    resources.getDimensionPixelSize(R.dimen.custom_feed_padding),
                    resources.getDimensionPixelSize(R.dimen.custom_feed_padding),
                )
            }

            contentContainer.addView(recyclerView)
            addView(contentContainer)

            // Initial state: hidden to the left
            translationX = -screenWidth.toFloat()
            alpha = 0f

            Log.d(TAG, "Overlay view created with translationX=${-screenWidth.toFloat()}")
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = launcher.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            launcher.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    /**
     * Get feed items to display
     * Override this method to provide custom content
     */
    private fun getFeedItems(): List<FeedItem> {
        // TODO: Replace with actual data source (RSS, API, etc.)
        return listOf(
            FeedItem(
                type = FeedItemType.HEADER,
                title = launcher.getString(R.string.custom_feed_title),
            ),
            FeedItem(
                type = FeedItemType.SHORTCUT_ROW,
                title = launcher.getString(R.string.custom_feed_quick_access),
            ),
            FeedItem(
                type = FeedItemType.CARD,
                title = launcher.getString(R.string.custom_feed_weather),
                subtitle = "25°C - Sunny",
                iconRes = R.drawable.ic_weather_sunny,
            ),
            FeedItem(
                type = FeedItemType.CARD,
                title = launcher.getString(R.string.custom_feed_calendar),
                subtitle = launcher.getString(R.string.custom_feed_no_events),
                iconRes = R.drawable.ic_calendar,
            ),
            FeedItem(
                type = FeedItemType.DIVIDER,
            ),
            FeedItem(
                type = FeedItemType.CARD,
                title = launcher.getString(R.string.custom_feed_news),
                subtitle = launcher.getString(R.string.custom_feed_news_placeholder),
            ),
        )
    }

    private fun attachOverlay() {
        Log.d(TAG, "attachOverlay called, isAttached=$isAttached")
        if (isAttached) return

        overlayView = createOverlayView()
        overlayView?.let { view ->
            // Add to LauncherRootView (parent of DragLayer) so it doesn't move with dragLayer
            val rootView = launcher.dragLayer.parent as? ViewGroup
            if (rootView != null) {
                Log.d(TAG, "Adding overlay view to rootView, childCount=${rootView.childCount}")
                rootView.addView(view, 0) // Add at bottom, behind dragLayer
                isAttached = true
                Log.d(TAG, "Overlay attached successfully to rootView")
            } else {
                Log.e(TAG, "Failed to get rootView, falling back to dragLayer")
                launcher.dragLayer.addView(view, 0)
                isAttached = true
            }
        }
    }

    private fun detachOverlay() {
        if (!isAttached) return

        overlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        overlayView = null
        isAttached = false
    }

    private fun updateOverlayPosition(progress: Float) {
        currentProgress = progress
        overlayView?.let { view ->
            // Move overlay based on progress
            val newTranslationX = -screenWidth * (1 - progress)
            view.translationX = newTranslationX
            // Fade in effect
            view.alpha = progress.coerceIn(0f, 1f)
            Log.d(
                TAG,
                "updateOverlayPosition: progress=$progress, translationX=$newTranslationX, alpha=${view.alpha}, visibility=${view.visibility}",
            )
        } ?: Log.d(TAG, "updateOverlayPosition: overlayView is null!")
        // Notify launcher to offset workspace
        callbacks?.onOverlayScrollChanged(progress)
    }

    private fun animateToProgress(targetProgress: Float, duration: Long = 300L) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            this.duration = duration
            addUpdateListener { animation ->
                updateOverlayPosition(animation.animatedValue as Float)
            }
            doOnEnd {
                if (targetProgress == 0f) {
                    // Clear focus when hidden
                    overlayView?.clearFocus()
                } else if (targetProgress == 1f) {
                    // Request focus when fully open to receive back key
                    overlayView?.requestFocus()
                }
            }
            start()
        }
    }

    /**
     * Handle back press when overlay is open
     * @return true if back press was consumed
     */
    fun onBackPressed(): Boolean {
        if (currentProgress > 0f) {
            Log.d(TAG, "onBackPressed: closing overlay")
            animateToProgress(0f)
            return true
        }
        return false
    }

    /**
     * Check if overlay is currently visible
     */
    fun isOverlayOpen(): Boolean = currentProgress > 0f

// ========== LauncherOverlay Interface ==========

    override fun onScrollInteractionBegin() {
        Log.d(TAG, "onScrollInteractionBegin called, isAttached=$isAttached")
        animator?.cancel()
        if (!isAttached) {
            attachOverlay()
        }
    }

    override fun onScrollInteractionEnd() {
        Log.d(TAG, "onScrollInteractionEnd called, currentProgress=$currentProgress")
        // Snap to fully open or closed based on progress
        val targetProgress = if (currentProgress > 0.5f) 1f else 0f
        animateToProgress(targetProgress)
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        Log.d(TAG, "onScrollChange called, progress=$progress, rtl=$rtl")
        updateOverlayPosition(progress)
    }

    override fun setOverlayCallbacks(callbacks: LauncherOverlayManager.LauncherOverlayCallbacks?) {
        Log.d(TAG, "setOverlayCallbacks called, callbacks=$callbacks")
        this.callbacks = callbacks
    }

// ========== LauncherOverlayManager Interface ==========

    override fun onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow called")
        // Register this overlay with the launcher
        launcher.setLauncherOverlay(this)
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow called")
        launcher.setLauncherOverlay(null)
        detachOverlay()
    }

    override fun openOverlay() {
        Log.d(TAG, "openOverlay called")
        if (!isAttached) {
            attachOverlay()
        }
        animateToProgress(1f)
    }

    override fun hideOverlay(animate: Boolean) {
        if (animate) {
            animateToProgress(0f)
        } else {
            updateOverlayPosition(0f)
        }
    }

    override fun hideOverlay(duration: Int) {
        animateToProgress(0f, duration.toLong())
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}


}
