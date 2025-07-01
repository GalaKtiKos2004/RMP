package ru.fefu.activitiesfefu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.view.isVisible
import ru.fefu.activitiesfefu.data.ActivityType
import android.util.Log

class ActivityListAdapter(
    private val onActivityClick: (ActivityListItem.Activity) -> Unit
) : ListAdapter<ActivityListItem, RecyclerView.ViewHolder>(
    ActivityListItemDiffCallback()
) {

    companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_ACTIVITY = 1
        private const val TAG = "ActivityListAdapter"
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ActivityListItem.Section -> VIEW_TYPE_SECTION
        is ActivityListItem.Activity -> VIEW_TYPE_ACTIVITY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section, parent, false)
                SectionViewHolder(view)
            }
            VIEW_TYPE_ACTIVITY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_activity, parent, false)
                ActivityViewHolder(view, onActivityClick)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ActivityListItem.Section -> (holder as SectionViewHolder).bind(item)
            is ActivityListItem.Activity -> (holder as ActivityViewHolder).bind(item)
        }
    }

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.section_title)
        fun bind(item: ActivityListItem.Section) {
            title.text = item.title
        }
    }

    class ActivityViewHolder(itemView: View, val onClick: (ActivityListItem.Activity) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val distance: TextView = itemView.findViewById(R.id.activity_distance)
        private val duration: TextView = itemView.findViewById(R.id.activity_time)
        private val type: TextView = itemView.findViewById(R.id.activity_type)
        private val timeAgo: TextView = itemView.findViewById(R.id.activity_time_ago)
        private val username: TextView = itemView.findViewById(R.id.activity_username)

        private var currentItem: ActivityListItem.Activity? = null

        init {
            itemView.setOnClickListener {
                try {
                    currentItem?.let { 
                        Log.d(TAG, "Item clicked: ${it.id}")
                        onClick(it) 
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling click", e)
                }
            }
        }

        fun bind(item: ActivityListItem.Activity) {
            try {
                currentItem = item
                distance.text = item.distance
                duration.text = item.duration
                
                // Отображаем только название типа активности без эмодзи
                val activityTypeName = when (item.type) {
                    ActivityType.BIKE -> "Велосипед"
                    ActivityType.RUN -> "Бег"
                    ActivityType.STEP -> "Шаг"
                }
                type.text = activityTypeName
                
                timeAgo.text = item.timeAgo
                
                // Отображаем имя пользователя, если оно есть
                item.username?.let {
                    username.text = "@$it"
                    username.isVisible = true
                } ?: run {
                    username.isVisible = false
                }
                
                Log.d(TAG, "Bound item: ${item.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error binding item", e)
            }
        }
    }

    class ActivityListItemDiffCallback : DiffUtil.ItemCallback<ActivityListItem>() {
        override fun areItemsTheSame(oldItem: ActivityListItem, newItem: ActivityListItem): Boolean {
            return if (oldItem is ActivityListItem.Activity && newItem is ActivityListItem.Activity) {
                oldItem.id == newItem.id
            } else oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ActivityListItem, newItem: ActivityListItem): Boolean {
            return oldItem == newItem
        }
    }
} 