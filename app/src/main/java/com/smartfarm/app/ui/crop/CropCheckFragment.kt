package com.smartfarm.app.ui.crop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.smartfarm.app.R
import com.smartfarm.app.data.FakeFarmRepository
import com.smartfarm.app.databinding.FragmentCropCheckBinding

/**
 * 내 작물 확인 화면 (대시보드 "내 작물 확인하기" 카드, 또는 경고 팝업의 [확인]에서 이동)
 *  - 작물 사진 (지금은 카메라 아이콘 자리표시)
 *  - [다시 촬영] 버튼
 *  - 병해충 로그 목록 + 건수
 * 화면 모양은 fragment_crop_check.xml, 여기서는 데이터만 채웁니다.
 */
class CropCheckFragment : Fragment() {

    private var _binding: FragmentCropCheckBinding? = null
    private val binding get() = _binding!!

    private val repository = FakeFarmRepository()
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
        val logs = repository.getPestLogs()
        pestAdapter.submitList(logs)
        binding.tvEmpty.isVisible = logs.isEmpty()
        binding.tvLogCount.text = getString(R.string.pest_log_count, logs.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLogs.adapter = null
        _binding = null
    }
}
