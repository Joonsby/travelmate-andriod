# travelmate-android

여행짜자(TravelMate)의 Android 앱입니다. Kotlin과 Jetpack Compose로 만듭니다.
백엔드는 iOS 앱과 같은 `travelmate-api` 를 그대로 씁니다.

- 계획과 결정 사항: [docs/ANDROID_PLAN.md](docs/ANDROID_PLAN.md)
- Phase 0 실험 결과: [docs/PHASE0.md](docs/PHASE0.md)
- Phase 1 진행 기록: [docs/PHASE1.md](docs/PHASE1.md)

## 구성

| 모듈 | 내용 |
|---|---|
| `:app` | 본 앱 (`com.joonsby.travelmate`). 시작 화면, 로그인 상태별 화면, 탭 껍데기 |
| `:core:model` | 서버 JSON 모델 |
| `:core:network` | Retrofit·OkHttp, 오류, 토큰 갱신, 오프라인 캐시 |
| `:core:data` | 토큰 저장, 로그인 상태, 실시간 연결, 설정값 |
| `:core:designsystem` | 디자인 토큰, Pretendard, 공통 컴포넌트 |
| `:feature:auth` | 로그인 화면, 심사용 로그인 |
| `:spike` | Phase 0 실험 앱 (`com.joonsby.travelmate.spike`). 스티커, 토스 링크, LiveKit 실험이 들어 있습니다. 결론이 나면 지웁니다 |

## 빌드

- 필요한 것: JDK 21, Android SDK 37
- `local.properties` 에 `sdk.dir` 를 적어 둡니다

```
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest       # 단위 테스트
./gradlew :app:installDebug -Ptravelmate.apiBaseUrl=http://10.0.2.2:3000/api   # 로컬 서버로 시험 (기본은 운영 서버)
./gradlew :spike:installDebug    # 실험 앱을 연결된 기기에 설치
```

## 글꼴

Pretendard (SIL Open Font License 1.1). 라이선스 원문은 `core/designsystem/src/main/assets/licenses/Pretendard-OFL.txt`.
