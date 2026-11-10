package com.smartfarm.app.ui.soil

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
import com.smartfarm.app.databinding.FragmentSoilLogBinding
import com.smartfarm.app.ui.common.ActivityLogAdapter
import com.smartfarm.app.ui.common.addRefreshMenu
import com.smartfarm.app.ui.common.showStatusBadge
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * 토양 수분 로그 화면 (대시보드 "토양수분" 카드에서 이동)
 * - 현재 토양 수분 + 적정/주의 배지 (기준 = 대시보드에서 고른 작물의 적정 범위)
 * - 시간별 습도 그래프
 * - 활동로그(자동급수 기록) 목록
 * - 상단바 ↻ 버튼으로 새로고침
 * 화면 모양은 fragment_soil_log.xml, 여기서는 데이터만 채웁니다.
 */
class SoilLogFragment : Fragment() {

    private var _binding: FragmentSoilLogBinding? = null
    private val binding get() = _binding!!

    private val repository = RealFarmRepository(ApiClient.farmApiService)
    private val logAdapter = ActivityLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoilLogBinding.inflate(inflater, container, false)
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

    /** 대시보드에서 고른 작물의 적정 범위 → 범위 글자, 그래프 초록 띠 (서버 응답 기다리지 않음) */
    private fun showCropStandard() {
        val standard = FarmSettings.currentStandard
        binding.tvRange.text = HtmlCompat.fromHtml(
            getString(R.string.soil_range_desc_fmt, standard.soilMin, standard.soilMax),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.chart.setNormalRange(standard.soilMin.toFloat(), standard.soilMax.toFloat())
    }

    /** XML 의 미리보기용 더미 값(32 등)을 지우고 "-" 로 비워둠 */
    private fun showLoadingPlaceholder() {
        binding.tvValue.text = getString(R.string.value_empty)
        binding.tvBadge.isVisible = false
    }

    /** 서버에서 데이터를 받아와 화면에 표시 (처음 들어올 때 + 새로고침 때) */
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) API 4개를 동시에 요청 (하나씩 기다리지 않음). 실패하면 null / 빈 목록
            val cropsJob = async { repository.refreshCropStandards() }          // 작물별 적정 범위
            val moistureJob = async { repository.getCurrentSoilMoisture() }     // API-002-A
            val historyJob = async { repository.getSoilMoistureHistory() }      // API-002-B
            val logsJob = async { repository.getSoilLogs() }                    // API-007-A (SOIL)

            // 상태 API(현재값)도 기다림: 농장 작물의 서버 기준값이 여기에 같이 옴
            val moisture = moistureJob.await()

            // 서버에서 작물 기준을 받았으면 범위 글자·초록 띠를 서버 값으로 다시 그림
            cropsJob.await()
            showCropStandard()   // 서버 기준값(GET /crops 또는 상태 API)이 반영된 범위로 다시 그림
            val standard = FarmSettings.currentStandard

            // 2) ① 현재 토양 수분 + 배지 (값이 없으면 "-", 배지 숨김)
            if (moisture != null) {
                binding.tvValue.text = moisture.toString()
                binding.tvBadge.showStatusBadge(standard.isSoilNormal(moisture))
                binding.tvBadge.isVisible = true
            } else {
                binding.tvValue.text = getString(R.string.value_empty)
                binding.tvBadge.isVisible = false
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