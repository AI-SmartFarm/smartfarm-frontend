# SmartFarmApp (Android / Kotlin + XML)

## 화면 흐름
```
대시보드 (My Farm)
 ├─ 현재온도 카드        → 온도 로그       (현재 값·배지 / 온도 그래프 / 활동로그 / ↻ 새로고침)
 ├─ 토양수분 카드        → 토양 수분 로그  (현재 값·배지 / 습도 그래프 / 활동로그 / ↻ 새로고침)
 ├─ 내 작물 확인하기 카드 → 내 작물 확인    (작물 사진 / 다시 촬영 / 병해충 로그)
 └─ 작물 선택하기        : 드롭다운

병해충 발생 알림 팝업
 - 테스트: 상단바 "My Farm" 글자를 길게 누르기
 - [확인 및 조치 내역 보기] → 내 작물 확인 / [닫기] → 팝업만 닫힘
```

## 파일 지도
| 화면 | XML | 코드 |
|---|---|---|
| 대시보드 | `fragment_home.xml` | `ui/home/HomeFragment.kt` |
| 온도 로그 | `fragment_temperature_log.xml` | `ui/temperature/TemperatureLogFragment.kt` |
| 토양 수분 로그 | `fragment_soil_log.xml` | `ui/soil/SoilLogFragment.kt` |
| 내 작물 확인 | `fragment_crop_check.xml` | `ui/crop/CropCheckFragment.kt` |
| 병해충 알림 팝업 | `dialog_warning.xml` | `ui/warning/WarningDialogFragment.kt` |
| 활동로그 한 줄 | `item_activity_log.xml` | `ui/common/ActivityLogAdapter.kt` |
| 병해충 로그 한 줄 | `item_pest_log.xml` | `ui/crop/PestLogAdapter.kt` |
| 그래프 | (속성: `values/attrs.xml`) | `ui/common/SimpleLineChartView.kt` |
| 화면 이동 | `navigation/nav_graph.xml` | `MainActivity.kt` |
| 데이터 | | `data/FakeFarmRepository.kt` |

## API 연결할 때 
`FakeFarmRepository`와 **같은 함수 이름**으로 실제 API를 부르는 Repository를 만들고,
각 Fragment의 `private val repository = FakeFarmRepository()` 한 줄만 바꾸면 됩니다.
- 새로고침 / Polling: 각 화면의 `loadData()`를 다시 부르면 화면이 갱신됨
- FCM 알림: 메시지를 받으면 `MainActivity.showPestAlert(PestAlert(병해충, 장치, 분))` 호출
