package app.lawnchair.overlay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.android.launcher3.databinding.CustomFeedStepCardBinding

/**
 * @author
 * ZhaoChengQuan.Created on:2026/1/5.
 * @describe
 */
class CustomFeedAdapter(private val context: Context, private var items: List<FeedItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CARD = 1
        private const val VIEW_TYPE_SHORTCUT_ROW = 2
        private const val VIEW_TYPE_DIVIDER = 3
        private const val VIEW_TYPE_SEARCH = 4
        private const val VIEW_TYPE_APP_GRID = 5
        private const val VIEW_TYPE_PLACEHOLDER = 6
        private const val VIEW_TYPE_STEP_OVERVIEW = 7
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.header_title)!!
        fun bind(item: FeedItem) {
            titleText.text = item.title
        }
    }

    class ShortcutRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.shortcut_row_title)!!
        fun bind(item: FeedItem) {
            titleText.text = item.title
        }
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.card_title)!!
        fun bind(item: FeedItem) {
            titleText.text = item.title
            item.action?.let { action ->
                itemView.setOnClickListener { action() }
            }
        }
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val searchBarText: TextView = itemView.findViewById<TextView>(R.id.search_bar_text)!!
        fun bind(item: FeedItem) {
            // Optional: bind text or click listener
             item.action?.let { action ->
                itemView.setOnClickListener { action() }
            }
        }
    }

    class AppGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById<TextView>(R.id.section_title)!!
        private val appGrid: android.widget.GridLayout = itemView.findViewById<android.widget.GridLayout>(R.id.app_grid)!!
        
        fun bind(item: FeedItem) {
            titleText.text = item.title
            appGrid.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            
            // Limit to 8 items for 2 rows x 4 columns as per design
            item.apps.take(8).forEach { app ->
                val appView = inflater.inflate(R.layout.item_feed_app, appGrid, false)
                val iconView = appView.findViewById<ImageView>(R.id.app_icon)!!
                
                if (app.iconDrawable != null) {
                    iconView.setImageDrawable(app.iconDrawable)
                } else if (app.iconRes != 0) {
                    iconView.setImageResource(app.iconRes)
                }
                
                // Add click listener
                 appView.setOnClickListener { app.action?.invoke() }
                 
                // Set layout params to share space equally
                val params = android.widget.GridLayout.LayoutParams(
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                )
                params.width = 0 // vital for weight to work
                appView.layoutParams = params

                appGrid.addView(appView)
            }
        }
    }

    class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val placeholderText: TextView = itemView.findViewById<TextView>(R.id.placeholder_text)!!
        fun bind(item: FeedItem) {
            placeholderText.text = item.title
            // Could change background color or icon based on type if needed
        }
    }

    class StepOverviewViewHolder(private val binding: CustomFeedStepCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedItem) {
            binding.tvTitle.text = item.title
            binding.tvBadgeText.text = item.badgeText
            binding.tvStepCount.text = formatStepCount(item.stepCount)
            binding.tvStepLabel.text = item.stepLabel

            // Check MainViewHolder implementation.
            // MainViewHolder:
            // binding.tvStepLabel.text = ... (formatted daily goal)
            // But MinimalFragment passed: stepLabel = getString(R.string.step_overview_step_label)
            // Wait, let's re-read MainViewHolder vs MinimalFragment interaction.

            // MinimalFragment:
            // stepLabel = getString(R.string.step_overview_step_label),
            // ringLabel = getString(R.string.step_overview_ring_label_completion)

            // MainViewHolder:
            // binding.tvStepLabel.text = itemView.context.getString(R.string.step_overview_subtitle_format,formatStepCount(item.dailyGoal))
            // It IGNORES item.stepLabel for tvStepLabel?
            // Actually MainItem.StepOverviewCardItem has: stepLabel, ringLabel.
            // MainViewHolder uses:
            // binding.tvStepLabel.text = ... (constructs string locally using R.string.step_overview_subtitle_format + item.dailyGoal)
            // So MainViewHolder IGNORES item.stepLabel passed from MinimalFragment!
            // Wait, MinimalFragment passes: subtitle = ... (R.string.step_overview_subtitle_format...)
            // And stepLabel = ... (R.string.step_overview_step_label)

            // Let's see MainViewHolder again.
            // .tvTitle.text = item.title
            // .tvBadgeText.text = item.badgeText
            // .tvStepCount.text = formatStepCount(item.stepCount)
            // .tvStepLabel.text = ... (R.string.step_overview_subtitle_format, dailyGoal)
            // .tvPercentage.text = ...

            // Wait, where is `subtitle` used?
            // MainViewHolder doesn't seem to use `subtitle` from item if it constructs it itself.
            // AND I need to be careful. MinimalFragment creates MainItem with `title`, `subtitle`, `stepLabel`, `ringLabel`.
            // MainViewHolder logic:
            // binding.tvTitle.text = item.title
            // binding.tvBadgeText.text = item.badgeText
            // binding.tvStepCount.text = ...
            // binding.tvStepLabel.text = itemView.context.getString(R.string.step_overview_subtitle_format, ...)

            // So if I want to match MainViewHolder logic, I should replicate it or pass the CORRECT string in FeedItem so I can just bind it.
            // Ideally FeedItem should contain PRE-FORMATTED strings so ViewHolder is dumb.
            // So in CustomFeedOverlay, I will format the strings (subtitle aka stepLabel) and pass them.
            // So in ViewHolder here, I should just use what is in FeedItem.

            // Let's use FeedItem fields mapping to View:
            // title -> tvTitle
            // badgeText -> tvBadgeText
            // stepCount -> tvStepCount (formatted)
            // stepLabel -> tvStepLabel
            // ringLabel -> (Where does ringLabel go in MainViewHolder? It is missing in MainViewHolder code provided!)
            // MainViewHolder lines 464-500.
            // It sets tvTitle, tvBadgeText, tvStepCount, tvStepLabel, tvPercentage.
            // It does NOT set ringLabel. Maybe the layout doesn't use it or it's static?
            // MinimalFragment passes ringLabel. Maybe MainViewHolder is incomplete or I missed something.
            // Ah, line 506 in CustomFeedOverlay. (No, looking at MainViewHolder)
            // I don't see ringLabel being used in MainViewHolder.

            // However, I see `binding.tvPercentage.text`.
            // So I need to calculate percentage.
            // FeedItem doesn't have percentage field directly, but I can calculate it or add it.
            // Better to calculate here or in Overlay.
            // Let's calculate here for simplicity or pass pre-calculated string.
            // MainViewHolder calculates it.

            // I'll stick to passing raw data in FeedItem (stepCount, dailyGoal) and calculating percentage here,
            // OR pass formatted strings.
            // Given FeedItem has `stepCount` and `dailyGoal`, I will calculate percentage here.

            binding.tvStepLabel.text = item.stepLabel // I will pass the fully formatted string here from Overlay.

            val percentage = if (item.dailyGoal > 0) {
                (item.stepCount.toFloat() / item.dailyGoal * 100).coerceAtMost(100f)
            } else {
                0f
            }
            val percentageInt = percentage.toInt()
            binding.tvPercentage.text = "$percentageInt%"
        }

        private fun formatStepCount(steps: Int): String {
            return if (steps >= 1000) {
                String.format("%,d", steps)
            } else {
                steps.toString()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.custom_feed_header, parent, false))
            VIEW_TYPE_CARD -> CardViewHolder(inflater.inflate(R.layout.custom_feed_card, parent, false))
            VIEW_TYPE_SHORTCUT_ROW -> ShortcutRowViewHolder(inflater.inflate(R.layout.custom_feed_shortcut_row, parent, false))
            VIEW_TYPE_DIVIDER -> DividerViewHolder(inflater.inflate(R.layout.custom_feed_divider, parent, false))
            VIEW_TYPE_SEARCH -> SearchViewHolder(inflater.inflate(R.layout.custom_feed_search, parent, false))
            VIEW_TYPE_APP_GRID -> AppGridViewHolder(inflater.inflate(R.layout.custom_feed_app_grid_section, parent, false))
            VIEW_TYPE_PLACEHOLDER -> PlaceholderViewHolder(inflater.inflate(R.layout.custom_feed_placeholder, parent, false))
            VIEW_TYPE_STEP_OVERVIEW -> StepOverviewViewHolder(CustomFeedStepCardBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("unknown view type")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item = items[position]
        when(holder){
            is HeaderViewHolder -> holder.bind(item)
            is CardViewHolder -> holder.bind(item)
            is ShortcutRowViewHolder -> holder.bind(item)
            is SearchViewHolder -> holder.bind(item)
            is AppGridViewHolder -> holder.bind(item)
            is PlaceholderViewHolder -> holder.bind(item)
            is StepOverviewViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            FeedItemType.HEADER -> VIEW_TYPE_HEADER
            FeedItemType.CARD -> VIEW_TYPE_CARD
            FeedItemType.SHORTCUT_ROW -> VIEW_TYPE_SHORTCUT_ROW
            FeedItemType.DIVIDER -> VIEW_TYPE_DIVIDER
            FeedItemType.SEARCH -> VIEW_TYPE_SEARCH
            FeedItemType.APP_GRID -> VIEW_TYPE_APP_GRID
            FeedItemType.PLACEHOLDER -> VIEW_TYPE_PLACEHOLDER
            FeedItemType.STEP_OVERVIEW -> VIEW_TYPE_STEP_OVERVIEW
        }
    }
    fun updateData(newItems: List<FeedItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
