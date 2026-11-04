package com.smartfarm.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 사용자가 고른 설정값을 휴대폰에 저장해 두는 곳 (앱을 껐다 켜도 유지).
 *
 * 사용 전에 화면에서 한 번 init(context) 를 불러야 저장소에 연결됩니다.
 * (대시보드·온도·토양 화면의 onViewCreated 에서 부름. 여러 번 불러도 괜찮음)
 * init 전에는 메모리에만 기억합니다.
 */
object FarmSettings {

    private const val PREFS_NAME = "farm_settings"
    private const val KEY_CROP_INDEX = "selected_crop_index"

    private var prefs: SharedPreferences? = null
    private var memoryCropIndex = 0

    /** 휴대폰 저장소 연결. 처음 한 번만 실제로 연결되고, 그다음부터는 아무것도 안 함 */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * 드롭다운에서 고른 작물 번호 (CropStandards.list 의 순서: 0 = 토마토, 1 = 오이 ...)
     * 값을 넣으면 휴대폰에 바로 저장됨.
     */
    var selectedCropIndex: Int
        get() {
            val saved = prefs?.getInt(KEY_CROP_INDEX, memoryCropIndex) ?: memoryCropIndex
            // CropStandard.kt 에서 작물을 지워서 번호가 범위를 벗어나면 첫 작물로
            return saved.coerceIn(0, CropStandards.list.lastIndex)
        }
        set(value) {
            memoryCropIndex = value
            prefs?.edit { putInt(KEY_CROP_INDEX, value) }
        }

    /** 지금 선택된 작물의 적정 범위. 각 화면은 이걸 보고 배지·범위 글자·그래프 띠를 정합니다 */
    val currentStandard: CropStandard
        get() = CropStandards.list[selectedCropIndex]
}
