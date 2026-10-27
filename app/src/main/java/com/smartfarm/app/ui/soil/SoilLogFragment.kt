package com.smartfarm.app.ui.soil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.smartfarm.app.R
import com.smartfarm.app.data.FakeFarmRepository
import com.smartfarm.app.data.FarmStandard
import com.smartfarm.app.databinding.FragmentSoilLogBinding
import com.smartfarm.app.ui.common.ActivityLogAdapter
import com.smartfarm.app.ui.common.addRefreshMenu
import com.smartfarm.app.ui.common.showStatusBadge

/**
 * 토양 수분 로그 화면 (대시보드 "토양수분" 카드에서 이동)
 *  - 현재 토양 수분 + 적정/주의 배지
 *  - 시간별 습도 그래프
 *  - 활동로그(자동급수 기록) 목록 + 건수
 *  - 상단바 ↻ 버튼으로 새로고침
 * 화면 모양은 fragment_soil_log.xml, 여기서는 데이터만 채웁니다.
 */
class SoilLogFragment : Fragment() {

    private var _binding: FragmentSoilLogBinding? = null
    private val binding get() = _binding!!

    private val repository = FakeFarmRepository()
    private val logAdapter = ActivityLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoilLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerLogs.adapter = logAdapter

        addRefreshMenu {
            loadData()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadData()
    }

    private fun loadData() {
        // ① 현재 토양 수분 + 배지
        val moisture = repository.getCurrentSoilMoisture()
        binding.tvValue.text = moisture.toString()
        binding.tvBadge.showStatusBadge(FarmStandard.isSoilNormal(moisture))

        // ② 그래프
        val history = repository.getSoilMoistureHistory()
        binding.chart.setData(history.labels, history.values)

        // ③ 활동로그 + 건수
        val logs = repository.getSoilLogs()
        logAdapter.submitList(logs)
        binding.tvEmpty.isVisible = logs.isEmpty()
        binding.tvLogCount.text = getString(R.string.soil_log_count, logs.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}
