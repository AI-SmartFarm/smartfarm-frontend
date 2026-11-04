package com.smartfarm.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartfarm.app.R
import com.smartfarm.app.data.CropStandards
import com.smartfarm.app.data.FarmSettings
import com.smartfarm.app.data.RealFarmRepository
import com.smartfarm.app.data.network.ApiClient
import com.smartfarm.app.databinding.FragmentHomeBinding
import com.smartfarm.app.ui.common.addRefreshMenu
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 대시보드 (My Farm)
 *  1. 카드 클릭 → 화면 이동
 *  2. 현재 온도·토양 수분 (서버 API-002-A) + 상단바 ↻ 새로고침
 *  3. 작물 선택 드롭다운 → 고르면 카드의 적정 범위 글자가 그 작물 기준으로 바뀜
 * 화면 모양은 전부 fragment_home.xml 에 있고, 여기서는 "동작"만 연결합니다.
 */
class HomeFragment : Fragment() {

    // ViewBinding 기본 패턴: 화면이 사라질 때 null로 만들어 메모리 누수 방지
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val repository = RealFarmRepository(ApiClient.farmApiService)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FarmSettings.init(requireContext())   // 저장해 둔 작물 선택 불러오기 (반드시 맨 앞)
        setupCardClicks()
        setupCropDropdown()
        showCropRanges()
        showSensorPlaceholder()

        // 상단바 오른쪽 ↻ 새로고침 (온도·토양 화면과 같은 방식)
        addRefreshMenu {
            loadSensor()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadSensor()
    }

    /** XML 의 미리보기용 더미 값(24°C, 32%)을 지우고 "-" 로 비워둠 */
    private fun showSensorPlaceholder() {
        binding.tvTemperatureValue.text = getString(R.string.value_empty)
        binding.progressTemperature.progress = 0
        binding.tvSoilValue.text = getString(R.string.value_empty)
        binding.progressSoil.progress = 0
    }

    /**
     * 현재 온도·토양 수분을 서버(API-002-A)에서 받아 카드에 표시.
     * 대시보드에 돌아올 때마다 다시 불러옵니다. 실패하면 "-" 표시.
     */
    private fun loadSensor() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sensor = repository.getCurrentSensor()
            val temperature = sensor?.temperature
            val moisture = sensor?.soilMoisture

            binding.tvTemperatureValue.text =
                if (temperature != null) String.format(Locale.US, "%.1f°C", temperature)
                else getString(R.string.value_empty)
            // 막대: 0~40°C 를 0~100% 로 (기준은 팀에서 정하면 바꾸기)
            binding.progressTemperature.progress =
                temperature?.let { (it / 40 * 100).toInt().coerceIn(0, 100) } ?: 0

            binding.tvSoilValue.text =
                if (moisture != null) "$moisture%" else getString(R.string.value_empty)
            binding.progressSoil.progress = moisture?.coerceIn(0, 100) ?: 0
        }
    }

    /** 선택된 작물의 적정 범위를 카드 아래 글자에 표시 (• 목표 온도 20~26°C 등) */
    private fun showCropRanges() {
        val standard = FarmSettings.currentStandard
        binding.tvTemperatureRange.text =
            getString(R.string.home_temperature_range_fmt, standard.tempMin, standard.tempMax)
        binding.tvSoilRange.text =
            getString(R.string.home_soil_range_fmt, standard.soilMin, standard.soilMax)
    }

    /** 카드를 누르면 nav_graph.xml 에 정의한 action 으로 화면 이동 */
    private fun setupCardClicks() {
        binding.cardTemperature.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_temperatureLog)
        }
        binding.cardSoil.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_soilLog)
        }
        binding.cardCropCheck.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_cropCheck)
        }
    }

    /** 작물 선택 드롭다운: 목록은 data/CropStandard.kt 의 CropStandards 에서 가져옴 */
    private fun setupCropDropdown() {
        val crops = CropStandards.list.map { it.name }
        val adapter = CropDropdownAdapter(requireContext(), crops)
        adapter.selectedPosition = FarmSettings.selectedCropIndex

        binding.dropdownCrop.setAdapter(adapter)
        // 두 번째 인자 false = 글자를 넣을 때 목록을 걸러내지 않음
        binding.dropdownCrop.setText(crops[FarmSettings.selectedCropIndex], false)

        binding.dropdownCrop.setOnItemClickListener { _, _, position, _ ->
            adapter.selectedPosition = position   // ✓ 표시 위치 변경
            FarmSettings.selectedCropIndex = position
            showCropRanges()                       // 카드의 범위 글자도 바로 바꾸기
            Toast.makeText(
                requireContext(),
                getString(R.string.crop_selected_toast, crops[position]),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
