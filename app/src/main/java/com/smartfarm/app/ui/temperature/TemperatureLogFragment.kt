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
// import com.smartfarm.app.data.FakeFarmRepository
import com.smartfarm.app.data.FarmSettings
import com.smartfarm.app.data.RealFarmRepository
import com.smartfarm.app.data.network.ApiClient
import com.smartfarm.app.databinding.FragmentTemperatureLogBinding
import com.smartfarm.app.ui.common.ActivityLogAdapter
import com.smartfarm.app.ui.common.addRefreshMenu
import com.smartfarm.app.ui.common.showStatusBadge
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
        binding.recyclerLogs.adapter = logAdapter

        addRefreshMenu {
            loadData()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadData()
    }

    /** 데이터를 불러와서 화면에 표시 (처음 들어올 때 + 새로고침 때) */
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val standard = FarmSettings.currentStandard

                val history = repository.getTemperatureHistory(farmId = "farm1", range = "24h")


                // ① 현재 온도 + 배지
                // realfarmrepository에 getCurrentTemperature() 함수 구현되지 않음
                // val temperature = repository.getCurrentTemperature()

                // history에서 가져온 데이터로 현재 온도 표시
                if (history.values.isNotEmpty()) {
                    // history의 가장 최근 온도 사용
                    val temperature = history.values.last().toDouble()

                    // 현재 온도 및 배지 설정
                    binding.tvValue.text = String.format(Locale.US, "%.1f", temperature)
                    binding.tvBadge.showStatusBadge(standard.isTemperatureNormal(temperature))
                }

                // 적정 범위 안내 텍스트 설정 (history 데이터 유무와 상관없이 표시)
                binding.tvRange.text = HtmlCompat.fromHtml(
                    getString(R.string.temp_range_desc_fmt, standard.tempMin, standard.tempMax),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                // ② 그래프 + 적정 범위 띠
                binding.chart.setNormalRange(standard.tempMin.toFloat(), standard.tempMax.toFloat())
                binding.chart.setData(history.labels, history.values)
                binding.tvLegend.text =
                    getString(R.string.temp_chart_legend_fmt, standard.tempMin, standard.tempMax)

                // ③ 활동로그 (제어 명령 API 연동 전까지 빈 리스트 처리)
                val logs = emptyList<com.smartfarm.app.data.ActivityLog>()
                logAdapter.submitList(logs)
                binding.tvEmpty.isVisible = logs.isEmpty()

            } catch (e: Exception) {
                android.util.Log.e("SmartFarmAPI", "데이터 로드 실패: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}
