package com.smartfarm.app.ui.home

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.smartfarm.app.R

/**
 * 작물 드롭다운 목록을 그려주는 Adapter.
 * 한 줄의 모양은 item_crop_dropdown.xml 이고,
 * 선택된 작물은 초록 글자 + ✓, 나머지는 검정 글자로 표시합니다.
 */
class CropDropdownAdapter(
    context: Context,
    private val crops: List<String>
) : ArrayAdapter<String>(context, R.layout.item_crop_dropdown, R.id.tvCropName, crops) {

    var selectedPosition = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) // 작물 이름은 여기서 자동으로 들어감
        val isSelected = position == selectedPosition

        view.findViewById<ImageView>(R.id.ivCheck).isVisible = isSelected
        view.findViewById<TextView>(R.id.tvCropName).setTextColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.green_primary else R.color.text_primary
            )
        )
        return view
    }

    /**
     * 기본 드롭다운은 입력한 글자로 목록을 걸러냄 (예: "토마토"가 선택돼 있으면 토마토만 보임).
     * 우리는 검색이 필요 없으니 항상 전체 목록을 보여주도록 바꿈.
     */
    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) =
            FilterResults().apply {
                values = crops
                count = crops.size
            }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) =
            notifyDataSetChanged()
    }
}
