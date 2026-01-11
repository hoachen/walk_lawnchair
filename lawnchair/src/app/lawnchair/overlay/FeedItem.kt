package app.lawnchair.overlay

import androidx.annotation.DrawableRes

/**
 * @author
 * ZhaoChengQuan.Created on:2026/1/5.
 * @describe
 */
data class FeedItem(
    val type: FeedItemType,
    val title: String = "",
    val subtitle: String = "",
    @DrawableRes val iconRes: Int = 0,
    val action: (() -> Unit)? = null,
    val apps: List<FeedApp> = emptyList(), // For APP_GRID
    // For STEP_OVERVIEW
    val stepCount: Int = 0,
    val dailyGoal: Int = 0,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val badgeText: String = "",
    val stepLabel: String = "",
    val ringLabel: String = "",
)

data class FeedApp(
    val title: String,
    @DrawableRes val iconRes: Int = 0,
    val iconDrawable: android.graphics.drawable.Drawable? = null,
    val action: (() -> Unit)? = null,
)

enum class FeedItemType {
    HEADER, CARD, SHORTCUT_ROW, DIVIDER, SEARCH, APP_GRID, PLACEHOLDER, STEP_OVERVIEW
}
