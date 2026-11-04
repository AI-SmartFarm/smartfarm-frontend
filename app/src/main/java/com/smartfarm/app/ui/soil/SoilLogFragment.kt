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
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 토양 수분 로그 화면 (대시보드 "토양수분" 카드에서 이동)
 * - 현재 토양 수분 + 적정/주의 배지 (기준 = 대시보드에서 고른 작물의 적정 범위)
 * - 시간별 습도 그래프
 * - 활동로그(자동급수 기록) 목록 + 건수
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
        binding.recyclerLogs.adapter = logAdapter

        addRefreshMenu {
            loadData()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadData()
    }

    private fun loadData() {
        android.util.Log.d("SmartFarmAPI", ">>> Soil loadData() 시작됨!")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 대시보드에서 고른 작물의 적정 범위
                val standard = FarmSettings.currentStandard

                // ① 백엔드 API에서 시간별 토양 수분 데이터 호출
                val history = repository.getSoilMoistureHistory(farmId = "farm1", range = "24h")

                // 가장 최근 수분 값을 현재 수분으로 활용 및 배지 반영
                if (history.values.isNotEmpty()) {
                    val moisture = history.values.last().toInt()
                    binding.tvValue.text = moisture.toString()
                    binding.tvBadge.showStatusBadge(standard.isSoilNormal(moisture))
                }

                // 적정 범위 안내 텍스트
                binding.tvRange.text = HtmlCompat.fromHtml(
                    getString(R.string.soil_range_desc_fmt, standard.soilMin, standard.soilMax),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                // ② 그래프 + 적정 범위 띠
                binding.chart.setNormalRange(standard.soilMin.toFloat(), standard.soilMax.toFloat())
                binding.chart.setData(history.labels, history.values)

                // ③ 활동로그 + 건수 (급수 제어 API 연동 전까지 빈 리스트 처리)
                val logs = emptyList<com.smartfarm.app.data.ActivityLog>()
                logAdapter.submitList(logs)
                binding.tvEmpty.isVisible = logs.isEmpty()
                binding.tvLogCount.text = getString(R.string.soil_log_count, logs.size)

            } catch (e: Exception) {
                android.util.Log.e("SmartFarmAPI", "토양 수분 데이터 로드 실패: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}