# Phase 1 진행 기록

최종 갱신 2026-11-25. 계획은 [ANDROID_PLAN.md](ANDROID_PLAN.md) 의 8장 Phase 1 을 따릅니다.

## 1. 모듈

| 모듈 | 내용 |
|---|---|
| `:core:model` | 서버 JSON 과 1:1 인 모델. 지금은 인증(`AuthTokens`·`AuthSession`·`AuthUser`)과 실시간 패킷만 |
| `:core:network` | Retrofit·OkHttp 조립, 오류(`ApiError`), 토큰 붙이기와 401 갱신(`TokenRefresher` Mutex), 오프라인 캐시, `ApiConfig`(상대 경로 → 절대 주소) |
| `:core:data` | 토큰 저장(Keystore AES-GCM, `noBackupFilesDir`), 로그인 상태(`SessionManager`), 실시간 연결(`ChatSocket`), 여행 변경 신호(`TripChangeBus`), 설정값(DataStore `tm.appearance`), Hilt 모듈 |
| `:core:designsystem` | iOS `Tokens.swift` 의 색·간격·모서리, Pretendard 4굵기, TM 버튼·카드·입력칸·배너·빈 화면·칩·아바타, 노을 하늘 배경, 하단 탭(가운데 +), 오프라인 안내, 토스트 |
| `:feature:auth` | 로그인 화면(카카오·Google), 심사용 로그인 시트, 소셜 SDK 연결 |
| `:app` | 진입점, 시작 화면, 상태별 최상위 화면, 탭 껍데기(탭 내용은 Phase 2 부터) |

iOS 와 같게 맞춘 것:
- 로그인·복원·만료 문구, 401 → 갱신 1회 → 실패 시 로그아웃, 앱 시작 때 연결 오류는 로그아웃하지 않고 「복원 실패」
- 로그인·로그아웃 때 오프라인 캐시 비우기, 계정이 바뀌는 동안 끝난 요청이 새 상태를 덮어쓰지 않기
- 실시간 연결: 인증 패킷 → ready, 1초부터 두 배씩 최대 30초 재연결, 내가 바꾼 `tripChanged` 는 무시
- 표지 그라데이션 색 고르는 공식 (iOS 와 같은 결과인지 테스트로 확인)

iOS 보다 나아진 것:
- 실시간 연결이 토큰 만료(4001)로 끊기면 토큰을 갱신한 뒤 다시 잇습니다. iOS 는 다른 요청이 갱신해 줄 때까지 같은 토큰으로 재시도합니다.

## 2. 확인한 것

| 항목 | 결과 |
|---|---|
| 단위 테스트 | 16개 통과 (network 7 · data 8 · designsystem 1). 동시 401 → 갱신 1회, 갱신 401 → 로그아웃, 연결 오류 → 복원 실패, 503 → 저장된 응답, 소켓 인증·ready·여행 변경 전달 |
| 빌드 | `assembleDebug`, `assembleRelease`(난독화·리소스 축소), `lintDebug` 통과. 릴리스 APK 약 10.9MB |
| 에뮬레이터 (API 37) | 시작 화면 → 로그인 화면 ([캡처](phase1/login-api37.png)), 앱 마크 5번 → 심사용 로그인 시트 ([캡처](phase1/review-login-api37.png)) |
| Google 버튼 | Credential Manager 가 Google 계정 화면을 띄움. 취소하면 오류 없이 돌아옴 |
| 카카오 버튼 | 카카오톡이 없어 카카오계정 웹 로그인(Chrome)을 띄움. 취소하면 오류 없이 돌아옴 |

## 3. 아직 확인하지 못한 것

- **실제 계정으로 로그인 성공** — 에뮬레이터에 Google 계정·카카오 계정이 없습니다. 디버그 키 해시와 SHA-1 은 등록해 두었으니 실기기에서 확인합니다
- 로그인 후 탭 화면, 로그아웃, 복원 실패 화면, 다크 모드 — 로그인을 해야 볼 수 있는 화면이라 같은 때 확인합니다
- 심사용 로그인 — 심사 계정 정보가 필요합니다

## 4. 다음 단계로 넘긴 것

- DTO 전체: 인증·내 정보만 만들었고, 나머지는 각 Phase 에서 그 화면과 함께 만듭니다
- 시트·스와이프 행·당겨서 새로고침: 처음 쓰는 화면(Phase 2 여행 목록)에서 만듭니다
- 프로필 사진(Coil), 아이콘 대응표 문서
- 런처 아이콘의 단색(테마 아이콘) 버전

## 5. 발견한 것 — 푸시가 APNs 로 들어가 있음

iOS 와 서버에 푸시가 이미 들어가 있는데 **APNs 직접 발송**이고, 서버가 기기 토큰을 16진수로만 받아 FCM 토큰은 등록되지 않습니다.
→ **2026-11-25 A안으로 재결정**: iOS 는 APNs 그대로, 서버에 Android 용 FCM 발송 경로만 추가합니다. ANDROID_PLAN.md 7장 D2-1, 9장.
