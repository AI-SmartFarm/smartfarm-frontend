package com.smartfarm.app.data

/**
 * 사용자가 고른 설정값을 앱 실행 중에 기억해 두는 곳.
 * (작물 목록 자체는 res/values/strings.xml 의 crop_list 에 있음)
 *
 * 지금은 앱을 끄면 처음 값(토마토)으로 돌아갑니다.
 * TODO: 앱을 껐다 켜도 유지하려면 SharedPreferences 에 저장
 */
object FarmSettings {
    var selectedCropIndex: Int = 0   // 0 = 토마토, 1 = 오이, 2 = 고추, 3 = 상추, 4 = 딸기
}
