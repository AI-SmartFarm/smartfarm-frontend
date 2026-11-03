package com.smartfarm.app.ui.temperature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartfarm.app.R
// import com.smartfarm.app.data.FakeFarmRepository
import com.smartfarm.app.data.RealFarmRepository
import com.smartfarm.app.data.network.ApiClient
import com.smartfarm.app.data.FarmStandard
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
        //load data 호출 확인
        android.util.Log.d("SmartFarmAPI", ">>> loadData() 시작됨!")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ② 백엔드 API에서 데이터 비동기 호출 (suspend 함수)
                val history = repository.getTemperatureHistory(farmId = "farm1", range = "24h")

                // ③ 그래프 데이터 반영
                if (history.values.isNotEmpty()) {
                    binding.chart.setData(history.labels, history.values)

                    // 가장 최근 온도를 현재 온도로 표시 (선택 사항)
                    val currentTemp = history.values.last().toDouble()
                    binding.tvValue.text = String.format(Locale.US, "%.1f", currentTemp)
                    binding.tvBadge.showStatusBadge(FarmStandard.isTemperatureNormal(currentTemp))
                }

                // ④ 활동 로그는 아직 API가 없다면 임시로 빈 리스트 처리 또는 기존 로직 유지
                // val logs = repository.getTemperatureLogs()
                // logAdapter.submitList(logs)
                // binding.tvEmpty.isVisible = logs.isEmpty()

            } catch (e: Exception) {
                // 네트워크 오류 처리 (필요시 토스트 메시지)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}
