package com.smartfarm.app.ui.crop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartfarm.app.data.dto.DiagnosisDto
import com.smartfarm.app.databinding.ItemPestLogBinding

/**
 * 병해충 로그 목록 Adapter (내 작물 확인 화면).
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
            // 날짜/시각 표시 (ISO-8601 문자열 원본 또는 필요 시 가공)
            binding.tvDate.text = item.diagnosedAt.take(19).replace("T", " ")

            // 병명 및 중증도 표시
            val title = if (item.infected) {
                "${item.diseaseName ?: "병해"} (${item.severityLevel ?: "경고"})"
            } else {
                "정상 (병해충 미탐지)"
            }
            binding.tvTitle.text = title

            // 세부 안내문구 또는 자동 조치 내역
            val detail = when {
                !item.guide.isNullOrBlank() -> item.guide
                item.responses.isNotEmpty() -> {
                    val res = item.responses.first()
                    "${res.actuator} ${res.action} (${res.durationMinutes ?: 0}분 가동)"
                }
                else -> "이상 없음"
            }
            binding.tvDetail.text = detail
        }
    }
}