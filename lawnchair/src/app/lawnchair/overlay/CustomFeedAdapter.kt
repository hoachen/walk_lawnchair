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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(
                    R.layout.custom_feed_header, parent,
                    false,
                ),

                )
            VIEW_TYPE_CARD-> CardViewHolder(inflater.inflate(
                R.layout.custom_feed_card, parent,
                false,
            ))

            VIEW_TYPE_SHORTCUT_ROW-> ShortcutRowViewHolder(inflater.inflate(
                R.layout.custom_feed_shortcut_row, parent,
                false,
            ))

            VIEW_TYPE_DIVIDER-> DividerViewHolder(inflater.inflate(
                R.layout.custom_feed_divider, parent,
                false,
            ))

            else -> throw IllegalArgumentException("unkonw view type ")

        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        var item = items.get(position)
        when(holder){
            is HeaderViewHolder -> holder.bind(item)
            is CardViewHolder -> holder.bind(item)
            is ShortcutRowViewHolder -> holder.bind(item)
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
        }
    }

}
