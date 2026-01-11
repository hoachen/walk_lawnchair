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
import com.ur.apps.walk.model.TaskModel
import com.ur.apps.walk.step.bean.ExerciseStats
import com.ur.apps.walk.step.callback.StepCountChangeCallBack
import com.ur.apps.walk.step.manager.StepManager

/**
 * @author
 * ZhaoChengQuan.Created on:2026/1/5.
 * @describe
 */

private const val TAG = "CustomFeedOverlay"

class CustomFeedOverlay(private val launcher: LawnchairLauncher) : LauncherOverlayManager,
    LauncherOverlayManager.LauncherOverlay, StepCountChangeCallBack {
    init {
        Log.d(TAG, "customFeedOverlay created ")
    }

    private var callbacks: LauncherOverlayManager.LauncherOverlayCallbacks? = null

    private var overlayView: View? = null
    private var feedAdapter: CustomFeedAdapter? = null


    private var isAttached = false
    private var currentProgress = 0f
    private var animator: ValueAnimator? = null

    private val screenWidth: Int get() = launcher.resources.displayMetrics.widthPixels


    private fun createOverlayView(): View {
        Log.d(TAG, "createOverlayView called, screenWidth=$screenWidth")

        // Custom FrameLayout that handles back key and gestures
        val overlayContainer = object : FrameLayout(launcher) {
            private var startX = 0f
            private var startY = 0f
            private var isScrolling = false
            private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop

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

            override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
                when (ev.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startX = ev.x
                        startY = ev.y
                        isScrolling = false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        if (isScrolling) return true
                        val dx = ev.x - startX
                        val dy = ev.y - startY
                        if (kotlin.math.abs(dx) > touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            // Only intercept if dragging LEFT (to close) implies dx < 0 ??
                            // Actually allow both directions to be responsive
                            isScrolling = true
                            // Prevent RecyclerView from stealing subsequent events
                            requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_MOVE -> {
                        if (isScrolling) {
                            val dx = event.x - startX
                            // progress range: 1.0 (open) to 0.0 (closed)
                            // View moves by dx. width corresponds to range 0..1
                            // If dx is -width, progress should change by -1.
                            val newProgress = (currentProgress + dx / width).coerceIn(0f, 1f)
                            updateOverlayPosition(newProgress)
                            // Reset startX to ensure smooth continuous scrolling
                            // actually simpler to just use absolute difference from initial touch
                            // But since we update progress immediately, we want delta.
                            // Let's stick to simple delta model:
                            // The problem with updating progress AND relying on touch is that
                            // if we move the view, the touch event coordinate (relative to view) MIGHT shift if the parent moves?
                            // This layout IS the moving part?
                            // Yes, this view has translationX.
                            // If we change translationX, the subsequent ACTION_MOVE event.x might be affected depending on OS.
                            // Usually event.getRawX() is safer for absolute screen movement tracking.
                        }
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        if (isScrolling) {
                            isScrolling = false
                            // Snap to nearest
                            val target = if (currentProgress > 0.5f) 1f else 0f
                            animateToProgress(target)
                        }
                    }
                }
                return true
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
                    // Start from top, handled by padding if needed
                }
            }

            // RecyclerView for feed items
            val recyclerView = RecyclerView(launcher).apply {
                id = View.generateViewId()
                layoutManager = LinearLayoutManager(launcher)
                feedAdapter = CustomFeedAdapter(launcher, getFeedItems())
                adapter = feedAdapter
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
    private var searchView: View? = null
    private var allAppsCache: List<FeedApp> = emptyList()

    private fun showSearchUI() {
        if (overlayView == null) return
        val container = overlayView as ViewGroup

        if (searchView == null) {
            val inflater = android.view.LayoutInflater.from(launcher)
            searchView = inflater.inflate(R.layout.custom_feed_full_search, container, false)

            // Setup Search Logic
            val searchInput = searchView!!.findViewById<android.widget.EditText>(R.id.search_input)!!
            val searchCancel = searchView!!.findViewById<android.widget.TextView>(R.id.search_cancel)!!
            val searchRecycler = searchView!!.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.search_results_recycler)!!

            // Adjust header padding for status bar
            val searchHeader = searchView!!.findViewById<android.widget.LinearLayout>(R.id.search_container_header)!!
            val statusBarHeight = getStatusBarHeight()
            searchHeader.setPaddingRelative(
                searchHeader.paddingStart,
                statusBarHeight + 20, // Add ~8dp extra
                searchHeader.paddingEnd,
                searchHeader.paddingBottom
            )

            searchRecycler.layoutManager = LinearLayoutManager(launcher)

            // Load all apps if not loaded
            if (allAppsCache.isEmpty()) {
                allAppsCache = getAllInstalledApps()
            }

            val searchAdapter = CustomSearchAdapter(allAppsCache)
            searchRecycler.adapter = searchAdapter

            searchCancel.setOnClickListener {
                hideSearchUI()
            }

            searchInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString()
                    val filtered = if (query.isEmpty()) {
                        allAppsCache
                    } else {
                        allAppsCache.filter { it.title.contains(query, ignoreCase = true) }
                    }
                    searchAdapter.updateList(filtered)
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

             // Create a container for the search view to handle background and touch
             // Actually searchView is the root from layout xml, usually MATCH_PARENT/MATCH_PARENT
        }

        if (searchView!!.parent == null) {
             container.addView(searchView)
             searchView!!.alpha = 0f
             searchView!!.animate().alpha(1f).setDuration(200).start()

             // Request focus and show keyboard
             val searchInput = searchView!!.findViewById<android.widget.EditText>(R.id.search_input)!!
             searchInput.requestFocus()
             val imm = launcher.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
             imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideSearchUI() {
        searchView?.let { view ->
            if (view.parent != null) {
                 // Hide keyboard
                 val searchInput = view.findViewById<android.widget.EditText>(R.id.search_input)!!
                 val imm = launcher.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                 imm.hideSoftInputFromWindow(searchInput.windowToken, 0)

                view.animate().alpha(0f).setDuration(200).withEndAction {
                    (view.parent as ViewGroup).removeView(view)
                }.start()
            }
        }
    }


    /**
     * Get ALL installed apps for search
     */
    private fun getAllInstalledApps(): List<FeedApp> {
        val launcherApps = launcher.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        val userHandle = android.os.Process.myUserHandle()
        val activities = launcherApps.getActivityList(null, userHandle)

        // Sort by label
        val sorted = activities.sortedWith(Comparator { a, b ->
            String.CASE_INSENSITIVE_ORDER.compare(a.label.toString(), b.label.toString())
        })

        return sorted.map { activityInfo ->
            val iconDrawable = activityInfo.getBadgedIcon(0)
             FeedApp(
                title = activityInfo.label.toString(),
                iconRes = 0,
                iconDrawable = iconDrawable,
                action = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                    intent.component = activityInfo.componentName
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    launcher.startActivitySafely(null, intent, null)
                }
            )
        }
    }

    // Existing getInstalledApps used for feed preview (limited count), effectively redundant now if we cache,
    // but keeping it simple. Refactor slightly to use cache if available or just getAll and take 16.
    private fun getInstalledApps(): List<FeedApp> {
        if (allAppsCache.isNotEmpty()) return allAppsCache.take(16)
        return getAllInstalledApps().take(16)
    }

    private fun getFeedItems(): List<FeedItem> {
        // Ensure cache is populated
        if (allAppsCache.isEmpty()) {
             allAppsCache = getAllInstalledApps()
        }
        val realApps = allAppsCache

        // Partition real apps for demo
        val commonApps = realApps.take(8)
        val recentApps = realApps.drop(8).take(8)

        return listOf(
            // Search Bar
            FeedItem(
                type = FeedItemType.SEARCH,
                title = "应用库",
                action = { showSearchUI() }
            ),

            // Common Apps
            FeedItem(
                type = FeedItemType.APP_GRID,
                title = "常用",
                apps = commonApps
            ),

            // Recent Apps
            FeedItem(
                type = FeedItemType.APP_GRID,
                title = "最近",
                apps = recentApps
            ),

            // Step Overview Card
            run {
                val stepManager = StepManager.getInstance(launcher)
                val currentSteps = stepManager.getTodayStats().steps
                val maxTaskStep = TaskModel.DEFAULT_TASKS.maxByOrNull { it.stepGoal }?.stepGoal ?: 8000
                val dailyGoal = maxTaskStep
                
                // Calculate derived values
                val distance = currentSteps * 0.7 / 1000.0
                val calories = currentSteps * 5 / 100
                
                val badgeText = if (currentSteps >= dailyGoal) {
                    launcher.getString(R.string.task_item_progress_label_completed)
                } else {
                    launcher.getString(R.string.step_overview_badge_in_progress)
                }

                // Format daily goal for label
                val goalFormatted = if (dailyGoal >= 1000) String.format("%,d", dailyGoal) else dailyGoal.toString()
                
                FeedItem(
                    type = FeedItemType.STEP_OVERVIEW,
                    title = launcher.getString(R.string.step_overview_title),
                    badgeText = badgeText,
                    stepCount = currentSteps,
                    dailyGoal = dailyGoal,
                    distance = distance,
                    calories = calories,
                    stepLabel = launcher.getString(R.string.step_overview_subtitle_format, goalFormatted),
                    ringLabel = launcher.getString(R.string.step_overview_ring_label_completion)
                )
            },

            // Rewards Placeholder
            FeedItem(
                type = FeedItemType.PLACEHOLDER,
                title = "领取步数奖励",
            ),

            // Time Rewards Placeholder
            FeedItem(
                type = FeedItemType.PLACEHOLDER,
                title = "时段奖励",
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


    // Inner Adapter for Search Results
    private inner class CustomSearchAdapter(private var apps: List<FeedApp>) : RecyclerView.Adapter<CustomSearchAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: android.widget.ImageView = view.findViewById<android.widget.ImageView>(R.id.app_icon)!!
            val title: android.widget.TextView = view.findViewById<android.widget.TextView>(R.id.app_title)!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.title.text = app.title
            if (app.iconDrawable != null) {
                holder.icon.setImageDrawable(app.iconDrawable)
            } else {
                holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            holder.itemView.setOnClickListener {
                app.action?.invoke()
            }
        }

        override fun getItemCount() = apps.size

        fun updateList(newApps: List<FeedApp>) {
            apps = newApps
            notifyDataSetChanged()
        }
    }

    /**
     * Handle back press when overlay is open
     * @return true if back press was consumed
     */
    fun onBackPressed(): Boolean {
        // Check search view first
        searchView?.let {
            if (it.parent != null) {
                hideSearchUI()
                return true
            }
        }

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
        // Clear cache
        allAppsCache = emptyList()
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
    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed")
        StepManager.getInstance(launcher).registerCallback(this)
        // Force update on resume
        updateFeedData()
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused")
        StepManager.getInstance(launcher).unregisterCallback(this)
    }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        // Clear cache
        allAppsCache = emptyList()
    }


    override fun onStepChange(stepCount: ExerciseStats?) {
        Log.d(TAG, "onStepChange: $stepCount")
        updateFeedData()
    }

    override fun onStepReachPeriod(hundred_level: Int) {
        // No-op
    }

    private fun updateFeedData() {
        if (feedAdapter != null) {
            launcher.runOnUiThread {
                feedAdapter?.updateData(getFeedItems())
            }
        }
    }
}
