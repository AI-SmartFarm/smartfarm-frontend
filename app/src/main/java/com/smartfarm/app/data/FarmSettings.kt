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
 *
 * 작물은 "번호"가 아니라 "이름"으로 저장합니다.
 * → 서버에서 받은 작물 목록의 순서가 앱 기본값과 달라도 고른 작물이 그대로 유지됨.
 */
object FarmSettings {

    private const val PREFS_NAME = "farm_settings"
    private const val KEY_CROP_NAME = "selected_crop_name"

    private var prefs: SharedPreferences? = null
    private var memoryCropName: String? = null

    /** 휴대폰 저장소 연결. 처음 한 번만 실제로 연결되고, 그다음부터는 아무것도 안 함 */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * 드롭다운에서 고른 작물 번호 (지금 CropStandards.list 안에서의 순서)
     * 값을 넣으면 그 작물의 "이름"이 휴대폰에 저장됨.
     */
    var selectedCropIndex: Int
        get() {
            val savedName = prefs?.getString(KEY_CROP_NAME, memoryCropName) ?: memoryCropName
            val index = CropStandards.list.indexOfFirst { it.name == savedName }
            // 저장된 작물이 목록에 없으면(처음 실행, 서버 목록에서 빠짐) 첫 작물
            return if (index >= 0) index else 0
        }
        set(value) {
            val name = CropStandards.list.getOrNull(value)?.name ?: return
            memoryCropName = name
            prefs?.edit { putString(KEY_CROP_NAME, name) }
        }

    /** 지금 선택된 작물의 적정 범위. 각 화면은 이걸 보고 배지·범위 글자·그래프 띠를 정합니다 */
    val currentStandard: CropStandard
        get() = CropStandards.list[selectedCropIndex]
}
