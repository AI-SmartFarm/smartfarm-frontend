package com.smartfarm.app.ui.temperature

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
import com.smartfarm.app.databinding.FragmentTemperatureLogBinding
import com.smartfarm.app.ui.common.ActivityLogAdapter
import com.smartfarm.app.ui.common.addRefreshMenu
import com.smartfarm.app.ui.common.showStatusBadge
import java.util.Locale

/**
 * 온도 로그 화면 (대시보드 "현재온도" 카드에서 이동)
 *  - 현재 온도 + 적정/주의 배지
 *  - 시간별 온도 그래프
 *  - 활동로그(자동 제어 내역) 목록
 *  - 상단바 ↻ 버튼으로 새로고침
 * 화면 모양은 fragment_temperature_log.xml, 여기서는 데이터만 채웁니다.
 */
class TemperatureLogFragment : Fragment() {

    private var _binding: FragmentTemperatureLogBinding? = null
    private val binding get() = _binding!!

    private val repository = FakeFarmRepository()
    private val logAdapter = ActivityLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemperatureLogBinding.inflate(inflater, container, false)
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

    /** 데이터를 불러와서 화면에 표시 (처음 들어올 때 + 새로고침 때) */
    private fun loadData() {
        // ① 현재 온도 + 배지
        val temperature = repository.getCurrentTemperature()
        binding.tvValue.text = String.format(Locale.US, "%.1f", temperature)
        binding.tvBadge.showStatusBadge(FarmStandard.isTemperatureNormal(temperature))

        // ② 그래프
        val history = repository.getTemperatureHistory()
        binding.chart.setData(history.labels, history.values)

        // ③ 활동로그
        val logs = repository.getTemperatureLogs()
        logAdapter.submitList(logs)
        binding.tvEmpty.isVisible = logs.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}
