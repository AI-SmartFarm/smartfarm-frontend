package com.smartfarm.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.smartfarm.app.R
import com.smartfarm.app.data.CropStandards
import com.smartfarm.app.data.FarmSettings
import com.smartfarm.app.data.RealFarmRepository
import com.smartfarm.app.data.network.ApiClient
import com.smartfarm.app.databinding.FragmentHomeBinding
import com.smartfarm.app.ui.common.addRefreshMenu
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 대시보드 (My Farm)
 *  1. 현재 온도·토양 수분 (API-002-A) + 작물 목표 범위 기준 상태 바 + ↻ 새로고침
 *  2. 작물 선택 드롭다운 → 고른 작물의 서버 기준값으로 범위 글자·상태 바를 다시 그림
 *  3. 카드 클릭 → 온도 로그 / 토양 수분 로그 / 내 작물 확인 화면으로 이동
 * 화면 모양은 fragment_home.xml 에 있고, 여기서는 "동작"만 연결합니다.
 */
class HomeFragment : Fragment() {

    // ViewBinding 기본 패턴: 화면이 사라질 때 null 로 만들어 메모리 누수 방지
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val repository = RealFarmRepository(ApiClient.farmApiService)

    // 마지막으로 받은 센서값 (작물을 바꾸면 이 값으로 바를 다시 그림)
    private var lastTemperature: Double? = null
    private var lastMoisture: Int? = null

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

        // 상단바 오른쪽 ↻ 새로고침
        addRefreshMenu {
            loadSensor()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadSensor()
    }

    /** XML 미리보기용 값(24°C, 32%)을 지우고 서버 값을 받기 전까지 "-" 로 비워둠 */
    private fun showSensorPlaceholder() {
        lastTemperature = null
        lastMoisture = null
        binding.tvTemperatureValue.text = getString(R.string.value_empty)
        binding.tvSoilValue.text = getString(R.string.value_empty)
        showBars()   // 값이 없으므로 빈 바
    }

    /**
     * 현재 온도·토양 수분(API-002-A)과 고른 작물의 기준값을 서버에서 받아 카드에 표시.
     * 대시보드에 돌아올 때마다 다시 불러오고, 실패하면 "-" 를 표시합니다.
     */
    private fun loadSensor() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 작물 기준값과 센서값을 동시에 요청 (하나씩 기다리지 않음)
            val cropsJob = async { repository.refreshCropStandards() }
            val sensorJob = async { repository.getCurrentSensor() }
            val cropsUpdated = cropsJob.await()
            val sensor = sensorJob.await()

            // 서버 기준값(작물 API 또는 상태 API)이 들어왔으면 드롭다운·범위 글자를 다시 그림
            if (cropsUpdated || sensor != null) {
                setupCropDropdown()
                showCropRanges()
            }

            lastTemperature = sensor?.temperature
            lastMoisture = sensor?.soilMoisture

            binding.tvTemperatureValue.text =
                lastTemperature?.let { String.format(Locale.US, "%.1f°C", it) }
                    ?: getString(R.string.value_empty)
            binding.tvSoilValue.text =
                lastMoisture?.let { "$it%" } ?: getString(R.string.value_empty)
            showBars()
        }
    }

    /**
     * 온도·토양 수분 바를 "선택한 작물의 목표 범위" 기준으로 그림.
     *  - 목표 범위가 바의 가운데 1/3 에 오도록 눈금을 잡음
     *      예) 고추 18~28°C → 바 전체 = 8~38°C  (18°C ≈ 33%, 28°C ≈ 67%)
     *  - 눈금보다 낮으면(영하 포함) 0%, 높으면 100%
     *  - 목표 범위 안이면 원래 색(온도 초록 / 수분 파랑), 벗어나면 빨강
     */
    private fun showBars() {
        val standard = FarmSettings.currentStandard

        val temperature = lastTemperature
        binding.progressTemperature.showLevel(
            progress = temperature?.let { rangeProgress(it, standard.tempMin, standard.tempMax) } ?: 0,
            isNormal = temperature == null || standard.isTemperatureNormal(temperature),
            normalColor = R.color.green_primary,
            normalTrack = R.color.green_light
        )

        val moisture = lastMoisture
        binding.progressSoil.showLevel(
            progress = moisture?.let { rangeProgress(it.toDouble(), standard.soilMin, standard.soilMax) } ?: 0,
            isNormal = moisture == null || standard.isSoilNormal(moisture),
            normalColor = R.color.blue_primary,
            normalTrack = R.color.blue_light
        )
    }

    /** 값 → 바 길이(0~100). 눈금 = (최소 - 범위폭) ~ (최대 + 범위폭) */
    private fun rangeProgress(value: Double, min: Int, max: Int): Int {
        val span = (max - min).coerceAtLeast(1)
        val low = min - span
        val high = max + span
        return ((value - low) / (high - low) * 100).roundToInt().coerceIn(0, 100)
    }

    /** 바 길이와 색 바꾸기 (범위 밖이면 빨강) */
    private fun LinearProgressIndicator.showLevel(
        progress: Int,
        isNormal: Boolean,
        @ColorRes normalColor: Int,
        @ColorRes normalTrack: Int
    ) {
        val ctx = requireContext()
        setIndicatorColor(ContextCompat.getColor(ctx, if (isNormal) normalColor else R.color.red_primary))
        trackColor = ContextCompat.getColor(ctx, if (isNormal) normalTrack else R.color.red_light)
        this.progress = progress
    }

    /** 선택된 작물의 적정 범위를 카드 아래 글자에 표시 (• 목표 온도 17~27°C 등) */
    private fun showCropRanges() {
        val standard = FarmSettings.currentStandard
        binding.tvTemperatureRange.text =
            getString(R.string.home_temperature_range_fmt, standard.tempMin, standard.tempMax)
        binding.tvSoilRange.text =
            getString(R.string.home_soil_range_fmt, standard.soilMin, standard.soilMax)
        showBars()   // 작물이 바뀌면 바 기준도 바뀜
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

    /** 작물 선택 드롭다운. 목록은 CropStandards(data/CropStandard.kt)에서 가져옴 */
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
            showCropRanges()                       // 범위 글자를 바로 바꿈 (먼저 앱 값)
            viewLifecycleOwner.lifecycleScope.launch {
                // ① 서버에 작물 변경 요청 → 성공하면 서버 자동 제어 기준도 그 작물로 바뀜
                val species = FarmSettings.currentStandard.species
                if (species.isNotBlank() && repository.changeFarmCrop(species)) {
                    loadSensor()
                }
                // ② 그 작물의 서버 기준값을 받아 다시 그림 (GET /api/v1/crops/{species})
                if (repository.refreshCropStandards()) showCropRanges()
            }
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
