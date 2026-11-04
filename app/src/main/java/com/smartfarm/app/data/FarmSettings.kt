package com.smartfarm.app.data

/**
 * 사용자가 고른 설정값을 앱 실행 중에 기억해 두는 곳.
 *
 * 지금은 앱을 끄면 처음 값(토마토)으로 돌아갑니다.
 * TODO: 앱을 껐다 켜도 유지하려면 SharedPreferences 에 저장
 */
object FarmSettings {
    /** 드롭다운에서 고른 작물 번호 (CropStandards.list 의 순서: 0 = 토마토, 1 = 오이 ...) */
    var selectedCropIndex: Int = 0

    /** 지금 선택된 작물의 적정 범위. 각 화면은 이걸 보고 배지·범위 글자·그래프 띠를 정합니다 */
    val currentStandard: CropStandard
        get() = CropStandards.list[selectedCropIndex]
}
