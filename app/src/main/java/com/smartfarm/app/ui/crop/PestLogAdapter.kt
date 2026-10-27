package com.smartfarm.app.ui.crop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartfarm.app.data.PestLog
import com.smartfarm.app.databinding.ItemPestLogBinding

/**
 * 병해충 로그 목록 Adapter (내 작물 확인 화면). 한 줄 모양은 item_pest_log.xml.
 */
class PestLogAdapter : RecyclerView.Adapter<PestLogAdapter.ViewHolder>() {

    private val items = mutableListOf<PestLog>()

    fun submitList(newItems: List<PestLog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPestLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemPestLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PestLog) {
            binding.tvDate.text = item.date
            binding.tvTitle.text = item.title
            binding.tvDetail.text = item.detail
        }
    }
}
