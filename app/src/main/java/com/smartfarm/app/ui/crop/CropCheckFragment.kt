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

            // 비동기 통신 응답 후 View가 여전히 살아있는지 널 체크
            _binding?.let { safeBinding ->
                result.onSuccess { diagnoses ->
                    pestAdapter.submitList(diagnoses)
                    safeBinding.tvEmpty.isVisible = diagnoses.isEmpty()
                    safeBinding.tvLogCount.text = getString(R.string.pest_log_count, diagnoses.size)
                }.onFailure { error ->
                    safeBinding.tvEmpty.isVisible = true
                    safeBinding.tvLogCount.text = getString(R.string.pest_log_count, 0)
                    context?.let { ctx ->
                        Toast.makeText(ctx, "진단 이력 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 2. 내 작물 최근 사진 조회 (API-007-B)
            val imageResult = repository.getLatestCropImage()
            _binding?.let { safeBinding ->
                imageResult.onSuccess { cropImage ->
                    // 필요 시 이미지 로딩 처리
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}