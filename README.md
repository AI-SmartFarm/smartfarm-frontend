# My Farm (SmartFarm 모바일 앱)

AI 기반 개방형 스마트팜의 **모니터링·알림 앱**입니다. 백엔드가 센서값과 AI 병해충 진단을 바탕으로 환경을 자동 제어하고, 앱은 그 상태와 결과를 사용자에게 보여줍니다.

- Android · Kotlin · XML(View) · Navigation · ViewBinding
- Retrofit + Gson + OkHttp · Coroutine
- Firebase Cloud Messaging(FCM) · Coil(사진)

## 화면

```
대시보드 (My Farm)
 ├─ 현재온도 카드        → 온도 로그       (현재 값·적정/주의 배지 / 24시간 그래프 / 순환팬 제어 기록)
 ├─ 토양습도 카드        → 토양 습도 로그  (현재 값·적정/주의 배지 / 24시간 그래프 / 급수 제어 기록)
 ├─ 내 작물 확인하기 카드 → 내 작물 확인    (최근 작물 사진 / 병해충 진단 이력)
 └─ 작물 선택하기        : 5종 드롭다운 → 작물별 적정 범위로 상태 바·범위 글자 갱신

병해충 경고 팝업 : 새 감염 진단이 있으면 어느 화면에서든 표시
                 [확인 및 조치 내역 보기] → 내 작물 확인 / [닫기]
```

모든 화면은 상단 ↻ 버튼으로 새로고침합니다.

## 코드 구조

| 위치 | 역할 |
|---|---|
| `MainActivity.kt` | 화면 틀(Toolbar·Navigation), FCM 토큰 등록, 알림 권한, 새 감염 진단 확인 → 경고 팝업 |
| `ui/home` | 대시보드 (`HomeFragment`, 작물 드롭다운 `CropDropdownAdapter`) |
| `ui/temperature`, `ui/soil` | 온도 로그 / 토양 수분 로그 |
| `ui/crop` | 내 작물 확인 (`CropCheckFragment`, 진단 이력 `PestLogAdapter`) |
| `ui/warning` | 병해충 경고 팝업 (`WarningDialogFragment`) |
| `ui/common` | 그래프(`SimpleLineChartView`, Canvas 직접 구현), 활동로그 Adapter, 공통 도우미 |
| `data/RealFarmRepository.kt` | 센서·작물 API 호출 → 화면 모델로 변환 (5초 제한, 실패 시 null) |
| `data/PestRepository.kt` | 병해충 진단·사진·FCM 토큰 API 호출 |
| `data/PestAlertChecker.kt` | 새 감염 진단 확인 (마지막 확인 번호 저장, 중복 팝업 방지) |
| `data/CropStandard.kt`, `FarmSettings.kt` | 작물별 적정 범위(앱 기본값 + 서버 값), 선택한 작물 저장 |
| `data/api`, `data/dto`, `data/network` | Retrofit 인터페이스, 서버 응답 모양, 서버 주소 설정 |
| `service` | FCM 수신(`MyFirebaseMessagingService`), 팝업/휴대폰 알림 분기(`PestAlertNotifier`) |

## 사용하는 백엔드 API

| API | 용도 |
|---|---|
| `GET /api/v1/farms/{farmId}/status` (002-A) | 현재 온도·토양 수분, 농장 작물의 적정 범위 |
| `GET /api/v1/farms/{farmId}/telemetry?range=24h` (002-B) | 24시간 그래프 (30분 평균) |
| `GET /api/v1/farms/{farmId}/control-logs?type=TEMP\|SOIL` (007-A) | 자동 제어 기록 |
| `GET /api/v1/crops/{species}` | 작물별 적정 온도·토양 수분 |
| `GET /api/v1/farms/{farmId}/diagnoses` (008) | 병해충 진단 이력 |
| `GET /api/v1/farms/{farmId}/images/latest` (007-B), `GET /api/v1/images/{id}/file` | 최근 작물 사진 |
| `POST /api/v1/fcm/token` (010) | 푸시 알림 토큰 등록 |
| `PUT /api/v1/farms/{farmId}/crop` | 작물 변경 (서버가 지원하면 자동 제어 기준도 변경) |

## 실행

1. 서버 주소: `data/network/ApiClient.kt` 의 `BASE_URL` 한 줄 (끝에 `/` 필수)
2. Firebase: `app/google-services.json` 포함 (프로젝트 GrowCare)
3. Android Studio 에서 Run. Android 13 이상은 처음 실행 시 알림 허용

## 로그 확인 (Logcat 검색어)

| 검색어 | 내용 |
|---|---|
| `SmartFarmAPI` | 센서·작물 API 결과와 실패 이유 |
| `okhttp` | 요청·응답 JSON 전체 |
| `PestAlert` | 새 감염 진단 확인 과정 |
| `FCM` | 토큰 등록, 푸시 수신 |
