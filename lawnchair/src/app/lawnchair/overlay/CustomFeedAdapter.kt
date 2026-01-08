package app.lawnchair.overlay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R

/**
 * @author
 * ZhaoChengQuan.Created on:2026/1/5.
 * @describe
 */
class CustomFeedAdapter(private val context: Context, private val items: List<FeedItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CARD = 1
        private const val VIEW_TYPE_SHORTCUT_ROW = 2
        private const val VIEW_TYPE_DIVIDER = 3
        private const val VIEW_TYPE_SEARCH = 4
        private const val VIEW_TYPE_APP_GRID = 5
        private const val VIEW_TYPE_PLACEHOLDER = 6
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
        }
    }
}
