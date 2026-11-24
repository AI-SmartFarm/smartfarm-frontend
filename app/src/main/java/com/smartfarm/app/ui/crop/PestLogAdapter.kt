package com.smartfarm.app.ui.crop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartfarm.app.R
import com.smartfarm.app.data.actuatorName
import com.smartfarm.app.data.dto.DiagnosisDto
import com.smartfarm.app.databinding.ItemPestLogBinding

/**
 * 병해충 로그 목록 Adapter (내 작물 확인 화면). 한 줄 모양은 item_pest_log.xml.
 */
class PestLogAdapter : RecyclerView.Adapter<PestLogAdapter.ViewHolder>() {

    private val items = mutableListOf<DiagnosisDto>()

    fun submitList(newItems: List<DiagnosisDto>) {
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

        fun bind(item: DiagnosisDto) {
            val context = binding.root.context

            // "2026-11-20T10:10:44+09:00" → "2026-11-20 10:10:44"
            binding.tvDate.text = item.diagnosedAt.take(19).replace("T", " ")

            // 제목: 감염이면 "병명 (중증도)", 아니면 미탐지
            binding.tvTitle.text = if (item.infected) {
                context.getString(
                    R.string.pest_log_infected_fmt,
                    item.diseaseName ?: context.getString(R.string.pest_log_unknown_disease),
                    item.severityLevel ?: context.getString(R.string.status_warning)
                )
            } else {
                context.getString(R.string.pest_log_not_detected)
            }

            // 설명: 방제 안내 문구 → 없으면 자동 조치 내역 → 둘 다 없으면 "이상 없음"
            val response = item.responses.orEmpty().firstOrNull()
            binding.tvDetail.text = when {
                !item.guide.isNullOrBlank() -> item.guide
                response != null -> context.getString(
                    R.string.pest_log_action_fmt,
                    actuatorName(response.actuator),
                    response.durationMinutes ?: 0
                )
                else -> context.getString(R.string.pest_log_no_issue)
            }
        }
    }
}
