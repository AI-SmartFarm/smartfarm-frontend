package com.smartfarm.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartfarm.app.R
import com.smartfarm.app.data.ActivityLog
import com.smartfarm.app.data.ActivityType
import com.smartfarm.app.databinding.ItemActivityLogBinding

/**
 * 활동로그 목록을 RecyclerView에 그려주는 Adapter (온도 로그, 토양 수분 로그에서 같이 사용).
 * 한 줄 모양은 item_activity_log.xml.
 * 사용법: adapter.submitList(로그목록)
 */
class ActivityLogAdapter : RecyclerView.Adapter<ActivityLogAdapter.ViewHolder>() {

    private val items = mutableListOf<ActivityLog>()

    fun submitList(newItems: List<ActivityLog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemActivityLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemActivityLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActivityLog) {
            binding.tvTime.text = item.time
            binding.tvMessage.text = item.message
            binding.ivType.setImageResource(
                when (item.type) {
                    ActivityType.WATERING -> R.drawable.ic_water_drop
                    ActivityType.WARNING -> R.drawable.ic_warning
                    ActivityType.FAN -> R.drawable.ic_air
                }
            )
        }
    }
}
