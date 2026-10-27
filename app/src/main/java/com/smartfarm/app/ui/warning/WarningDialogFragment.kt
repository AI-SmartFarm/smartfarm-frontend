package com.smartfarm.app.ui.warning

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.smartfarm.app.R
import com.smartfarm.app.data.PestAlert
import com.smartfarm.app.databinding.DialogWarningBinding

/**
 * 병해충 발생 알림 팝업.
 * nav_graph.xml 에 <dialog> 로 등록되어 있어서 Navigation 으로 띄웁니다.
 *
 * 띄우는 방법 (어느 화면에서든):
 *   (requireActivity() as MainActivity).showPestAlert(PestAlert("흰가루병", "순환팬", 60))
 */
class WarningDialogFragment : DialogFragment() {

    private var _binding: DialogWarningBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogWarningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // nav_graph.xml 의 <argument> 로 넘어온 값 꺼내기
        val args = requireArguments()
        val pestName = args.getString(ARG_PEST_NAME) ?: ""
        val deviceName = args.getString(ARG_DEVICE_NAME) ?: ""
        val timerMinutes = args.getInt(ARG_TIMER_MINUTES)

        binding.tvAlertTitle.text = getString(R.string.alert_title, pestName)
        binding.tvAlertDevice.text = getString(R.string.alert_message_line2, deviceName)
        binding.tvActionTitle.text = getString(R.string.alert_action_title, deviceName)
        binding.tvActionTimer.text = getString(R.string.alert_action_timer, timerMinutes)
        binding.tvActionTimer.isVisible = timerMinutes > 0

        // [확인 및 조치 내역 보기] → 팝업 닫고 "내 작물 확인" 화면으로
        binding.btnConfirm.setOnClickListener {
            findNavController().navigate(R.id.action_warningDialog_to_cropCheck)
        }
        // [닫기] → 팝업만 닫기
        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        // 기본 흰 사각형 배경을 없애고(둥근 어두운 카드만 보이게), 화면 폭의 88%로 표시
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.88).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // nav_graph.xml 의 <argument android:name="..."> 와 반드시 같은 이름
        private const val ARG_PEST_NAME = "pestName"
        private const val ARG_DEVICE_NAME = "deviceName"
        private const val ARG_TIMER_MINUTES = "timerMinutes"

        fun createArgs(alert: PestAlert): Bundle = bundleOf(
            ARG_PEST_NAME to alert.pestName,
            ARG_DEVICE_NAME to alert.deviceName,
            ARG_TIMER_MINUTES to alert.timerMinutes
        )
    }
}
