package com.smartfarm.app.ui.crop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartfarm.app.R
import com.smartfarm.app.data.PestRepository
import com.smartfarm.app.databinding.FragmentCropCheckBinding
import kotlinx.coroutines.launch
import android.widget.ImageView
import coil.load
import com.smartfarm.app.data.network.ApiClient

/**
 * 내 작물 확인 화면 (병해충 이력 및 작물 사진)
 */
class CropCheckFragment : Fragment() {

    private var _binding: FragmentCropCheckBinding? = null
    // 안전한 binding 접근을 위해 get()에 널 체크 또는 _binding 사용
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
        binding.recyclerLogs.adapter = pestAdapter

        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. 병해충 진단 이력 조회 (API-008)
            val result = repository.getDiagnoses()
            // 가장 최근 진단 사진 경로 (007-B가 없을 때 대신 사용)
            var latestDiagnosisImage: String? = null

            // 비동기 통신 응답 후 View가 여전히 살아있는지 널 체크
            _binding?.let { safeBinding ->
                result.onSuccess { diagnoses ->
                    pestAdapter.submitList(diagnoses)
                    safeBinding.tvEmpty.isVisible = diagnoses.isEmpty()
                    latestDiagnosisImage = diagnoses
                        .firstOrNull { !it.imageUrl.isNullOrBlank() }
                        ?.imageUrl

                }.onFailure { error ->
                    safeBinding.tvEmpty.isVisible = true
                    context?.let { ctx ->
                        Toast.makeText(ctx, "진단 이력 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 2. 내 작물 최근 사진 조회 (API-007-B)
            //    → 성공하면 그 사진, 실패(서버 미구현 404 등)하면 최근 진단 사진
            val imageResult = repository.getLatestCropImage()
            val photoPath = imageResult.getOrNull()?.imageUrl ?: latestDiagnosisImage
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