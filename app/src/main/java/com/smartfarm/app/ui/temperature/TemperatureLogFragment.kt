package com.smartfarm.app.ui.temperature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartfarm.app.R
import com.smartfarm.app.data.FarmSettings
import com.smartfarm.app.data.RealFarmRepository
import com.smartfarm.app.data.network.ApiClient
import com.smartfarm.app.databinding.FragmentTemperatureLogBinding
import com.smartfarm.app.ui.common.ActivityLogAdapter
import com.smartfarm.app.ui.common.addRefreshMenu
import com.smartfarm.app.ui.common.showStatusBadge
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

    private val repository = RealFarmRepository(ApiClient.farmApiService) // 기존 Fake대신 Real 레포지토리 연결
    private val logAdapter = ActivityLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemperatureLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FarmSettings.init(requireContext())   // 저장해 둔 작물 선택 불러오기 (반드시 맨 앞)
        binding.recyclerLogs.adapter = logAdapter

        // 서버와 상관없는 부분(작물 기준 범위)은 바로 표시하고, 숫자는 받아오기 전까지 "-"
        showCropStandard()
        showLoadingPlaceholder()

        addRefreshMenu {
            loadData()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadData()
    }

    /** 대시보드에서 고른 작물의 적정 범위 → 범위 글자, 범례, 그래프 초록 띠 (서버 응답 기다리지 않음) */
    private fun showCropStandard() {
        val standard = FarmSettings.currentStandard
        binding.tvRange.text = HtmlCompat.fromHtml(
            getString(R.string.temp_range_desc_fmt, standard.tempMin, standard.tempMax),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.tvLegend.text =
            getString(R.string.temp_chart_legend_fmt, standard.tempMin, standard.tempMax)
        binding.chart.setNormalRange(standard.tempMin.toFloat(), standard.tempMax.toFloat())
    }

    /** XML 의 미리보기용 더미 값(24.5 등)을 지우고 "-" 로 비워둠 */
    private fun showLoadingPlaceholder() {
        binding.tvValue.text = getString(R.string.value_empty)
        binding.tvBadge.isVisible = false
    }

    /** 서버에서 데이터를 받아와 화면에 표시 (처음 들어올 때 + 새로고침 때) */
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) API 4개를 동시에 요청 (하나씩 기다리지 않음). 실패하면 null / 빈 목록
            val cropsJob = async { repository.refreshCropStandards() }          // 작물별 적정 범위
            val temperatureJob = async { repository.getCurrentTemperature() }   // API-002-A
            val historyJob = async { repository.getTemperatureHistory() }       // API-002-B
            val logsJob = async { repository.getTemperatureLogs() }             // API-007-A (TEMP)

            // 상태 API(현재값)도 기다림: 농장 작물의 서버 기준값이 여기에 같이 옴
            val temperature = temperatureJob.await()

            // 서버에서 작물 기준을 받았으면 범위 글자·범례·초록 띠를 서버 값으로 다시 그림
            cropsJob.await()
            showCropStandard()   // 서버 기준값(GET /crops 또는 상태 API)이 반영된 범위로 다시 그림
            val standard = FarmSettings.currentStandard

            // 2) ① 현재 온도 + 배지 (값이 없으면 "-", 배지 숨김)
            if (temperature != null) {
                binding.tvValue.text = String.format(Locale.US, "%.1f", temperature)
                binding.tvBadge.showStatusBadge(standard.isTemperatureNormal(temperature))
                binding.tvBadge.isVisible = true
            } else {
                showLoadingPlaceholder()
            }

            // ② 그래프
            val history = historyJob.await()
            binding.chart.setData(history.labels, history.values)

            // ③ 활동로그
            val logs = logsJob.await()
            logAdapter.submitList(logs)
            binding.tvEmpty.isVisible = logs.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}
