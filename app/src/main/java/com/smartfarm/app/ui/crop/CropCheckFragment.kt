package com.smartfarm.app.ui.crop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.smartfarm.app.R
import com.smartfarm.app.data.FarmSettings
import com.smartfarm.app.data.PestRepository
import com.smartfarm.app.data.dto.isForCrop
import com.smartfarm.app.data.dto.isRealInfection
import com.smartfarm.app.data.network.ApiClient
import com.smartfarm.app.databinding.FragmentCropCheckBinding
import com.smartfarm.app.ui.common.addRefreshMenu
import kotlinx.coroutines.launch

/**
 * 내 작물 확인 화면 (대시보드 "내 작물 확인하기" 카드 또는 병해충 팝업에서 이동)
 *  - 작물 사진: 최근 작물 사진(API-007-B), 없으면 가장 최근 진단 사진
 *  - 병해충 로그: AI 진단 이력(API-008) 중 지금 고른 작물의 감염 진단만 최신순으로 표시
 *  - 상단바 ↻ 새로고침으로 다시 불러오기
 */
class CropCheckFragment : Fragment() {

    private var _binding: FragmentCropCheckBinding? = null
    private val binding get() = _binding!!

    private val repository = PestRepository()
    private val pestAdapter = PestLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCropCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FarmSettings.init(requireContext())   // 대시보드에서 고른 작물 불러오기
        binding.recyclerLogs.adapter = pestAdapter

        // 상단바 오른쪽 ↻ 새로고침
        addRefreshMenu {
            loadData()
            Toast.makeText(requireContext(), R.string.refreshed_toast, Toast.LENGTH_SHORT).show()
        }
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. 병해충 진단 이력 (API-008)
            val result = repository.getDiagnoses()
            var latestDiagnosisImage: String? = null   // 사진 API 가 실패하면 대신 쓸 최근 진단 사진

            // 응답을 기다리는 동안 화면이 사라졌을 수 있으니 _binding 으로 확인
            _binding?.let { safeBinding ->
                result.onSuccess { diagnoses ->
                    // 지금 고른 작물의 진단만 (다른 작물 진단은 제외)
                    val selectedSpecies = FarmSettings.currentStandard.species
                    val cropDiagnoses = diagnoses
                        .filter { it.isForCrop(selectedSpecies) }
                        .sortedByDescending { it.diagnosisId }

                    // 병해충 로그는 감염된 진단만 표시 (정상 진단은 이력에 넣지 않음)
                    val infectedLogs = cropDiagnoses.filter { it.isRealInfection() }
                    pestAdapter.submitList(infectedLogs)
                    safeBinding.tvEmpty.isVisible = infectedLogs.isEmpty()
                    latestDiagnosisImage = cropDiagnoses
                        .firstOrNull { !it.imageUrl.isNullOrBlank() }
                        ?.imageUrl
                }.onFailure { error ->
                    pestAdapter.submitList(emptyList())
                    safeBinding.tvEmpty.isVisible = true
                    context?.let { ctx ->
                        Toast.makeText(
                            ctx,
                            getString(R.string.pest_log_load_failed, error.message.orEmpty()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // 2. 최근 작물 사진 (API-007-B). 고른 작물 사진이 아니거나 실패하면 그 작물의 최근 진단 사진으로 대신
            val selectedSpecies = FarmSettings.currentStandard.species
            val photoPath = repository.getLatestCropImage().getOrNull()
                ?.takeIf { selectedSpecies.isBlank() || it.species.equals(selectedSpecies, ignoreCase = true) }
                ?.imageUrl
                ?: latestDiagnosisImage
            showCropPhoto(photoPath)
        }
    }

    /** 서버 사진 경로("/api/v1/images/34/file")를 받아 작물 사진 칸에 표시. 없으면 카메라 아이콘 유지 */
    private fun showCropPhoto(path: String?) {
        val safeBinding = _binding ?: return
        if (path.isNullOrBlank()) return

        safeBinding.ivCropPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        safeBinding.ivCropPhoto.load(ApiClient.fullUrl(path)) {
            crossfade(true)
            error(R.drawable.ic_camera)   // 사진을 못 받으면 카메라 아이콘
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
